# Putting MILES on F-Droid

Everything in this folder is already validated. This is the order to use it.

## Status

| Check | Result |
|---|---|
| Clean clone → `./gradlew :app:assembleRelease` | **BUILD SUCCESSFUL** (16 MB unsigned APK) |
| Release variant works with no keystore | ✅ fixed (signing only applied when a keystore exists) |
| Gradle wrapper committed | ✅ `gradlew` + `gradle-wrapper.jar` |
| `compileSdk` | ✅ plain API 36 (no 36.1 minor requirement) |
| Proprietary dependencies | ✅ none — no Play Services, Firebase, or proprietary map SDK |
| Licence | ✅ GPLv3 |
| Screenshots | ✅ 6 reviewed images in `fastlane/metadata/android/en-US/images/phoneScreenshots/` |
| Build recipe | ✅ `build/com.aistudio.miles.track.gradle` in this folder |
| App metadata | ✅ `metadata/com.aistudio.miles.track.yml` in this folder |

## Steps

1. **Merge the open PRs** so `main` has everything, then tag:
   ```bash
   git tag 1.0.7
   git push origin 1.0.7
   ```
   F-Droid builds from **tags on the default branch**, not branches.

2. **Create a F-Droid account** at <https://f-droid.org> and confirm the email.

3. **Submit** at <https://f-droid.org/submit>:
   - Source repo: `https://github.com/anshlabs716/miles`
   - Licence: `GPL-3.0-only`
   - Build: F-Droid will propose `./gradlew assembleRelease`; paste
     `fdroid/build/com.aistudio.miles.track.gradle` if it asks for a recipe.
   - Request **inclusion in F-Droid's official repository**.

4. **If the build fails** on `com.android.application version 9.1.1`, F-Droid's
   builder doesn't have that AGP yet. Lower `agpVersion` in
   `fdroid/build/com.aistudio.miles.track.gradle` **and** `agp` in
   `gradle/libs.versions.toml` together, re-tag, and resubmit.

## What is deliberately not here

The listing screenshots exclude anything personal: the profile screen (age,
weight, height, heart rates, BMI) and any map view showing the owner's location
dot or home suburb were never added.
