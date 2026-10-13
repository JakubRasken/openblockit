#!/bin/bash
# usage: ui_bounds.sh <package> <activity> <out.json>
adb shell am start -n "$1/$2" >/dev/null 2>&1
sleep 5
adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
adb shell cat /sdcard/ui.xml > "$3"
python3 - "$3" <<'PY'
import re, sys, json
xml = open(sys.argv[1]).read()
out = {}
for m in re.finditer(r'resource-id="([^"]+)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
    rid = m.group(1).split("/")[-1]
    if rid and rid != "":
        out[rid] = [int(m.group(2)), int(m.group(3)), int(m.group(4)), int(m.group(5))]
json.dump(out, open(sys.argv[1] + ".json", "w"), indent=1, sort_keys=True)
print(f"{len(out)} ids")
PY
