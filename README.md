# MioTimer

**MioTimer** is a small, offline-first Android timer built for situations where a normal fixed countdown is not enough.

It combines three timer modes in one app:

- **Random Timer** — starts a countdown with a random duration inside a configurable range.
- **Countdown** — a simple user-defined countdown.
- **Workout** — reusable multi-phase sessions mixing timed steps, manual repetition steps and rest periods.

A home-screen widget can start the Random Timer with one tap.

Repository: <https://github.com/fraschizzato/MioTimer>

## Why MioTimer?

The project started from a practical need: timers used during exercises or short recurring activities should require as little interaction as possible and should keep accurate time even when the UI is not in the foreground.

MioTimer therefore keeps the timer engine in an Android foreground service and calculates elapsed time from `SystemClock.elapsedRealtime()` rather than relying on UI refreshes as the clock source.

## Features

### Random Timer

- configurable minimum and maximum duration;
- random duration selected at start;
- one-tap Android home-screen widget;
- useful for variable pauses, drills, prompts and interval-style activities.

### Countdown

- countdown duration entered in seconds;
- pause, resume and stop controls;
- optional sound and vibration when the timer finishes.

### Workout sessions

Sessions are stored locally and can contain any sequence of:

- `TIMED` — runs for a configured number of seconds;
- `REPS` — waits for manual completion after a repetition-based activity;
- `REST` — timed recovery period.

This makes the workout runner suitable for simple calisthenics circuits, mobility routines, stretching sequences and other structured activities without requiring a cloud account.

### Display behaviour

MioTimer can optionally:

- keep the screen awake while a timer is running;
- keep it awake for another 15 seconds after completion;
- vibrate on completion;
- play an audible completion signal.

## Privacy and offline operation

MioTimer has no account system, analytics SDK, advertising SDK or cloud backend. The current application does not request the Android `INTERNET` permission.

Preferences and saved workout definitions stay on the device in local Android storage. See [PRIVACY.md](PRIVACY.md) for the current privacy statement.

## Technical overview

- Kotlin
- Jetpack Compose / Material 3
- Android foreground service
- `SystemClock.elapsedRealtime()` for monotonic countdown timing
- Gson for local workout serialization
- Android App Widget for one-tap Random Timer launch
- minimum Android SDK: 28
- compile / target SDK: 36
- JDK: 17+

The public application ID is:

```text
io.github.fraschizzato.miotimer
```

## Project structure

```text
MioTimer/
├─ app/
│  └─ src/main/
│     ├─ java/io/github/fraschizzato/miotimer/
│     │  ├─ MainActivity.kt
│     │  ├─ Models.kt
│     │  ├─ Prefs.kt
│     │  ├─ RandomTimerWidget.kt
│     │  └─ TimerService.kt
│     ├─ res/
│     └─ AndroidManifest.xml
├─ gradle/wrapper/
├─ bootstrap_build.ps1
├─ LICENSE
├─ COMMERCIAL-LICENSE.md
└─ THIRD_PARTY_NOTICES.md
```

## Build on Windows

Requirements:

- JDK 17 or newer;
- Android SDK with API 36 installed;
- `ANDROID_HOME` / Android SDK configuration available to Gradle.

From PowerShell:

```powershell
cd C:\publicGithub\MioTimer
powershell -ExecutionPolicy Bypass -File .\bootstrap_build.ps1
```

The bootstrap script downloads a local Gradle 8.13 distribution when needed, generates the Gradle wrapper and builds the debug APK.

Expected output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Once the wrapper has been generated you can use the normal commands, for example:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Android foreground service

Active timers use a foreground service so timing does not depend on the activity remaining visible. The service is currently declared with the Android `specialUse` foreground-service type and describes itself as a user-initiated countdown/workout timer.

Google Play requirements around foreground services can change. The manifest declaration and Play Console foreground-service declaration should therefore be reviewed again before each store release.

## Status

MioTimer is currently an early public release (`0.1.0`). The timer engine, widget and local workout editor are implemented. Store packaging, final visual identity, accessibility review, localization and release signing are intentionally handled as a separate release phase.

## License

The original MioTimer source code is **source-available** under the **PolyForm Noncommercial License 1.0.0**. Noncommercial use, modification and redistribution are permitted under those terms.

Commercial use is not granted by that license and requires a separate commercial agreement from the copyright holder. See [LICENSE](LICENSE) and [COMMERCIAL-LICENSE.md](COMMERCIAL-LICENSE.md).

Third-party libraries, Android components and build tools remain under their respective licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

PolyForm Noncommercial is not an OSI-approved open-source license, so this repository should be described as **source-available**, not as open source.

## Contributing

Bug reports and feature ideas are welcome. Because the project may also be commercially licensed, code contributions require compatible contributor licensing before they can be merged. See [CONTRIBUTING.md](CONTRIBUTING.md).
