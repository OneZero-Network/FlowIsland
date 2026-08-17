# FlowIsland

**Everything active. At a glance.**

FlowIsland is an Android-first activity companion for Android 15+.
It provides a compact Dynamic-Island-style surface backed by native Android notifications, Android 16 progress-centric notifications, and an optional user-enabled floating overlay.

## Product principles

- Android 15+ only
- Target Android 16 / API 36
- Offline-first
- No account required
- No backend required
- No external API required for V1
- No advertising SDK
- No analytics SDK
- No fake live data
- No system impersonation
- No hidden accessibility tricks
- Battery-conscious activity lifecycle

## V1 activities

- Timer
- Pomodoro
- Stopwatch
- Media
- Study
- Cooking
- Fitness
- Trip
- Expenses
- Local downloads/exports
- Local task/AI-style processing progress
- Reminders
- Manual delivery tracking
- Manual flight countdown

Live sports, stocks, crypto, courier location, airline status, traffic and routing are deliberately not fabricated. They require external data sources and can be added as optional integrations later.

## Architecture

```text
Activity Engine
      |
      +---- Room active snapshot
      |
      +---- Android notification / ProgressStyle
      |
      +---- Optional floating overlay
      |
      +---- Home screen / activity detail / widget
```

The Activity Engine is the single runtime source of truth. Room is the durable recovery layer.

Timers use monotonic elapsed time during normal operation and a wall-clock recovery anchor across device reboot. Countdown expiry is backed by AlarmManager rather than an always-running background loop.

## Android build

- `minSdk 35`
- `targetSdk 36`
- `compileSdk 36`
- Kotlin 2.1.0
- AGP 8.7.3

The project intentionally does not commit a Gradle wrapper JAR. GitHub Actions installs Gradle 8.9 directly. Android Studio can also import the project and configure its local Gradle tooling.

## GitHub Actions

`.github/workflows/build.yml` performs:

- JDK 17 setup
- Android SDK setup
- Gradle 8.9 setup
- unit tests
- lint
- debug APK build
- debug AAB build

A manual signed-release workflow is available when the signing secrets are configured.

## Play Store release signing

Configure these repository secrets:

- `FLOWISLAND_KEYSTORE_BASE64`
- `FLOWISLAND_KEYSTORE_PASSWORD`
- `FLOWISLAND_KEY_ALIAS`
- `FLOWISLAND_KEY_PASSWORD`

The release build deliberately does **not** fall back to the debug key. This prevents accidentally uploading a debug-signed artifact as if it were a production release.

## GitHub 503

A GitHub web upload returning HTTP 503 is a server-side availability/service error; it is not evidence that Kotlin/Gradle code is invalid. This repository is therefore equipped with a CI workflow so that code validation happens independently of the GitHub web upload page.

If the GitHub web uploader continues returning 503, use GitHub Desktop or Git from a local checkout instead of repeatedly uploading through the browser.

## Verification status

Read `BUILD_REPORT.md` before treating the build as Play Store ready. The source has been hardened for the important lifecycle/platform failure modes, but the current sandbox cannot run a real Android Gradle build because it has no Android SDK/Google Maven access.
