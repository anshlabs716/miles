# Contributing to MILES

Thanks for helping. MILES is privacy-first, local-first and free of proprietary
lock-in, and those rules apply to contributions too.

## Ground rules

1. **Real data only.** Never fake, simulate or invent sensor values, GPS positions,
   traffic, business information or imagery. If a signal is unavailable, disable the
   feature or show it as unavailable — don't stand in a fake number.
2. **No advertising, no tracking, no mandatory accounts.**
3. **No Google Play Services as a requirement.** Optional capabilities may use them only
   if the core app still works without them.
4. **Respect licences.** Third-party code keeps its own licence. Anything that is not
   free/open-source cannot be added, because the project is distributed on F-Droid.
5. **Keep user data local** unless the user explicitly exports it.

## Development

Requirements: JDK 17+, Android SDK, Gradle.

```bash
git clone https://github.com/anshlabs716/miles.git
cd miles
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## Before you open a pull request

- Add or update unit tests for the logic you changed.
- Make sure `gradle :app:testDebugUnitTest` passes.
- Update `CHANGELOG.md`.
- If you changed user-visible behaviour, say clearly whether it uses measured data or an
  estimate — and label estimates in the UI.
- Don't add hardcoded placeholder values to the UI.

## Reporting bugs

Open an issue with your device model, Android version, and what you expected versus what
happened. If a number looks wrong, please include what the real sensor showed (steps,
GPS track, heart rate) so it can be traced.
