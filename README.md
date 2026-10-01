--- README.md
# AutoScannerELM 🚗🎙️🤖

**Next-Generation OBD2 Car Diagnostic Scanner with AI Voice Control**
*Диагностический OBD2 сканер нового поколения с голосовым управлением на базе ИИ*

---

## 🇬🇧 English

**AutoScannerELM** is a modern Android application designed to interact with ELM327 OBD2 adapters for comprehensive vehicle diagnostics. What truly sets it apart is its **advanced AI-powered voice control**, allowing drivers to monitor their vehicle's health, read diagnostic trouble codes (DTCs), and receive real-time, plain-language explanations **completely hands-free**, keeping their eyes on the road.

### 🌟 Key Features
- 🎙️ **AI Voice Control**: Powered by Google AI Studio (Gemini), enabling natural language voice commands to query car status, interpret error codes, and get maintenance advice without touching the screen.
- 🔌 **ELM327 Compatibility**: Seamless Bluetooth/Wi-Fi connection with standard ELM327 OBD2 adapters.
- 📊 **Real-Time Diagnostics**: Read and clear DTCs, monitor live sensor data (RPM, coolant temperature, vehicle speed, etc.).
- 🧠 **Smart AI Diagnostics**: Beyond simple code reading, the AI explains what the error means, its potential severity, and recommended actions in a human-like voice.
- 📱 **Modern Android Architecture**: Built with 100% Kotlin, following MVVM architecture, Coroutines, and modern Android UI best practices.

### 🎙️ AI Voice Control in Action
1. **Activate**: Say *"Hey AutoScanner"* or tap the microphone icon on the dashboard.
2. **Ask**: Use natural language, e.g., *"What's the engine temperature?"*, *"Read any error codes?"*, or *"Explain code P0300 in simple terms"*.
3. **Respond**: The AI processes the request, fetches real-time data directly from the ELM327 adapter, and replies with a natural, human-like voice response.




🇷🇺 Русский

AutoScannerELM — это современное Android-приложение для взаимодействия с ELM327 OBD2 адаптерами и проведения комплексной диагностики автомобиля. Его главное преимущество — продвинутое голосовое управление на базе искусственного интеллекта, которое позволяет водителю следить за состоянием автомобиля, считывать коды ошибок и получать мгновенные рекомендации полностью без использования рук (hands-free), не отвлекаясь от дороги.

🌟 Ключевые особенности

🎙️ Голосовое управление на базе ИИ: Интеграция с Google AI Studio (Gemini) позволяет использовать естественные голосовые команды для запроса состояния автомобиля, расшифровки кодов ошибок и получения советов по обслуживанию без прикосновений к экрану.

🔌 Поддержка ELM327: Беспроводное подключение (Bluetooth/Wi-Fi) к стандартным ELM327 OBD2 адаптерам.

📊 Диагностика в реальном времени: Чтение и сброс кодов неисправностей (DTC), мониторинг живых данных с датчиков (обороты двигателя, температура, скорость и др.).

🧠 Умная ИИ-диагностика: ИИ не просто показывает код ошибки, но и объясняет простыми словами, что он означает, насколько это критично и какие действия рекомендуется предпринять, озвучивая ответ голосом.

📱 Современная архитектура Android: Написано на 100% Kotlin с использованием паттерна MVVM, Coroutines и современных UI-подходов.

🎙️ Как работает голосовое управление с ИИ

Активация: Скажите "Привет, Автосканер" или нажмите на иконку микрофона на главном экране.

Запрос: Задайте вопрос естественным языком, например: "Какая температура двигателя?", "Есть ли какие-то ошибки?" или "Объясни простыми словами, что значит ошибка P0300".

Ответ: ИИ обрабатывает запрос, получает актуальные данные напрямую от ELM327 адаптера и дает понятный голосовой ответ, позволяя вам держать руки на руле, а взгляд на дороге.


+++ README.md (修改后)
# AutoScannerELM (Автосканер) 🚗🎙️🤖

**Next-Generation OBD2 Car Diagnostic Scanner with AI Voice Control**
*Диагностический OBD2-сканер нового поколения с голосовым управлением на базе ИИ*

