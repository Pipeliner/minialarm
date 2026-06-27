# MiniAlarm — Specification

## 1. Goal

A single-purpose Android app that lets the user set a system alarm with the
fewest possible interactions and the smallest possible footprint:

> Type four digits (`HH` `MM`, 24-hour). Press Enter. The alarm is set and the
> app closes.

Nothing else. No alarm list, no settings, no labels, no repeat, no snooze
configuration, no networking, no analytics, no background services.

## 2. User-facing behaviour

### 2.1 Screen

A single screen containing:

1. **Display** — shows the time being entered, always rendered as `HH:MM`
   (24-hour, zero-padded). Not-yet-typed positions render as the placeholder
   `–`. Examples:
   - nothing typed → `––:––`
   - `0` typed → `0–:––`
   - `09` typed → `09:––`
   - `093` typed → `09:3–`
   - `0930` typed → `09:30`
2. **Numeric keypad** — digit keys `0`–`9`, a **backspace** key, and an
   **Enter** key. This is the only input surface; the system soft keyboard is
   not used.

### 2.2 Digit entry rules ("explicit 0", always valid)

The user types exactly four digits, **including leading zeros** (9:05 am is
entered as `0`,`9`,`0`,`5`). Input is constrained so that a complete entry is
*always* a valid 24-hour time — impossible digits are rejected (ignored):

| Position | Meaning      | Accepted digits                                  |
|----------|--------------|--------------------------------------------------|
| 1        | hour tens    | `0` `1` `2`                                       |
| 2        | hour ones    | `0`–`9` if pos 1 ∈ {0,1}; `0`–`3` if pos 1 = `2`  |
| 3        | minute tens  | `0`–`5`                                           |
| 4        | minute ones  | `0`–`9`                                           |

- A digit that violates the table for the current position is ignored (no
  change, no crash).
- **Backspace** removes the most recently entered digit. Backspace on an empty
  buffer does nothing.
- Valid hour range: `00`–`23`. Valid minute range: `00`–`59`.

### 2.3 Enter

- **Enter with four digits entered**: the alarm is set for `HH:MM` and the
  activity finishes (the app closes).
- **Enter with fewer than four digits**: ignored (no-op). The buffer is
  unchanged and the app stays open.

### 2.4 Setting the alarm

The alarm is set by delegating to the system clock app via the standard
`AlarmClock.ACTION_SET_ALARM` intent:

- `EXTRA_HOUR` = entered hour (0–23)
- `EXTRA_MINUTES` = entered minute (0–59)
- `EXTRA_SKIP_UI` = `true` (set silently, without opening the clock app's UI)

This requires the normal (install-time, auto-granted) permission
`com.android.alarm.permission.SET_ALARM`. No runtime permission prompt occurs.

If no app on the device can handle the intent, the activity still finishes
without crashing (best-effort; out of scope to handle a deviceless state
beyond not crashing).

## 3. Non-functional requirements

- **Lightweight**: no runtime dependencies. The release app uses only the
  Android framework (`android.app.Activity`, framework views). No AppCompat, no
  Material, no AndroidX, no Kotlin stdlib bloat beyond what is unavoidable.
- **Efficient**: one activity, no services/receivers, no persisted state, no
  wakelocks. Work happens only in response to taps.
- **Minimal APK**: resource shrinking / minify enabled for release.

## 4. Architecture

- `TimeEntry` — a pure Kotlin class (no Android imports) holding the digit
  buffer and implementing the rules in §2.2/§2.3. Fully unit-testable on the
  JVM. Responsibilities: accept/reject digits, backspace, expose `hour`,
  `minute`, `isComplete`, and `display()`.
- `MainActivity` — wires keypad taps to `TimeEntry`, updates the display, and
  on Enter (when complete) fires the alarm intent and calls `finish()`.

This separation keeps all logic in `TimeEntry` (cheap, exhaustive unit tests)
and keeps `MainActivity` thin (verified with Robolectric + Espresso).

## 5. Test strategy ("all kinds of tests")

1. **Unit tests (JVM, JUnit)** — `TimeEntryTest`: exhaustive coverage of the
   digit rules, rejection of impossible digits, backspace, boundary times
   (`00:00`, `23:59`), display formatting at every length, and validity.
2. **Local integration tests (Robolectric, JVM)** —
   `MainActivityRobolectricTest`: drives real button views, asserts the display
   updates, asserts that Enter fires an `ACTION_SET_ALARM` intent with the
   correct extras, and that the activity finishes. Runs without a device.
3. **Instrumentation tests (Espresso, on-device/emulator)** —
   `MainActivityEspressoTest`: the same critical flows exercised on a real
   Android runtime (`Intents` verification of the outgoing intent).

## 6. Out of scope (explicitly removed)

Alarm editing/listing, labels, repeat days, ringtone choice, AM/PM mode,
12-hour mode, vibration settings, snooze config, persistence, theming options,
landscape-specific layouts, accessibility extras beyond defaults, localization
beyond a single `strings.xml`, and any network or storage permission.
