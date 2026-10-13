#!/usr/bin/env python3
"""Measure glass/digit/grid positions in a home screenshot."""
import sys

from PIL import Image


def scan(path):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = im.load()

    def is_card(c):
        return abs(c[0] - 51) < 12 and abs(c[1] - 42) < 12 and abs(c[2] - 39) < 12

    def is_cream(c):
        return c[0] > 190 and c[1] > 175 and c[2] > 160

    def is_grid(c):
        return abs(c[0] - 39) < 10 and abs(c[1] - 35) < 10 and abs(c[2] - 32) < 10

    glass_top = glass_bot = None
    for y in range(200, 1400):
        for x in (120, 540, 960):
            if is_card(px[x, y]):
                glass_top = y if glass_top is None else min(glass_top, y)
        if glass_top and y > glass_top + 40:
            if not any(is_card(px[x, y]) for x in (120, 540, 960)):
                glass_bot = y
                break

    d_top = d_bot = None
    for y in range(200, 1400):
        hit = any(is_cream(px[x, y]) for x in range(100, 900, 7))
        if hit:
            d_top = y if d_top is None else d_top
            d_bot = y
    grid_top = None
    for y in range((glass_bot or 700) + 10, 2000):
        if is_grid(px[540, y]):
            grid_top = y
            break
    print(f"{path}: glass={glass_top}..{glass_bot} (h={(glass_bot or 0) - (glass_top or 0)}) "
          f"digits={d_top}..{d_bot} (h={(d_bot or 0) - (d_top or 0)}) grid_top={grid_top}")


for p in sys.argv[1:]:
    scan(p)
