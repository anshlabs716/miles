# Testing

This page describes how MILES is tested and how to run the tests yourself.

## Running the tests

```bash
./gradlew test
```

## What is covered

Manual and unit testing of tracking, storage and export logic.

## Adding tests

- Add tests alongside the code they cover.
- Prefer testing behavior over implementation details.
- Keep fixtures free of real personal data, keys, or credentials.

## Current status

Automated test coverage is a work in progress. Contributions that add tests are
welcome.
