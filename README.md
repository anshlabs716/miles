# 🗺️ MILES

### A modern, customizable, privacy-focused activity tracker for Android.

<p align="center">
  <img src="https://img.shields.io/badge/STATUS-BETA-orange?style=for-the-badge" alt="Beta">
  <img src="https://img.shields.io/badge/DEVELOPMENT-ACTIVE-yellow?style=for-the-badge" alt="Active Development">
  <img src="https://img.shields.io/badge/PLATFORM-ANDROID-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/KOTLIN-100%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/JETPACK%20COMPOSE-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/MAPS-OPENSTREETMAP-7EBC6F?style=for-the-badge&logo=openstreetmap&logoColor=white" alt="OpenStreetMap">
  <img src="https://img.shields.io/badge/HEALTH%20CONNECT-SUPPORTED-4285F4?style=for-the-badge" alt="Health Connect">
  <img src="https://img.shields.io/badge/WEAR%20OS-COMING%20SOON-4285F4?style=for-the-badge&logo=wearos&logoColor=white" alt="Wear OS Coming Soon">
  <img src="https://img.shields.io/badge/PRIVACY-LOCAL--FIRST-8A2BE2?style=for-the-badge" alt="Privacy">
  <img src="https://img.shields.io/badge/LICENSE-TBD-lightgrey?style=for-the-badge" alt="License">
</p>

<p align="center">
  <a href="https://github.com/anshlabs716/miles">
    <img src="https://img.shields.io/github/stars/anshlabs716/miles?style=for-the-badge&logo=github" alt="GitHub Stars">
  </a>
  <a href="https://github.com/anshlabs716/miles/issues">
    <img src="https://img.shields.io/github/issues/anshlabs716/miles?style=for-the-badge" alt="Issues">
  </a>
  <a href="https://github.com/anshlabs716/miles/commits/main">
    <img src="https://img.shields.io/github/last-commit/anshlabs716/miles?style=for-the-badge" alt="Last Commit">
  </a>
  <a href="https://github.com/anshlabs716/miles">
    <img src="https://img.shields.io/github/repo-size/anshlabs716/miles?style=for-the-badge" alt="Repository Size">
  </a>
</p>

> ⚠️ **MILES IS CURRENTLY IN BETA.**
>
> MILES is **not finished**. Features, UI, APIs, architecture, data formats, and behavior are actively being developed and can change at any time estimated time till finished alone 2-3 weeks with a friend 1 week. 

---

# 🗺️ What is MILES?

**MILES** is a modern Android activity and step tracker designed to combine powerful fitness tracking with a highly customizable interface and a privacy-first approach.

The goal is to build a powerful alternative to major fitness platforms while avoiding unnecessary ecosystem lock-in.

MILES is designed around:

* 📍 Real GPS
* 👟 Real sensors
* 🗺️ Real maps
* 📊 Real statistics
* 🎨 Deep customization
* ♿ Accessibility
* ⌚ Wear OS support
* 🔗 External app/device connectivity
* 📤 Portable data
* 🔒 Privacy

**No fake stats. No fake heart rate. No fake GPS.**

If real sensor data isn't available, MILES should tell you instead of making something up.

---

# 🚶 Activity Tracking

MILES is designed to track real physical activity using Android's location and sensor systems.

### Activities

* 🚶 Walking
* 🏃 Running
* 🚴 Cycling
* 🥾 Hiking
* 🏋️ Other supported activities
* 🤖 Automatic activity detection

### Tracking data

* 👟 Steps
* 📏 Distance
* ⏱️ Duration
* ⚡ Current pace
* 📊 Average pace
* 🚀 Current speed
* 📈 Average speed
* ⛰️ Elevation
* ❤️ Heart rate when a real source is available
* 🗺️ GPS route
* 📍 Current location

### Smart tracking

