#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
SCRIPT_ID='TemporalFractions_run_v20261104T215458Z'
BASE='/storage/emulated/0'
ZIP="${1:-$BASE/Documents/zip/TemporalFractions_v20261104T215458Z.zip}"
if [[ ! -f "$ZIP" && $# -eq 0 ]];then ZIP="$BASE/Download/TemporalFractions_v20261104T215458Z.zip";fi
[[ -f "$ZIP" ]] || { printf 'ZIP missing: %s\n' "$ZIP" >&2;exit 2; }
LOG_DIR="$BASE/Documents/log"
mkdir -p "$LOG_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
LOG="$LOG_DIR/TemporalFractions_full_terminal_v${STAMP}_pid$$.log"
TEMP=''
cleanup(){ result=$?;trap - EXIT;[[ -z "$TEMP" ]] || rm -rf -- "$TEMP";exit "$result"; }
trap cleanup EXIT
TEMP="$(mktemp -d "$HOME/.temporalfractions_extract.XXXXXXXX")"
printf 'Runner: %s\nFull diagnostic log: %s\n' "$SCRIPT_ID" "$LOG"
if (set -Eeuo pipefail;unzip -q "$ZIP" -d "$TEMP";SOURCE="$TEMP/TemporalFractions_v20261104T215458Z";[[ -f "$SOURCE/build_v20261104T215458Z.sh" ]] || { printf 'Build script missing: %s\n' "$SOURCE" >&2;exit 3; };bash "$SOURCE/build_v20261104T215458Z.sh" "$SOURCE") 2>&1 | tee "$LOG" | awk '/^(Script:|SDK:|APK:|Fetching pinned|Checking fixed-duration mathematics|Temporal Fractions fixed-duration tests passed|Building WavPack native decoder|Compiling Java JNI declarations|Compiling Kotlin and bundling Kotlin standard library|Downloading standalone D8|Retrying standalone D8|Using standalone D8:|Converting Kotlin and Java classes to DEX|Packaging Android resources|Signing and verifying APK|Opening Android package installer|BUILD SUCCESS:|BUILD FAILED:|Build failed \(exit)/{print;fflush()}';then printf 'Completed. Full diagnostic log: %s\n' "$LOG";else RC=$?;printf 'Build failed (exit %s). Full diagnostic log: %s\nLast 25 lines:\n' "$RC" "$LOG" >&2;tail -n 25 "$LOG" >&2;exit "$RC";fi
