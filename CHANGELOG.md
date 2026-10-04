# Changelog

All notable changes to MILES are recorded here.

## [Unreleased]

### Fixed
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