* 🤖 Automatic activity detection
* ⏸️ Auto-pause
* ▶️ Auto-resume
* 📍 GPS jump detection
* 🧠 Activity fingerprinting
* 🔋 Battery-aware tracking
* 🎯 Adaptive GPS accuracy
* 🗺️ Route smoothing
* 🏠 Indoor/outdoor detection
* 📊 Movement confidence
* 🧠 Pattern recognition
* 🛣️ Route deviation detection
* 🔄 Smart activity recovery
* 📍 Smart GPS source fallback
* 🗺️ Automatic route classification
* 🚦 Context-aware tracking
* 📉 Activity anomaly detection

---

# 🧠 Activity Fingerprinting

MILES can learn the characteristics of frequently used routes and activities.

For example:

* Regular walking routes
* Regular running routes
* Common activity locations
* Typical activity durations
* Typical movement patterns

This intelligence is intended to work **locally on the device**.

Your movement patterns should not need to be uploaded to a server.

---

# 👟 Step Tracking

MILES is designed to use the device's real step sensors.

Where supported, MILES should use Android step-counter and step-detector sensors rather than attempting to manufacture step counts from GPS.

If the device does not provide a usable step sensor:

> **Step sensor unavailable**

No fake steps.

Step data can be used for:

* Daily steps
* Step goals
* Activity statistics
* Trends
* History
* Widgets
* Notifications
* Health Connect

---

# ❤️ Heart Rate

MILES does **not** generate fake heart-rate readings.

Possible real sources include:

* 📱 Supported phone sensors
* ⌚ Wear OS devices
* 📡 Bluetooth heart-rate monitors
* 🔗 Other compatible external sensors

If no valid heart-rate source exists:

> **Heart rate unavailable**

No fake BPM.

---

# 🗺️ Maps

MILES uses real interactive maps.

## 🌍 OpenStreetMap

OpenStreetMap is the default mapping foundation.

Planned functionality:

* 📍 Live device location
* 🔵 Current-location marker
* 🧭 Heading
* 🎯 Recenter
* 🔄 Follow-location mode
* 🔍 Zoom
* 🗺️ Route drawing
* 🛣️ Route recording
* 📏 Route distance
* 📌 Waypoints
* 🔥 Activity heatmaps
* 🔄 Route replay
* 🛣️ Route comparison
* 🛠️ Route building

## 🛰️ Map Types

MILES is designed to support:

* 🗺️ Standard maps
* 🛰️ Satellite maps
* ⛰️ Terrain maps

Additional providers may require API keys.

OpenStreetMap functionality should remain usable independently.

---

# 📍 Real-Time Location

The map should use the **actual device location**.

While tracking:

```text
GPS
 ↓
Android Location APIs
 ↓
MILES Tracking Engine
 ↓
Live Activity State
 ↓
Map + Statistics
```

The location should continuously update during active tracking.

MILES should handle:

* Location permissions
* Location disabled
* Poor GPS accuracy
* Background tracking
* GPS signal loss
* GPS jumps
* Battery optimization
* GPS reacquisition

---

# 📊 Statistics

MILES is designed to provide detailed activity statistics.

### Daily

* 👟 Steps
* 📏 Distance
* ⏱️ Active time
* 🏃 Activities
* ❤️ Heart rate when available
* 🔥 Calories where reliable data is available

### Trends

* 📈 Daily trends
* 📈 Weekly trends
* 📈 Monthly trends
* 📈 Yearly trends
* 🏆 Personal records
* 📊 Activity comparisons
* 🗓️ Activity calendar

### Activity details

* Duration
* Distance
* Pace
* Speed
* Elevation
* Steps
* Heart rate
* Route
* Splits

Every value should originate from real recorded data.

---

# 🎯 Goals & Challenges

MILES will support customizable activity goals.

Possible goals:

* 👟 Step goals
* 📏 Distance goals
* ⏱️ Active-time goals
* 🏃 Activity goals
* ❤️ Heart Point-style goals

Optional:

* 🏆 Fitness challenges
* 🔥 Streaks
* 🥇 Achievements
* ⭐ Milestones
* 🏅 Personal records
* 📊 Progress tracking

---

# 😴 Sleep Data

