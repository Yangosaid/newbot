package main

import (
	"os"
	"path/filepath"
	"regexp"
	"strings"
)

// Split APKs leave holes in the base APK: resources that only have values in
// a density split (e.g. appcompat's xxhdpi PNGs) decode from the base as
// empty APKTOOL_DUMMY_* placeholders. mergeSplitResources fills them in from
// a decoded config split by matching resource IDs through public.xml.
//
// Only file-based resources (res/drawable-*/..., res/mipmap-*/...) are taken
// from the split. Its values-* files are not: apktool cannot resolve the
// base-resource references inside them, so they decode incomplete.

var rePublic = regexp.MustCompile(`<public type="([^"]+)" name="([^"]+)" id="(0x[0-9a-f]+)"\s*/>`)

type resKey struct{ typ, name string }

func readPublic(dec string) (map[string]resKey, error) {
	b, err := os.ReadFile(filepath.Join(dec, "res", "values", "public.xml"))
	if err != nil {
		return nil, err
	}
	m := map[string]resKey{}
	for _, g := range rePublic.FindAllStringSubmatch(string(b), -1) {
		m[g[3]] = resKey{g[1], g[2]}
	}
	return m, nil
}

func isDummy(name string) bool { return strings.HasPrefix(name, "APKTOOL_DUMMY_") }

// mergeSplitResources copies file resources from splitDec into dec and returns
// how many base placeholders it resolved.
func mergeSplitResources(dec, splitDec string) (int, error) {
	basePub, err := readPublic(dec)
	if err != nil {
		return 0, err
	}
	splitPub, err := readPublic(splitDec)
	if err != nil {
		return 0, err
	}
	// type -> dummy name -> real name
	renames := map[string]map[string]string{}
	for id, sk := range splitPub {
		bk, ok := basePub[id]
		if !ok || isDummy(sk.name) || !isDummy(bk.name) {
			continue
		}
		if renames[bk.typ] == nil {
			renames[bk.typ] = map[string]string{}
		}
		renames[bk.typ][bk.name] = sk.name
	}

	// Copy file resources (non-values dirs) that the base does not have.
	provided := map[resKey]bool{}
	resDir := filepath.Join(splitDec, "res")
	dirs, err := os.ReadDir(resDir)
	if err != nil {
		return 0, err
	}
	for _, d := range dirs {
		if !d.IsDir() || d.Name() == "values" || strings.HasPrefix(d.Name(), "values-") {
			continue
		}
		typ := strings.SplitN(d.Name(), "-", 2)[0]
		files, _ := os.ReadDir(filepath.Join(resDir, d.Name()))
		for _, f := range files {
			name := f.Name()
			if isDummy(name) {
				continue
			}
			dst := filepath.Join(dec, "res", d.Name(), name)
			if !exists(dst) {
				if err := copyFile(filepath.Join(resDir, d.Name(), name), dst); err != nil {
					return 0, err
				}
			}
			provided[resKey{typ, resName(name)}] = true
		}
	}

	if len(renames) == 0 {
		return 0, nil
	}
	n := 0
	for _, m := range renames {
		n += len(m)
	}
	return n, applyRenames(dec, renames, provided)
}

func resName(file string) string {
	if strings.HasSuffix(file, ".9.png") {
		return strings.TrimSuffix(file, ".9.png")
	}
	return strings.TrimSuffix(file, filepath.Ext(file))
}

// applyRenames rewrites every reference to a resolved placeholder in the
// decoded base, and drops the placeholder's empty definition when the split
// supplied a real file for it.
type rule struct {
	re  *regexp.Regexp
	rep string
}

func applyRenames(dec string, renames map[string]map[string]string, provided map[resKey]bool) error {
	var refRules, valueRules, publicRules []rule
	for typ, m := range renames {
		for dummy, real := range m {
			q := regexp.QuoteMeta(dummy)
			refRules = append(refRules, rule{regexp.MustCompile(`([@?]\+?` + regexp.QuoteMeta(typ) + `/)` + q + `\b`), "${1}" + real})
			def := regexp.MustCompile(`(type="` + regexp.QuoteMeta(typ) + `" name=")` + q + `"`)
			if provided[resKey{typ, real}] {
				// remove empty <item type=".." name="dummy" /> definitions
				valueRules = append(valueRules, rule{regexp.MustCompile(`\s*<item type="` + regexp.QuoteMeta(typ) + `" name="` + q + `"\s*/>`), ""})
			}
			valueRules = append(valueRules, rule{def, "${1}" + real + `"`})
			publicRules = append(publicRules, rule{regexp.MustCompile(`(<public type="` + regexp.QuoteMeta(typ) + `" name=")` + q + `"`), "${1}" + real + `"`})
		}
	}
	rewrite := func(p string, rules ...[]rule) error {
		b, err := os.ReadFile(p)
		if err != nil {
			return err
		}
		s := string(b)
		if !strings.Contains(s, "APKTOOL_DUMMY_") {
			return nil
		}
		for _, rs := range rules {
			for _, r := range rs {
				s = r.re.ReplaceAllString(s, r.rep)
			}
		}
		return os.WriteFile(p, []byte(s), 0o644)
	}
	if err := rewrite(filepath.Join(dec, "AndroidManifest.xml"), refRules); err != nil {
		return err
	}
	return filepath.Walk(filepath.Join(dec, "res"), func(p string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || !strings.HasSuffix(p, ".xml") {
			return err
		}
		dir := filepath.Base(filepath.Dir(p))
		if dir == "values" || strings.HasPrefix(dir, "values-") {
			if filepath.Base(p) == "public.xml" {
				return rewrite(p, publicRules)
			}
			return rewrite(p, valueRules, refRules)
		}
		return rewrite(p, refRules)
	})
}

func copyFile(src, dst string) error {
	b, err := os.ReadFile(src)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(dst), 0o755); err != nil {
		return err
	}
	return os.WriteFile(dst, b, 0o644)
}
