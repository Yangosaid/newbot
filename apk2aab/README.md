# apk2aab

Converts an Android **APK** or a **split-APK bundle** (`.xapk`, `.apks`, `.apkm`)
into an **Android App Bundle** (`.aab`).

Based on the approach of [iricartb/go-apk2aab-tool](https://github.com/iricartb/go-apk2aab-tool)
(apktool → aapt2 → bundletool), rewritten to:

- run on Linux / macOS / Windows (no hardcoded Windows paths or SDK layout);
- accept split bundles: picks the base APK, merges native libs from ABI splits
  (`config.arm64_v8a`, …) and drawables from density splits (`config.xxhdpi`, …),
  and skips Play-injected feature modules (e.g. `gpdeku`);
- read `versionCode`, `versionName`, `minSdk`, `targetSdk` from the APK instead of
  hardcoding `1` / `1.0`;
- strip Play split-delivery markers from the manifest (`requiredSplitTypes`,
  `com.android.vending.splits*`, `com.android.stamp.*`, `derived.apk.id`);
- keep files that were stored uncompressed in the APK (e.g. Unity `assets/bin/Data`)
  uncompressed in the bundle;
- not copy the old APK signature into the bundle.

## Usage

```bash
./fetch-tools.sh                 # downloads apktool, bundletool, aapt2, android.jar into ./tools
go build -o apk2aab .
./apk2aab -o game.aab game.xapk  # or game.apk
```

Requires Java 11+ and Go 1.21+. Flags: `-tools DIR`, `-apktool`, `-bundletool`,
`-aapt2`, `-android-jar`, `-java`, `-version-code`, `-version-name`, `-keep-temp`, `-v`.

If `ANDROID_HOME` is set, the newest `build-tools/*/aapt2` and `platforms/*/android.jar`
are used when `./tools` doesn't have them. Use an aapt2 at least as new as the
app's target SDK (the 2021 aapt2 bundled in apktool 2.6 cannot read API 35+ framework jars).

## Signing

The output is **unsigned**. Sign it with your Play upload key:

```bash
jarsigner -keystore upload.jks -sigalg SHA256withRSA -digestalg SHA-256 game.aab upload
```

## Limitations

- Language splits (`config.en`, …) and dynamic feature modules are not merged.
- An APK downloaded from Google Play already contains code Play injects at delivery
  (e.g. `com.pairip` integrity checks, the `gpdeku` module). Rebuilding from the
  original project is always preferable to re-uploading a converted store APK.