MILES can support sleep information from connected applications and supported integrations.

Sleep data can be displayed alongside activity information where supported.

---

# 🔗 Health Connect

MILES is designed to integrate with Android Health Connect.

Potential data:

* 👟 Steps
* 📏 Distance
* 🏃 Exercise
* ❤️ Heart rate
* 🔥 Calories
* 😴 Sleep
* Other supported activity records

Health Connect access remains optional and user-controlled.

---

# 🔗 Connected Apps

MILES can connect with other compatible applications.

Potential integrations include:

* Fitness applications
* Health applications
* Music applications
* Navigation applications
* Sensor applications

Users should control which applications MILES can interact with.

---

# 🎵 Music Integration

MILES works **with your music apps**, rather than replacing them.

Using Android MediaSession APIs where supported, MILES can interact with the currently active compatible music application.

Possible controls:

* ▶️ Play
* ⏸️ Pause
* ⏮️ Previous
* ⏭️ Next
* 🎵 Track information

The goal is to support whatever compatible music player the user actually uses.

---

# 📱 App Usage Integration

Where necessary, MILES may use Android app-usage visibility to determine which compatible music application is active.

This is intended to improve music integration rather than replace or override the user's chosen player.

Users should have control over this permission.

---

# 📡 Nearby Devices & Bluetooth

MILES is designed to communicate with nearby compatible devices.

Potential functionality:

* 📡 Nearby-device discovery
* 🔵 Bluetooth
* 🔗 Bluetooth Low Energy
* ❤️ Heart-rate sensors
* 👟 External activity sensors
* ⌚ Wear OS
* 📱 Device-to-device communication
* 🔄 Data synchronization
* 📡 External GNSS receivers

Possible supported external sensors:

* Bluetooth heart-rate monitors
* BLE foot pods
* Cycling sensors
* Smartwatches
* External GPS/GNSS receivers
* Other compatible fitness sensors

MILES may support selecting the preferred location source:

```text
Automatic
Phone GPS
External GNSS
```

---

# ⌚ Wear OS — Coming Soon

MILES will have a **separate Wear OS APK**.

> 🚧 **MILES Wear OS is coming soon.**

The Phone/Tablet and Wear OS apps are separate applications, but they are designed to **link together when the user chooses to pair them**.

```text
┌────────────────────────┐
│   MILES Phone/Tablet   │
│       APK              │
└───────────┬────────────┘
            │
      Pair / Link
            │
┌───────────▼────────────┐
│     MILES Wear OS      │
│       APK              │
└────────────────────────┘
```

### Phone → Watch

The Phone/Tablet application can eventually:

* 🔗 Find the MILES Wear OS app
* 🤝 Pair/unpair
* 🔋 View watch battery
* 📡 View connection status
* ⚙️ Manage watch settings
* 🏃 Configure activity settings
* 🎛️ Configure complications
* 🧩 Configure Tiles
* 🔔 Manage watch notifications
* 📺 Configure watch activity screens
* 🔄 Send supported settings

### Watch → Phone

The Wear OS application can eventually:

* 🔗 Find MILES Phone/Tablet
* 🤝 Pair/unpair
* 📡 View phone connection
* ⚙️ Manage supported phone settings
* ▶️ Start activities
* ⏸️ Pause activities
* ▶️ Resume activities
* ⏹️ Stop activities
* 📍 Show phone GPS status
* 📱 Control supported phone actions

### During an activity

Both applications can communicate activity state when linked.

For example:

```text
Phone starts activity
        ↓
Watch displays live activity
        ↓
Watch pauses activity
        ↓
Phone updates
```

Or:

```text
Watch starts activity
        ↓
Phone displays live activity
```

If the connection disappears, the Wear OS app should continue recording independently and transfer data when the connection is restored.

### Standalone operation

The apps should **not be permanently dependent on each other**.

* MILES Phone/Tablet works without Wear OS.
* MILES Wear OS works without the phone being continuously connected.
* Linking is optional.

### Wear OS features

Planned:

