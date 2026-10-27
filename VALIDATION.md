# Validation · 27 October 2026

## Version 0.2 checks

- Final `gradlew.bat assembleDebug lintDebug`: successful with JBR 21, Gradle 8.11.1, AGP 8.9.1 and SDK 35. Zero lint errors; four warnings (target SDK age, backup extraction rules, English-only checkbox text, intentional synchronous session persistence). Source-8 and deprecated compatibility APIs produce compiler warnings.
- 94 existing advisor/command-safety assertions passed.
- 38 new frame-statistics, session-comparability, restore-parser and thermal-policy assertions passed, compiled with `javac --release 8`. Total: 132 assertions.
- Installed and launched the 0.2 build on the available Android 13/API 33 x86_64 emulator.
- Opened Lab and verified its session controls. Granted this app notification permission for the test. Started its foreground recorder; `dumpsys activity services com.primo.optimizer` confirmed `isForeground=true` with notification 71.
- Collected QA-only device conditions, including a BEFORE run of 126 seconds/27 samples and an AFTER run of 96 seconds/21 samples. Both reported approximately five-second maximum sampling gaps, memory, battery percentage and battery-sensor temperature. No game was launched and no FPS improvement was established. These durations fall outside the allowed comparison ratio.
- Saved a custom Image quality priority profile at 30 FPS and 100% requested render scale. The UI showed saved targets separately from the unchanged Recommendation only / OS not checked state.
- Opened the CSV import explanation and Android document picker. File selection/import completion was not verified: the emulator became active in another app, and UI automation was stopped. A three-row synthetic fixture was staged solely for importer QA, not treated as benchmark evidence; no locally tested profile was certified.
- The foreground recorder was no longer active at the end of UI testing, confirmed by the service listing.
- Added activity page/section state preservation, session record deletion and unavailable-thermal fallback after these smoke checks. The final source was rebuilt and linted successfully. Those last UI changes were not retested on the shared emulator.

## Limits still requiring direct tests

- Actual rooted Android 14+ apply, read-back, recovery after partial failure and restoration. Strict AOSP-style parser tests cover captured old values, disabled scaling, zero/uncapped FPS, absent custom overrides, unsupported ANGLE/loading boost, unknown formats and invalid package names. They do not substitute for rooted-device integration testing.
- Restore is scoped to captured effective mode and supported custom FPS/scale. Original vendor-default versus override provenance and unrelated OS settings are not captured. Unknown/conflicting states fail closed.
- Actual rising-thermal notification delivery. Threshold, change direction and rate limiting have logic coverage; device sensors and channel behavior require runtime tests.
- Successful SAF CSV import/export and cancellation across provider implementations; malformed CSV parsing has unit coverage. Exports are user-directed local JSON, not automatically sent anywhere.
- Positive end-to-end game comparisons, measured FPS/frametime improvements, locally tested-profile certification, emulator wizard output, personal feedback and live verification on a root-enabled device.
- Physical-device coverage, Android 5–12 / 14+ runtime coverage, BlueStacks/LDPlayer direct compatibility, accessibility and comprehensive rotation/portrait QA. The document picker caused an orientation change during testing, motivating the added page/section preservation.
- Release signing and store-distribution requirements, including review of the foreground-service special-use declaration.

## Earlier version 0.1 checks

- APK installation, cold launch and overview visual inspection on the same Android 13 emulator.
- Selected an installed game package; saved recommendation and manual emulator override persisted after force-stop/relaunch.
- Android 13 root version gate prevented an unsupported command request.

No OS game tuning, thermal bypass or game-file modification was performed during this validation. Synthetic frame data is only a parser test fixture, not a performance result. QA scene labels identify recordings as non-gameplay. Screenshots/UI dumps live in ignored `test-output/`; they are not shipped as benchmark data.
