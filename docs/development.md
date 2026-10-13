# Development

This page describes how to set up a development environment and work on
MILES.

## Requirements

- Android Studio (latest stable)
- JDK 17
- Android SDK with API 36
- A device or emulator running Android 8.0 (API 26) or newer

## Getting the code

```bash
git clone https://github.com/anshlabs716/miles.git
cd miles
```

## Build

```bash
./gradlew assembleDebug
```

## Run

```bash
./gradlew installDebug
```

## Test

```bash
./gradlew test
```

## Workflow

- Branch from `main` (see [CONTRIBUTING.md](../CONTRIBUTING.md)).
- Keep changes focused and documented.
- Update `CHANGELOG.md` for user-facing changes.
