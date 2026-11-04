#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
SCRIPT_ID='TemporalFractions_build_v20261104T220602Z'
PROJECT="${1:?Provide extracted project directory}"
BASE='/storage/emulated/0'
STATE="$HOME/.temporal_fractions_build"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
APK="$BASE/Documents/apk/TemporalFractions_v${STAMP}.apk"
LOG="$BASE/Documents/log/TemporalFractions_build_v${STAMP}.log"
HASH="$BASE/Documents/txt/TemporalFractions_apk_sha256_v${STAMP}.txt"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
JAR="${ANDROID_JAR:-}"
WORK=''
fail(){ printf 'BUILD FAILED: %s\n' "$*" >&2;exit 1; }
for exe in aapt2 kotlinc java javac jar curl apksigner keytool zip unzip clang git sha256sum find tee;do command -v "$exe" >/dev/null 2>&1 || fail "Missing $exe; install: pkg install kotlin openjdk-21 aapt2 curl apksigner clang git zip unzip coreutils";done
[[ -f "$PROJECT/AndroidManifest.xml" && -f "$PROJECT/native/WavPackBridge_v20261104T220602Z.c" ]] || fail "Incomplete source ZIP: $PROJECT"
if [[ -z "$JAR" ]];then JAR="$(find "$SDK/platforms" -mindepth 2 -maxdepth 2 -name android.jar -type f 2>/dev/null | sort -V | tail -n 1 || true)";fi
[[ -f "$JAR" ]] || fail "Missing Android platform android.jar; set ANDROID_JAR or install Android SDK android-35"
JAVA_BIN="$(readlink -f "$(command -v javac)")"
JDK="${JAVA_HOME:-$(dirname "$(dirname "$JAVA_BIN")")}"
[[ -f "$JDK/include/jni.h" ]] || fail "JNI headers missing: $JDK/include/jni.h"
umask 077
mkdir -p "$STATE" "$BASE/Documents/apk" "$BASE/Documents/log" "$BASE/Documents/txt"
WORK="$(mktemp -d "$STATE/work.XXXXXXXX")"
cleanup(){ result=$?;trap - EXIT;[[ -z "$WORK" ]] || rm -rf -- "$WORK";if [[ "$result" -ne 0 ]];then printf 'Build failed (exit %s). Log: %s\n' "$result" "$LOG" >&2;fi;exit "$result"; }
trap cleanup EXIT
exec > >(tee "$LOG") 2>&1
printf 'Script: %s\nSource: %s\nSDK: %s\nAPK: %s\n' "$SCRIPT_ID" "$PROJECT" "$JAR" "$APK"
VENDOR="$STATE/vendor/WavPack-5.8.1"
if [[ ! -f "$VENDOR/include/wavpack.h" ]];then mkdir -p "$STATE/vendor";rm -rf "$VENDOR" "$WORK/WavPack-5.8.1";printf 'Fetching pinned upstream WavPack 5.8.1 source; Internet required on first build\n';git clone --depth 1 --branch 5.8.1 https://github.com/dbry/WavPack.git "$WORK/WavPack-5.8.1";[[ -f "$WORK/WavPack-5.8.1/include/wavpack.h" ]] || fail 'WavPack clone incomplete';mv "$WORK/WavPack-5.8.1" "$VENDOR";fi
REV="$(git -C "$VENDOR" rev-parse --short=7 HEAD)"
[[ "$REV" == '4827b98' ]] || fail "WavPack version does not match pinned 4827b98: $REV"
WLICENSE="$VENDOR/license.txt"
[[ -f "$WLICENSE" ]] || WLICENSE="$VENDOR/LICENSE"
[[ -f "$WLICENSE" ]] || fail 'WavPack BSD license missing'
mkdir -p "$WORK/classes" "$WORK/dex" "$WORK/compiled" "$WORK/res" "$WORK/lib/arm64-v8a" "$WORK/assets"
cp -R "$PROJECT/res/." "$WORK/res/"
NATIVE_SRCS=(common_utils decorr_utils entropy_utils extra1 extra2 open_utils open_filename open_legacy open_raw pack pack_dns pack_floats pack_utils read_words tags tag_utils unpack unpack_floats unpack_seek unpack_utils write_words)
NATIVE_FILES=()
for item in "${NATIVE_SRCS[@]}";do [[ -f "$VENDOR/src/$item.c" ]] || fail "Missing WavPack native source $item.c";NATIVE_FILES+=("$VENDOR/src/$item.c");done
JANSI_DIR="${PREFIX:-/data/data/com.termux/files/usr}/lib/jansi"
KOTLINC=(kotlinc)
if [[ -f "$JANSI_DIR/libjansi.so" ]];then KOTLINC+=("-J-Dlibrary.jansi.path=$JANSI_DIR");printf 'Using Termux native Jansi: %s\n' "$JANSI_DIR/libjansi.so";else printf 'Optional native Jansi unavailable; Kotlin may show a harmless warning\n';fi
printf 'Checking fixed-duration mathematics\n'
"${KOTLINC[@]}" "$PROJECT/src/TimeMath_v20261104T220602Z.kt" "$PROJECT/tests/FractionMathTest_v20261104T220602Z.kt" -include-runtime -d "$WORK/math-test.jar"
java -cp "$WORK/math-test.jar" cc0.temporalfractions.a0he7vjet48.FractionMathTest
printf 'Building WavPack native decoder for Android ARM64\n'
clang --target=aarch64-linux-android31 -std=c11 -D_GNU_SOURCE -D_FILE_OFFSET_BITS=64 -DHAVE___BUILTIN_CLZ=1 -O2 -fPIC -fvisibility=hidden -ffunction-sections -fdata-sections -shared -Wl,--gc-sections -Wl,--no-undefined -I"$VENDOR/include" -I"$VENDOR/src" -I"$JDK/include" -I"$JDK/include/linux" "$PROJECT/native/WavPackBridge_v20261104T220602Z.c" "${NATIVE_FILES[@]}" -lm -o "$WORK/lib/arm64-v8a/libtemporalwavpack.so"
[[ -s "$WORK/lib/arm64-v8a/libtemporalwavpack.so" ]] || fail 'Native .so missing'
printf 'Compiling Java JNI declarations\n'
javac --release 11 -encoding UTF-8 -classpath "$JAR" -d "$WORK/classes" "$PROJECT/src/WavPackNative.java"
(cd "$WORK/classes" && jar cf "$WORK/java.jar" .)
printf 'Compiling Kotlin and bundling Kotlin standard library\n'
"${KOTLINC[@]}" -jvm-target 11 -no-reflect -classpath "$JAR:$WORK/classes" -include-runtime -d "$WORK/kotlin.jar" "$PROJECT/src/"*.kt
D8_VERSION='9.1.29'
D8_DIR="$STATE/tools"
D8_JAR="$D8_DIR/r8lib-$D8_VERSION.jar"
mkdir -p "$D8_DIR"
if [[ ! -s "$D8_JAR" ]] || ! java -cp "$D8_JAR" com.android.tools.r8.D8 --version 2>/dev/null | grep -Fq "$D8_VERSION";then
printf 'Downloading standalone D8 %s from the official Google R8 archive\n' "$D8_VERSION"
D8_DOWNLOAD="$WORK/r8lib.jar"
if ! curl -fLsS --retry 2 --connect-timeout 20 'https://storage.googleapis.com/r8-releases/raw/9.1.29/r8lib.jar' -o "$D8_DOWNLOAD";then printf 'Retrying standalone D8 from Google Maven\n';curl -fLsS --retry 2 --connect-timeout 20 'https://dl.google.com/dl/android/maven2/com/android/tools/r8/9.1.29/r8-9.1.29.jar' -o "$D8_DOWNLOAD" || fail 'Unable to download R8 9.1.29 from either official source';fi
unzip -tqq "$D8_DOWNLOAD" || fail 'Standalone D8 JAR failed ZIP integrity check'
java -cp "$D8_DOWNLOAD" com.android.tools.r8.D8 --version | grep -Fq "$D8_VERSION" || fail 'Standalone D8 JAR reported the wrong version'
mv -f "$D8_DOWNLOAD" "$D8_JAR"
fi
printf 'Using standalone D8: %s\n' "$(java -cp "$D8_JAR" com.android.tools.r8.D8 --version)"
printf 'Converting Kotlin and Java classes to DEX\n'
java -cp "$D8_JAR" com.android.tools.r8.D8 --min-api 31 --lib "$JAR" --output "$WORK/dex" "$WORK/kotlin.jar" "$WORK/java.jar"
[[ -s "$WORK/dex/classes.dex" ]] || fail 'D8 output missing'
printf 'Packaging Android resources\n'
aapt2 compile --dir "$WORK/res" -o "$WORK/compiled"
aapt2 link -I "$JAR" --manifest "$PROJECT/AndroidManifest.xml" --min-sdk-version 31 --target-sdk-version 35 --version-code "$(date -u +%y%m%d%H)" --version-name "$STAMP" -o "$WORK/base.apk" "$WORK/compiled/"*.flat
cp "$WORK/dex/"classes*.dex "$WORK/"
cp "$WLICENSE" "$WORK/assets/wavpack_license.txt"
cp "$PROJECT/docs/THIRD_PARTY_LICENSES.txt" "$WORK/assets/third_party_licenses.txt"
cp "$PROJECT/docs/LICENSE_CC0.txt" "$WORK/assets/app_license_cc0.txt"
(cd "$WORK" && zip -q -u base.apk classes*.dex lib/arm64-v8a/libtemporalwavpack.so assets/wavpack_license.txt assets/third_party_licenses.txt assets/app_license_cc0.txt)
unzip -l "$WORK/base.apk" | grep -q 'lib/arm64-v8a/libtemporalwavpack.so' || fail 'APK lacks WavPack library'
INPUT="$WORK/base.apk"
if command -v zipalign >/dev/null 2>&1;then zipalign -f -p 4 "$INPUT" "$WORK/aligned.apk";INPUT="$WORK/aligned.apk";fi
KEY="$STATE/TemporalFractions_signing.keystore"
SECRET="$STATE/TemporalFractions_signing.pass"
if [[ ! -f "$KEY" ]];then [[ ! -f "$SECRET" ]] || fail 'Signing password exists without keystore';od -An -N24 -tx1 /dev/urandom | tr -d '[:space:]' > "$SECRET";keytool -genkeypair -noprompt -storetype PKCS12 -keystore "$KEY" -alias temporalfractions -keyalg RSA -keysize 3072 -validity 10000 -storepass "$(cat "$SECRET")" -keypass "$(cat "$SECRET")" -dname 'CN=Temporal Fractions, O=CC0 Open Source, C=US';fi
[[ -s "$SECRET" ]] || fail 'Signing keystore password missing; do not regenerate existing key'
[[ -s "$KEY" ]] || fail 'Signing keystore missing'
printf 'Signing and verifying APK\n'
apksigner sign --ks "$KEY" --ks-key-alias temporalfractions --ks-pass "file:$SECRET" --out "$APK" "$INPUT"
apksigner verify --verbose "$APK"
sha256sum "$APK" > "$HASH"
printf 'BUILD SUCCESS: %s\nSHA256: %s\n' "$APK" "$(sha256sum "$APK")"
if command -v termux-open >/dev/null 2>&1;then printf 'Opening Android package installer\n';if ! termux-open --content-type application/vnd.android.package-archive "$APK";then printf 'Installer launch failed; APK retained at %s\n' "$APK";fi;else printf 'termux-open unavailable; install APK manually: %s\n' "$APK";fi
