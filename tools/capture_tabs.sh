#!/usr/bin/env bash
# Capture the four home tabs of an app, verifying focus before every shot.
# usage: capture_tabs.sh <package> <activity> <out-prefix>
set -uo pipefail

PKG="$1"
ACT="$2"
PREFIX="$3"
OUT_DIR=".tools/shots"
mkdir -p "$OUT_DIR"

focus() {
  adb shell dumpsys window 2>/dev/null | grep mCurrentFocus | head -1
}

assert_focus() {
  local want="$1"
  local line
  line="$(focus)"
  if ! echo "$line" | grep -q "$want"; then
    echo "FOCUS MISMATCH: wanted $want, got: $line" >&2
    return 1
  fi
  echo "  focus ok: $line"
}

adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
adb shell wm dismiss-keyguard >/dev/null 2>&1
adb shell am start -n "$PKG/$ACT" >/dev/null 2>&1 || adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 4
assert_focus "$PKG"
adb exec-out screencap -p > "$OUT_DIR/$PREFIX-session.png"
echo "saved $PREFIX-session.png"

# nav item centers: nav card spans x 204..880, 4 items of 48dp (=144px), centered
for i in 1 2 3; do
  x=$((326 + 144 * i))
  adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
  adb shell input tap "$x" 2270
  sleep 2
  assert_focus "$PKG"
  adb exec-out screencap -p > "$OUT_DIR/$PREFIX-tab$i.png"
  echo "saved $PREFIX-tab$i.png"
done

# back to the first tab
adb shell input tap 326 2270
sleep 2
