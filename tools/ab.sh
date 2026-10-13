#!/bin/bash
# Verified A/B screenshot: ab.sh <pkgA> <actA> <outA> <pkgB> <actB> <outB>
set -u
capture() {
  local pkg="$1" act="$2" out="$3"
  adb shell am start -n "$pkg/$act" >/dev/null 2>&1
  for i in $(seq 1 12); do
    sleep 1
    local focus
    focus=$(adb shell dumpsys window 2>/dev/null | grep mCurrentFocus | head -1)
    if echo "$focus" | grep -q "$pkg"; then
      sleep 1
      adb exec-out screencap -p > "$out"
      local size
      size=$(stat -f%z "$out" 2>/dev/null || stat -c%s "$out")
      echo "captured $pkg -> $out (${size} bytes, focus ok)"
      return 0
    fi
  done
  echo "FAILED to focus $pkg (last: $focus)"
  return 1
}

capture "$1" "$2" "$3" && capture "$4" "$5" "$6"
