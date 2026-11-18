// apk2aab converts an Android APK, or a split-APK bundle (.xapk / .apks /
// .apkm), into an Android App Bundle (.aab).
//
// Pipeline (same idea as github.com/iricartb/go-apk2aab-tool, rewritten to be
// cross-platform and to understand split bundles):
//
//  1. (bundles only) unpack, pick the base APK, collect native libs from the
//     ABI config splits, skip density/language/feature splits.
//  2. apktool d -s       decode resources + manifest, keep dex as-is
//  3. sanitize manifest   drop Play split-delivery markers that are invalid
//     in an uploaded bundle (requiredSplitTypes, vending.splits, stamp...)
//  4. aapt2 compile/link  build proto-format resources
//  5. lay out the base module (manifest/ dex/ res/ assets/ lib/ root/)
//  6. bundletool build-bundle (keeps the original APK's uncompressed assets)
package main

import (
	"archive/zip"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"runtime"
	"sort"
	"strings"
)

type tools struct {
	java, apktool, bundletool, aapt2, androidJar string
}

type split struct {
	path    string
	name    string // value of manifest split="", empty for base
	feature bool
	hasLib  bool
}

var verbose bool

func main() {
	var (
		out      = flag.String("o", "", "output .aab path (default: input name with .aab)")
		toolsDir = flag.String("tools", defaultToolsDir(), "directory holding apktool.jar, bundletool.jar, aapt2, android.jar")
		t        tools
		keepTmp  = flag.Bool("keep-temp", false, "keep the working directory")
		vcode    = flag.String("version-code", "", "override versionCode")
		vname    = flag.String("version-name", "", "override versionName")
	)
	flag.StringVar(&t.java, "java", "", "java binary (default: $JAVA_HOME/bin/java or java on PATH)")
	flag.StringVar(&t.apktool, "apktool", "", "apktool.jar path")
	flag.StringVar(&t.bundletool, "bundletool", "", "bundletool.jar path")
	flag.StringVar(&t.aapt2, "aapt2", "", "aapt2 binary path (must understand the target SDK's android.jar)")
	flag.StringVar(&t.androidJar, "android-jar", "", "android.jar of the target SDK (or any jar with framework resources.arsc)")
	flag.BoolVar(&verbose, "v", false, "show tool output")
	flag.Usage = func() {
		fmt.Fprintf(os.Stderr, "usage: apk2aab [flags] app.apk|app.xapk|app.apks\n\n")
		flag.PrintDefaults()
	}
	flag.Parse()
	if flag.NArg() != 1 {
		flag.Usage()
		os.Exit(2)
	}
	in := flag.Arg(0)
	if *out == "" {
		*out = strings.TrimSuffix(in, filepath.Ext(in)) + ".aab"
	}
	if err := run(in, *out, *toolsDir, t, *keepTmp, *vcode, *vname); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}

func defaultToolsDir() string {
	if exe, err := os.Executable(); err == nil {
		d := filepath.Join(filepath.Dir(exe), "tools")
		if st, err := os.Stat(d); err == nil && st.IsDir() {
			return d
		}
	}
	return "tools"
}

func step(format string, a ...any) { fmt.Printf("==> "+format+"\n", a...) }
func warn(format string, a ...any) { fmt.Printf("    warning: "+format+"\n", a...) }

