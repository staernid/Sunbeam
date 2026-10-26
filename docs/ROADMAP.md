# Roadmap

Goal: replace the Java, single-module, View-based app with a modular Kotlin + Compose app that keeps the streaming
core intact. Structural model: [Voice](https://github.com/PaulWoitaschek/Voice) (`app/`, `core/*`, `features/*`,
`navigation/`, `plugins/` for convention plugins, Metro DI, Navigation 3, Room/DataStore, version catalog).

Principle: **strangler, not big bang.** The app must stream after every phase. The streaming core (`nvstream`,
`MediaCodecDecoderRenderer`, JNI, input translation) is the valuable part, and it's the riskiest to rewrite blind. It is
wrapped and tested before it is touched.

## Phase 0: Fork hygiene (days)

- [x] Add `artemis` and `moonlight` remotes
- [x] Upstream triage ([UPSTREAM_TRIAGE.md](UPSTREAM_TRIAGE.md))
- [x] Review triage with the maintainer, mark decisions
- [x] Cherry-port group A (bug fixes) and new translations
- [x] Name: Sunbeam, at github.com/staernid
- [ ] Detach the fork (GitHub Support request). Keep history and `LICENSE.txt`
- [x] Remove Apollo-only features (virtual display, server commands, clipboard sync, OTP). No migration of their settings
- [x] Point `moonlight-common-c` submodule at upstream `moonlight-stream`
- [x] Set `applicationId` to `io.github.staernid.sunbeam` and app label to Sunbeam
- [x] Rename source package (`com.limelight` -> `io.github.staernid.sunbeam`)
- [x] CI on GitHub Actions: build, unit tests, lint. Drop `appveyor.yml`

## Phase 1: Build foundation

- [x] Version catalog (`libs.versions.toml`), Gradle Kotlin DSL, Kotlin 2.x support
- [x] Toolchain: JDK 17 target compatibility, `targetSdk` 36, core library desugaring (triage B13-B17 aligned; NDK r27 kept for CI/workstation compatibility)
- [x] Convention plugins in `plugins/` (`sunbeam.android.application`, `sunbeam.android.library`, `sunbeam.android.compose`, `sunbeam.lint`)
- [x] Strip `minSdk 21` legacy; set floor to API 28 (Android 9.0 Pie) aligning with Voice
- [x] Flavors: collapsed legacy `root`/`nonRoot` and `game` down to standard `debug` and `release` variants
- [x] Lint, ktlint, detekt and Compose stability config

## Phase 2: Make the core testable

- [ ] Extract from `Game.java` (4.3k lines down to ~2.6k): stream lifecycle, keyboard routing, and pointer/touch/pen/mouse routing extracted to `StreamLifecycleManager`, `StreamErrorResolver`, `KeyboardInputHandler`, `PointerInputHandler` (overlay/menu, display handling remaining)
- [ ] Extract from `ControllerHandler.java`: rumble, LED, and motion subsystems extracted to `ControllerRumbleManager`, `ControllerLightsManager`, `ControllerSensorManager` (device registry & driver glue remaining)
- [x] Define a small `core:stream` API: `StreamSession(config) -> state Flow + events`, implemented by `NvStreamSession` bridging native JNI callbacks to reactive Flows; UI never touches `NvConnection` directly
- [x] JNI behind one module `core:native`
- [x] Unit tests for the protocol side (`nvstream.http`, pairing, serverinfo parsing) and the keyboard/mouse translators
- [ ] A fake host for end-to-end tests where possible

## Phase 3: Data and settings

- `PreferenceConfiguration` (SharedPreferences, Java) → typed settings on DataStore, with migration from the existing keys
- Hosts and profiles → Room (or DataStore), with import of the current storage
- Use the feature flags Sunshine reports in `serverinfo` to drive the UI

## Phase 4: Compose UI, screen by screen

Order by risk. The stream screen goes last.

1. Theme and design system (Material 3, dynamic color, TV/tablet/foldable window size classes)
2. Host list and pairing
3. App grid
4. Settings (the biggest win, replaces a 1.1k-line `StreamSettings`)
5. Profiles, on-screen controller editor, gamepad debug
6. Stream screen: keep the `SurfaceView`, wrap it in `AndroidView`, move overlays/menu to Compose

Each screen: `features:<name>` module, UI + presenter (Molecule or ViewModel, to decide), screenshot tests.

## Phase 5: Streaming improvements

- Make the SGSR upscaler a proper render stage (latency measured, HDR/10-bit, lifecycle-safe)
- Frame-pacing: replace ad-hoc EWMA with a documented, tested model and a latency overlay
- Re-evaluate `setProducerThrottlingEnabled`, SPS-patch removal (triage A9/A10) with real measurements
- Gamepad, keyboard capture and external-display polish

## Phase 6: Release

- Localization pipeline (Weblate or equivalent), accessibility pass, TV/Chromebook check
- Signed release builds, changelogs under `fastlane/`, Obtainium/F-Droid metadata
- Remove the legacy Java UI

## Open questions

- Final `applicationId` (currently `io.github.staernid.sunbeam`)
- New `minSdk` (Decided: 28, matching Voice)
- DI choice (Metro as in Voice, or Hilt/Koin)
- Does SBS 3D / MiDaS stay in the core app or become an optional module?
