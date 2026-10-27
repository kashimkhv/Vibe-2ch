# Двач — клиент 2ch.hk на Kotlin + Compose

Переписанный с нуля Android-клиент двача. Оригинал — [vortexwolf/2ch-Browser](https://github.com/vortexwolf/2ch-Browser)
(Java, MVP + Moxy, RxJava 2, support-library 27, последний коммит — июль 2019).
Здесь тот же продукт, но на актуальном стеке 2026 года: Kotlin 2.4, Compose, корутины, Hilt, Room.

## Стек

| Слой | Чем сделано |
|---|---|
| Язык | Kotlin 2.4.21 (K2), JDK 21, jvmTarget 17 |
| UI | Jetpack Compose (BOM 2026.09.00), Material 3, dynamic color |
| Навигация | Navigation Compose с type-safe маршрутами (`@Serializable`) |
| Асинхронность | Coroutines + Flow / StateFlow, без RxJava |
| DI | Hilt 2.60 (KSP, без kapt) |
| Сеть | Retrofit 3 + OkHttp 5 + kotlinx.serialization |
| Картинки | Coil 3 (общий OkHttp-клиент и кэш, анимированные GIF) |
| Локальные данные | Room 2.8 (избранное, история) + DataStore (настройки) |
| Сборка | AGP 9.4.1, Gradle 9.8.2, version catalog, configuration cache |

`minSdk 26`, `compileSdk 37`, `targetSdk 36`.

## Про API: старые эндпоинты мертвы

Приложение 2019 года ходило на `/boards.json`, `/makaba/mobile.fcgi?task=get_boards`,
`/makaba/posting.fcgi`. В 2026 всё это отдаёт `404`. Живые эндпоинты, на которых
работает этот клиент:

| Что | Эндпоинт |
|---|---|
| Список досок | `GET /api/mobile/v2/boards` |
| Список тредов | `GET /{board}/index.json?page=N` |
| Каталог | `GET /{board}/catalog.json` (пока не используется) |
| Тред целиком | `GET /{board}/res/{num}.json` |
| Постинг | `POST /user/posting` (multipart) — **не реализован** |
| Капча | `/api/captcha/emoji/id`, `/api/captcha/emoji/click` — **не реализован** |

2ch.hk за Cloudflare, поэтому запросы идут с UA десктопного браузера
(см. `NetworkModule`).

## Сборка

Нужны JDK 21 и Android SDK с платформой 37.

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # юнит-тесты
./gradlew :app:assembleRelease        # R8 + shrinkResources, нужен keystore
```

`local.properties` с `sdk.dir` создаётся Android Studio автоматически и в git не попадает.

Release-сборка подписывается ключом из вашего хранилища: добавьте `signingConfigs`
в `app/build.gradle.kts`. Оригинальный `applicationId` (`ua.in.quireg.chan`) взять
нельзя — он занят в Google Play, поэтому здесь `dev.dvach.client`.

## Структура

```
app/src/main/java/dev/dvach/client/
├── data/
│   ├── remote/          # Retrofit-интерфейс и DTO движка
│   ├── local/           # Room: избранное и история
│   ├── settings/        # DataStore: тема, превью, базовый URL
│   └── ChanRepository   # DTO → доменные модели, абсолютные URL файлов
├── domain/model/        # Board, ThreadSummary, Post, Attachment
├── di/                  # Hilt-модули (сеть, БД, биндинги)
├── ui/
│   ├── boards/          # список досок с поиском и группировкой по категориям
│   ├── threads/         # список тредов с пагинацией
│   ├── thread/          # тред: посты, картинки, цитаты, закладки
│   ├── bookmarks/       # избранное
│   ├── settings/        # тема, превью, «о приложении»
│   ├── components/      # HtmlText (рендер разметки постов без WebView), состояния
│   └── navigation/      # NavHost и type-safe маршруты
└── util/                # форматирование размеров, дат, ссылок
```

### HtmlText

Посты приходят как HTML. WebView не используется: свой парсер поддерживает
`<br> <b> <i> <u> <s> <a> <span style="color"> <code> <sub> <sup>`, спойлеры
(раскрываются тапом) и кликабельные цитаты `>>123456` со скроллом к посту.

## Что уже работает

- доски: список, поиск, группировка по категориям, обновление;
- треды: список с превью ОП-поста, пагинация, счётчики постов/файлов;
- тред: все посты, картинки и webm-превью, переход по цитатам, копирование ссылки,
  открытие в браузере, избранное, история посещений;
- темы: системная/светлая/тёмная, Material You, отключение превью;
- офлайн-кэш HTTP-ответов (OkHttp, 64 МБ).

## Чего пока нет

- **постинг и капча** (эмодзи-капча через `/api/captcha/emoji/*`) — следующий шаг;
- свой просмотрщик картинок (сейчас открывается во внешнем браузере);
- плеер webm;
- каталог тредов (`catalog.json`);
- поиск по постам внутри треда, «мой пост», автообновление треда;
- тёмная тема для самих картинок/спойлеров — только UI.

## Тесты

`./gradlew :app:testDebugUnitTest` — 7 тестов:

- `DvachJsonTest` — парсинг реальных ответов API (фикстуры в `app/src/test/resources`),
  ловит переименования полей и неожиданные `null`;
- `FormattersTest` — размеры файлов, счётчики, ссылки, очистка HTML.

## Лицензия

Код оригинала — MIT. Здесь та же лицензия, см. `LICENSE`.
