# FramePilot · Android Optimizer 0.2

Native Java app for selecting game settings and collecting evidence about their effects. Android 5.0+ (API 21), SDK target 35. No native libraries, accounts, ads or internet permission. Optional notification permission enables session/thermal notifications.

**Status:** version 0.2.0 is a debug/test MVP. Build and lint pass, and 132 pure Java assertions pass. Device readings, profile saving and foreground recording were exercised on an Android 13 emulator. Root apply/restore, completed file import/export, and direct BlueStacks/LDPlayer compatibility still require integration testing. See [VALIDATION.md](VALIDATION.md) for the actual test scope; these results are not gaming benchmarks.

## Contents

- [Features](#what-is-new)
- [First session](#first-session)
- [Checking whether tuning worked](#how-to-tell-if-a-setting-worked)
- [Measured frame-time CSV](#frame-time-csv)
- [Compatibility](#compatibility)
- [Build and install](#build-and-install)
- [Development and project structure](#development-and-project-structure)
- [Permissions and local data](#permissions-and-local-data)
- [Restore and failure handling](#restore-and-failure-handling)
- [Troubleshooting](#troubleshooting)
- [Release readiness](#release-readiness)
- [Platform references](#references)

## What is new

- **Session comparison:** user-started foreground recording samples available system RAM, battery-sensor temperature, battery percentage, battery saver and thermal severity every five seconds. Stop manually or at the 30-minute limit. Interrupted runs and sampling gaps are disclosed. Device-wide readings are not attributed automatically to the selected game.
- **Measured frame times:** import real profiler output as one-column CSV to calculate average FPS, p95 frame time, and frames over the chosen target budget (+5% tolerance). No synthetic FPS is generated. Live capture from third-party games is not implemented.
- **Custom profiles:** save manual in-game quality, requested FPS cap, render scale and consistency/image-quality/battery priorities. Explicit custom targets remain separate from automatic recommendations and applied OS state.
- **Captured-value restore:** root transactions journal the current Android user, mode and supported custom FPS/scale before mutation. Restore reads values back and retains the snapshot on failure. It restores captured effective values, not all OS settings or original vendor-default/override provenance. Unknown output, unsupported ANGLE/loading boost, and conflicting external changes block mutation/restore.
- **Game guidance:** starter templates for Mobile Legends, PUBG/BGMI, Genshin, Call of Duty and Plants vs. Zombies. These are editorial, untested starting points. Locally tested profiles require comparable completed sessions plus at least 120 imported measured frames per run. Evidence is scoped to the reported device and installed game version, and remains user supplied.
- **Thermal alerts:** during recording, notify when reported severity rises to Moderate or above, with a one-minute rate limit. Notifications and their channel must be enabled. Thermal protections remain enabled.
- **Emulator assistant:** separate BlueStacks 5 / LDPlayer / other setup paths, user-entered host CPU/RAM, conservative trial allocations, resolution/FPS steps, renderer comparisons and links to official instructions. The app cannot control or measure Windows hardware.
- **Compatibility report:** detected, unavailable, denied/not-checked and untested capabilities; last OS observation; recovery record; profile/session data; export through Android's document picker as JSON. Export excludes other installed apps and device serials but includes scene labels; review before sharing.
- **Personal calibration:** smooth/stuttered/too-hot feedback adjusts automatic suggestions conservatively. Feedback expires for recommendation purposes when the reported device/game version changes. Explicit custom targets are not silently changed.
- **One-tap readiness check:** reports memory pressure, heat and battery saver. This is not a RAM speed boost. It does not kill other apps, disable thermal protection or claim a universal FPS unlock.

## First session

1. Build and install the debug APK, then open **FramePilot**.
2. Open **Games** and choose your installed game. The list includes launcher apps because games do not consistently declare a game category. Package-name entry requires an installed, visible, launchable app.
3. In **Overview**, select a light, moderate or heavy workload. Review RAM, CPU cores, driver-reported GPU renderer, display refresh, battery-saver and thermal information. The recommendation is a conservative RAM/core-count heuristic, not a GPU benchmark or FPS forecast.
4. Use **Lab → Profiles** to save quality, FPS and scale targets. Image-quality priority supplies 100% scale/30 FPS starting targets; battery priority supplies 70%/30 FPS; consistency starts from the automatic recommendation. You can edit those targets before saving. Saving does not change Android or the game.
5. For non-rooted play, change graphics quality, shadows, effects and offered FPS options inside the game. There is no universal permission to edit another game's private settings.
6. Optionally use **Controls** on a supported Android 14+ rooted system. Grant root explicitly, inspect the capture/apply dialog, and use **Verify OS settings now** after applying. Restore is limited to captured supported values, as described below.
7. Record repeatable BEFORE/AFTER sessions under **Lab → Sessions**, return to the app to stop recording, and import actual game frame times if available. Device readings alone do not measure smoothness.
8. Use **Lab → Profiles → Personal calibration** to report smooth, stuttered or too-hot play. Stutter/heat feedback lowers automatic suggestions conservatively; smooth feedback does not automatically increase targets. Explicit custom settings remain unchanged.
9. Export **Lab → Report → Diagnostic JSON** when investigating compatibility. Review the file before sharing it.

For an emulator, enable **Treat this device as an emulator** if detection misses it. The **Lab → Emulator** assistant uses manually entered host information and supplies separate BlueStacks 5 / LDPlayer instructions; it does not configure the Windows host automatically.

## How to tell if a setting worked

1. **Recommendation saved:** a preference exists; nothing has been applied.
2. **OS request / read-back:** Controls can capture and apply supported root requests, then read back FPS, scale and mode. Acceptance is configuration evidence only. Last observations can become stale; use Verify OS settings now.
3. **Game behavior measured:** record the same scene BEFORE and AFTER one change, restart the game after render-scale changes, and import measured frame durations from that game's real profiler/export. Compare average FPS and p95 frame time. Lower p95 generally means fewer long frames, but different FPS caps change expected results.
4. Repeat runs under similar temperature, charging, brightness and scene conditions. Neither extra free RAM nor a single run establishes causality. The app cannot independently authenticate external CSV source attribution.

Choose a scene/route name and record each run for at least 60 seconds. The AFTER run must follow BEFORE, use the same name (case insensitive), match the reported device/game version, last between 80% and 125% of BEFORE's duration, and have no sample gaps over 15 seconds. Interrupted runs are excluded from comparison.

## Frame-time CSV

In **Lab → Sessions**, choose Import measured frame-time CSV for the corresponding completed run. Export or transform your real game profiler data to:

```csv
frame_time_ms
16.67
16.54
33.10
```

Numbers here illustrate the schema, not actual benchmark results. UTF-8, optional BOM, CRLF or LF; one positive duration per line; at least two frames; maximum 2 MB. No timestamps, extra columns, FPS values or fabricated data. Local test saving requires at least 120 frames per run. Imported statistics are attached to the selected session identity, not a newly replaced record.

## Compatibility

| Environment | Behavior | Limits |
| --- | --- | --- |
| Android 5–9 | Device readings, profiles, recorder, CSV import, export, setup guidance | Thermal severity unavailable; no root graphics path in this version |
| Android 10–13 | Above, plus reported thermal severity/alerts where available | Graphics changed manually in-game |
| Android 14+ without root | Above, plus manual desktop ADB commands | Debugging authorization required; manual commands are not covered by the in-app recovery journal |
| Android 14+ with root | Runtime-checked per-game GameManager FPS/scale, read-back, captured-value restore | Vendor shell format, opt-outs, game classification, root grant and external changes can block features |
| Desktop Android emulators | Same APK, virtual-device readings, profiles and assistant | Detection may miss spoofed identities; manual emulator override available; BlueStacks/LDPlayer require direct compatibility testing |

No application can guarantee zero lag, support every historical Android release or bypass every game engine cap. An OS FPS cap cannot increase the engine's native frame rate. Games may ignore interventions, and root command read-back does not prove effective game rendering. Battery sensor temperature is not CPU/GPU temperature.

## Build and install

Open in Android Studio with JDK 17 or 21 and Android SDK 35 installed, or use PowerShell:

### Prerequisites

- Git and Android Studio, or an Android SDK installation with command-line tools.
- Android SDK Platform 35 and compatible Build Tools (the local validation used Build Tools 35.0.0).
- JDK 17 or 21. Android Studio's bundled JBR 21 was used for validation. A newer unrelated system JDK may not be compatible with the selected Gradle version.
- Internet access for the first Gradle/dependency download. Runtime app use does not need network access.
- An Android 5.0+ device or emulator for installation. ADB is required for command-line installation and optional desktop commands.

The repository includes the Gradle wrapper. It downloads Gradle 8.11.1 and validates its configured SHA-256 checksum. The Android Gradle Plugin version is 8.9.1. A separate global Gradle installation is unnecessary.

### Clone and open

```sh
git clone https://github.com/supremobv/Android-Optimizer.git
cd Android-Optimizer
```

In Android Studio, choose **Open**, select the repository folder, select JDK 17/21 as the Gradle JDK, install SDK 35 if prompted, and sync. SDK paths belong in your environment or ignored `local.properties`, not in committed project files.

### Windows / PowerShell

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat assembleDebug lintDebug
```

### Linux / macOS

Set `JAVA_HOME` to a compatible local JDK and `ANDROID_HOME` to your Android SDK, then run:

```sh
./gradlew assembleDebug lintDebug
```

If your checkout does not preserve executable permissions, run `chmod +x gradlew` first. Build output is ignored by Git on all platforms.

### Install on a selected device

After enabling USB debugging and authorizing your computer, list device serials and choose the intended target:

```sh
adb devices
adb -s YOUR_DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s YOUR_DEVICE_SERIAL shell am start -n com.primo.optimizer/.MainActivity
```

Replace `YOUR_DEVICE_SERIAL` with the serial from your own device list. Selecting a serial avoids accidentally installing to a different phone or active emulator. Emulator-specific ADB connection instructions vary; use that emulator's official documentation rather than assuming a fixed localhost port.

The application ID is `com.primo.optimizer`. The debug APK can update an existing installation signed with the same debug key. A signing-key conflict requires an intentional migration or uninstall; uninstall deletes local app data and must not be used as a substitute for restoring pending OS tuning.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Debug signing is for local testing. Release signing, distribution policy review (including foreground-service special-use justification), accessibility/rotation QA, translations, backup rules and broader device testing are needed before public distribution.

Pure Java tests:

```powershell
New-Item -ItemType Directory -Force test-output | Out-Null
& "$env:JAVA_HOME\bin\javac.exe" --release 8 -d test-output app/src/main/java/com/primo/optimizer/Advisor.java app/src/main/java/com/primo/optimizer/FrameStats.java app/src/main/java/com/primo/optimizer/GameState.java app/src/main/java/com/primo/optimizer/ThermalPolicy.java tests/AdvisorTest.java tests/FeatureLogicTest.java
& "$env:JAVA_HOME\bin\java.exe" -cp test-output com.primo.optimizer.AdvisorTest
& "$env:JAVA_HOME\bin\java.exe" -cp test-output com.primo.optimizer.FeatureLogicTest
```

On Linux/macOS, use the same source-file list with `javac --release 8 -d test-output` and run the two classes with `java -cp test-output`.

Expected assertion results:

```text
Passed 94 advisor and command safety checks
Passed 38 session comparison and restore parsing checks
```

These are standalone Java test programs rather than JUnit/instrumentation tests. Running Gradle's default test task alone does not execute them. Android build/lint and the two test entry points are separate checks.

## Development and project structure

```text
app/
  build.gradle                         App version, SDK levels, Java settings
  src/main/AndroidManifest.xml         Launcher, permissions, recorder service
  src/main/java/com/primo/optimizer/
    MainActivity.java                 Overview, library, controls and navigation
    FeaturePanels.java                Profiles, sessions, assistant and report UI
    Advisor.java                      Conservative resource recommendations
    DeviceSnapshot.java               Android resource and sensor readings
    GpuProbe.java                     Offscreen EGL driver-renderer query
    Profiles.java                     Local JSON/profile helpers and identity
    GameCatalog.java                  Editorial game starter templates
    SessionService.java               Bounded foreground device sampling
    FrameStats.java                   Measured CSV statistics/comparison rules
    ThermalPolicy.java                Rising-severity alert/rate-limit rules
    RootShell.java                    Timed root-command execution boundary
    GameState.java                    Strict supported-state parser
    RootTuner.java                    Recovery journal, apply, verify and restore
  src/main/res/                       Native theme and vector launcher icon
tests/
  AdvisorTest.java                     94 advisor/command assertions
  FeatureLogicTest.java                38 statistics/restore/thermal assertions
gradle/wrapper/                       Versioned Gradle wrapper and checksum
README.md                             Setup, workflow, architecture and limits
VALIDATION.md                         Executed checks and unverified behaviors
```

The UI uses platform Android views and Java 8 source compatibility. No third-party app runtime libraries or ABI-specific native binaries are bundled. New Android API calls must be guarded for minSdk 21; keep unsupported capabilities visible rather than assuming a vendor implements them.

When extending root controls, validate identifiers and numeric values before building commands, durably journal supported old values before mutation, preserve the first snapshot across subsequent requests, and verify the result by read-back. Never treat shell exit status alone as game-performance evidence. Unknown vendor formats must stop the operation.

When extending measurements, preserve source attribution, units and sampling gaps. Do not generate estimated FPS and present it as a measured result. Game templates should remain untested until evidence exists for the relevant device/game version.

## Permissions and local data

| Permission / capability | Purpose | When used |
| --- | --- | --- |
| `FOREGROUND_SERVICE` | Keep the user-started session recorder visible to Android | During recording only |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Declare the recorder's special-use service type on newer Android | User-started bounded recording |
| `POST_NOTIFICATIONS` | Session and rising-thermal notifications on Android 13+ | Optional runtime grant; app/channel settings also apply |
| Launcher intent queries | Discover visible launchable apps | Games library refresh |
| Android document picker | Import a selected CSV or save a diagnostic JSON | User chooses a file/destination; no broad storage permission |
| Root manager grant | Execute the supported per-game OS controls | Only after an explicit root check/action |

No internet, accessibility-service, VPN, screen-capture, overlay, broad installed-app-list or all-files-access permission is requested. Root is not required for profiles, device readings, the recorder, calibration, the assistant or diagnostic export. ADB commands require separate desktop debugging authorization; they are not executed as ADB by the app.

Profiles, last completed BEFORE/AFTER records, imported frame statistics, feedback, assistant preferences and pending recovery records are stored locally in app-private preferences. The app keeps one completed run per side for each selected package; starting a replacement prompts before overwriting it. Imported raw frame rows are reduced to statistics rather than retained as a full trace. Completed records can be deleted from the Sessions screen.

Exports contain reported device specs, the chosen package/version, scene labels, profiles, recovery information and recorded conditions. They do not contain a scan of other installed applications, advertising identifiers or device serials. There is no automatic upload. Disable/deny notifications if desired, but then thermal alerts may not be visible; this does not disable Android's own thermal protections.

## Restore and failure handling

Root snapshots are saved durably before the first modifying command and preserved across repeated changes. Restore affects only the selected game's captured mode and supported custom FPS/scale, leaving other game modes untouched. Absent custom configuration is removed using reset --mode 4. Existing supported custom values are reissued explicitly. Original default-versus-override provenance is not available from the shell output. Unrelated OS settings are not captured.

Settings that cannot be represented safely by this tool block tuning. If another tool changes FPS/scale while a snapshot is pending, FramePilot stops instead of overwriting those changes. A partial failure retains the journal; export diagnostics and inspect before retrying. Repeated apply does not replace the original snapshot.

Restore before uninstalling. Uninstalling does not undo OS tuning. Legacy version 0.1 requests have no original-state journal and cannot be restored exactly. Manual ADB requests save command output to desktop files but are not automatically managed by the in-app root restore path.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Gradle fails before compiling | Check JDK 17/21, SDK 35, `ANDROID_HOME` or local SDK path, and first-download network access. Use the checked-in wrapper. |
| ADB lists multiple targets | Use `adb -s YOUR_DEVICE_SERIAL` for every install/command. Do not assume the first device is the right one. |
| A game is missing from Games | Refresh the launcher list. Android package visibility, a missing launcher activity or work-profile boundaries can prevent discovery. Package entry does not bypass those restrictions. |
| GPU/SoC identity looks surprising | Drivers and emulators may expose virtual, spoofed or unknown identities. The displayed renderer is driver-reported and is not a throughput measurement. |
| Thermal severity is unavailable | Android versions below 10 do not expose this app's thermal API; some vendor implementations may also be unavailable. Battery temperature is a separate sensor. |
| Root graphics actions are absent | This app requires Android 14+, explicit root grant and the required live `cmd game` commands. Root alone does not guarantee game support. |
| A request is blocked by the parser | The output/settings cannot be captured and restored safely by the supported format. Keep the diagnostic evidence; do not bypass the guard with an assumed default. |
| OS read-back matches but the game looks unchanged | Restart after scale changes. Game classification, opt-outs, vendor implementation and the game's own engine may prevent an effect. Measure the actual game rather than trusting the settings label. |
| A session is interrupted or has long gaps | The OS may stop/suspend the service. Keep the recorder notification enabled, stop the run normally when possible, and repeat comparable runs. Force-stop/reboot does not invent missing samples. |
| Thermal alerts do not appear | Check Android notification permission, app notification settings, the Thermal alerts channel, an active recording, and a reported severity rise to Moderate or above. |
| CSV import fails | Use a single `frame_time_ms` column, positive finite millisecond durations, UTF-8 text, at least two frames and no more than 2 MB. Do not supply instantaneous FPS as milliseconds. |
| A local test cannot be saved | Both completed runs need the same scene, matching device/game identity, acceptable durations/gaps and at least 120 imported measured frames each. User-supplied attribution remains unverified. |
| Restore stops after another optimizer was used | Inspect/export the retained recovery record. FramePilot stops when current values conflict with its last request instead of silently overwriting external changes. |
| BlueStacks/LDPlayer is not recognized | Enable the manual emulator override and use the assistant. Actual host CPU/GPU performance is not visible through the virtual Android specs. |

## Release readiness

Before distributing a release beyond local testing:

- Exercise apply, read-back, partial failure, permission denial and restore on supported rooted Android 14+ systems, including unknown vendor output and externally changed settings.
- Complete document-provider import/export tests, notification/channel tests, service interruption tests, and accessibility/rotation/portrait checks.
- Test representative physical phones, older supported Android releases, current Android releases, BlueStacks and LDPlayer. Do not turn compile-time compatibility into a universal runtime claim.
- Calibrate game-specific recommendations using repeatable real measurements. Keep the starter templates and user-supplied evidence distinguished from independently verified benchmarks.
- Review current distribution requirements for SDK targeting, foreground-service special-use declarations, backup behavior and privacy disclosures.
- Configure a private release signing key outside the repository and verify the signed artifact. APK/AAB outputs, local SDK settings, keystores and QA output are ignored by Git.

The repository publishes source and build instructions. A hosted release APK, automatic update service, CI pipeline and public benchmark database are not included.

## References

- [Android Game Mode interventions](https://developer.android.com/games/optimize/adpf/gamemode/gamemode-interventions)
- [FPS throttling limits](https://developer.android.com/games/optimize/adpf/gamemode/fps-throttling)
- [Thermal API](https://developer.android.com/games/optimize/adpf/thermal)
- [Android 14 background process limits](https://developer.android.com/about/versions/14/behavior-changes-all)
- [AOSP GameManager shell implementation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/services/core/java/com/android/server/app/GameManagerShellCommand.java)
- [BlueStacks settings](https://support.bluestacks.com/hc/en-us/articles/360053245871-How-to-use-the-BlueStacks-5-Settings-menu)
- [LDPlayer CPU/RAM allocation](https://www.ldplayer.net/support/ways-to-configure-ram-and-cpu-assignment.html)

See `VALIDATION.md` for executed checks and what remains unverified.
