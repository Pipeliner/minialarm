# MiniAlarm

A deliberately tiny Android app that does exactly one thing:

> Type four digits (`HH` `MM`, 24-hour). Press **Enter**. The system alarm is
> set and the app closes.

No alarm list, settings, labels, repeat, networking, services, or persistence.
The full behaviour is specified in [SPEC.md](SPEC.md).

## How it works

- A custom on-screen **numeric keypad** (digits `0`–`9`, backspace `⌫`, enter
  `✓`) over a large display.
- Digits are entered with **explicit leading zeros** (9:05 → `0`,`9`,`0`,`5`).
  Input is constrained so a complete four-digit entry is *always* a valid
  24-hour time — impossible digits are ignored.
- The display always renders as `HH:MM`, with `–` for not-yet-typed positions
  (`––:––` → `09:––` → `09:30`).
- **Enter** (with four digits) fires the standard
  `AlarmClock.ACTION_SET_ALARM` intent (`EXTRA_HOUR`, `EXTRA_MINUTES`,
  `EXTRA_SKIP_UI=true`) and calls `finish()`. The system clock app sets the
  alarm silently.

## Lightweight by design

- **Zero runtime dependencies** — the release app uses only the Android
  framework (`android.app.Activity`, framework views). No AppCompat, no
  Material, no AndroidX in the shipped APK.
- One activity, no services/receivers/wakelocks, no stored state.
- One install-time permission: `com.android.alarm.permission.SET_ALARM` (no
  runtime prompt).
- R8 minify + resource shrinking on release.
- **Release APK ≈ 19 KB.**

## Project layout

```
app/src/main/java/com/minialarm/
  TimeEntry.kt       # pure (Android-free) entry/validation/formatting logic
  MainActivity.kt    # thin UI: wires keypad → TimeEntry → alarm intent
app/src/main/res/    # layout, strings, key style
app/src/test/        # JVM unit tests + Robolectric integration tests
app/src/androidTest/ # Espresso instrumentation tests
SPEC.md              # the specification (spec-driven development)
```

## Build

```bash
# Requires an Android SDK; point to it via local.properties (sdk.dir=...)
# or the ANDROID_HOME env var.
./gradlew :app:assembleRelease   # -> app/build/outputs/apk/release/app-release-unsigned.apk
./gradlew :app:assembleDebug
```

## Tests ("all kinds of tests")

| Layer | What it covers | Command | Runs without a device? |
|-------|----------------|---------|------------------------|
| Unit (JUnit) | `TimeEntry` rules: digit acceptance/rejection, backspace, boundaries (`00:00`, `23:59`), display formatting, exhaustive "every valid time is enterable / every complete entry is valid" | `./gradlew :app:testDebugUnitTest` | ✅ |
| Integration (Robolectric) | `MainActivity` wiring: keypad taps update display, Enter fires `ACTION_SET_ALARM` with correct extras and finishes, incomplete entry is a no-op | `./gradlew :app:testDebugUnitTest` | ✅ |
| Instrumentation (Espresso) | The same critical flows on a real Android runtime, with `Intents` verification of the outgoing intent | `./gradlew :app:connectedDebugAndroidTest` | ❌ (needs emulator/device) |

The unit + Robolectric suites (26 tests) run on the JVM with no device.
