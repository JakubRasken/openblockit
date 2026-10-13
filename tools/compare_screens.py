#!/usr/bin/env python3
"""Pixel-diff two phone screenshots and report the worst-matching regions.

Usage: python3 tools/compare_screens.py ORIGINAL.png OURS.png OUT_DIR
"""
import os
import sys

from PIL import Image, ImageChops


def main() -> None:
    if len(sys.argv) != 4:
        print(__doc__)
        raise SystemExit(2)
    a_path, b_path, out_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    os.makedirs(out_dir, exist_ok=True)

    a = Image.open(a_path).convert("RGB")
    b = Image.open(b_path).convert("RGB")
    if a.size != b.size:
        b = b.resize(a.size)

    w, h = a.size
    box = (0, int(h * 0.04), w, h - int(h * 0.03))
    a2 = a.crop(box)
    b2 = b.crop(box)

    diff = ImageChops.difference(a2, b2).convert("L")
    total = a2.size[0] * a2.size[1]
    hist = diff.histogram()
    mean = sum(i * v for i, v in enumerate(hist)) / total
    print(f"image: {a.size}  overall mean diff: {mean:.2f} (0 = identical)")

    cols, rows = 6, 12
    cw = a2.size[0] // cols
    ch = a2.size[1] // rows
    regions = []
    for r in range(rows):
        for c in range(cols):
            region = diff.crop((c * cw, r * ch, (c + 1) * cw, (r + 1) * ch))
            rh = region.histogram()
            rtotal = sum(rh)
            rmean = sum(i * v for i, v in enumerate(rh)) / max(rtotal, 1)
            regions.append((rmean, r, c))
    regions.sort(reverse=True)
    print("worst regions (mean diff, grid row/col, pixel box):")
    for rmean, r, c in regions[:12]:
        print(
            f"  {rmean:6.1f}  row {r:2d} col {c}  "
            f"y {r * ch}-{(r + 1) * ch}  x {c * cw}-{(c + 1) * cw}"
        )

    heat = diff.point(lambda x: min(255, x * 2))
    overlay = Image.merge("RGB", (heat, Image.new("L", diff.size, 0), diff))
    blended = Image.blend(b2, overlay, 0.55)
    out_path = os.path.join(out_dir, "diff_overlay.png")
    blended.save(out_path)
    print("saved", out_path)


if __name__ == "__main__":
    main()
