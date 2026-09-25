# Changelog

All notable changes to MILES are recorded here.

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