func run(in, out, toolsDir string, t tools, keepTmp bool, vcode, vname string) error {
	if _, err := os.Stat(in); err != nil {
		return err
	}
	tmp, err := os.MkdirTemp("", "apk2aab-")
	if err != nil {
		return err
	}
	if keepTmp {
		fmt.Println("    work dir:", tmp)
	} else {
		defer os.RemoveAll(tmp)
	}

	if err := t.resolve(toolsDir); err != nil {
		return err
	}

	// 1. Input: single APK or split bundle.
	base := in
	var abiSplits, resSplits []split
	ext := strings.ToLower(filepath.Ext(in))
	if ext != ".apk" {
		step("Unpacking split bundle %s", filepath.Base(in))
		dir := filepath.Join(tmp, "bundle")
		if err := unzip(in, dir); err != nil {
			return err
		}
		splits, err := t.scanSplits(dir)
		if err != nil {
			return err
		}
		base = ""
		for _, s := range splits {
			switch {
			case s.name == "":
				if base != "" {
					return fmt.Errorf("more than one base APK found (%s, %s)", base, s.path)
				}
				base = s.path
			case s.feature:
				warn("skipping feature split %q (%s) - Play-injected or dynamic feature modules are not rebuilt", s.name, filepath.Base(s.path))
			case s.hasLib && strings.HasPrefix(s.name, "config."):
				abiSplits = append(abiSplits, s)
			case reDensity.MatchString(s.name):
				resSplits = append(resSplits, s)
			case strings.HasPrefix(s.name, "config."):
				warn("skipping config split %q - base resources are used for this configuration", s.name)
			default:
				warn("skipping split %q (belongs to a skipped feature module)", s.name)
			}
		}
		if base == "" {
			return errors.New("no base APK found in bundle")
		}
		fmt.Println("    base:", filepath.Base(base))
		for _, s := range abiSplits {
			fmt.Println("    abi split:", s.name)
		}
		for _, s := range resSplits {
			fmt.Println("    density split:", s.name)
		}
	}

	// 2. Decode.
	dec := filepath.Join(tmp, "input")
	step("Decoding base APK with apktool")
	if err := t.runJava(t.apktool, "d", base, "-s", "-f", "-o", dec); err != nil {
		return fmt.Errorf("apktool: %w", err)
	}

	for i, s := range resSplits {
		sd := filepath.Join(tmp, fmt.Sprintf("split%d", i))
		if err := t.runJava(t.apktool, "d", s.path, "-s", "-f", "-o", sd); err != nil {
			return fmt.Errorf("apktool (%s): %w", s.name, err)
		}
		n, err := mergeSplitResources(dec, sd)
		if err != nil {
			return fmt.Errorf("merging %s: %w", s.name, err)
		}
		fmt.Printf("    merged %s (%d resources filled in)\n", s.name, n)
	}
	if n := countDummies(dec); n > 0 {
		fmt.Printf("    %d empty placeholder resources remain (normally resources removed by R8/shrinking)\n", n)
	}

	meta, err := readApktoolYml(filepath.Join(dec, "apktool.yml"))
	if err != nil {
		return err
	}
	if vcode != "" {
		meta["versionCode"] = vcode
	}
	if vname != "" {
		meta["versionName"] = vname
	}
	for _, k := range []string{"minSdkVersion", "targetSdkVersion", "versionCode", "versionName"} {
		if meta[k] == "" {
			return fmt.Errorf("could not read %s from apktool.yml (pass it as a flag)", k)
		}
	}
	fmt.Printf("    versionCode=%s versionName=%s minSdk=%s targetSdk=%s\n",
		meta["versionCode"], meta["versionName"], meta["minSdkVersion"], meta["targetSdkVersion"])

	// 3. Sanitize manifest.
	step("Removing split-delivery markers from AndroidManifest.xml")
	if err := sanitizeManifest(dec); err != nil {
		return err
	}

	// 4. aapt2.
	step("Compiling resources with aapt2")
	resZip := filepath.Join(tmp, "compiled_resources.zip")
	if err := t.exec(t.aapt2, "compile", "--dir", filepath.Join(dec, "res"), "-o", resZip); err != nil {
		return fmt.Errorf("aapt2 compile: %w", err)
	}
	step("Linking resources (proto format) with aapt2")
	linked := filepath.Join(tmp, "linked.apk")
	if err := t.exec(t.aapt2, "link", "--proto-format", "-o", linked,
		"-I", t.androidJar,
		"--manifest", filepath.Join(dec, "AndroidManifest.xml"),
		"--min-sdk-version", meta["minSdkVersion"],
		"--target-sdk-version", meta["targetSdkVersion"],
		"--version-code", meta["versionCode"],
		"--version-name", meta["versionName"],
		"-R", resZip, "--auto-add-overlay"); err != nil {
		return fmt.Errorf("aapt2 link: %w", err)
	}

	// 5. Module layout.
	step("Building base module")
	mod := filepath.Join(tmp, "module")
	if err := unzip(linked, mod); err != nil {
		return err
	}
	if err := buildModule(dec, mod, abiSplits); err != nil {
		return err
	}
	modZip := filepath.Join(tmp, "base.zip")
	if err := zipDir(mod, modZip); err != nil {
		return err
	}

	// 6. bundletool.
	step("Building %s with bundletool", out)
	cfg := filepath.Join(tmp, "BundleConfig.json")
	if err := writeBundleConfig(base, cfg); err != nil {
		return err
	}
	os.Remove(out)
	if err := t.runJava(t.bundletool, "build-bundle", "--modules="+modZip, "--config="+cfg, "--output="+out); err != nil {
		return fmt.Errorf("bundletool: %w", err)
	}
	if st, err := os.Stat(out); err == nil {
		fmt.Printf("    done: %s (%.1f MB) - unsigned, sign it with your upload key before uploading\n", out, float64(st.Size())/1e6)
	}
	return nil
}

