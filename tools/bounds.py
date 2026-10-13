#!/usr/bin/env python3
"""Dump view bounds (id -> rect) from `adb shell dumpsys activity top`.

Usage: bounds.py com.package/.Activity out.json
"""
import json
import re
import subprocess
import sys

pkg, activity, out = sys.argv[1], sys.argv[2], sys.argv[3]
subprocess.run(["adb", "shell", "am", "start", "-n", f"{pkg}/{activity}"],
               capture_output=True)
import time
time.sleep(4)
for attempt in range(6):
    dump = subprocess.run(["adb", "shell", "dumpsys", "activity", "top"],
                          capture_output=True, text=True).stdout
    tasks = dump.split("TASK ")
    for task in tasks:
        if pkg not in task:
            continue
        bounds = {}
        for line in task.splitlines():
            m = re.search(r"app:id/([A-Za-z0-9_]+)\s*$", line)
            if not m:
                continue
            b = re.search(r"(-?\d+),(-?\d+)-(-?\d+),(-?\d+)", line)
            if not b:
                continue
            x0, y0, x1, y1 = map(int, b.groups())
            bounds[m.group(1)] = [x0, y0, x1, y1]
        if bounds:
            with open(out, "w") as f:
                json.dump(bounds, f, indent=1, sort_keys=True)
            print(f"{pkg}: {len(bounds)} views -> {out}")
            sys.exit(0)
    time.sleep(2)
print(f"{pkg}: NO VIEWS FOUND")
sys.exit(1)
