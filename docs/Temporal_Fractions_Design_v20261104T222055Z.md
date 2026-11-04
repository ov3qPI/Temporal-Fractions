# Temporal Fractions
## Native Kotlin Android application — Design specification v0.3
**Document ID:** `Temporal_Fractions_Design_v20261104T222055Z`  
**Supersedes:** `Temporal_Fractions_Design_v20261104T174921Z`  
**Proposed application ID:** `cc0.temporalfractions.a0he7vjet48`  
**Status:** Implementation underway; first APK built and installed, revised interface awaiting device verification.  
**Initial target:** Samsung Galaxy XCover6 Pro, Android API 36, ARM64, built with Termux.  
**Licensing:** Original application code/documentation CC0-1.0; upstream WavPack remains BSD-3-Clause; additional packaged runtimes retain their respective upstream licenses.
## 1. Product purpose and scope
Temporal Fractions is an offline native Kotlin Android countdown timer whose selectable durations are dyadic (`1/2^n`) and triadic (`1/3^n`) subdivisions of three **fixed reference astronomical intervals**: a standard 86,400-second day, a mean synodic month, and a mean December-solstice-to-December-solstice year. The central purpose is to **calibrate the user's subjective sense of astronomical timescales** by allowing fractions of these large reference intervals to be experienced as ordinary elapsed time.
Every START action launches a **countdown lasting exactly the selected duration from that moment**. The application does not attempt to synchronize a timer to an actual new moon, solstice, calendar midnight, lunar phase, or fractional astronomical boundary. The astronomical labels describe duration *scale*, not calendar-phase position.
Users can store any number of named timer presets, sort them by duration, run multiple independent timers, and associate each preset with a chosen audio document. A triggered timer causes an Android exact alarm-clock event to launch long-form audio. Audio plays until the chosen file ends, the user presses STOP, audio focus or other system conditions interrupt it, or an error stops it. **There is no arbitrary playback duration limit.** Playback never loops automatically.
A global Connected Audio Only setting permits playback through verified 3.5 mm, USB audio, or Bluetooth audio endpoints while seeking to avoid the phone speaker. The setting fails closed on uncertainty and is indicated on the main screen. The OS/driver route transitions cannot be guaranteed completely free of transient speaker leakage; this is a best-effort platform safety property rather than an absolute security guarantee.
### Explicit in-scope features
- One vertically oriented inch-tape-style ruler in the timer editor, switched between dyadic and triadic subdivision using two adjacent tabs. Never show the ruler on the home screen.
- Selection of day, mean synodic month, or mean December-solstice year as the reference interval.
- Exact fixed rational subdivisions with accurate-to-nanosecond internal representation where the constants permit; human-readable time previews.
- Multiple concurrent one-shot countdowns and unlimited saved presets; duration-ascending/descending sorting.
- User-selected audio accepted when Android can natively play it, including device-specific supported formats, plus WavPack `.wv` via a bundled native decoder.
- Simple playback with one STOP action in app and notification; no seeking, next/previous, playlist, loop, or app-imposed runtime cutoff.
- Android exact alarm scheduling, persistent run state, reboot restoration, and audible-playback diagnostics.
- Persistent optional connected-audio-only mode, status icon, and silent blocking of unverified outputs.
- No GPS permission, online lookup, runtime ephemeris, astronomical event table, analytics, or account.
### Explicit exclusions
- **Next Boundary**, phase alignment, new-moon detection, annual solstice detection, or automatic recurrence at astronomical dates.
- Varying timer duration according to which real-world month or year the user is currently in.
- Ephemeris files, event timestamp catalogs, GPS, background astronomical synchronization, or perpetual media loops.
- An absolute promise that an Android phone will never emit audio through its speaker during hardware/OS routing changes.
## 2. Reference astronomical periods
The app stores **three unchanging, versioned constants** expressed as integer nanoseconds. For transparency, the UI may show their scientific definitions and reference epoch. The constants below describe mean periods around the epoch J2000, not lengths of any particular current lunar month or year.
| Unit ID | Physical interpretation | Frozen reference days | Frozen reference seconds |
|---|---|---:|---:|
| `DAY` | Standard elapsed day, not sunrise-to-sunrise or DST-sensitive civil day | `1` | `86400` |
| `SYNODIC_MONTH` | Mean geocentric interval between successive solar-lunar conjunctions, around J2000 | `29.5305888531` | `2551442.87690784` |
| `DECEMBER_SOLSTICE_YEAR` | Mean interval between successive December solstices, around J2000 | `365.24274049` | `31556972.778336` |
**Primary reference for lunar mean:** NASA GSFC's *Calendars and their History* gives a synodic-month approximation `29.5305888531 + 0.00000021621 T − 3.64×10⁻¹⁰ T²` days, where `T` is Julian centuries of dynamical time from J2000. Freeze `T = 0` for this application. Source: https://eclipse.gsfc.nasa.gov/SEhelp/calendars.html
**Reference for December-solstice mean:** Jean Meeus's equinox/solstice approximation (as reproduced in implementations of *Astronomical Algorithms*, 2nd ed.) contains a December-solstice polynomial whose J2000 linear coefficient corresponds to approximately `365.24274049` days per year. It is *not* interchangeable with the commonly quoted mean tropical year of approximately `365.24218967` days. Sources: https://rubydoc.info/gems/astronoby/Astronoby/EquinoxSolstice and https://www.rmg.co.uk/sites/default/files/Calendars-from-around-the-world.pdf
**Important interpretation of accuracy:** These constants have precision finer than one second in their stored numerical values. That does **not** mean the actual Moon or solstice cycle can be predicted by the same constant within one second for arbitrary dates: both physical periods vary, and long-term mean definitions depend on epoch and model. v0.2 intentionally prefers a stable, fixed reference over live physical variation. An astronomy-definition change requires an explicit versioned product decision, never a silent daily update.
### Exact canonical nanoseconds
- Day: `86400000000000` ns.
- Mean synodic month: `2551442876907840` ns.
- Mean December-solstice year: `31556972778336000` ns.
These integer values are exactly the listed decimal-day constants multiplied by 86,400 seconds and one billion nanoseconds per second; no runtime floating-point approximation is necessary. All fractions are computed from these original integers, without round-off accumulated across division levels.
## 3. Fraction mathematics and selection
Choose base period `L`, radix `b ∈ {2, 3}`, and subdivision level `n ≥ 0`. Selected duration is `D = L / b^n`. The level-zero selection represents the entire reference period. Dyadic levels are `1, 1/2, 1/4, 1/8, 1/16, 1/32, …`; triadic levels are `1, 1/3, 1/9, 1/27, 1/81, 1/243, …`.
The fraction's label and rational representation are authoritative. Compute `b^n` with `BigInteger`, then divide the fixed nanosecond numerator using a documented nearest-nanosecond rounding rule (`HALF_UP` or explicitly chosen equivalent). Do not calculate successive levels by repeatedly dividing the preceding level. The saved preset stores `periodId`, `radix`, and `level`, not merely the displayed decimal duration. One START action freezes the computed duration and deadline for its run; any later software update must not silently resize an already scheduled countdown.
**Default selection floor:** Do not offer subdivisions shorter than one elapsed second, since Android exact alarms cannot promise nanosecond or millisecond callback delivery. The displayed fractional duration may still include noninteger seconds, for example `1/81 day = 1066.666666667 s`, with its accurate target stored at the selected resolution. Distinguish *mathematical interval precision* from *actual alarm firing latency*.
Illustrative durations:
| Selection | Duration |
|---|---|
| `1/16 day` | `1 h 30 min` |
| `1/9 day` | `2 h 40 min` |
| `1/27 day` | `53 min 20 s` |
| `1/81 day` | `17 min 46.666666667 s` |
| `1/3 mean synodic month` | `850480.95896928 s` |
Presets are sortable deterministically because the reference periods never change. Primary sort is computed nanosecond duration, ascending or descending; stable ties use case-insensitive name and then ID. The UI can show the corresponding days/hours/minutes/seconds without imposing a civil calendar interpretation.
## 4. Time behavior and precision policy
Every timer is a **one-shot elapsed-duration countdown** starting on button press. It is not a clock-time reminder, calendar event, or astronomical phase tracker. The start instant and selected interval define the deadline; no external astronomical input is consulted.
When armed, store a wall-clock deadline (Unix epoch milliseconds with any retained finer-resolution fraction) together with a `SystemClock.elapsedRealtime()` deadline and a boot-session identifier. During the same boot, elapsed time is the preferred basis for remaining-time presentation, resisting manual clock changes and timezone/DST changes. The OS `AlarmManager` uses an appropriate actual-wall-clock trigger, which must be audited after clock changes. Following reboot, Android's prior monotonic epoch is lost; reconstruct the remaining time using the persisted UTC deadline and clearly identify any unrecoverable clock-change ambiguity. Do not re-start the full duration after reboot.
Android alarm delivery is never guaranteed to the nanosecond. Schedule the nearest representable millisecond according to the documented policy, log requested vs scheduled vs observed trigger times, and display appropriate precision for the UI. Very long month/year intervals must be supported without `Int` overflow; deadline calculations must use checked 64-bit or wider arithmetic. Do not keep a busy running thread solely to show a countdown.
Because this product does not track *real* astronomical event instants, **no ephemeris file, monthly event table, Julian-date conversion, seasonal data update, network download, or position permission is necessary**.
## 5. Interface and interaction
The application has two clearly separated surfaces: **Home** and **Timer Editor**. The home screen is a compact dashboard; the distinctive vertically scrolling measuring-tape ruler is visible **only** while creating or editing a timer. There is no permanent inline creation form on Home.
```text
HOME
TEMPORAL FRACTIONS                         [+ TIMER]
Astronomical-scale countdowns
[🎧 CONNECTED AUDIO ONLY]                 [SETTINGS]
RUNNING TIMERS
Rest interval                     00:43:19 left
[CANCEL TIMER]
SAVED PRESETS                           [SHORTEST ↑]
Night rest   ·   Day / 16   ·   1h 30m
[START]   [EDIT]   [⋮ duplicate / delete]
[SHOW HISTORY]

TIMER EDITOR
[‹ BACK]                               NEW TIMER
REFERENCE INTERVAL
[Day                 ▾]
SUBDIVISION
[DYADIC · ÷2]                   [TRIADIC · ÷3]
┌───────────────────────────────────────┐
│ ━━━━━━━━━━━━━  1                     │
│ ━━━━━━━━━     1/2                    │
│ ━━━━━━━       1/4                    │
│ ━━━━━         1/8                    │
│ ━━━━━━━━━━━━━  1/16  ◀ selected      │
│              scroll vertically       │
└───────────────────────────────────────┘
SELECTED DURATION
Day · 1/16 · 1h 30m 0s
PRESET NAME
[Optional name                         ]
ALARM AUDIO
[CHOOSE AUDIO]                  [CLEAR AUDIO]
[START TIMER]
[SAVE AS PRESET]
```
The editor provides a **single full-width ruler**. Selecting DYADIC swaps its marks to powers of two; selecting TRIADIC swaps its marks to powers of three. The selected fraction is highlighted and displayed with its elapsed duration. Each row is a tappable graduation; a vertical drag scrolls through additional fractions. Selecting a graduation updates the duration without re-creating the ruler or jumping back to its first mark.
The interval selector, preset name and audio file picker all belong to the editor, not Home. The Home toolbar uses a conspicuous `+ TIMER` button to open the editor; editing a saved preset opens that same editor, prepopulated with its values. Back returns to Home without creating an alarm. Starting a timer or saving a preset returns to Home on success.
The home screen prioritizes running countdowns and saved presets, with a short label and sort toggle. Secondary actions (duplicate/delete) are behind a three-dot menu. Audio restriction is a global setting accessible from Home, marked by a small headphone icon when active. No selectable calendar alignment is present.
### Presets and active runs
Users may create, rename, duplicate, edit and delete any number of presets. Each preset selects its reference period, radix, level, optional audio document (or inherited global default), and display name. Saving a preset does **not** start a timer. The preset list permits **Shortest first** and **Longest first** with frozen reference durations, not dynamic current-cycle lengths.
An active run is an independent snapshot of its original preset and duration. Multiple instances of the same preset are allowed. Editing or deleting a preset does not rewrite or silently cancel a running instance. Active runs have CANCEL; once a run fires and playback starts, the playback screen and media notification expose only STOP as the transport action. Do not show seek, pause, next, or previous.
The main toolbar shows a small headset/shield icon whenever Connected Audio Only is enabled, even if no headset is connected. Provide a distinct blocked/unavailable status on the settings screen and active-run details.
## 6. Exact alarms and Android lifecycle
Use Android `AlarmManager.setAlarmClock(...)` for scheduled countdown expirations, one stable uniquely identifiable `PendingIntent` per active run. Determine whether `USE_EXACT_ALARM` or `SCHEDULE_EXACT_ALARM` is appropriate to the app's distribution and applicable Android policy, inspect exact-alarm authorization before arming, and never silently fall back to an inexact timer. Where authorization is absent, show what needs to change.
A receiver must validate run ID, scheduling generation, and current database state, rejecting duplicate, stale, canceled, or already-fired callbacks. On successful delivery, persist the fired state before requesting the foreground media service; report any startup failure. Reconstruct pending alarms after reboot and applicable authorization/time changes without manufacturing a fresh full-duration countdown. Force-stop, device shutdown, battery restrictions, muted media volume, inaccessible files and OS policy may prevent or delay delivery; do not promise flawless alarm behavior.
Multiple countdowns may expire at the same time. Never start multiple overlapping media outputs. Use a deterministic explicit precedence/queue policy; a scheduled alarm preempts manual audition, and no pending sound should surprise-play after the user presses STOP. Maintain a human-readable audit for runs whose sound was suppressed or interrupted by another run. Default to no automatic sound queue after STOP.
The app is a countdown scheduler until expiration: no permanent background service, wakelock, or polling loop is required merely to wait for a future timer. A foreground playback service is started only to play selected media.
## 7. Audio capability and playback design
### Supported audio: system-native plus WavPack
**Accept any locally selected file that this specific Android device can natively decode and play**, with no hardcoded extension whitelist limiting Android's capabilities. The native backend is `MediaPlayer`/other appropriate framework playback APIs. Include all Android-handset standard supported audio types such as MP3, AAC in supported containers, FLAC, Opus, Vorbis, AMR, PCM/WAV and MIDI, and any additional manufacturer-supported audio decoding and container combinations. A user may select an audio-only track in a multimedia container when Android can actually play it. The filesystem extension and MIME type are hints, **not proof** of decoder support.
**WavPack `.wv`** remains separately supported through pinned WavPack 5.8.1, the BSD-3-Clause decoder, a JNI bridge and `AudioTrack` PCM streaming copied/refactored from Solar Elevation Alarm. WavPack files must be verified by the actual WavPack parser rather than presumed valid from extension.
Use Android Storage Access Framework `ACTION_OPEN_DOCUMENT` and persist read URI permission when offered. The file picker should show `audio/*` initially but allow selection via an all-files view for obscure but natively playable containers and `.wv` when document providers misclassify them. After selection, validate by attempting actual appropriate framework extraction/preparation or WavPack opening; reject on failure with a specific user-facing error. At timer firing, reopen and validate the document again; files may have been moved, deleted, revoked or changed. No blanket external-storage access is needed.
**Native decoder support is device-dependent.** There is no promise that every format appearing in Android's public format lists plays on every device or every framework path; the runtime test is authoritative. For reference: https://developer.android.com/media/platform/supported-formats
### Playback semantics
- Treat the Android alarm clock scheduling API as the alarm mechanism while using `AudioAttributes.USAGE_MEDIA` and `CONTENT_TYPE_MUSIC` for ordinary media volume/routing and headset/Bluetooth support. Warn when media volume is zero.
- Start playback from the beginning; play each selected file **once**, in full, with no loops, seek controls or playlist transitions.
- **No maximum playback duration**: do not install a six-hour cap or any other arbitrary time-limit watchdog. Long files may run for as long as the Android service and playback environment permit; natural EOF ends playback.
- Continue playback when the activity is backgrounded or swiped away where Android permits, using a properly declared `mediaPlayback` foreground service and a wake lock held only while required.
- User-facing controls: STOP on the app screen and on the media notification; media-session external pause/skip/seek commands may map to STOP, consistent with the Solar design.
- Stop and clean up on explicit STOP, natural EOF, media focus loss as configured, decode errors, missing audio route in strict mode, hardware access errors, or OS-initiated service destruction.
- Never advertise infinitely guaranteed playback: OS shutdown, power policy, memory pressure, hardware failure and media-access loss remain possible.
### Dual playback implementation
**Backend A — Android-native:** Prefer `MediaPlayer` where its actual container/codec support includes the document, especially formats such as MIDI not naturally surfaced as PCM by `MediaExtractor`. `MediaPlayer` implements `AudioRouting` and exposes `getRoutedDevices()` on API 36, allowing the same routing-policy interface as WavPack. For formats with a tested `MediaExtractor`/`MediaCodec` path, a unified PCM-to-`AudioTrack` pipeline is an optional reliability refinement, not a reason to exclude native formats.
**Backend B — WavPack:** Port the Solar JNI decoder and `AudioTrack` stream path, preserving format validation and source-file ownership. Both backends share a single foreground service, media session, notification STOP action, audio-focus handling, priority arbitration, status reporting, and output-route guard.
**Solar-source fact:** As inspected for this design, the existing `SolarAlarmPlaybackService.java` and `SolarAlarmWvPlayback.java` do not visibly impose a six-hour elapsed playback stop. This app intentionally follows that open-ended behavior, while omitting any proposed extra duration cap.
## 8. Connected Audio Only: global output policy
A persistent global setting **Connected Audio Only** limits the app's intended audible output to selected eligible endpoints: connected 3.5 mm headphones/headsets, appropriate USB-C digital audio outputs, and active Bluetooth audio outputs (A2DP and supported BLE headsets). USB charging alone and paired-but-disconnected Bluetooth devices are not eligible. Speaker, earpiece, HDMI, and unclassified routes are not automatically permitted by this setting. Optionally allow the user to enable/disable wired, USB and Bluetooth classes separately.
A fail-closed playback handshake is required: verify at least one eligible connected output; select it as the preferred route if possible; initialize the player at silence; check the **actual routed devices** after its stream becomes active; enable audible output only when every active output is permitted. Register routing-change and device-connectivity callbacks; recheck while playing; on removal or detection of any unpermitted/unknown route, immediately mute/stop and release the player. If no eligible output exists at the moment an alarm fires, record the alarm as triggered but **blocked from sounding**, show a silent notification, and do not reroute to the speaker.
For Android 16 / API 36, both `AudioTrack` and `MediaPlayer` expose `getRoutedDevices()` as a list; prefer this over the single `getRoutedDevice()` for multi-output cases. `setPreferredDevice()` is only a routing request, not assurance of actual routing. On older APIs, disable or explicitly degrade the strict feature if complete output verification is impossible. Android may switch audio paths asynchronously, including a tiny period before the app detects a route change; **this feature cannot certify absolute silence from the phone speaker**. Advise using hardware-isolated output for security-critical situations.
The restriction must also prevent the app from playing fallback ringtone sounds or alert tones on the speaker, and its own blocked notification should be silent. It does not suppress other applications' audio or system alerts. Show a permanent small icon while active, a clear warning on the settings page, a current-route explanation, and a user-initiated low-volume route test.
Sources: https://developer.android.com/reference/android/media/AudioTrack and https://developer.android.com/reference/android/media/MediaPlayer
## 9. Persistent data and migration
Use Android `SQLiteOpenHelper` or an equivalently lightweight transactional local store. Separate reusable **preset** definitions from each independently scheduled **run**.
**Preset:** `presetId`, `name`, `periodId`, `radix`, `level`, `audioUri`, `audioDisplayName`, `inheritDefaultAudio`, `createdUtc`, `updatedUtc`. There is **no mode field** because all timers are countdowns.
**Run:** `runId`, `presetSnapshot`, `constantSetVersion`, `durationNs`, `startUtc`, `deadlineUtc`, `startElapsedRealtime`, `bootSession`, `scheduleGeneration`, `status`, `lastError`, `firedUtc`, `completedUtc`.
**Settings:** `connectedAudioOnly`, `allowedOutputClasses`, `defaultAudioUri`, `presetSortOrder`, `referenceConstantVersion`, and applicable diagnostic preferences.
**Diagnostics:** bounded local logs of planned and actual alarm timestamps, player engine, validated route(s), and stop/block reason. Export only at explicit user request, warning that audio filenames and alarm times may be sensitive. No GPS or personal tracking data required.
Persist schema versions and migration logic. Updates must not change the package ID, signing key, saved presets, or active run durations. If the numerical reference constants ever change in a future version, preserve existing run deadlines and make the selected constant-set version inspectable. Sorting references the installed constant-set version and is stable across app launches.
## 10. Module architecture
```text
TemporalFractionsActivity.kt        ruler view, presets, active timers
FractionRulerView.kt               dyadic/triadic vertical selection
ReferencePeriods.kt                frozen integer-nanosecond reference constants
FractionEngine.kt                  BigInteger dyadic/triadic duration calculation
PresetRepository.kt                local preset/run database, transactions and sorting
TimerScheduler.kt                  AlarmManager exact countdown scheduling
TimerAlarmReceiver.kt              fired state, deduplication and service handoff
TimerSystemReceiver.kt             boot, time-change and permission-state audits
PlaybackService.kt                 foreground media service, media session and STOP
NativeMediaPlayback.kt              Android framework-supported files
WavPackPlayback.kt                 WavPack PCM-to-AudioTrack pipeline
OutputRouteGuard.kt                permitted-device and actual-route checks
AudioDocumentRepository.kt         SAF selection, persisted URIs and revalidation
WavPackBridge.c                    JNI adapter to pinned upstream WavPack
AndroidManifest.xml                exact alarm, service and receiver declarations
```
There is deliberately **no `AstronomicalEventStore`, calendar-boundary engine, ephemeris reader, or external astronomical dataset**. The reference constants are source literals in one audited Kotlin module, documented with source, epoch and unit tests. Only the audio decoder needs native C/JNI code.
## 11. Android platform/permission plan
- Android alarm-clock exact events through `AlarmManager` with distribution-policy-appropriate exact alarm permissions and capability checks.
- `RECEIVE_BOOT_COMPLETED` for safe rescheduling after reboot, without starting media playback directly from boot broadcasts.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` with the correct service type while media is actually playing.
- `WAKE_LOCK` while needed for stable audio stream progress, released on STOP, EOF and failures.
- `POST_NOTIFICATIONS` as applicable on Android 13+, with honest warnings if denied.
- `ACTION_OPEN_DOCUMENT` and persistable URI read grants, **not** `MANAGE_EXTERNAL_STORAGE`.
- No location or internet permission in the release APK. No full-screen interrupting alarm UI by default.
- Observe Android 15+ audio-focus eligibility requirements and vendor battery restrictions.
Set `minSdk 31` provisionally, target API 36 for the first ARM64 release, and test exact routing behavior on the Samsung Galaxy XCover6 Pro. Do not infer permissions or capabilities solely from the SDK number.
## 12. Termux build and source organization
The developer build uses locally installed Kotlin compiler, Android `android.jar`, Android build tools, D8, AAPT2, APKSigner and Clang/NDK-compatible C/JNI build tooling. External dependencies must be version-pinned. Keep signing secrets outside shared Android storage and preserve the keystore for updates.
```text
/storage/emulated/0/Documents/kt/       Kotlin source
/storage/emulated/0/Documents/c/        WavPack JNI source
/storage/emulated/0/Documents/xml/      Android resources and manifest
/storage/emulated/0/Documents/sh/       Termux build scripts
/storage/emulated/0/Documents/md/       design, README and licenses
/storage/emulated/0/Documents/apk/      signed APK artifacts
/storage/emulated/0/Documents/txt/      checksums and diagnostic exports
$HOME/.temporal_fractions_build/        private compiler workspace and signing keys
```
The current directory sorter may need a separate `.kt`→`Documents/kt` mapping. Script identities and downloadable build artifact names use `_vYYYYMMDDTHHMMSSZ`, but Android package ID stays stable (`cc0.temporalfractions.a0he7vjet48` proposed) across rebuilds so updates preserve data. Include SHA-256 and signature verification in the build procedure.
Reuse/refactor from the user repository `ov3qPI/Solar-Elevation-Alarm`, especially `SolarAlarmAudio.java`, `SolarAlarmPlaybackService.java`, `SolarAlarmWvPlayback.java`, `SolarAlarmWavPack.java`, the JNI bridge, exact scheduling patterns, and media notification behavior. Inspect and test actual source functionality rather than copying assumptions. WavPack 5.8.1 pinned at commit `4827b98` is BSD-3-Clause and requires its license in distributions. Original code is CC0; Kotlin runtime and all other third-party assets retain their licenses.
## 13. Reliability and privacy
The app should work entirely offline after installation. No runtime calculations of lunar phases or real solstice events are performed. No astronomical lookup tables can become stale or missing. Its vulnerability surface is therefore the Android alarm/clock system, long-duration playback, media file access, and audio routing.
Armed timers survive ordinary app restarts and Recents dismissal when Android permits. Reboot recovery must explicitly distinguish remaining vs missed timers; do not silently fire old alarms without a documented policy. Report exact-alarm permission failures, URI revocation, unsupported native codec errors, unavailable output routes and unexpected player shutdowns clearly. Keep all diagnostic data local and user-exported only.
Avoid CPU-intensive polling while the countdown is pending. When a media file plays for many hours, keep its decoder streaming efficiently and release all wake locks/resources at natural EOF. A file that is corrupt or stalls must be diagnosed and stopped by error recovery, but **not by a fixed elapsed-playback timeout**.
## 14. Acceptance tests
| Category | Mandatory checks |
|---|---|
| Reference constants | Exact nanosecond values, source attribution, immutable version identity, no accidental mean-tropical vs December-solstice confusion |
| Fraction engine | All permitted dyadic/triadic levels, rounding, 1-second UI floor, overflow, known sample durations |
| Countdown semantics | `start + fixed fraction`, not lunar/solstice alignment; consistent results on any calendar date |
| Presets | Unlimited CRUD, duplication, stable short/long sorting, same-preset multiple runs, safe schema migration |
| Scheduling | Exact-alarm authorization, screen off, Doze, reboot, clock edit, timezone/DST changes, stale/duplicate events, very long durations |
| Native playback | Actual device support for MP3, AAC, FLAC, Opus, Vorbis, AMR, PCM/WAV, MIDI and supported container variants; device-specific formats accepted when decodable |
| WavPack | Native `.wv` open/decode/EOF; corrupt or truncated WV; audio sampling and channels; JNI resource cleanup |
| Playback length | Long files play naturally past six hours without app-imposed cutoff; STOP, EOF, focus loss and failures clean up |
| Source documents | MIME/extension disagreement, persisted URI grants, missing/moved/deleted files, read errors |
| Concurrent alarms | One deterministic playback source at a time; manual preview never suppresses scheduled alarms; STOP ends session |
| Route restriction | Headphone jack, USB audio, A2DP/BLE, disconnected and multiply routed devices, unplug and reconnect during playback, blocked alarm notice |
| Device testing | Samsung XCover6 Pro API 36; live headset/speaker checks; overnight pending timers; long-form audio with screen off |
| Distribution | CC0 source and docs, WavPack BSD-3-Clause license, audited Kotlin/runtime notices, reproducible signed APK and checksum |
**Release gate:** Real-device validation is mandatory before promising reliable alarm delivery or meaningful speaker avoidance. A successful build alone is insufficient.
## 15. Implementation sequence
1. **Fixed reference periods and fraction engine:** freeze J2000 constants, implement and test exact integer/rational subdivisions and formatting.
2. **Kotlin ruler UI and presets:** implement a home dashboard and editor-only single ruler with dyadic/triadic tabs, preset CRUD, duration sorting, and active run display.
3. **Android exact countdowns:** durable run snapshots, scheduling, reboot and clock-change audits, trigger diagnostics.
4. **Generalized native audio plus WavPack:** Android MediaPlayer validation/playback across native supported formats, port WavPack decoder, shared notification and STOP, natural EOF without duration cap.
5. **Connected Audio Only:** unified routing checks for both playback backends, app icon and blocked state, rapid-transition tests.
6. **Reproducible Termux APK:** pinned dependencies, source/license inventory, signing, APK checksum and acceptance testing.
## 16. Decisions and open questions
### Settled by product direction
- Timer **only**; no Next Boundary mode or calendar-phase alignment.
- Mean, **constant** lunar and solstitial reference periods, not current lunar/annual event-pair durations.
- Subsecond representation for reference means, without claiming physical cycles are invariant to subsecond accuracy.
- Any audio Android can natively play on the device, plus bundled WavPack.
- No application-imposed playback cutoff; STOP or natural EOF controls ordinary end of session.
- An editor-only switchable dyadic/triadic ruler, unlimited sortable presets, multiple runs, optional external-audio-only restriction.
### Provisional implementation details
- Product name `Temporal Fractions` and stable package `cc0.temporalfractions.a0he7vjet48`.
- J2000 mean astronomical constants specified in §2; any replacement requires an explicit reference-standard change.
- One-second minimum selectable fraction and nearest-nanosecond calculation.
- Audio priority if multiple countdowns fire at nearly the same instant.
- Android API 36 used for the strongest available real-route verification; older platform policy to be decided after testing.
- Whether to provide one global default media document in addition to per-preset documents.
## 17. References and design provenance
- NASA GSFC, *Calendars and their History*: https://eclipse.gsfc.nasa.gov/SEhelp/calendars.html
- Jean Meeus seasonal-event polynomial reproduction: https://rubydoc.info/gems/astronoby/Astronoby/EquinoxSolstice
- Royal Museums Greenwich, seasonal-year means around 2000 CE: https://www.rmg.co.uk/sites/default/files/Calendars-from-around-the-world.pdf
- U.S. Naval Observatory, *The Seasons and the Earth's Orbit*: https://aa.usno.navy.mil/faq/seasons_orbit
- Android supported media formats: https://developer.android.com/media/platform/supported-formats
- Android `MediaPlayer` routing: https://developer.android.com/reference/android/media/MediaPlayer
- Android `AudioTrack` routing: https://developer.android.com/reference/android/media/AudioTrack
- Android exact alarms: https://developer.android.com/develop/background-work/services/alarms
- Android foreground service types: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files
- User's Solar Elevation Alarm repository: https://github.com/ov3qPI/Solar-Elevation-Alarm
- WavPack upstream source and BSD-3-Clause license: https://github.com/dbry/WavPack

Audio routing diagnostics: The editor offers a TEST AUDIO and STOP AUDIO control when an audio file is selected; Settings > DIAGNOSTICS shows connected output devices, media routing policy, media volume, and a timestamped local playback trace that can be copied. A restricted route must be positively verified before recording audio is released. Android audio routing may change asynchronously and requires physical device testing.
