# Как выложить на GitHub

## Вариант 1 — через gh (нужна авторизация)

```bash
gh auth login                    # или: gh auth login --web
gh repo create leather-calculator --public --source=. --remote=origin --push
gh release create v1.19 app/build/outputs/apk/release/app-release.apk \
   --title "1.19" --notes "Калькулятор кожи: шаблоны, мастер, PDF/SVG/DXF, замер шкуры"
```

## Вариант 2 — вручную

1. Создайте пустой репозиторий на github.com (без README).
2. В каталоге проекта:

```bash
git remote add origin https://github.com/<логин>/leather-calculator.git
git branch -M main
git push -u origin main
```

3. APK приложите в разделе Releases (файл `leathercalc-1.19.apk`).

## Что уже сделано в репозитории

- `.gitignore`: не попадают `build/`, `.gradle/`, `local.properties`, `*.jks`, `*.apk`.
- Пароли от ключа подписи вынесены в `~/.gradle/gradle.properties` — в репозитории их нет.
- README (что умеет, как собирать), LICENSE (MIT), docs/FEATURES.md (полная спека),
  Gradle wrapper 8.13.
