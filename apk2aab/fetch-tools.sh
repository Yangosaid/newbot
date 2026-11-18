#!/usr/bin/env bash
# Downloads the external tools apk2aab needs into ./tools
#   apktool.jar    - decodes the APK                (GitHub releases)
#   bundletool.jar - builds the .aab                (GitHub releases)
#   aapt2          - from apktool-lib on Maven Central (understands API 35/36 framework)
#   android.jar    - framework resources for API 36, from Robolectric's android-all (Maven Central)
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p tools
APKTOOL_VER=2.6.1
BUNDLETOOL_VER=1.11.0
APKTOOL_LIB_VER=3.0.3
ANDROID_ALL=16-robolectric-13921718
M2=https://repo1.maven.org/maven2

case "$(uname -s)" in
  Linux*) OSDIR=linux; EXE= ;;
  Darwin*) OSDIR=macosx; EXE= ;;
  MINGW*|MSYS*|CYGWIN*) OSDIR=windows; EXE=.exe ;;
  *) echo "unsupported OS" >&2; exit 1 ;;
esac

tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT

[ -f tools/apktool.jar ] || curl -fL -o tools/apktool.jar \
  "https://github.com/iBotPeaches/Apktool/releases/download/v${APKTOOL_VER}/apktool_${APKTOOL_VER}.jar"
[ -f tools/bundletool.jar ] || curl -fL -o tools/bundletool.jar \
  "https://github.com/google/bundletool/releases/download/${BUNDLETOOL_VER}/bundletool-all-${BUNDLETOOL_VER}.jar"
if [ ! -f "tools/aapt2${EXE}" ]; then
  curl -fL -o "$tmp/apktool-lib.jar" "$M2/org/apktool/apktool-lib/${APKTOOL_LIB_VER}/apktool-lib-${APKTOOL_LIB_VER}.jar"
  unzip -o -q -j "$tmp/apktool-lib.jar" "prebuilt/${OSDIR}/aapt2${EXE}" -d tools
  chmod +x "tools/aapt2${EXE}"
fi
if [ ! -f tools/android.jar ]; then
  curl -fL -o "$tmp/android-all.jar" "$M2/org/robolectric/android-all/${ANDROID_ALL}/android-all-${ANDROID_ALL}.jar"
  (cd "$tmp" && unzip -o -q android-all.jar resources.arsc AndroidManifest.xml && zip -q -0 android.jar resources.arsc AndroidManifest.xml)
  mv "$tmp/android.jar" tools/android.jar
fi
ls -la tools
