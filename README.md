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
  ![GPLv3 License](https://img.shields.io/badge/License-GPLv3-blue.svg)
</p>

**MILES** is a modern Android activity and step tracker focused on real sensor data, GPS tracking, portable data, customization, and a clean UI.

> **Status:** Beta / active development. The project is not a stable release yet.

## What MILES is built to do

- Real step counting from Android hardware sensors
- Real GPS/location tracking
- Walking, running, cycling, hiking, and workout recording
- Local activity history and statistics
- OpenStreetMap-based mapping
- Bluetooth Low Energy sensor support
- Health Connect interoperability
- JSON, CSV, GPX, and TCX data portability
- Optional fitness pet and rest-day system
- Custom movement reminders
- Advanced controls in MILES Studio
- Launcher icon and theme customization
- Accessibility options
- Phone-side Wear OS preparation

MILES does not intentionally generate fake GPS, heart-rate, or step data.

---

## Tracking

### Steps

MILES uses Android step sensors in this order:

1. `TYPE_STEP_COUNTER`
2. `TYPE_STEP_DETECTOR`
3. Accelerometer fallback when a usable hardware step sensor is unavailable

Daily step state is persisted so restarting the app does not reset the displayed day. Day rollover is handled locally.

The dashboard also animates the real step value smoothly when it changes.

### GPS

The location engine uses Android location APIs with a fused-provider path and a platform location fallback.

GPS controls are designed to support:

- GPS enabled/disabled
- Phone GPS
- External GNSS source
- Automatic source selection
- Update interval
- Minimum movement distance
- Accuracy filtering
- GPS jump filtering
- Route smoothing
- Battery-aware tracking
- Live location
- Background tracking

Only real location updates are recorded.

### Activity recording

Recorded activities can contain:

- Activity type
- Start/end time
- Duration
- Distance
- Pace
- Speed
- Steps
- Elevation
- Heart rate when a real source is available
- GPS track points

---

## Bluetooth & sensors

MILES includes a BLE/GATT foundation for external fitness sensors.

Supported foundation:

- BLE discovery
- Bluetooth permission handling
- GATT connections
- Connection state
- Heart-rate service/characteristic handling
- Sensor source selection
- External-device status

The sensor architecture is designed for:

- Heart-rate monitors
- Cycling sensors
- Foot pods
- External GNSS receivers
- Wearable sensors

### MILES Studio controls

Advanced users can configure and diagnose:

- Sensor source
- Sensor refresh rate
- Step sensor state
- GPS state
- GPS update rate
- Bluetooth tunnelling state
- BLE connection state
- GATT information
- Signal information
- Tracking diagnostics

**Important:** a BLE connection is not automatically Bluetooth tunnelling. Full sensor tunnelling/routing remains a separate implementation task.

---

## Permissions & background tracking

MILES declares the Android permissions required by its tracking features, including:

- Location
- Activity recognition
- Nearby Bluetooth devices
- Notifications
- Body sensors where required
- Foreground location/health services
- Battery-optimization exemption request
- Health Connect data access

The intended first-run flow explains each sensitive permission before requesting it.

Background recording uses a foreground tracking service so an active workout can continue outside the main UI.

---

## MILES Studio

MILES Studio keeps technical controls out of the normal dashboard.

Planned/active diagnostic areas:

### GPS / GNSS

- Provider
- Source
- Refresh interval
- Minimum distance
- Accuracy
- Satellite information where available
- GPS filtering
- Route smoothing
- Tracking events

### Sensors

- Step sensor availability
- Step source
- Heart-rate source
- Sensor refresh rate
- Sensor availability

### Bluetooth

- Scan results
- Connection state
- GATT services
- Characteristics
- Debug information

### Health Connect

- Availability
- Permission state
- Read diagnostics

### Wear

- Paired-watch detection
- Connection state
- Watch capability information

### Logs

- Tracking events
- Sensor events
- Background-service state
- Import/export errors
- Debug-log export

---

## Lazy Days

Lazy Days are optional rest days.

Rules:

- Maximum **3 Lazy Days per week**
- The user chooses when to use them
- The normal daily goal resumes automatically
- A Lazy Day is not treated as a missed pet goal
- Lazy Days can be disabled

The pet system does not need to be enabled to use normal activity tracking.

---

## Fitness Pet

The fitness pet is completely optional.

Choose:

- Dog
- Cat
- Parrot
- Bunny
- Off

The pet uses the daily activity goal as its feeding progress. A Lazy Day is exempt. Pet settings and state are stored locally.

The feature is intended as a lightweight companion, not a replacement for normal health tracking.

---

## Move reminders

MILES provides customizable movement reminders using Android notifications and background scheduling.

Options include:

- Enable/disable
- Custom interval
- Custom notification message
- Test notification
- Persistent scheduling
- Notification channel

Reminders are optional and are not intended to spam the user.

---

## Health Connect

MILES includes an Android Health Connect integration foundation for interoperable fitness data.

The integration is intended for supported records such as:

- Steps
- Exercise sessions
- Other supported health/activity records as the integration expands

Health Connect is optional and permission-controlled.

---

## Import & export

MILES is designed so activity data is not trapped in the app.

| Format | Use |
| --- | --- |
| JSON | MILES activity data and backup data |
| CSV | Spreadsheet/data analysis |
| GPX | GPS routes |
| TCX | Workout/activity tracks |

The repository contains import/export groundwork and a TCX parser. Full validation, restore, and broad Google Fit/Takeout mapping are still being completed.

### Google Fit / Takeout

MILES is intended to accept exported Google Fit data where its format can be mapped safely into MILES activities. Unsupported or malformed records should be reported rather than silently converted into fake data.

---

## Backup & restore

The target backup system is local and portable.

A complete backup should include relevant:

- Activities
- GPS points
- Goals/settings
- Pet state
- Lazy Day configuration
- Other user-created MILES data

Full backup-file creation and restore validation are still in development.

---

## Maps

MILES uses an OpenStreetMap-based mapping foundation.

Target map capabilities include:

- Live location
- Route drawing
- Route recording
- Distance measurement
- Heading
- Recenter/follow mode
- Waypoints
- Route replay
- Activity heatmaps
- Route comparison
- GPX/TCX track display

---

## UI & animation

MILES is designed to keep the normal interface clean and move technical controls into Studio.

Current UI direction includes:

- Material 3 / Jetpack Compose
- Smooth screen transitions
- Fluid step-number animation
- Animated activity rings
- Loading states
- Responsive layouts
- Reduced-motion support
- Larger UI/text options
- High-contrast options
- Screen-reader-friendly labels
- Larger touch targets

Accessibility options must affect the real UI; they are not intended to be cosmetic switches.

---

## Customization

MILES includes:

- Light theme
- Twilight theme
- AMOLED theme
- AMOLED Red theme
- Material/system-style theme
- Optional Liquid Glass treatment
- Multiple launcher icons

Launcher icon switching uses Android launcher aliases and is designed to enable the selected alias before disabling the previous one.

---

## Media controls

MILES can integrate with a compatible active Android media session.

Supported controls include:

- Play/pause
- Previous
- Next
- Current track information

MILES does not replace the user's music application.

---

## Wear OS

The **Wear OS APK is intentionally not included in the current overhaul.**

The phone app can prepare for Wear OS by:

- Detecting compatible paired watches
- Showing paired/watch state
- Keeping phone-side watch metadata
- Preparing the MILES watch communication protocol

Full MILES-specific watch synchronization, watch recording, watch complications, Tiles, and watch-side sensors require the future Wear OS application.

**Wear OS APK: planned for a later phase.**

---

## Privacy

MILES follows a local-first design.

The app is intended to work without requiring:

- A MILES account
- A MILES cloud service
- A subscription
- Advertising
- Forced analytics
- A forced Google account

Data can be exported or shared by the user when desired.

Third-party integrations such as Health Connect may have their own permission and privacy requirements.

---

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Android SDK
- Android Sensor APIs
- Android Location APIs
- Bluetooth Low Energy / GATT
- Room
- Health Connect
- Coroutines
- OpenStreetMap-based mapping

---

## Build

### Requirements

- JDK 17
- Android SDK
- Git

### Clone

```bash
git clone https://github.com/anshlabs716/miles.git
cd miles
```

### Build

```bash
chmod +x gradlew
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Install with ADB

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Development status

| Area | Status |
| --- | --- |
| Android phone/tablet app | In active development |
| Real step engine | Implemented; hardware validation required |
| Real GPS engine | Implemented; hardware validation required |
| Background tracking | Implemented foundation; hardening/testing ongoing |
| BLE/GATT | Foundation implemented |
| Heart-rate BLE | Foundation implemented |
| Cycling sensors | In development |
| Foot pods | In development |
| External GNSS | In development |
| Bluetooth tunnelling | In development |
| Lazy Days | Implemented foundation; enforcement/testing ongoing |
| Fitness Pet | Implemented UI/data foundation; testing ongoing |
| Move reminders | Implemented foundation; edge-case testing ongoing |
| Health Connect | Integration in development |
| JSON/CSV/GPX | Import/export foundation |
| TCX | Parser added; integration ongoing |
| Google Fit/Takeout | Mapping in development |
| Full backup/restore | In development |
| MILES Studio | Active foundation; diagnostics expanding |
| Accessibility | In development |
| Icon switching | Code implemented; launcher testing required |
| Predictive back | In development |
| Phone-side Wear detection | Implemented foundation |
| Wear OS APK | Planned later |
| Stable release | Not available |

---

## Roadmap

### Tracking

- [x] Real step sensor engine
- [x] Persistent daily step state
- [x] Real GPS engine
- [x] Foreground tracking foundation
- [ ] GPS filtering hardening
- [ ] Route smoothing hardening
- [ ] External GNSS
- [ ] Cycling sensors
- [ ] Foot pods
- [ ] Bluetooth tunnelling

### Experience

- [x] Clean dashboard foundation
- [x] Fluid step animation
- [x] Activity animations
- [ ] Final transition polish
- [ ] Full accessibility verification
- [ ] Predictive-back verification
- [ ] Final launcher verification

### Data

- [x] JSON/CSV/GPX groundwork
- [x] TCX parser
- [ ] Full TCX import
- [ ] Full Google Fit/Takeout mapping
- [ ] Portable backup file
- [ ] Restore
- [ ] Duplicate/conflict handling
- [ ] Complete validation UI

### Health & personalization

- [x] Pet choices
- [x] Pet opt-out
- [x] Lazy Day data model
- [x] Move-reminder foundation
- [ ] Final Lazy Day enforcement verification
- [ ] Final pet feeding persistence verification
- [ ] Health Connect completion

### Wear OS

- [x] Phone-side paired-watch detection
- [x] Phone-side protocol foundation
- [ ] Wear OS APK
- [ ] Watch activity recording
- [ ] Watch GPS
- [ ] Watch heart rate
- [ ] Watch ↔ phone sync

---

## Contributing

Bug reports, testing, Android sensor work, UI improvements, performance fixes, documentation, and import/export testing are welcome.

1. Fork the repository.
2. Create a branch.
3. Make and test your changes.
4. Open a pull request.

---

## License

License: **TBD**

---

## MILES

**Real data. Real tracking. Portable data. Your device. Your data.**

Made by **AnshLabs716**.
