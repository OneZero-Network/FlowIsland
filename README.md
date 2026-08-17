# FlowIsland

**Everything active. At a glance.**

An Android-first activity companion: start a timer, workout, study session, trip,
or any other ongoing activity once, then monitor and control it from a compact
floating island, Android notifications, and (on Android 16+) native Live
Updates — without repeatedly reopening the app.

- Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, WorkManager/AlarmManager
- 100% local: no account, no backend, no analytics/ad SDKs, works in airplane mode
- `minSdk 28` (Android 9) / `compileSdk` & `targetSdk 36` (Android 16)

## Get an APK without installing anything

This repo builds itself. Push it to GitHub (or fork it) and the included
GitHub Actions workflow (`.github/workflows/build.yml`) will:

1. Install a JDK, the Android SDK, and Gradle on a GitHub-hosted runner
2. Run the unit tests and lint
3. Build a debug APK, a release APK, and a release AAB
4. Upload all three as downloadable workflow artifacts (Actions tab → the run → Artifacts)

No local Android Studio install is required to get an installable APK — the
runner has an internet connection to Google's Maven repository, which this
build genuinely needs and which local sandboxes often don't have.

### Running it locally instead

If you do have Android Studio: **File → Open**, point it at this folder. Studio
will offer to generate the Gradle wrapper automatically (this repo doesn't
commit `gradle-wrapper.jar`, only `gradle/libs.versions.toml`, since it was
authored outside Studio). Then Run on a device/emulator running Android 9+.

### Release signing

The release build works out of the box **without** a keystore — it just falls
back to debug signing, so `assembleRelease` in CI always produces a real,
installable, R8-minified APK.

To get a Play-Store-uploadable, properly-signed build, add these repo secrets
(Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `FLOWISLAND_KEYSTORE_BASE64` | `base64 -i your.keystore \| pbcopy` (or equivalent) |
| `FLOWISLAND_KEYSTORE_PASSWORD` | your keystore password |
| `FLOWISLAND_KEY_ALIAS` | your key alias |
| `FLOWISLAND_KEY_PASSWORD` | your key password |

No signing credentials are ever hardcoded in this repo.

## What's real vs. simplified in this build

This is a genuine, from-scratch implementation of the architecture described in
the project brief, not a mockup — see `BUILD_REPORT.md` for the full,
unvarnished list of what's fully implemented, what's deliberately simplified,
and what was **never verified against a real compile** because this repo was
authored in a sandbox without access to Google's Maven repository or an
Android SDK/emulator. Read it before treating this as review-ready.

## Architecture

```
app/src/main/java/com/flowisland/android/
  core/
    activity/       Activity Engine: the single StateFlow source of truth
    database/        Room entities + DAOs (one per activity type's history)
    datastore/       Settings (theme, island prefs, per-type toggles)
    notification/    Notification + Live Update bridge, action receiver
    overlay/         Floating island WindowManager service + controller
    permissions/     Permission-state checks (requests happen in Compose)
    compatibility/   OEM/Android-version capability detection
    location/        Plain LocationManager wrapper (no Play Services dep)
    reminder/        AlarmManager scheduling (setAlarmClock, no special perm)
    time/            Timestamp-based TimerSpec -- the core correctness piece
    ui/              Theme, shared components (island pill, cards, timer text)
  feature/           One package per activity type + home/settings/onboarding
  navigation/        NavHost + routes
  widget/            Glance home-screen widget
```

The Activity Engine (`core/activity/ActivityEngine.kt`) is the one piece
everything else is built around: a single `StateFlow<List<ActivityUiState>>`
that the home screen, the Activity Switcher, notifications, the Android 16
Live Update, the floating overlay, and the widget all read from — never a
separately-derived copy.
