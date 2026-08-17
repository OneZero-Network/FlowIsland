# FlowIsland — Hardened Build Report

## Scope

This revision was made from the uploaded `FlowIsland-fixed(1).zip` without deleting any existing file. Five files were added; the original 96 files remain present.

The goal of this pass was not to add cosmetic features. It focused on the failure modes that would make the app unreliable in a real Android 15/16 installation:

- process-death recovery
- timer correctness across reboot
- real timer expiry instead of a permanently active island
- Android 15 overlay/foreground-service ordering
- Android 16 promoted ongoing notification request
- truthful terminal notifications
- recovery of fitness/trip tracking state
- GitHub Actions build verification
- Play-release signing not silently falling back to a debug key

## SDK / release target

- `minSdk = 35` (Android 15)
- `targetSdk = 36` (Android 16)
- `compileSdk = 36`
- `versionName = 1.0.0`
- `versionCode = 1`
- `applicationId = com.flowisland.android`

Google Play requires new apps and updates submitted from August 31, 2026 to target API 36 or higher.

## Important verification limitation

The current execution environment has Java/Kotlin available but does **not** have the Android SDK, Gradle installation, or access to Google's Maven repository. Therefore I did **not** claim a successful local Android compile or emulator test.

A GitHub Actions workflow was added specifically so the first authoritative Gradle compile/test/lint happens on a runner with Android SDK and network access.

Workflow:

`.github/workflows/build.yml`

The normal CI job runs:

- unit tests
- lint
- debug APK build
- debug AAB build

The manual release job builds a signed release APK/AAB only when the required private keystore secrets are supplied.

## Hardening performed

### 1. Durable active-activity store

Added:

- `ActiveActivityEntity`
- `ActiveActivityDao`
- `ActivityStateCodec`

Room schema is now version 2 with a real `1 -> 2` migration.

The Activity Engine now restores ongoing activities after process death instead of losing all in-memory state.

Media is deliberately excluded because its source of truth is the current system media session.

### 2. Reboot-safe timers

`TimerSpec` now contains both monotonic and wall-clock anchors.

Normal operation uses `SystemClock.elapsedRealtime()` so manual clock changes do not distort a running timer.

If the device reboots and elapsed realtime resets, the persisted wall-clock anchor is used for recovery.

Pause/resume also re-anchors the timer after a reboot so accumulated pause history cannot create a time jump.

### 3. Real timer expiry

Added:

- `ActivityExpiryScheduler`
- `ActivityExpiryReceiver`

Countdown activities are scheduled through `AlarmManager.setAlarmClock()` rather than relying on an in-process coroutine delay.

This means a timer can expire even when the app process is gone.

The alarm is recreated after reboot from the durable Room snapshot.

Pomodoro intentionally remains under the Pomodoro cycle manager so an expired focus/break phase can transition to its next phase instead of being incorrectly marked finished.

### 4. Android 15 overlay hardening

Android 15 narrowed the `SYSTEM_ALERT_WINDOW` background foreground-service exemption. The app must have a visible overlay before relying on that exemption.

`OverlayController` now only starts a new overlay service while the application process is foregrounded.

`FlowIslandOverlayService` creates the visible overlay before promoting itself to a foreground service.

If the user denies overlay permission, the application continues through native notifications.

### 5. Android 16 promoted ongoing notification

The Android 16 notification path now requests promoted ongoing treatment using the documented notification extra while retaining the standard notification fallback.

`Notification.ProgressStyle` remains the progress-centric system surface for suitable activities.

### 6. Truthful terminal notifications

Completed, cancelled, failed and expired activities no longer all display the word `Completed`.

### 7. Study-session recovery

Study history no longer depends on an in-memory registry surviving process death.

The durable activity snapshot provides the title/start/planned duration needed when the session terminates.

### 8. Fitness/trip recovery

Fitness and trip trackers now mirror distance/start information into the durable activity payload and can reconstruct active tracking after the app process restarts, provided the user has granted location access.

The app still does not claim traffic, routing, ETA, courier location, airline status, sports scores, stock prices or crypto prices because those require external data sources and are outside the API-free V1 scope.

### 9. Play release signing safety

The release build no longer silently falls back to the debug signing key.

The signed release workflow requires these GitHub Actions secrets:

- `FLOWISLAND_KEYSTORE_BASE64`
- `FLOWISLAND_KEYSTORE_PASSWORD`
- `FLOWISLAND_KEY_ALIAS`
- `FLOWISLAND_KEY_PASSWORD`

Without them, developers can still build/debug the project, but a Play-uploadable signed release is intentionally not produced.

## Files added

- `.github/workflows/build.yml`
- `app/src/main/java/com/flowisland/android/core/activity/ActivityExpiryReceiver.kt`
- `app/src/main/java/com/flowisland/android/core/activity/ActivityExpiryScheduler.kt`
- `app/src/main/java/com/flowisland/android/core/activity/ActivityStateCodec.kt`
- `app/src/main/java/com/flowisland/android/core/database/ActiveActivity.kt`

No original project file was deleted.

## Remaining real-world verification

Before calling the APK Play Store release-ready, run the GitHub Actions build and then test on physical Android 15/16 devices, especially:

- Pixel
- Samsung
- OnePlus
- Xiaomi
- Motorola

Test at minimum:

1. Start a timer.
2. Leave the app.
3. Lock the device.
4. Kill the app process.
5. Reopen it.
6. Confirm timer state remains correct.
7. Reboot the device.
8. Confirm the timer does not jump backwards.
9. Confirm timer expiry produces the correct terminal state.
10. Enable overlay permission and start an activity.
11. Disable overlay permission and confirm notification fallback.
12. Deny notification permission and confirm the app does not crash.
13. Start fitness/trip tracking and test process restart.
14. Test dark/light mode and large text.
15. Test battery saver and Doze behavior.
16. Run Play Console pre-launch/security checks.

Those are verification tasks, not claims that they have already passed in this environment.