* 👟 Steps
* 📍 Watch GPS
* ❤️ Heart rate
* 🏃 Workouts
* ⏱️ Live statistics
* ⏸️ Auto-pause
* 🎵 Music controls
* 🧩 Complications
* 🧱 Tiles
* 🔋 Battery information
* 📐 Wear-specific DPI scaling
* 🔄 Activity synchronization

---

# 📤 Import & Export

MILES is designed around portable data.

| Format   | Purpose               |
| -------- | --------------------- |
| **JSON** | Full MILES backup     |
| **CSV**  | Data analysis         |
| **GPX**  | GPS routes            |
| **TCX**  | Workout/activity data |

Planned functionality:

* 📥 Import activities
* 📤 Export activities
* 📦 Full backup
* 🔄 Full restore
* 🗺️ GPX import/export
* 🏃 TCX import/export
* 📊 CSV export
* 🧾 JSON backup
* 🔍 Import validation
* ⚠️ Import error reporting
* 🔄 Google Fit-compatible data where practical

Your data should not be trapped inside MILES.

---

# 📸 Sharing

MILES can share activities with:

* 📊 Activity statistics
* 🗺️ Routes
* 📸 Photos
* 📏 Distance
* ⏱️ Duration
* ⚡ Pace
* 👟 Steps

The goal is to provide useful activity cards without creating a MILES social network.

---

# 🏠 Widgets

Planned Android widgets:

* 👟 Steps
* 📏 Distance
* 🎯 Goals
* 🏃 Current activity
* 📊 Statistics
* 📅 Recent activity
* ⚡ Quick-start activity
* ⌚ Device information

---

# 🔔 Live Activities

MILES includes an optional Live Activities system.

Settings:

```text
Settings
└── Notifications
    └── Live Activities
        ├── ON
        └── OFF
```

When enabled, an active workout can expose live information outside the main app.

Possible information:

* 👟 Steps
* 📏 Distance
* ⏱️ Duration
* ⚡ Pace
* 🚀 Speed
* ❤️ Heart rate when available
* 📍 Tracking status
* ⏸️ Pause
* ▶️ Resume
* ⏹️ Stop

Live tracking information should only appear while an activity is actually running.

---

# 🎨 Themes

MILES includes multiple base themes.

### ☀️ Light

Bright, clean Material-style interface.

### 🌆 Twilight

Dark blue/purple evening-style interface.

### 🖤 AMOLED

Completely black OLED-focused interface.

### ❤️ AMOLED Red

Completely black interface with red accents and a red MILES icon.

### 📱 Stock

Follows the device's system appearance and colors where possible.

---

# 🧊 Liquid Glass

Liquid Glass is **NOT a theme**.

It is an optional visual overlay that can be enabled on top of any base theme.

Examples:

```text
AMOLED + Liquid Glass
```

```text
AMOLED Red + Liquid Glass
```

```text
Twilight + Liquid Glass
```

```text
Light + Liquid Glass
```

```text
Stock + Liquid Glass
```

Liquid Glass can affect:

* Cards
* Panels
* Bottom sheets
* Dialogs
* Navigation surfaces
* Widgets
* Other supported UI elements

Readability and accessibility remain priorities.

---

# 🎨 Icon Customization

MILES supports customizable launcher icons.

Planned variants:

* MILES Default
* Light
* AMOLED
* AMOLED Red
* Stock

Changing the icon setting should actually change the Android launcher icon.

---

# ♿ Accessibility

Accessibility is intended to be fully functional.

Planned controls:

* 🔠 Larger text
* 🅰️ Bold text
* 🔳 Increased contrast
* 👁️ Improved readability
* 🎨 Color-vision-friendly options
* 🎞️ Reduced motion
* 🚫 Disable animations
* 👆 Larger touch targets
* 🔊 TalkBack support
* 📖 Screen-reader labels
* 📈 Accessible charts
* 🧭 Improved navigation
* 📱 Larger UI elements
* ⚙️ Adjustable graph readability

Accessibility settings should modify the actual UI and behavior.

