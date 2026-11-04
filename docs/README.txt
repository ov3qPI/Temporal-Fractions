Temporal Fractions — Kotlin Android countdown timer
Version: 20261104T222055Z
Package: cc0.temporalfractions.a0he7vjet48
Android: 12+; connected-audio-only enforcement requires Android 16/API 36 for multi-route inspection
CPU: ARM64-v8a
UI: Compact single-line toolbar with + TIMER and settings icon, adaptive empty state, condensed running countdown and preset rows, inline sorting, expandable history, and a connected-audio-only indicator when enabled. The editor retains its full-width switchable ruler.
Build: direct kotlinc, javac, d8, aapt2, clang and apksigner; no Gradle
First build fetches and verifies a pinned official WavPack 5.8.1 Git checkout from GitHub, so the first build requires network access. Later builds reuse the cache in Termux-private storage.
App itself does not request INTERNET or location permissions.
Script: build_v20261104T222055Z.sh
Preferred installation: run the one-command temporary extraction wrapper supplied with this ZIP. It cleans temporary extraction on either success or failure.
Builder cleans intermediate compilation files on exit and retains build log, APK, SHA256, signing key and vendor cache. The signing key stays under $HOME/.temporal_fractions_build. Preserve it securely for updates.
Outputs: /storage/emulated/0/Documents/apk, Documents/log, Documents/txt
Permissions: Android Alarms & reminders must be granted to schedule countdowns; notification permission supports progress and alarm statuses.
A source file is selected with Android's document picker and kept by persisted URI grant. Supported playback is Android MediaPlayer compatible audio plus .wv through WavPack.
Mean references: day 86400s; synodic month 2551442.87690784s; December solstice year 31556972.778336s.
All selections start countdowns from NOW, not astronomical phase boundaries. No automatic repeats and no time limit imposed on playback.
Connected-audio-only attempts to fail closed on disallowed actual routes, but hardware and OS transitions may leak sound briefly; never rely on it as guaranteed zero acoustic output.
Test exact alarms, reboot restoration, route unplugging, Bluetooth, USB, WavPack and long files on the target handset before relying on the app for important alerts.

The builder optionally detects a local Android-native Jansi installation. If none exists, harmless Kotlin Jansi warnings may appear in the retained log; no additional package is required.
Quiet one-command build: TERMUX_COMMAND.txt streams the bundled run_v20261104T222055Z.sh, which extracts the project, captures all terminal output in Documents/log, shows concise progress, and removes extracted files on success or failure.

Editor update 20261104T222055Z: Compact editing layout, center-anchored interactive fraction ruler, stable scroll restoration, and single-row audio chooser.

D8 toolchain update: The build caches Google standalone D8/R8 9.1.29 under Termux-private ~/.temporal_fractions_build/tools, tests the advertised version, and uses it only for Temporal Fractions without replacing the installed d8 package. Internet is required on first use.

Recovery build 20261104T222055Z: The preceding modernization build compiled but failed to open on-device. Restored the known-working activity startup, original theme, and Back behavior; retains R8 9.1.29 and the compact editor. Java/JNI and Kotlin now use JVM 11 to avoid Java 8 obsolescence warnings. MediaSession flags removed, notification STOP uses an Icon action, and unnecessary AudioTrack null assertion removed. No runtime crash cause is asserted without AndroidRuntime evidence.
Android runtime diagnosis: diagnose_v20261104T222055Z.sh requires an authorized ADB connection to the same device via wireless debugging; it captures startup exceptions to Documents/log without affecting app data.

Audio routing diagnostics: The editor offers a TEST AUDIO and STOP AUDIO control when an audio file is selected; Settings > DIAGNOSTICS shows connected output devices, media routing policy, media volume, and a timestamped local playback trace that can be copied. A restricted route must be positively verified before recording audio is released. Android audio routing may change asynchronously and requires physical device testing.
Notification and back warning cleanup 20261104T222055Z: Intentional Connected Audio Only route blocks now display Timer audio blocked and remain blocked in history; MediaPlayer, WavPack, and foreground-service errors display Timer audio failed and are marked failed in history. Existing Activity back behavior remains unchanged; its retained deprecated fallback is locally suppressed, avoiding a repeat of the previous startup regression.
