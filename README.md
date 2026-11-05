# Temporal Fractions

A native, offline Android countdown timer based on precise subdivisions of fixed astronomical reference intervals. Built in Kotlin with a Termux-first, no-Gradle toolchain.

## Features

- Day: 86,400 seconds; mean synodic month: 29.5305888531 days; December-solstice year: 365.24274049 days.
- Dyadic (`1/2ⁿ`) and triadic (`1/3ⁿ`) subdivisions with a scrollable fraction ruler.
- Countdown timers begin immediately when started; they do not synchronize to an astronomical event or cycle boundary.
- Exact Android alarm scheduling, reboot restoration, saved presets, and persistent run records.
- User-selected audio: Android-supported media, WAV, and WavPack (`.wv`) through a native JNI decoder.
- Connected Audio Only: long-press the red headphones icon on Home to toggle restricted playback to eligible wired, USB, or Bluetooth audio outputs, with fail-closed routing checks. A normal tap displays the gesture hint.
- Audio test and a dedicated Home-screen Diagnostics button with copyable timer and audio routing information.
- Pure black and pure red interface across the editor, ruler, controls, icons, and custom app dialogs. Selected indicators are outlined, never solid red.

## Tested status

The November 4, 2026 device tests confirmed app startup, timer expiration, playback through Bluetooth earbuds, blocking playback without eligible earbuds, and stopping playback when the earbuds disconnected mid-session. The last on-device playback-tested source version is `20261104T213805Z`. The latest locally packaged palette revision `20261104T222055Z` corrects filled indicators and replaces gray native dialogs with black/red custom dialogs. It is not yet independently runtime-tested or published to GitHub; the prior black/red palette revision is `20261104T220602Z`. WavPack, USB, analog routing, long-form playback, reboot restoration, and background behavior require further device testing.

## Termux build

The source tree is a direct build project, not a Gradle project. Android 12+ and ARM64 are the intended targets. The build expects the Android platform SDK's `android.jar` (Android 35 was used in development).

Install build tools as required by your Termux setup. From the repository root, invoke:

```bash
bash build_v20261104T222055Z.sh "$PWD"
```

The build uses `kotlinc`, `javac`, `clang`, `aapt2`, Google's standalone D8/R8 `9.1.29`, and `apksigner`. On its first execution it fetches pinned WavPack 5.8.1 sources and D8; subsequent builds reuse caches. It creates a signed APK in `/storage/emulated/0/Documents/apk/`, logs in `/storage/emulated/0/Documents/log/`, and a hash record in `/storage/emulated/0/Documents/txt/`. APK installation opens automatically when `termux-open` is available.

The signing key and password are generated locally under Termux's private `$HOME/.temporal_fractions_build` directory; **back them up securely and never commit or share them**. The published source repository contains no signing credentials or binaries.

Alternatively, use the separately distributed versioned ZIP with its `run_v20261104T222055Z.sh` helper for quiet full-output logging and automatic extraction cleanup. That helper expects the original versioned ZIP layout and should not be run against a Git checkout directly.

## Project layout

- `src/` — Kotlin activities, timer store, scheduler, playback service, routing diagnostics, and Java JNI declarations.
- `native/` — the WavPack JNI C bridge; pinned upstream decoder source is fetched only at build time.
- `res/`, `AndroidManifest.xml` — Android resources and platform declarations.
- `tests/` — fixed-duration mathematics tests.
- `docs/` — original design, app dedication, third-party notices, and build notes.
- `build_v20261104T222055Z.sh` — complete no-Gradle Termux build.
- `diagnose_v20261104T222055Z.sh` — optional ADB runtime crash diagnostics.
- `publish_v20261104T220019Z.sh` — one-time GitHub repository creation helper for Termux.

## Licensing

Original Temporal Fractions source, Android resources, scripts, and documentation are dedicated under **CC0 1.0 Universal**; see [LICENSE](LICENSE) and [the CC0 legal code](https://creativecommons.org/publicdomain/zero/1.0/legalcode).

WavPack is BSD-3-Clause and copyright its respective authors. The Kotlin standard library is Apache-2.0; see [third-party notices](docs/THIRD_PARTY_LICENSES.txt). The WavPack upstream license is copied into the APK during the build.

## Safety and reliability

Audio routing checks aim to prevent speaker fallback but hardware and Android routing transitions cannot be guaranteed acoustically silent in every circumstance. Always test with your device and headphones before relying on alarms for important alerts. This project does not request Internet or location permission at runtime.
