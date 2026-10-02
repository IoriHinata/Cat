# Animal Collector

**Animal Collector** — Android-приложение на русском языке с WebView-интерфейсом и небольшим Python-движком коллекции.

## Запуск

1. Откройте корневую папку в Android Studio Ladybug или новее.
2. Выберите JDK 17, установите Android SDK Platform 35 и синхронизируйте Gradle.
3. Запустите конфигурацию `app` на устройстве Android 8.0+ (API 26).
4. Для сборки выполните `gradle :app:assembleDebug` (либо используйте Android Studio).

Проект использует тонкий Java Android-хост и WebView. UI и прикладная логика находятся в `app/src/main/assets/`: HTML/CSS/JavaScript рисуются в WebView, а `engine.py` загружается через `fetch` и выполняется Pyodide. Разрешение камеры запрашивается только по действию пользователя через ограниченный Android API-мост.

### Python и WebView

APK содержит нативный Android-хост и исходник Python; Python компилируется и исполняется в рантайме Pyodide/CPython, работающем в WebAssembly внутри WebView. Gradle лишь упаковывает `engine.py` как asset — Python не компилируется в Java, Kotlin или DEX.

Локальные файлы публикуются только через `WebViewAssetLoader` по `https://appassets.androidplatform.net/assets/`, а не через `file://` или встроенный HTTP-сервер. JavaScript вызывает Python исключительно через `callPython(name, ...args)` и JSON; Python экспортирует `initialize`, `dispatch`, `get_state`, `export_state` и `import_state`, поэтому интерфейс не изменяет внутреннее состояние напрямую.

Сейчас Pyodide загружается с CDN, поэтому первый запуск требует сеть. Для полностью офлайн-режима необходимо поместить в `assets` дистрибутив Pyodide/WASM и требуемые пакеты, а затем заменить CDN `indexURL` на локальный путь.

## Архитектура

`MainActivity` не содержит прикладной логики: он создаёт WebView, выдаёт разрешение камеры и безопасно публикует APK-assets. `index.html` — интерфейс, `app.js` — адаптер и рендеринг, а `engine.py` — единственный владелец состояния. Состояние сохраняется в `localStorage` как JSON-экспорт Python-движка.

## Приватность и ограничения 1.0

Геолокация не используется. Система не содержит рекламы, платежей или loot boxes. Камера не получает и не передаёт изображения в текущем прототипе: кнопка демонстрирует узкий, контролируемый пользователем Android permission flow.

## Тестирование

Проверки Python-API можно запускать обычным интерпретатором Python; Android-сборка проверяет упаковку assets в APK.

## Roadmap

* Упаковать проверенную версию Pyodide/WASM и требуемые пакеты для офлайн-запуска.
* Добавить пользовательский flow захвата фотографии после проектирования отдельного безопасного Android API.
* Расширить Python-движок правилами коллекции и миграциями экспортированного состояния.

## APK через GitHub Actions

После push в ветку `work`/`main`, pull request или ручного запуска workflow **Build Android APK** собирает `assembleDebug` на JDK 17 и публикует `animal-collector-debug-apk` как artifact на 14 дней. APK можно скачать на странице **Actions → нужный запуск → Artifacts**.

## Локальный профиль и хранение

Имя исследователя и карточки хранятся как JSON в `localStorage` origin `appassets.androidplatform.net`. В приложении нет входа, облачного аккаунта или передачи карточек. При очистке данных или удалении приложения Android/WebView может удалить это локальное состояние; используйте экспорт состояния для будущего резервного копирования.