They should **not** be placeholder switches that do nothing.

---

# 🧪 MILES Studio

**MILES Studio** is the advanced developer/technical environment.

It belongs inside:

```text
Settings
└── Developer options
    └── MILES Studio
```

Normal MILES settings stay clean.

Studio exposes advanced technical controls and diagnostics.

### GPS & GNSS

* 📍 GPS accuracy mode
* ⏱️ Location update interval
* 📏 Minimum update distance
* 🛰️ GNSS status
* 📡 Satellite count
* 📍 Location provider
* 🎯 Raw GPS accuracy
* ⛰️ Altitude source
* 📡 External GNSS source
* 📊 GNSS diagnostics

### Tracking engine

* 🧠 Activity-recognition confidence
* ⏸️ Auto-pause sensitivity
* 📍 GPS filtering
* 🗺️ Route smoothing
* 🔋 Battery tracking mode
* 📋 Background tracking diagnostics
* 🧾 Location update logs
* 🧪 Fake/test activity data

### Sensors

* 👟 Step sensor diagnostics
* ❤️ Heart-rate diagnostics
* 📡 Raw sensor data
* 📈 Sensor update frequency
* 🔗 Sensor source selection
* ⏱️ Connection latency
* 📦 Packet-loss diagnostics

### Bluetooth

* 📶 BLE scan logs
* 🔗 Connected GATT services
* 📦 Sensor packet diagnostics
* 🔄 Connection logs
* 📡 Device diagnostics

### Maps

* 🗺️ Debug map overlay
* 📍 Location source
* 📡 GPS source diagnostics
* 🛰️ Map-provider diagnostics
* 💾 Cached-map management

### Media

* 🎵 MediaSession diagnostics
* 📱 Active-player diagnostics
* 🔗 Music integration diagnostics
* 📋 Usage-access diagnostics

### System

* 🔋 Battery profiling
* ⚡ Performance profiling
* 🧾 Tracking event log
* 💾 Database diagnostics
* 🔗 Health Connect diagnostics
* 🔔 Notification diagnostics
* ⌚ Wear OS diagnostics
* 📤 Export debug logs

---

# 📐 MILES Studio — Distance Calculator

The Phone/Tablet version of MILES Studio includes a technical Distance Calculator.

### Inputs

* 📍 Point A → Point B
* 📍 Multiple points
* 🧭 Manual coordinates
* 🗺️ Points drawn on a map
* 📥 Imported GPX
* 📥 Imported TCX
* 📊 Recorded activity
* 📍 Selected GPS points
* 🏃 Activity splits

### Calculations

* 📏 Straight-line distance
* 🛣️ Route distance
* 📐 2D distance
* ⛰️ 3D distance
* 📈 Elevation gain
* 📊 Calculated vs recorded distance

### Technical information

* Total distance
* Point count
* Straight-line distance
* 3D distance
* Elevation gain
* Average point spacing
* Selected GPS points
* Activity split distances

### Units

* km
* m
* mi
* ft

### Output

Results can be:

* 📋 Copied
* 📤 Exported
* 🔄 Compared

**The Distance Calculator is Phone/Tablet only.**

---

# 📱 Phone & Tablet

The main MILES APK is designed for:

* 📱 Phones
* 📲 Tablets
* 🔄 Foldables
* ↔️ Landscape
* 🖥️ Large screens

The UI should adapt rather than simply stretching the phone interface.

---

# 🔒 Privacy

MILES follows a **local-first** philosophy.

MILES does not intend to require:

* ❌ MILES account
* ❌ Email/password
* ❌ Forced cloud account
* ❌ Subscription
* ❌ Advertising
* ❌ Analytics
* ❌ Forced Google account

Activity data should remain on the device unless the user explicitly chooses to:

* Export it
* Back it up
* Share it
* Sync it through an enabled integration

---

# 🏗️ Technology

MILES currently uses:

* **Kotlin**
* **Jetpack Compose**
* **Android SDK**
* **Gradle**
* **Android Location APIs**
* **Android Sensor APIs**
* **OpenStreetMap-based mapping**

