# Скриншоты

Снимаются с живого приложения: `scripts/capture-screenshots.sh` (adb) или вручную.
Актуальный способ на телефоне без adb — через Shizuku:

```bash
screencap -p /sdcard/Download/DSH-Go/shots/01-leather.png
```

Нужны шесть кадров:

| Файл | Экран | Что должно быть видно |
|---|---|---|
| 01-leather.png | Кожа | витрина с площадью и ценой, шаблоны, список деталей |
| 02-layout.png | Раскладка | схема листа с деталями, заполнение, кнопки PDF/SVG/DXF |
| 03-costs.png | Деньги | каталог фурнитуры, смета, цена |
| 04-learn.png | Учебник | прогресс, урок с фото |
| 05-wizard.png | Мастер | шаг «Изделие» с шаблонами и эскизом |
| 06-measure.png | Замер шкуры | фото с контуром и эталоном, площадь |

После съёмки: `python3 scripts/optimize-screenshots.py`, затем вставить в README:

```markdown
| Кожа | Раскладка |
|---|---|
| ![Кожа](docs/screenshots/01-leather-small.png) | ![Раскладка](docs/screenshots/02-layout-small.png) |
```
