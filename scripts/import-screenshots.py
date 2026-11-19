#!/usr/bin/env python3
"""Импорт свежих скриншотов с телефона: OCR → экран → сжатие в docs/screenshots.

Использование: python3 scripts/import-screenshots.py [каталог]
По умолчанию: /sdcard/DCIM/Screenshots, только файлы за последние 2 дня.
"""
import glob, os, subprocess, sys, tempfile, time
from PIL import Image

SRC = sys.argv[1] if len(sys.argv) > 1 else "/sdcard/DCIM/Screenshots"
OUT = "docs/screenshots"
os.makedirs(OUT, exist_ok=True)

MARKERS = [
    ("wizard",   ["Шаг 1 из 4", "Шаг 2 из 4", "Постройте по размерам"]),
    ("measure",  ["Точек контура", "Длина эталона"]),
    ("stock",    ["Итоги склада", "Списать под проект"]),
    ("stitch",   ["Нить на шов", "Катушки хватит"]),
    ("learn",    ["пройдено", "Учебник кожевника", "Все уроки"]),
    ("layout",   ["Заполнение листа", "Печать 1:1"]),
    ("projects", ["Текущий проект", "Все проекты"]),
    ("costs",    ["Прибыль в час", "Фурнитура всего", "PDF-смета"]),
    ("leather",  ["Деталей кроя", "Шаблоны изделий"]),
]


def ocr(path):
    fd, base = tempfile.mkstemp()
    os.close(fd)
    subprocess.run(["tesseract", path, base, "-l", "rus+eng", "--psm", "6"],
                   capture_output=True)
    txt = base + ".txt"
    text = open(txt, errors="ignore").read() if os.path.exists(txt) else ""
    for f in (base, txt):
        if os.path.exists(f):
            os.unlink(f)
    return text


def classify(text):
    for name, keys in MARKERS:
        if any(k in text for k in keys):
            return name
    return "other"


files = sorted(glob.glob(os.path.join(SRC, "Screenshot_*")), key=os.path.getmtime)
cutoff = time.time() - 2 * 86400
files = [f for f in files if os.path.getmtime(f) >= cutoff]

if not files:
    print("Свежих скриншотов нет в", SRC)
    sys.exit(0)

counters = {}
for path in files:
    kind = classify(ocr(path))
    if kind == "other":
        print(f"{os.path.basename(path)} — не экран приложения, пропуск")
        continue
    counters[kind] = counters.get(kind, 0) + 1
    name = f"{kind}-{counters[kind]}.png"
    img = Image.open(path).convert("RGB")
    if img.width > 720:
        img = img.resize((720, round(img.height * 720 / img.width)), Image.LANCZOS)
    target = os.path.join(OUT, name)
    img.save(target, "PNG", optimize=True)
    print(f"{os.path.basename(path)} → {target} ({kind}, {os.path.getsize(target)//1024} KiB)")