The architecture is intended to separate:

* UI
* Tracking
* Sensors
* Location
* Maps
* Data
* Import/export
* Health Connect
* Bluetooth
* Media controls
* Notifications
* Widgets
* Wear communication
* MILES Studio

---

# 📁 Project Structure

```text
miles/
├── app/
│   └── Android application
├── gradle/
│   └── Gradle configuration
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── metadata.json
├── .env.example
└── .gitignore
```

The repository is currently being expanded from its initial Android project structure.

---

# 🛠️ Build MILES

You can compile MILES yourself if you want to experiment with the project.

## Requirements

* Android SDK
* JDK
* Git
* A compatible Android development environment
* Gradle wrapper included with the repository

## Clone

```bash
git clone https://github.com/anshlabs716/miles.git
cd miles
```

## Linux

Make the Gradle wrapper executable:

```bash
chmod +x gradlew
```

Build the debug APK:

```bash
./gradlew assembleDebug
```

The APK will normally be generated in:

```text
app/build/outputs/apk/debug/
```

## Windows

```powershell
gradlew.bat assembleDebug
```

## Install with ADB

Connect your Android device and enable USB debugging.

Check the device:

```bash
adb devices
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Build paths and APK names may change as the project develops.

---

# 🧪 Development Status

| Area                 | Status               |
| -------------------- | -------------------- |
| Android project      | 🟢 Started           |
| Kotlin               | 🟢 Active            |
| Jetpack Compose      | 🟢 Active            |
| Core UI              | 🟡 Developing        |
| GPS                  | 🟡 Developing        |
| Real location        | 🟡 Developing        |
| Step tracking        | 🟡 Developing        |
| Maps                 | 🟡 Developing        |
| OpenStreetMap        | 🟡 Developing        |
| Satellite maps       | 🟡 Planned           |
| Terrain maps         | 🟡 Planned           |
| Activity tracking    | 🟡 Developing        |
| Statistics           | 🟡 Developing        |
| Smart Tracking       | 🟡 Developing        |
| Themes               | 🟡 Developing        |
| Liquid Glass         | 🟡 Developing        |
| Icon customization   | 🟡 Developing        |
| Accessibility        | 🟡 Developing        |
| Health Connect       | 🟡 Developing        |
| Bluetooth sensors    | 🟡 Planned           |
| Import/export        | 🟡 Developing        |
| MILES Studio         | 🟡 Developing        |
| Distance Calculator  | 🟡 Developing        |
| Live Activities      | 🟡 Developing        |
| Widgets              | 🟡 Planned           |
| Music integration    | 🟡 Planned           |
| Phone ↔ Wear linking | 🟡 Planned           |
| **Wear OS APK**      | 🟠 **COMING SOON**   |
| Stable release       | 🔴 Not yet available |

---

# 🗺️ Roadmap

## Phase 1 — Foundation

* [x] Android project
* [x] Kotlin
* [x] Gradle setup
* [x] Initial manifest permissions
* [ ] Core architecture

## Phase 2 — Real Tracking

* [ ] Real step counting
* [ ] Real GPS
* [ ] Live location
* [ ] Walking
* [ ] Running
* [ ] Cycling
* [ ] Hiking
* [ ] Distance
* [ ] Pace
* [ ] Speed
* [ ] Elevation
* [ ] Background tracking
* [ ] Smart Tracking

## Phase 3 — Maps

* [ ] OpenStreetMap
* [ ] Live location marker
* [ ] Route recording
* [ ] Standard maps
* [ ] Satellite maps
* [ ] Terrain maps
* [ ] Route builder
* [ ] Route replay
* [ ] Waypoints
* [ ] Heatmaps

## Phase 4 — MILES Experience

* [ ] Light
* [ ] Twilight
* [ ] AMOLED
* [ ] AMOLED Red
* [ ] Stock
* [ ] Liquid Glass
* [ ] Icon customization
* [ ] Full accessibility
* [ ] Widgets
* [ ] Live Activities
* [ ] Music controls
* [ ] Sharing

## Phase 5 — Data

* [ ] Activity history
* [ ] Statistics
* [ ] JSON
* [ ] CSV
* [ ] GPX
* [ ] TCX
* [ ] Health Connect
* [ ] Google Fit compatibility
* [ ] Full backup/restore

## Phase 6 — Advanced

* [ ] Activity Fingerprinting
* [ ] Bluetooth sensors
* [ ] External GNSS
* [ ] MILES Studio
* [ ] Distance Calculator
* [ ] Developer diagnostics
* [ ] Advanced sensor tools

## Phase 7 — Wear OS

* [ ] **MILES Wear OS APK — COMING SOON**
* [ ] Wear activity tracking
* [ ] Watch GPS
* [ ] Heart-rate sensors
* [ ] Workouts
* [ ] Complications
* [ ] Tiles
* [ ] Music controls
* [ ] Wear DPI/scaling
* [ ] Phone ↔ Watch pairing
* [ ] Watch → Phone controls
* [ ] Phone → Watch controls
* [ ] Live activity linking
* [ ] Activity transfer
* [ ] Offline recording
* [ ] Automatic reconnection

## Phase 8 — Polish

* [ ] Performance optimization
* [ ] Battery optimization
* [ ] Accessibility testing
* [ ] Pixel testing
* [ ] Tablet testing
* [ ] Wear OS testing
* [ ] Release builds
* [ ] Stable release

---

# 🚧 BETA WARNING

**MILES is currently BETA and is NOT FINISHED.**

This is an active development project.

Expect:

* 🐛 Bugs
* 💥 Crashes
* 🚧 Missing features
* 🎨 UI changes
* 🔌 Broken integrations
* 🧪 Experimental functionality
* 🔄 Data-model changes
* 🔧 API changes

Features listed in this README may be:

* Implemented
* Partially implemented
* Being tested
* Planned
* Experimental

Always keep backups of important activity data.

---

# 🤝 HELP ME BUILD MILES

I'm building MILES while learning Android development, and I'm still struggling with some parts of making a full Android application by myself.

I would **really appreciate help with the coding**.

Things I may need help with include:

* Kotlin
* Jetpack Compose
* Android architecture
* Gradle
* GPS/location APIs
* Step sensors
* Activity recognition
* Background tracking
* Maps
* Bluetooth/BLE
* Health Connect
* Wear OS
* Accessibility
* UI/UX
* Testing
* Performance
* Battery optimization
* Data import/export

### You can help by:

* 🐛 Reporting bugs
* 💡 Suggesting features
* 🔧 Fixing code
* 🧪 Testing builds
* 📖 Improving documentation
* ♿ Testing accessibility
* 🗺️ Improving map functionality
* 📍 Helping with GPS
* 👟 Helping with sensor tracking
* ⌚ Helping with Wear OS
* 🎨 Improving the UI

I'm learning as I build this project, so **please don't be afraid to point out problems or show me better ways to implement something.**

Every contribution helps.

---

# 🤝 Contributing

If you want to contribute:

1. Fork the repository.
2. Create a branch.
3. Make your changes.
4. Test your changes.
5. Open a pull request.

For major changes, please open an issue first.

---

# 📜 License

**License: TBD**

The final license will be selected before the first stable release.

---

# 💙 Philosophy

> **Your activity data should belong to you.**

MILES aims to provide powerful activity tracking without forcing users into an account, subscription, advertising ecosystem, or unnecessary cloud service.

**Real data. Real tracking. Real maps. Your device. Your data.**

---

# ⭐ MILES

### Built for Android. Built with Kotlin. Built while learning.

**Phone/Tablet APK:** 🟡 Beta / Active Development

**Wear OS APK:** 🟠 Coming Soon

**Phone ↔ Wear OS:** 🔗 Designed to link and communicate when paired

Made by **AnshLabs716**.

If you can help make MILES better, **please do.** ❤️
