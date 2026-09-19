# 🗺️ MILES

### A modern, customizable, privacy-focused activity tracker for Android.

<p align="center">
  <img src="https://img.shields.io/badge/STATUS-1.0.1-success?style=for-the-badge" alt="Version 1.0.1">
  <img src="https://img.shields.io/badge/DEVELOPMENT-ACTIVE-yellow?style=for-the-badge" alt="Active Development">
  <img src="https://img.shields.io/badge/PLATFORM-ANDROID-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/KOTLIN-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/COMPOSE-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/LICENSE-GPLv3-blue?style=for-the-badge" alt="GPLv3 License">
</p>

<p align="center">
  <a href="https://github.com/anshlabs716/miles-wearos"><img src="https://img.shields.io/badge/⌚%20MILES%20WEAR%20OS-Companion-black?style=for-the-badge" alt="MILES Wear OS"></a>
</p>

**MILES** is a modern Android activity and step tracker focused on real sensor data, GPS tracking, portability, customization, and a clean UI.

> **Status:** Version 1.0.1 — first official APK release.

## ✨ Features

- Real step counting
- Real GPS tracking
- Walking, running, cycling, hiking, and workout recording
- Local activity history and statistics
- OpenStreetMap-based mapping
- Bluetooth Low Energy sensor support
- Health Connect integration
- JSON, CSV, GPX, and TCX data support
- Optional Fitness Pet 🐕
- Lazy Days
- Movement reminders
- MILES Studio diagnostics
- Themes and launcher icon customization
- Accessibility options
- Privacy-focused, local-first design

MILES does not intentionally generate fake GPS, heart-rate, or step data.

## ⌚ Wear OS

The Wear OS companion is developed separately to keep the main Android project clean.

**MILES Wear OS → https://github.com/anshlabs716/miles-wearos**

The watch project brings MILES tracking, sensors, workout controls, and phone ↔ watch syncing to Wear OS.

## 🔒 Privacy

MILES is designed to work without requiring:

- A MILES account
- A MILES cloud service
- A subscription
- Advertising
- Forced analytics
- A forced Google account

Your activity data stays under your control.

## 🛠️ Technology

- Kotlin
- Jetpack Compose
- Material 3
- Android SDK
- Sensor & Location APIs
- Bluetooth Low Energy / GATT
- Room
- Health Connect
- Coroutines
- OpenStreetMap-based mapping

## 📦 Build

Requirements: **JDK 17, Android SDK, Git**

```bash
git clone https://github.com/anshlabs716/miles.git
cd miles
chmod +x gradlew
./gradlew assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 🚧 Development

| Area | Status |
| --- | --- |
| Android app | **1.0.1 released** |
| Step tracking | Implemented |
| GPS tracking | Implemented |
| BLE sensors | Foundation implemented |
| Health Connect | In development |
| MILES Studio | Active development |
| Wear OS | **Separate project** |

## 🗺️ Roadmap

- More sensor support
- Better GPS filtering and route smoothing
- Health Connect completion
- Portable backup/restore
- More accessibility polish
- Continued MILES Studio development
- Wear OS companion development

## 📄 License

**GPLv3**

---

**Real data. Real tracking. Portable data. Your device. Your data.**

Made by **AnshLabs716**.

**Android + Linux only.**
