# Time Reaper

A lightweight Android app for creating a batch of alarms in one tap — e.g. 5 alarms, 1 minute apart, starting at 06:30. Alarms are created in the phone's **system Clock app**, so they ring normally even when Time Reaper is closed or uninstalled.

Built with Kotlin and Jetpack Compose. It replaces an earlier .NET MAUI version (Time Cascade) that was heavier and unreliable.

## Features

- **Start time** — pick with a time picker, or use a quick preset (**15 min, 30 min, 45 min, 1 hour, 2 hours** from now).
- **Number of alarms** — 1 to 500 (default 2).
- **Interval** — minutes between alarms (1 to 1440). Times wrap past midnight, e.g. 23:50 + 30 min → 00:20.
- **Label** — optional name for every alarm in the batch (defaults to "Time Reaper"). Samsung Clock automatically groups alarms with the same label.
- **Duplicate skipping** — if the interval wraps around and produces the same time twice, the duplicate is skipped.
- **Progress** — the button shows "Creating alarm N of M…" while working.
- **Dark / light theme** — dark by default (cyan on near-black), toggle saved across launches.
- **Share** — share the installed APK with someone directly from the app.

## How it works

Android has no public API for writing to the Clock app's alarm list, so Time Reaper sends one [`AlarmClock.ACTION_SET_ALARM`](https://developer.android.com/reference/android/provider/AlarmClock#ACTION_SET_ALARM) intent per alarm, with `EXTRA_SKIP_UI = true` to ask the Clock app to save it silently.

**Alarms are created one at a time.** Sending the intents back-to-back loses most of them:

1. Once the Clock app comes to the foreground, Android blocks further activity launches from Time Reaper (background activity start restrictions, Android 10+).
2. The Clock app's handler merges intents that arrive while it is still processing the previous one.

So `AlarmScheduler` launches each intent only while Time Reaper is in the foreground, waits for the Clock app to take over and then hand control back (tracked with a lifecycle observer), pauses 300 ms, and only then sends the next one. If the Clock app never opens within 3 seconds, that alarm is counted as failed instead of created.

### Samsung / other OEM notes

- Samsung One UI (and some others such as MIUI) ignore `EXTRA_SKIP_UI` and show the Clock screen for each alarm. **Press back** after each one and Time Reaper sends the next.
- Stock Android / Pixel creates alarms silently.
- Time Reaper can't read the system alarm list (no public API), so it can't detect alarms that already exist from earlier runs.

## Requirements

- Android 8.0 (API 26) or newer
- Permission: `com.android.alarm.permission.SET_ALARM` — a normal permission, granted automatically at install

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3 |
| Architecture | Single activity + `AlarmViewModel` (Compose state) |
| Build | Android Gradle Plugin 8.7.3, Java 17 |
| SDK | minSdk 26, targetSdk / compileSdk 35 |

## Project structure

```
app/src/main/java/com/timereaper/app/
├── MainActivity.kt          Entry point; theme preference, hosts MainScreen
├── alarm/AlarmScheduler.kt  Sends SET_ALARM intents one at a time
├── model/
│   ├── AlarmResult.kt       Created times + skipped count
│   └── QuickPreset.kt       15 min … 2 hours presets
├── viewmodel/AlarmViewModel.kt  Form state, progress, starts scheduling
└── ui/
    ├── screen/MainScreen.kt Form, presets, time picker, share
    └── theme/               Colors, typography, dark/light theme
```

## Building

Requires the Android SDK (set `ANDROID_HOME` or create `local.properties` with `sdk.dir=...`).

```bash
# Debug APK
./gradlew assembleDebug

# Install debug build on a connected device (USB debugging on)
./gradlew installDebug

# Release APK (arm64-v8a only, R8-minified and resource-shrunk)
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Release signing

Release builds read signing details from `keystore.properties` in the project root (not committed):

```properties
storeFile=release-keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

The keystore (`*.jks`) and `keystore.properties` are in `.gitignore` — keep them out of version control.