// ---------- tools ----------

func (t *tools) resolve(dir string) error {
	pick := func(cur *string, name string, cands ...string) error {
		if *cur != "" {
			if _, err := os.Stat(*cur); err != nil {
				return fmt.Errorf("%s not found at %s", name, *cur)
			}
			return nil
		}
		for _, c := range cands {
			if c == "" {
				continue
			}
			if _, err := os.Stat(c); err == nil {
				*cur = c
				return nil
			}
		}
		return fmt.Errorf("%s not found (looked in %s); pass -%s or run fetch-tools.sh", name, strings.Join(cands, ", "), name)
	}
	exe := ""
	if runtime.GOOS == "windows" {
		exe = ".exe"
	}
	if t.java == "" {
		if jh := os.Getenv("JAVA_HOME"); jh != "" {
			t.java = filepath.Join(jh, "bin", "java"+exe)
		} else if p, err := exec.LookPath("java"); err == nil {
			t.java = p
		} else {
			return errors.New("java not found: set JAVA_HOME or put java on PATH")
		}
	}
	sdk := os.Getenv("ANDROID_HOME")
	if sdk == "" {
		sdk = os.Getenv("ANDROID_SDK_ROOT")
	}
	var sdkAapt2, sdkJar string
	if sdk != "" {
		sdkAapt2 = latest(filepath.Join(sdk, "build-tools", "*", "aapt2"+exe))
		sdkJar = latest(filepath.Join(sdk, "platforms", "android-*", "android.jar"))
	}
	if err := pick(&t.apktool, "apktool", filepath.Join(dir, "apktool.jar")); err != nil {
		return err
	}
	if err := pick(&t.bundletool, "bundletool", filepath.Join(dir, "bundletool.jar")); err != nil {
		return err
	}
	if err := pick(&t.aapt2, "aapt2", filepath.Join(dir, "aapt2"+exe), sdkAapt2); err != nil {
		return err
	}
	return pick(&t.androidJar, "android-jar", filepath.Join(dir, "android.jar"), sdkJar)
}

func latest(glob string) string {
	m, _ := filepath.Glob(glob)
	sort.Slice(m, func(i, j int) bool { return naturalLess(m[i], m[j]) })
	if len(m) == 0 {
		return ""
	}
	return m[len(m)-1]
}

func naturalLess(a, b string) bool {
	re := regexp.MustCompile(`\d+|\D+`)
	pa, pb := re.FindAllString(a, -1), re.FindAllString(b, -1)
	for i := 0; i < len(pa) && i < len(pb); i++ {
		if pa[i] == pb[i] {
			continue
		}
		var na, nb int
		_, ea := fmt.Sscan(pa[i], &na)
		_, eb := fmt.Sscan(pb[i], &nb)
		if ea == nil && eb == nil {
			return na < nb
		}
		return pa[i] < pb[i]
	}
	return len(pa) < len(pb)
}

func (t *tools) runJava(jar string, args ...string) error {
	return t.exec(t.java, append([]string{"-jar", jar}, args...)...)
}

func (t *tools) exec(bin string, args ...string) error {
	cmd := exec.Command(bin, args...)
	var buf strings.Builder
	if verbose {
		cmd.Stdout, cmd.Stderr = os.Stdout, os.Stderr
	} else {
		cmd.Stdout, cmd.Stderr = &buf, &buf
	}
	if err := cmd.Run(); err != nil {
		if !verbose {
			s := buf.String()
			if len(s) > 4000 {
				s = "...\n" + s[len(s)-4000:]
			}
			return fmt.Errorf("%w\n%s", err, s)
		}
		return err
	}
	return nil
}

var (
	reDensity = regexp.MustCompile(`^config\.(ldpi|mdpi|tvdpi|hdpi|xhdpi|xxhdpi|xxxhdpi|nodpi)$`)
	reSplit   = regexp.MustCompile(`(?m)^\s*A: split="([^"]*)"`)
	reFeature = regexp.MustCompile(`(?m)isFeatureSplit\(0x[0-9a-f]+\)=(true|0xffffffff)`)
)

func (t *tools) scanSplits(dir string) ([]split, error) {
	var res []split
	err := filepath.Walk(dir, func(p string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || strings.ToLower(filepath.Ext(p)) != ".apk" {
			return err
		}
		cmd := exec.Command(t.aapt2, "dump", "xmltree", "--file", "AndroidManifest.xml", p)
		b, err := cmd.Output()
		if err != nil {
			return fmt.Errorf("reading manifest of %s: %w", filepath.Base(p), err)
		}
		s := split{path: p}
		if m := reSplit.FindSubmatch(b); m != nil {
			s.name = string(m[1])
		}
		s.feature = reFeature.Match(b)
		s.hasLib, err = zipHasPrefix(p, "lib/")
		if err != nil {
			return err
		}
		res = append(res, s)
		return nil
	})
	return res, err
}

