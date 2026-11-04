#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail
SCRIPT_ID='TemporalFractions_diagnose_v20261104T215458Z'
BASE='/storage/emulated/0'
PACKAGE='cc0.temporalfractions.a0he7vjet48'
LOG_DIR="$BASE/Documents/log"
mkdir -p "$LOG_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
LOG="$LOG_DIR/TemporalFractions_android_runtime_v${STAMP}_pid$$.log"
command -v adb >/dev/null 2>&1 || { printf 'ADB unavailable: pkg install android-tools\n' >&2;exit 1; }
STATE="$(adb get-state 2>/dev/null || true)"
[[ "$STATE" == 'device' ]] || { printf 'ADB is not connected. Enable Wireless debugging and pair/connect Termux ADB to 127.0.0.1 first.\n' >&2;adb devices;exit 2; }
printf 'Capturing app-launch runtime logs: %s\n' "$LOG"
adb logcat -c -b main -b system -b crash
adb shell am start -W -n "$PACKAGE/.MainActivity" || true
sleep 5
adb logcat -d -v threadtime -b main -b system -b crash > "$LOG"
printf 'Full Android runtime log: %s\n' "$LOG"
if grep -n -E -A35 -B5 'FATAL EXCEPTION|AndroidRuntime|Fatal signal|NoClassDefFoundError|VerifyError|UnsatisfiedLinkError|ClassNotFoundException' "$LOG" | tail -n 100;then :;else printf 'No obvious runtime exception found; please share the full log.\n';fi
