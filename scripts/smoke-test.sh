#!/usr/bin/env bash
# Installs the phone APK on the running emulator, launches it and collects evidence.
# Usage: smoke-test.sh <apk> <out-dir>
set -u

APK="$1"
OUT="$2"
PKG="proton.android.authenticator"

mkdir -p "$OUT"

adb install -r -g "$APK" 2>&1 | tee "$OUT/install.txt"
adb logcat -c
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 > "$OUT/launch.txt" 2>&1
sleep 30

adb logcat -d -b all > "$OUT/logcat.txt"
adb exec-out screencap -p > "$OUT/screen.png"
adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity" > "$OUT/resumed.txt" || true

if adb shell pidof "$PKG" > "$OUT/pid.txt" && [ -s "$OUT/pid.txt" ]; then
  echo "APP RUNNING (pid $(cat "$OUT/pid.txt"))"
else
  echo "APP NOT RUNNING"
fi

echo "---- crash excerpts ----"
grep -E "FATAL EXCEPTION|Process: $PKG|ANR in" -A 25 "$OUT/logcat.txt" | head -150 || true
echo "---- resumed ----"
cat "$OUT/resumed.txt"
