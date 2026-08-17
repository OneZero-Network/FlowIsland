# FlowIsland — Build Report

**This report exists because the brief explicitly required one, and because
honesty about what was and wasn't verified matters more here than usual: this
entire codebase was written in a sandboxed environment with no access to
Google's Maven repository, no Android SDK, no emulator, and no physical
device. Nothing in this repo has been compiled, run, or tested by the author.
The GitHub Actions workflow (`.github/workflows/build.yml`) performs the first
real compile this code will ever go through.**

That is a materially different starting point from a normal build report, and
this document is written accordingly: as a map of where the risk sits, not a
checklist of green checkmarks.

## Version info

- Kotlin 2.1.0, AGP 8.7.3, Compose BOM 2024.12.01, Hilt 2.53, Room 2.6.1
- `minSdk 28` (Android 9) — lowered from the brief's suggested Android 15
  floor at the user's explicit request, for broader device reach. `compileSdk`
  / `targetSdk 36` (Android 16), satisfying Google Play's Aug 31 2026
  target-API requirement.
- `versionCode 1`, `versionName "1.0.0"`, `applicationId com.flowisland.android`

## What's fully implemented, with real logic (not stubs)

- **Activity Engine**: immutable state model, StateFlow source of truth,
  priority sorting, pin/hide/dismiss — unit tested.
- **Timer correctness**: every countdown/count-up is timestamp-based off
  `SystemClock.elapsedRealtime()`, never a ticking counter. Unit tested
  against simulated pause/resume/screen-off gaps.
- **Timer, Pomodoro** (real focus↔break auto-cycling), **Stopwatch** (with
  laps), **Study**, **Cooking** (multi-step, auto-advance or manual skip),
  **Fitness** and **Trip** (real GPS distance/pace via plain
  `LocationManager`, no Play Services dependency), **Expense** (Room-backed,
  category totals), **Reminder** (`AlarmManager.setAlarmClock`, survives
  reboot via a boot receiver), **Delivery** and **Flight** (manual
  status/state only — no fabricated live data, per the brief's explicit
  requirement), **Download/Export** (real CSV export of local history with
  genuine row-by-row progress), **AI/Local Task** (a real, reusable
  `LocalTaskUpdater` interface, demonstrated with a genuine `VACUUM`/`REINDEX`
  of the on-device database — not a fake progress bar).
- **Media**: reflects real system media-session playback state — see the
  permission deviation below.
- **Notifications**: `Notification.ProgressStyle` "Live Update" on Android
  16+ (gated by `canPostPromotedNotifications()`), falling back to a
  standard `setUsesChronometer` notification everywhere else — which the
  system animates natively with zero app-side polling.
- **Floating overlay**: a real `WindowManager` + `ComposeView` service,
  started/stopped only when the user has both enabled it in Settings *and*
  granted `SYSTEM_ALERT_WINDOW`, and only while there's something to show.
- **Settings, onboarding (3 screens), privacy policy, delete-all-data,
  history ledger, home-screen widget** — all wired to real DataStore/Room
  state, not placeholders.

## Deliberate deviations from the brief, and why

1. **Media tracking requires "Notification access" (`BIND_NOTIFICATION_LISTENER_SERVICE`).**
   The brief's default permission list excludes this, but genuinely observing
   "what's playing right now" across other apps has no other public Android
   API. This is opt-in only, off by default, disclosed in-app before the
   system settings screen opens, and the Media activity type is simply
   unavailable if declined. Documented as the "Reality Rule" taking
   precedence over the default permission list where the two conflict.
2. **Reminders declare `SCHEDULE_EXACT_ALARM`.** In practice, ordinary
   reminders use `AlarmManager.setAlarmClock()`, which needs no special
   permission and is Doze-exempt by design (it's the API meant for
   user-facing alarms). The declared permission is a reserved path for a
   possible future custom-interval feature, not something the current code
   requests at runtime.
3. **`minSdk` lowered to 28** at the user's explicit request after being
   shown the Android-version-distribution tradeoff (roughly half of active
   devices are below Android 15 as of mid-2026).
4. **Single `:app` Gradle module**, not the `core/feature/service` multi-module
   split implied by the brief's folder tree. The *package* structure inside
   `:app` mirrors that tree exactly (`core.activity`, `feature.timer`, etc.);
   the Gradle-module boundary was collapsed specifically to reduce
   first-build configuration risk in an environment where no build could be
   verified before shipping. Splitting into real modules later is a
   mechanical refactor, not an architecture change.
5. **No legacy raster launcher icons.** Only the adaptive-icon XML
   (`mipmap-anydpi-v26`) is provided, which is sufficient because `minSdk 28`
   is already above the API 26 adaptive-icon floor. A launcher that somehow
   ignores adaptive icons on a modern OS would show a default icon; this is
   very unlikely in practice but is a real, known gap rather than a silent one.
6. **Hindi/Marathi localization is partial**, covering onboarding, home,
   common actions, and settings section headers — not every string in the
   app. Full coverage needs a professional translation pass before shipping
   multi-language, not a mechanical extension of what's here.
7. **Instrumentation tests were not written**, only unit tests (Timer
   correctness, Priority Engine). The brief's instrumentation-test list
   (onboarding, timer, permissions flows, etc.) requires a running emulator
   to author against meaningfully; writing untestable instrumentation tests
   blind would be worse than omitting them and saying so.
8. **Quick Settings tile was not implemented** — deferred rather than shipped
   as a non-functional stub.

## What genuinely was not verified

- **No Gradle build has ever succeeded or failed on this code.** The sandbox
  this was written in cannot reach `dl.google.com` / Google's Maven, which
  AGP, Compose, Room's KSP, and Hilt's codegen all need. The first real
  signal on whether this compiles is the GitHub Actions run.
- **No unit test has actually executed** — they're written against the real
  `TimerSpec`/`PriorityEngine` APIs by inspection, not run.
- **No device or emulator testing** of any kind: no OEM matrix (Pixel/
  Samsung/OnePlus/Xiaomi/Motorola), no dark/light mode visual check, no
  TalkBack pass, no large-font check, no rotation/process-death check beyond
  the architecture being *designed* to survive them (timestamp-based timers,
  Room/DataStore persistence, no reliance on retained in-memory-only state
  for anything durable) — that design intent is not the same as verification.
- **No Play Store submission readiness check** beyond following the stated
  policies in the brief; Play's actual review process is not something this
  process can simulate.

## Most likely first-build failure points, if any

If the GitHub Actions run fails, these are the places to look first, roughly
in order of likelihood:
1. Version-catalog mismatches (an AGP/Kotlin/Compose-compiler combination
   that doesn't line up) — resolvable by bumping versions in
   `gradle/libs.versions.toml`.
2. The Glance widget (`widget/FlowIslandWidget.kt`) — Glance's API has moved
   between versions more than most Jetpack libraries; this file was kept
   deliberately minimal for exactly this reason.
3. The overlay service's manual `ViewTreeLifecycleOwner`/`ViewModelStoreOwner`/
   `SavedStateRegistryOwner` wiring (`core/overlay/FlowIslandOverlayService.kt`)
   — this is real, standard plumbing for hosting Compose in a `Service`, but
   it's the single most "hand-assembled" piece of platform integration in the
   codebase.

Fix-and-repush is the expected workflow here, not "wait for a perfect first
run" — that's what the CI feedback loop is for.