[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-green)](https://developer.android.com/jetpack/compose)
[![Gemini](https://img.shields.io/badge/AI-Gemini-orange)](https://ai.google.dev)
[![Platform](https://img.shields.io/badge/Android-API%2024%2B-brightgreen)](#)

---

## 🇬🇧 English

**AutoScannerELM** is a modern Android application designed to interact with ELM327 OBD2 adapters for comprehensive vehicle diagnostics. What truly sets it apart is its **advanced AI-powered voice control**, allowing drivers to monitor their vehicle's health, read diagnostic trouble codes (DTCs), and receive real-time, plain-language explanations **completely hands-free**, keeping their eyes on the road.

### 🌟 Key Features

- 🎙️ **AI Voice Control** — Powered by Google Gemini, enabling natural-language voice commands to query car status, interpret error codes, and get maintenance advice without touching the screen. Text-to-speech responses keep you focused on driving.
- 🔌 **ELM327 Compatibility** — Seamless Bluetooth connection with standard ELM327 OBD2 adapters, with selectable OBD protocols and foreground-service based connectivity.
- 📊 **Real-Time Diagnostics** — Read and clear DTCs (Modes 01–03), monitor live sensor data (RPM, coolant temperature, speed, throttle and more) via animated gauges, HUD displays and charts.
- 🧠 **Smart AI Diagnostics** — Beyond simple code reading, the built-in AI explains what an error means, how severe it is, and which actions are recommended — in plain language and spoken aloud.
- 🔍 **Advanced PID Discovery** — Adaptive sensor resolver, manufacturer-specific sensor repository and multi-ECU scanner uncover non-standard sensors on top of the standard OBD2 PID set.
- 📈 **Logging & History** — Record sensor sessions, browse logs history and visualize data with interactive graphs.
- 👤 **Profiles & Customization** — Configurable sensor profiles and custom PIDs, voice-assistant settings, multiple UI modes (tiles / minimal / HUD).
- 📱 **Modern Android Architecture** — 100% Kotlin, Jetpack Compose UI, MVVM with ViewModel & Coroutines, Room database, KSP, Robolectric/Roborazzi screenshot tests.

### 🎙️ AI Voice Control in Action

1. **Activate** — Say *"Hey AutoScanner"* or tap the microphone icon on the dashboard.
2. **Ask** — Use natural language, e.g. *"What's the engine temperature?"*, *"Read any error codes?"*, or *"Explain code P0300 in simple terms"*.
3. **Respond** — The AI processes the request, fetches real-time data directly from the ELM327 adapter, and replies with a natural, human-like voice response.

### 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM, Coroutines, Flow |
| AI | Google Gemini API |
| Speech | Voice input + Text-to-Speech |
| Connectivity | Bluetooth (Classic/BLE), ELM327 AT-command protocol |
| Storage | Room (SQLite), DataStore-style profiles |
| Build | Gradle (Kotlin DSL), KSP, Secrets plugin |

### 📂 Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt              # App entry point
├── ui/
│   ├── screens/                 # Home, AI Assistant, DTC scan/clear, Live sensors, Logs, HUD showcase
│   ├── components/              # Gauges, charts, dialogs, voice button, dashboard tiles
│   ├── theme/                   # Compose theming (colors, typography)
│   └── MainViewModel.kt         # MVVM state management
└── data/
    ├── elm327/                  # ELM327 manager, PID discovery, multi-ECU scanner, formula evaluator
    ├── ai/                      # Gemini service, DTC knowledge base, internet lookups
    ├── tts/                     # Text-to-speech engine
    ├── sound/                   # Sound alerts
    ├── db/                      # Room database (logs, profiles)
    ├── logging/                 # Sensor session logging
    └── service/                 # Foreground Bluetooth service
```

### ⚙️ Getting Started

**Prerequisites**
- JDK 11+ and Android Studio (latest stable)
- An Android device (API 24+) with Bluetooth — emulators cannot connect to real ELM327 adapters
- A cheap ELM327 OBD2 Bluetooth adapter (widely available online)
- A Google AI Studio / Gemini API key

**Setup**

1. Clone the repository:
   ```bash
   git clone <repo-url>
   cd autoscan-ai
   ```
2. Configure the API key. Copy `.env.example` and set your Gemini key, or provide it via the `secrets` Gradle plugin:
   ```
   GEMINI_API_KEY=your_actual_key
   ```
3. Open the project in Android Studio and let Gradle sync.
4. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```
5. Plug the ELM327 into your car's OBD2 port, pair it in Android Bluetooth settings (default PIN is usually `1234` or `0000`), then connect from the app.

### 🔐 Permissions

The app requests Bluetooth, location (required for classic Bluetooth pairing on Android), microphone (voice commands), notifications and foreground-service permissions — all essential for hands-free in-car diagnostics.

### 🤝 Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request. Please make sure new code follows the existing Kotlin/Compose style and passes `./gradlew test`.

### 📄 License

This project is provided as-is for educational purposes. See the repository for license details.

---

## 🇷🇺 Русский

**AutoScannerELM (Автосканер)** — это современное Android-приложение для взаимодействия с ELM327 OBD2-адаптерами и проведения комплексной диагностики автомобиля. Его главное преимущество — продвинутое **голосовое управление на базе искусственного интеллекта**, которое позволяет водителю следить за состоянием автомобиля, считывать коды ошибок и получать мгновенные рекомендации полностью без использования рук (hands-free), не отвлекаясь от дороги.

### 🌟 Ключевые особенности

- 🎙️ **Голосовое управление на базе ИИ** — интеграция с Google Gemini позволяет использовать естественные голосовые команды для запроса состояния автомобиля, расшифровки кодов ошибок и получения советов по обслуживанию без прикосновений к экрану. Ответы озвучиваются через синтез речи (TTS).
- 🔌 **Поддержка ELM327** — беспроводное Bluetooth-подключение к стандартным ELM327 OBD2-адаптерам с выбором OBD-протокола и стабильной работой через foreground-сервис.
- 📊 **Диагностика в реальном времени** — чтение и сброс кодов неисправностей (DTC, режимы 01–03), мониторинг живых данных датчиков (обороты, температура антифриза, скорость, дроссель и др.) на анимированных приборах, HUD-экране и графиках.
- 🧠 **Умная ИИ-диагностика** — ИИ не просто показывает код ошибки, а объясняет простыми словами, что он означает, насколько это критично и какие действия рекомендуется предпринять — и озвучивает ответ.
- 🔍 **Расширенное обнаружение PID** — адаптивный резолвер датчиков, база сенсоров по производителям и сканер нескольких ЭБУ находят нестандартные датчики сверх базового набора OBD2.
- 📈 **Журналы и история** — запись сессий датчиков, просмотр истории логов и интерактивные графики данных.
- 👤 **Профили и настройка** — настраиваемые профили и пользовательские PID, параметры голосового ассистента, несколько вариантов интерфейса (плитки / минимализм / HUD).
- 📱 **Современная архитектура Android** — 100% Kotlin, Jetpack Compose, MVVM с ViewModel и Coroutines, Room, KSP, скриншот-тесты Roborazzi.

### 🎙️ Как работает голосовое управление с ИИ

1. **Активация** — скажите *«Привет, Автосканер»* или нажмите на иконку микрофона на главном экране.
2. **Запрос** — задайте вопрос естественным языком, например: «Какая температура двигателя?», «Есть ли какие-то ошибки?» или «Объясни простыми словами, что значит ошибка P0300».
3. **Ответ** — ИИ обрабатывает запрос, получает актуальные данные напрямую от ELM327-адаптера и даёт понятный голосовой ответ, позволяя держать руки на руле, а взгляд — на дороге.

### 🛠️ Технологии

| Компонент | Технология |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Архитектура | MVVM, Coroutines, Flow |
| ИИ | Google Gemini API |
| Речь | Голосовой ввод + синтез речи (TTS) |
| Подключение | Bluetooth (Classic/BLE), AT-команды ELM327 |
| Хранение | Room (SQLite), профили и настройки |
| Сборка | Gradle (Kotlin DSL), KSP, плагин Secrets |

### 📂 Структура проекта

```
app/src/main/java/com/example/
├── MainActivity.kt              # Точка входа приложения
├── ui/
│   ├── screens/                 # Главный экран, ИИ-ассистент, скан/сброс DTC, живые датчики, логи, витрина HUD
│   ├── components/              # Приборы, графики, диалоги, голосовая кнопка, плитки дашборда
│   ├── theme/                   # Тема Compose (цвета, типографика)
│   └── MainViewModel.kt         # Управление состоянием (MVVM)
└── data/
    ├── elm327/                  # Менеджер ELM327, обнаружение PID, сканер ЭБУ, вычислитель формул
    ├── ai/                      # Сервис Gemini, база знаний DTC, поиск в интернете
    ├── tts/                     # Движок синтеза речи
    ├── sound/                   # Звуковые оповещения
    ├── db/                      # База данных Room (логи, профили)
    ├── logging/                 # Запись сессий датчиков
    └── service/                 # Foreground-сервис Bluetooth
```

### ⚙️ Установка и запуск

**Требования**
- JDK 11+ и Android Studio (последняя стабильная версия)
- Android-устройство (API 24+) с Bluetooth — эмулятор не может подключаться к реальным ELM327-адаптерам
- Любой доступный ELM327 OBD2 Bluetooth-адаптер
- Ключ API Google AI Studio / Gemini

**Шаги**

1. Клонируйте репозиторий:
   ```bash
   git clone <url-репозитория>
   cd autoscan-ai
   ```
2. Настройте ключ API. Скопируйте `.env.example` и укажите ключ Gemini (или передайте его через Gradle-плагин `secrets`):
   ```
   GEMINI_API_KEY=ваш_ключ
   ```
3. Откройте проект в Android Studio и дождитесь синхронизации Gradle.
4. Соберите и запустите:
   ```bash
   ./gradlew assembleDebug
   ```
5. Вставьте ELM327 в разъем OBD2 автомобиля, выполните сопряжение в настройках Bluetooth (стандартный PIN — обычно `1234` или `0000`) и подключитесь из приложения.

### 🔐 Разрешения

Приложение запрашивает разрешения Bluetooth, геолокации (необходимо для классического Bluetooth-сопряжения на Android), микрофона (голосовые команды), уведомлений и foreground-сервиса — всё это необходимо для работы hands-free-диагностики в автомобиле.

### 🤝 Вклад в проект

Мы рады вкладу сообщества! Создавайте issues и pull request'и. Новый код должен следовать существующему стилю Kotlin/Compose и проходить проверку `./gradlew test`.

### 📄 Лицензия

Проект предоставляется как есть в образовательных целях. Подробности лицензии — в репозитории.
