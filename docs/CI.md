# Сборка в GitHub Actions

`.github/workflows/android.yml` собирает проект на каждом пуше в `main`, на pull request,
на теги `v*` и по кнопке (workflow_dispatch).

Что делает: JDK 21 → Android SDK 35 → юнит-тесты → debug APK → release APK (если задан
ключ) → публикация APK и отчёта тестов в артефакты; на теге `v*` — GitHub Release
с APK и автоматическими release notes.

## Секреты для подписи release

Добавьте в Settings → Secrets and variables → Actions:

| Секрет | Что это |
|---|---|
| `KEYSTORE_BASE64` | keystore в base64: `base64 -w0 keys/leathercalc-release.jks` |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | алиас ключа (`leathercalc`) |
| `KEY_PASSWORD` | пароль ключа |

Без этих секретов CI соберёт только debug APK и не упадёт.

## Как выпустить новую версию

```bash
# поднять versionCode/versionName в app/build.gradle
git commit -am "1.20"
git tag v1.20
git push --tags
```

Через несколько минут в Releases появится сборка.
