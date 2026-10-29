# Changelog

All notable changes to MILES are recorded here.

## [Unreleased]

## [1.1.3]

### Changed
- Targets API 37 (Android 17). `compileSdk` and `targetSdk` are bumped from 36 to 37 so
  MILES builds and runs against the Android 17 SDK. No behaviour changes are expected
  on older systems; `minSdk` stays at 26.

## [1.1.2]

### Fixed
- Heart rate on Android 16 (targeting SDK 36): `Sensor.TYPE_HEART_RATE` is no longer
  covered by `BODY_SENSORS` there, so the phone's optical sensor could not be read even
  after the permission was granted. MILES now declares and requests
  `android.permission.health.READ_HEART_RATE` on Android 16 and `BODY_SENSORS` below,
  with `maxSdkVersion="35"` on the legacy permission. The sensor is also re-checked
  after the permission dialog is answered, so it registers as soon as it is granted.
- The workout Biometrics tab says "Heart rate permission needed" or "Waiting for
  heart rate" instead of "No HRM sensor connected" when a sensor exists but has not
  produced a reading yet.
- Pace now pads its seconds everywhere it is shown: 4'03"/km instead of 4'3"/km.
  All screens share one formatter (`formatPaceSecPerKm`), and an unknown pace reads
  `--:--` rather than a meaningless `0'00"`.

## [1.1.1]

### Fixed
- Steps are now the hardware step sensor's count only. Distance is never converted into
  a step count, so a device without a step sensor shows 0 instead of an invented number,
  and a ride no longer reports roughly 1900 fabricated "steps".
- Activity export (GPX, TCX, KML, GeoJSON, JSON) writes a real file through Android's
  Storage Access Framework instead of showing a toast with a character count.
- A recovered workout recomputes speed, pace and elevation from the saved track, so it
  no longer saves with 0.0 km/h average speed and 0 m elevation gain.
- Saved max speed and best pace are measured from the recorded track instead of being
  set to 1.35x and 0.9x the average.
- Saved heart-rate average and peak come from real sensor samples only. With no sensor
  there is no average and no peak.
- The phone's own optical heart-rate sensor is actually read
  (`DeviceHeartRateSensor`), which is what `BODY_SENSORS` is for. The permission is
  only requested on devices that have heart-rate hardware.

### Added
- Unit tests for steps, speed, elevation, best pace and heart-rate accounting
  (`WorkoutMetricsTest`, 62 tests passing).

## [1.1.0]

### Added
- A Health Connect permissions rationale screen, declared for `VIEW_PERMISSION_USAGE` and
  `ACTION_SHOW_PERMISSIONS_RATIONALE`. Android 14+ requires it, and without it the Health
  Connect permission sheet could silently fail to appear.
- fastlane changelogs for builds 5, 6 and 7.

### Fixed
- CSV import: parsing, map route points, steps, calories, timestamps and activity names.
- `miles.backup` restore for activities, routes, privacy zones, goals and preferences.
- A crash when saving a workout, caused by restored numeric preferences being read with
  the wrong type.
- Satellite and hybrid map layers no longer request tiles from the undocumented Google
  endpoint. They now use Esri World Imagery, and the on-map attribution names the real
  source.
- Dashboard weather is now opt-in: with "Online Weather (Open-Meteo)" off (the default)
  MILES makes no weather request at all.
- The Wear OS local-network beacon no longer broadcasts on every launch. It starts only
  after you pair/enable a watch and stops again when you unpair.
- Added the missing `RECEIVE_BOOT_COMPLETED` permission, so the widget sync receiver
  actually runs after a reboot.
- Health Connect now asks only for the two data types MILES reads (steps and exercise
  sessions), matching the permission rationale shown to the user.

### Changed
- Added the missing fastlane changelog for build 4.

## [1.0.8] - 2026-09-29

### Fixed
- Fixed a crash when scrolling to the bottom of Settings; the About card referenced an empty logo image.
- Added a clear “Permission not granted — Open app settings” action when Health Connect access is missing.
- Prevented the system and Health Connect permission dialogs from launching at the same time.
- The About card now reads the real app version instead of showing an old hardcoded version.
- The Source Code button now opens the actual MILES repository.

### Changed
- Bumped the app to version 1.0.8 (build 4).
- Enabled R8 minification and resource shrinking for release builds, reducing the APK from roughly 23 MB to roughly 3 MB.
- Removed the unnecessary Foojay toolchain resolver from the build configuration.

## [1.0.7] - 2026-09-25

### Fixed
- Dashboard calories were stuck at 0: they now include everyday walking, not just
  saved workouts.
- Dashboard distance was stuck at 0.00 km: it is now derived from real steps using a
  stride estimate based on the user's configured height.
- Active minutes now come from real step events; when none have been observed yet, an
  estimate from the real step count is shown and clearly labelled "min est".
- Calorie maths used to assume every user weighs 70 kg. It now uses the real configured
  body weight.
- Removed a hardcoded "5 Day Streak" placeholder. The streak is now calculated from real
  consecutive goal-meeting days.
- Daily Goals card gained a real active-minutes row.
- Removed hardcoded route ETA ("14 min") and speed ("5.2 km/h"); both are now derived from
  the real route distance and the user's own recorded average speed.
- Removed four hardcoded sample places from the route builder.
- Home screen widget no longer displays fabricated values.

### Added
- GMS-free local-network sync with MILES Wear OS (discovery beacon + TCP messaging),
  replacing the previous simulated watch integration. No Google Play Services involved.
- 22 unit tests covering the calorie, distance, active-minute and streak logic.

### Changed
- compileSdk pinned to standard API 36 so the project builds on plain Android 16 SDKs
  (F-Droid and other distro builders).
- Added PRIVACY.md, CONTRIBUTING.md, CHANGELOG.md and F-Droid (fastlane) metadata.

## [1.0.6]

- Previous release.
