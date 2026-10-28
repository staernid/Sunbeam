# AGENTS.md

Sunbeam: Android game-stream client (Moonlight lineage) for the official Sunshine server. Moving from legacy Java in `:app` to modular Kotlin + Compose (`core/*`, `features/*`). Next implementation tasks: [docs/tickets/README.md](docs/tickets/README.md).

## Commands

- Build: `./gradlew :app:assembleDebug`
- Unit tests (JVM/Robolectric): `./gradlew testDebugUnitTest`. See [android_test_setup.md](android_test_setup.md).
- Native code lives in `core/native/src/main/jni/moonlight-core` (submodules). Run `git submodule update --init --recursive` first.
- Lint & Code Health: `./gradlew lintDebug ktlintCheck detekt`

## Git

- Remotes: `origin` (ours), `artemis` (ClassicOldSong, reference only), `moonlight` (moonlight-stream). Don't push to the last two.
- Taking upstream changes: use `git cherry-pick -x` so the origin stays traceable. Record the decision in [docs/UPSTREAM_TRIAGE.md](docs/UPSTREAM_TRIAGE.md).
- Binary blobs (`*.a`, openssl, opus) change in their own commits.
- Never rewrite published history or force-push without being asked.

## Conventions

- New code is Kotlin. Don't add Java files. When you touch a Java file for more than a small fix, convert or extract the part you touch.
- Don't grow `Game.java`, `ControllerHandler.java` or `StreamSettings.java`. Extract into a new class and call it from there.
- Comments only for a non-obvious *why*. Don't restate the code. Keep existing comments you aren't changing.
- No `printStackTrace`. Use `LimeLog` until the logging module exists, and don't log host names, addresses or pairing data.
- Target official Sunshine only. Don't add Apollo-specific features, and remove the inherited ones when you touch that code.
- Keep it simple. If an edge case needs a lot of code or a data migration, name it and ask.

## Safety

- Never uninstall an installed build to get around `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. It wipes the user's paired hosts. Ask first.
- Don't commit `local.properties`, keystores or signing config.
- Latency-sensitive paths (`MediaCodecDecoderRenderer`, input send path, `GlesPassthroughBridge`): no allocation in per-frame or per-event code, and say how you measured any change.
- Video decoder behavior varies per SoC. State which devices a change was tested on, or say it was untested.

## Reference

- [Voice](https://github.com/PaulWoitaschek/Voice) is the structural model: `core/`, `features/`, `navigation/`, Metro DI, Navigation 3, version catalog, convention plugins in `plugins/`.
- Triage of upstream commits: [docs/UPSTREAM_TRIAGE.md](docs/UPSTREAM_TRIAGE.md).
