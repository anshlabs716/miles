# Releasing

This page describes how maintainers cut a release.

## Versioning

This project follows [Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH`.

## Steps

1. Make sure `main` builds and all checks pass.
2. Update `CHANGELOG.md` with the changes for this version.
3. Bump the version in the project's version file(s):
   `app/build.gradle.kts` (`versionName` / `versionCode`)
4. Commit with a message such as `chore: release v1.1.0`.
5. Tag the release: `git tag -a v1.1.0 -m "MILES v1.1.0"`.
6. Push the commit and the tag: `git push origin main --tags`.
7. Create the GitHub release from the tag and paste the changelog section.

## Checklist

- [ ] Changelog updated
- [ ] Version bumped
- [ ] Tag pushed
- [ ] GitHub release published
