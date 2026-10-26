# Upstream triage

Compared against `moonlight-stream/moonlight-android` (`moonlight/master`, "Version 12.2", 2026-09-12).
Our base is `ClassicOldSong/moonlight-android` (`artemis/moonlight-noir`), which forked from moonlight at
`27ded2ad` (2024-11-15). **55 upstream commits** landed since; ~26 are Weblate translations.

Remotes: `origin` (ours), `artemis` (ClassicOldSong), `moonlight` (moonlight-stream).
Reproduce: `git log --no-merges HEAD..moonlight/master`.

Verdict legend: **PORT** = take it as is, **ADAPT** = take the idea, our code differs, **SKIP** = not wanted.
"Decision" is filled in during review with the user.

## A. Bug fixes and compatibility (verified missing in our tree)

| # | Upstream commit | What | Status in fork | Rec. | Decision |
|---|---|---|---|---|---|
| 1 | `ddb674a9` | Android 16.1 native keyboard capture (`setKeyboardCaptureEnabled`, `CAPTURE_KEYBOARD`); Samsung `SemWindowManager` path kept as fallback and no longer logs when the class is absent. Fixes upstream #975, #1281, #1505 | Ported with reflection fallback for compileSdk < 37 | PORT | DONE (`987f9db4`) |
| 2 | `b9c5eddd` | `KEYCODE_PLUS` needs Shift; host has a single `=/+` VK | Ported | PORT | DONE (`887bc9d5`) |
| 3 | `3c6a0d12` | Rumble broken by deprecated `Vibrator` API; use `VibratorManager.getDefaultVibrator()` on API 31+ | Ported | PORT | DONE (`ab6af8a5`) |
| 4 | `abde6021` | Controller LED crashes/ANRs: run `LightsSession` work on the background thread, guard `close()`, fix `hasRgbControl()` on <API 34 | Ported | PORT | DONE (`86c0d103`) |
| 5 | `8974dcda` | Keycodes from Android 15/16: F13-F24, `PRINT`, `SCREENSHOT` | Ported | PORT | DONE (`5f3c6d6b`) |
| 6 | `280454fd` | Xbox Series S/X init packets (`SERIES_S_INIT`, PIDs `0x0b05/0x0b12/0x0b13`, `0x02fe`) | Ported | PORT | DONE (`ce4886ce`) |
| 7 | `583f662a` | 8BitDo vendor in `XboxOneController` | Present | none | ALREADY PRESENT |
| 8 | `0711e236` | `GameManager` crashes on Meta Quest / partial OEM impls | Adapted with try/catch | ADAPT | DONE (`07e2ae49`) |
| 9 | `6d4c64a5` | `Surface.setProducerThrottlingEnabled(false)` on the video surface, lower latency (API 37 guard) | Missing | PORT, measure first | Pending measurement |
| 10 | `68adf9ec` | Stop H.264 `constraint_set`/`level_idc` SPS patching on Android 8+ | Missing (we still patch) | PORT, needs device testing | Pending device test |
| 11 | `c2e224eb` | Sync SDL joystick code with upstream | Ported | PORT | DONE (`0d5b11ab`) |
| 12 | `3df0103a` | Remove dead `< API 21` paths | n/a (our `minSdk` is 21) | PORT, trivial | Postponed to Phase 2 |

## B. Platform, build and dependencies

| # | Upstream commit(s) | What | Fork today | Rec. |
|---|---|---|---|---|
| 13 | `0dc4c4fe`, `31b70030` | Update moonlight-common-c, libopus 1.6.1, openssl 4.0.2 (prebuilt `.a` files moved `libopus/` to `opus/`) | Old | PORT. Large binary diff, do it as its own commit |
| 14 | `1fa0e2a0` | NDK r29 | r27 | PORT |
| 15 | `9d8b073c`, `801dba1b`, `4b2221d3` | compileSdk/AGP 9.4, target API 36 | AGP 8.13, `targetSdk 34` | PORT. Subsumed by the rewrite's toolchain bump |
| 16 | `9221a0ca` | JDK 17 | JDK 11 | PORT |
| 17 | `b3a7e32a`, `98c12beb`, `4eb24a8d` | jmDNS, BouncyCastle, OkHttp 5.5; core library desugaring fixes jmDNS on old Android | OkHttp 4.12 | PORT |
| 18 | `3f114ac7`, `578f38f6`, `8d720748`, `ad861490` | AppVeyor and Lint fixes | | SKIP. We will use GitHub Actions |
| 19 | `5c0c2390` | Unused LNP permission justification string | | SKIP |

## C. Translations

~26 Weblate commits plus new locales (Bulgarian, Tamil, Latvian, ...) and `locales_config.xml`.
Rec.: **PORT the `values-*` resources wholesale**. This conflicts with our added strings, so merge
the files per locale and keep our keys. The rewrite will move strings to a Compose-friendly setup, so
do this early.

## D. Unmerged upstream branches

None are useful: `frame-pacing` (2017), `resolution_dialog` (2022 WIP), `unicode` (2021), and the 2014-15 branches.
Our own frame-pacing code (EWMA jitter in `MediaCodecDecoderRenderer`) already goes well beyond them.

## E. Things in our fork to review (hacky or opinionated)

These are not upstream commits, but the same review applies before we carry them into the rewrite.

| Area | Concern |
|---|---|
| `GlesPassthroughBridge` + SGSR1 shader (`34d518ad`, ours, "quick n dirty") | Works in a sidecar GL pass. Needs a real design: latency cost, HDR/10-bit, and a Surface lifecycle test |
| `Game.java` (4.3k lines), `ControllerHandler.java` (3.5k), `StreamSettings.java` (1.1k) | God classes. They block any UI rewrite, so the plan extracts them first |
| Whitespace and tab noise, `printStackTrace`, `Class.forName` reflection | Cleanup during the Kotlin port |
| Disclaimer section in the old README | Personal history, removed from our README |
| Apollo-only features (virtual display, server commands, clipboard sync, OTP pairing) | Decided: remove. We target official Sunshine only |
| `moonlight-common-c` submodule | Done: now `moonlight-stream/moonlight-common-c` at `5f22801`. Newer upstream needs `callbacks.c` (`receiveTimeUs`/`enqueueTimeUs`) and `nanors` in `Android.mk` (see B13) |
| SBS 3D with MiDaS v2 Lite | Niche, ships a model. Decide whether it stays in the core app |
| `applicationId` `com.limelight`, `.noir` suffixes, package `com.limelight` | Rename required for a proper fork identity (see ROADMAP phase 0) |

## Suggested order

1. A1-A6, A8 (small, high user value, low risk).
2. C translations.
3. B13-B17 as one dependency/toolchain commit.
4. A9, A10 behind a flag, with latency measurements on real devices.
