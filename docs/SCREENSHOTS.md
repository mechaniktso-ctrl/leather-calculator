# Скриншоты

Снимаются с телефона штатно: **Power + Volume Down**. Снимки попадают в
`/sdcard/DCIM/Screenshots/`.

Затем импорт:

```bash
python3 scripts/import-screenshots.py
```

Скрипт берёт файлы за последние 2 дня, распознаёт текст (Tesseract, rus+eng),
определяет экран по маркерам и кладёт сжатые PNG в `docs/screenshots/`:
`leather-N.png`, `layout-N.png`, `costs-N.png`, `learn-N.png`, `wizard-N.png`,
`measure-N.png`, `projects-N.png`, `stitch-N.png`, `stock-N.png`.

Если Tesseract на телефоне не установлен: `apt-get install tesseract-ocr tesseract-ocr-rus`.

## Нужные кадры

| Экран | Что должно быть видно |
|---|---|
| Кожа | витрина с площадью и ценой, шаблоны, детали |
| Раскладка | схема листа, заполнение, кнопки PDF/SVG/DXF |
| Деньги | каталог фурнитуры, смета, цена |
| Учебник | прогресс и урок с фото |
| Мастер | шаг «Изделие» с шаблонами и эскизом |
| Замер шкуры | фото с контуром и эталоном, площадь |

## Старый способ (adb)

`scripts/capture-screenshots.sh` — проходит по экранам через `adb shell input tap`
и делает `screencap`. Нужен подключённый adb.