func countDummies(dec string) int {
	b, _ := os.ReadFile(filepath.Join(dec, "res", "values", "public.xml"))
	return strings.Count(string(b), `name="APKTOOL_DUMMY_`)
}

// ---------- manifest / metadata ----------

func readApktoolYml(path string) (map[string]string, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	m := map[string]string{}
	re := regexp.MustCompile(`(?m)^\s+(minSdkVersion|targetSdkVersion|versionCode|versionName):\s*'?([^'\r\n]*)'?\s*$`)
	for _, g := range re.FindAllStringSubmatch(string(b), -1) {
		m[g[1]] = strings.TrimSpace(g[2])
	}
	return m, nil
}

var (
	reSplitAttrs = regexp.MustCompile(`\s+android:(requiredSplitTypes|splitTypes|isSplitRequired)="[^"]*"`)
	reMetaData   = regexp.MustCompile(`(?s)\s*<meta-data\s[^>]*?android:name="(com\.android\.vending\.splits(\.required)?|com\.android\.vending\.derived\.apk\.id|com\.android\.stamp\.source|com\.android\.stamp\.type)"[^>]*?/>`)
	reSplitsXML  = regexp.MustCompile(`android:name="com\.android\.vending\.splits"[^>]*?android:resource="@xml/([^"]+)"`)
)

func sanitizeManifest(dec string) error {
	p := filepath.Join(dec, "AndroidManifest.xml")
	b, err := os.ReadFile(p)
	if err != nil {
		return err
	}
	s := string(b)
	if m := reSplitsXML.FindStringSubmatch(s); m != nil {
		if err := removeXMLResource(dec, m[1]); err != nil {
			return err
		}
	}
	n := len(reSplitAttrs.FindAllString(s, -1)) + len(reMetaData.FindAllString(s, -1))
	s = reSplitAttrs.ReplaceAllString(s, "")
	s = reMetaData.ReplaceAllString(s, "")
	fmt.Printf("    removed %d entries\n", n)
	return os.WriteFile(p, []byte(s), 0o644)
}

// removeXMLResource deletes res/xml/<name>.xml and its public.xml declaration.
func removeXMLResource(dec, name string) error {
	os.Remove(filepath.Join(dec, "res", "xml", name+".xml"))
	pub := filepath.Join(dec, "res", "values", "public.xml")
	b, err := os.ReadFile(pub)
	if err != nil {
		return nil
	}
	re := regexp.MustCompile(`\s*<public type="xml" name="` + regexp.QuoteMeta(name) + `" id="[^"]*"\s*/>`)
	return os.WriteFile(pub, re.ReplaceAll(b, nil), 0o644)
}

// ---------- module layout ----------

func buildModule(dec, mod string, abiSplits []split) error {
	if err := os.MkdirAll(filepath.Join(mod, "manifest"), 0o755); err != nil {
		return err
	}
	if err := os.Rename(filepath.Join(mod, "AndroidManifest.xml"), filepath.Join(mod, "manifest", "AndroidManifest.xml")); err != nil {
		return err
	}
	// dex
	dexes, _ := filepath.Glob(filepath.Join(dec, "*.dex"))
	if len(dexes) > 0 {
		os.MkdirAll(filepath.Join(mod, "dex"), 0o755)
	}
	for _, d := range dexes {
		if err := os.Rename(d, filepath.Join(mod, "dex", filepath.Base(d))); err != nil {
			return err
		}
	}
	// assets, lib
	for _, d := range []string{"assets", "lib"} {
		if exists(filepath.Join(dec, d)) {
			if err := os.Rename(filepath.Join(dec, d), filepath.Join(mod, d)); err != nil {
				return err
			}
		}
	}
	for _, s := range abiSplits {
		if err := unzipPrefix(s.path, "lib/", mod); err != nil {
			return err
		}
	}
	// root: unknown files, kotlin metadata, META-INF (without signatures)
	root := filepath.Join(mod, "root")
	if err := copyTree(filepath.Join(dec, "unknown"), root, skipRoot); err != nil {
		return err
	}
	if err := copyTree(filepath.Join(dec, "kotlin"), filepath.Join(root, "kotlin"), nil); err != nil {
		return err
	}
	for _, mi := range []string{filepath.Join(dec, "original", "META-INF"), filepath.Join(dec, "META-INF")} {
		if err := copyTree(mi, filepath.Join(root, "META-INF"), skipSignature); err != nil {
			return err
		}
	}
	return nil
}

