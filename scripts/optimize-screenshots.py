#!/usr/bin/env python3
"""Уменьшает скриншоты до 720 px по ширине и сжимает PNG для репозитория."""
import glob, os
from PIL import Image
for path in sorted(glob.glob("docs/screenshots/*.png")):
    if path.endswith("-small.png"):
        continue
    img = Image.open(path).convert("RGB")
    if img.width > 720:
        img = img.resize((720, round(img.height * 720 / img.width)), Image.LANCZOS)
    out = path.replace(".png", "-small.png")
    img.save(out, "PNG", optimize=True)
    print(out, os.path.getsize(out) // 1024, "KiB")
