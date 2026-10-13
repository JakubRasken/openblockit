#!/usr/bin/env bash
# End-to-end functional test for openblockit: session start, enforcement, parachute exit.
# Usage: e2e_session.sh [distraction-package]
set -uo pipefail

PKG=com.openblockit
DISTRACTION="${1:-com.bonkraigames.wrangleranch}"
OUT=.tools/shots
mkdir -p "$OUT"

focus() { adb shell dumpsys window 2>/dev/null | grep mCurrentFocus | head -1; }
step() { echo; echo "--- $*"; }

adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
adb shell wm dismiss-keyguard >/dev/null 2>&1
adb logcat -c

step "launch app"
adb shell am force-stop $PKG
adb shell am start -n $PKG/.MainActivity >/dev/null 2>&1
sleep 5
focus

step "reset to a 1 minute session + 2 parachutes for a fast test"
adb shell "run-as $PKG sh -c 'sed -i s/name=\"minutes\" value=\"[0-9]*\"/name=\"minutes\" value=\"1\"/ /data/data/$PKG/shared_prefs/blockit.xml'" 2>/dev/null
adb shell "run-as $PKG sh -c 'sed -i s/name=\"parachute_count\" value=\"[0-9]*\"/name=\"parachute_count\" value=\"2\"/ /data/data/$PKG/shared_prefs/blockit.xml'" 2>/dev/null
adb shell am force-stop $PKG
adb shell am start -n $PKG/.MainActivity >/dev/null 2>&1
sleep 4

step "slide to start (swipe the knob from left to right)"
adb shell input swipe 170 1905 1000 1905 400
sleep 4
focus
adb exec-out screencap -p > "$OUT/e2e-01-session.png"

step "service state"
adb shell dumpsys activity services $PKG 2>/dev/null | grep -E "ServiceRecord|isForeground|startRequested|foregroundServiceType" | head -6

step "notification (needs POST_NOTIFICATIONS)"
adb shell dumpsys notification --noredact 2>/dev/null | grep -A3 "$PKG" | grep -E "pkg=|channel|id=" | head -6

step "open a distraction app: $DISTRACTION"
adb shell monkey -p "$DISTRACTION" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 4
focus
adb exec-out screencap -p > "$OUT/e2e-02-block.png"

step "press home"
adb shell input keyevent KEYCODE_HOME
sleep 3
focus

step "crashes so far"
adb logcat -d 2>/dev/null | grep -B2 -A12 "FATAL EXCEPTION" | head -40
echo "(end of crash scan)"