func skipRoot(rel string) bool {
	return rel == "stamp-cert-sha256" || strings.HasPrefix(rel, "META-INF/") && skipSignature(strings.TrimPrefix(rel, "META-INF/"))
}

func skipSignature(rel string) bool {
	u := strings.ToUpper(rel)
	if u == "MANIFEST.MF" {
		return true
	}
	for _, e := range []string{".SF", ".RSA", ".DSA", ".EC"} {
		if !strings.Contains(u, "/") && strings.HasSuffix(u, e) {
			return true
		}
	}
	return false
}

// writeBundleConfig keeps files that were stored uncompressed in the original
// APK (e.g. Unity's assets/bin/Data) uncompressed in the bundle too.
func writeBundleConfig(apk, out string) error {
	r, err := zip.OpenReader(apk)
	if err != nil {
		return err
	}
	defer r.Close()
	var globs []string
	for _, f := range r.File {
		if f.Method == zip.Store && !f.FileInfo().IsDir() && strings.HasPrefix(f.Name, "assets/") {
			globs = append(globs, f.Name)
		}
	}
	cfg := map[string]any{
		"compression": map[string]any{"uncompressedGlob": globs},
	}
	b, _ := json.MarshalIndent(cfg, "", "  ")
	return os.WriteFile(out, b, 0o644)
}

// ---------- zip / fs helpers ----------

func exists(p string) bool { _, err := os.Stat(p); return err == nil }

func zipHasPrefix(path, prefix string) (bool, error) {
	r, err := zip.OpenReader(path)
	if err != nil {
		return false, err
	}
	defer r.Close()
	for _, f := range r.File {
		if strings.HasPrefix(f.Name, prefix) {
			return true, nil
		}
	}
	return false, nil
}

func unzip(src, dst string) error { return unzipPrefix(src, "", dst) }

func unzipPrefix(src, prefix, dst string) error {
	r, err := zip.OpenReader(src)
	if err != nil {
		return err
	}
	defer r.Close()
	for _, f := range r.File {
		if !strings.HasPrefix(f.Name, prefix) {
			continue
		}
		p := filepath.Join(dst, filepath.FromSlash(f.Name))
		if !strings.HasPrefix(p, filepath.Clean(dst)+string(os.PathSeparator)) {
			return fmt.Errorf("illegal path in zip: %s", f.Name)
		}
		if f.FileInfo().IsDir() {
			os.MkdirAll(p, 0o755)
			continue
		}
		if err := os.MkdirAll(filepath.Dir(p), 0o755); err != nil {
			return err
		}
		rc, err := f.Open()
		if err != nil {
			return err
		}
		w, err := os.Create(p)
		if err != nil {
			rc.Close()
			return err
		}
		_, err = io.Copy(w, rc)
		rc.Close()
		w.Close()
		if err != nil {
			return err
		}
	}
	return nil
}

func zipDir(dir, out string) error {
	f, err := os.Create(out)
	if err != nil {
		return err
	}
	defer f.Close()
	zw := zip.NewWriter(f)
	err = filepath.Walk(dir, func(p string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() {
			return err
		}
		rel, _ := filepath.Rel(dir, p)
		h, err := zip.FileInfoHeader(info)
		if err != nil {
			return err
		}
		h.Name = filepath.ToSlash(rel)
		h.Method = zip.Deflate
		w, err := zw.CreateHeader(h)
		if err != nil {
			return err
		}
		in, err := os.Open(p)
		if err != nil {
			return err
		}
		defer in.Close()
		_, err = io.Copy(w, in)
		return err
	})
	if err != nil {
		return err
	}
	return zw.Close()
}

func copyTree(src, dst string, skip func(rel string) bool) error {
	if !exists(src) {
		return nil
	}
	return filepath.Walk(src, func(p string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		rel, _ := filepath.Rel(src, p)
		rel = filepath.ToSlash(rel)
		if info.IsDir() || (skip != nil && skip(rel)) {
			return nil
		}
		t := filepath.Join(dst, filepath.FromSlash(rel))
		if err := os.MkdirAll(filepath.Dir(t), 0o755); err != nil {
			return err
		}
		in, err := os.Open(p)
		if err != nil {
			return err
		}
		defer in.Close()
		o, err := os.Create(t)
		if err != nil {
			return err
		}
		defer o.Close()
		_, err = io.Copy(o, in)
		return err
	})
}
