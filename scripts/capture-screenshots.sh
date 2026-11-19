#!/usr/bin/env bash
# Снимает скриншоты приложения через adb (телефон в отладке или шиму).
# Использование: ./scripts/capture-screenshots.sh [серийник]
set -euo pipefail
ADB=${ADB:-adb}
DEV=${1:-}
OUT="docs/screenshots"
mkdir -p "$OUT"
if [ -z "$DEV" ]; then DEV=$($ADB devices | awk 'NR>1 && $2=="device"{print $1; exit}'); fi
[ -z "$DEV" ] && { echo "Нет устройства adb"; exit 1; }

shot() { "$ADB" -s "$DEV" exec-out screencap -p > "$OUT/$1.png"; echo "  $OUT/$1.png"; }
tap() { "$ADB" -s "$DEV" shell input tap "$1" "$2"; sleep 1; }

"$ADB" -s "$DEV" shell am start -n ru.leather.calc/.MainActivity >/dev/null
sleep 2
shot 01-leather
tap 540 2072   # Кожа
shot 02-pieces
tap 541 2072   # Раскладка
shot 03-layout
tap 743 2072   # Шов
shot 04-stitch
tap 945 2072   # Ещё
sleep 1
shot 05-more
echo "Готово. Сожмите PNG: python3 scripts/optimize-screenshots.py"
