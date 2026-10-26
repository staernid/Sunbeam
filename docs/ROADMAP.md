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
- [ ] Review triage with the maintainer, mark decisions
- [ ] Cherry-port group A (bug fixes), then translations, then deps (own commits, `-x`)
- [x] Name: Sunbeam, at github.com/staernid
- [ ] Detach the fork (GitHub Support request). Keep history and `LICENSE.txt`
- [ ] Remove Apollo-only features (virtual display, server commands, clipboard sync, OTP). No migration of their settings
- [ ] Point `moonlight-common-c` submodule at upstream `moonlight-stream`, if our native code still builds against it
- [ ] Rename package (`com.limelight`) and `applicationId` (proposed `io.github.staernid.sunbeam`), in one commit
- [ ] CI on GitHub Actions: build, unit tests, lint. Drop `appveyor.yml`

## Phase 1: Build foundation

- Version catalog (`libs.versions.toml`), Gradle Kotlin DSL, convention plugins
- Toolchain: AGP 9.x, Kotlin 2.x, JDK 17+, NDK r29, `targetSdk` 36 (do this together with triage B13-B17)
- Strip `minSdk 21` legacy; choose a new floor (Voice uses 28) after checking the device base
- Flavors: collapse `root`/`nonRoot` and `game`; decide what's still needed
- Lint, ktlint and Compose stability config

## Phase 2: Make the core testable

- Extract from `Game.java` (4.3k lines): input routing, stream lifecycle, overlay/menu, display handling
- Extract from `ControllerHandler.java` (3.5k): device registry, rumble/LED/motion, driver glue
- Define a small `core:stream` API: `StreamSession(config) -> state Flow + events`; UI never touches `NvConnection` directly
- JNI behind one module `core:native`
- Unit tests for the protocol side (`nvstream.http`, pairing, serverinfo parsing) and the keyboard/mouse translators
- A fake host for end-to-end tests where possible

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

- Final `applicationId`
- New `minSdk`
- DI choice (Metro as in Voice, or Hilt/Koin)
- Does SBS 3D / MiDaS stay in the core app or become an optional module?
