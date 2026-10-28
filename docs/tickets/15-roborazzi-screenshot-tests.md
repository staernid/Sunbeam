# Ticket 15: Add Roborazzi JVM Screenshot Tests for Compose Screens

## Context

All `:features:*` modules already use Robolectric + Compose UI Test (`testDebugUnitTest`). Adding **Roborazzi** enables fast, deterministic JVM screenshot verification of every screen (`HostsScreen`, `AppsScreen`, `SettingsScreen`, `ProfilesScreen`, `StreamScreen`) in both Light and Dark/OLED themes without needing an emulator.

## Files to Modify

- `gradle/libs.versions.toml`
- `plugins/build.gradle.kts` / feature `build.gradle.kts` files
- `features/hosts/src/test/kotlin/io/github/staernid/sunbeam/features/hosts/HostsScreenTest.kt`
- `features/apps/src/test/kotlin/io/github/staernid/sunbeam/features/apps/AppsScreenTest.kt`
- `features/settings/src/test/kotlin/io/github/staernid/sunbeam/features/settings/SettingsScreenTest.kt`
- `features/profiles/src/test/kotlin/io/github/staernid/sunbeam/features/profiles/ProfilesScreenTest.kt`
- `features/stream/src/test/kotlin/io/github/staernid/sunbeam/features/stream/StreamScreenTest.kt`

## Steps

1. Add `io.github.takahirom.roborazzi` plugin and `roborazzi`, `roborazzi-compose`, and `roborazzi-junit-rule` dependencies to `gradle/libs.versions.toml`.
2. Configure `@GraphicsMode(GraphicsMode.Mode.NATIVE)` on the screen test classes in `:features:hosts`, `:features:apps`, `:features:settings`, `:features:profiles`, and `:features:stream`.
3. Add screenshot capture assertions (`captureRoboImage`) for key UI states (empty state, populated list, dialog/drawer open) and record baseline reference images via `./gradlew recordRoborazziDebug`.
4. Verify screenshot tests pass with `./gradlew verifyRoborazziDebug`.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew verifyRoborazziDebug
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt
```
