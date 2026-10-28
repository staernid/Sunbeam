# Ticket 02: Bridge DataStore `StreamSettings` into `Game.java` & `PreferenceConfiguration`

## Context

`SettingsScreen` (`:features:settings`) writes user preferences to `DataStorePreferencesRepository` (`:core:preferences`), while `Game.java` still reads preferences via `PreferenceConfiguration.readPreferences(Context)` from legacy `SharedPreferences`.
Currently, `StreamSessionLauncher` only passes 4 fields (`width`, `height`, `fps`, `bitrate`) via `Intent` extras (`StreamIntentParser`). Every other setting toggled in Compose `SettingsScreen` (HDR, codec, frame pacing, audio config, SGSR upscaling, ultra-low latency, multi-controller, rumble, deadzone, flip face buttons, gyro, on-screen controls, performance overlay) is ignored when `Game.java` starts!

## Files to Modify

- `core/preferences/src/main/kotlin/io/github/staernid/sunbeam/core/preferences/models/StreamSettings.kt` (read-only reference unless helper needed)
- `app/src/main/java/io/github/staernid/sunbeam/stream/StreamIntentParser.kt`
- `app/src/main/kotlin/io/github/staernid/sunbeam/ui/stream/StreamSessionLauncher.kt`
- `app/src/main/java/io/github/staernid/sunbeam/preferences/PreferenceConfiguration.java`
- `app/src/main/java/io/github/staernid/sunbeam/Game.java`
- `app/src/test/java/io/github/staernid/sunbeam/stream/StreamIntentParserTest.kt`
- `app/src/test/kotlin/io/github/staernid/sunbeam/ui/stream/StreamSessionLauncherTest.kt`

## Steps

1. **Create a Kotlin mapper from `StreamSettings` to `PreferenceConfiguration`**:
   - Add a function `StreamIntentParser.applyStreamSettings(prefConfig: PreferenceConfiguration, settings: StreamSettings)` (or in a dedicated Kotlin helper `PreferenceConfigurationBridge.kt`) that populates all fields of `PreferenceConfiguration` from the typed `StreamSettings` model (`width`, `height`, `fps`, `bitrate`, `videoFormat`, `framePacing`, `enableHdr`, `fullRange`, `enableUltraLowLatency`, `videoSuperResolution`, `videoScaleMode`, `audioConfiguration`, `playHostAudio`, `multiController`, `enableRumble`, `deadzonePercentage`, `flipFaceButtons`, `gamepadMotionSensors`, `onscreenController`, `hideOscWhenHasGamepad`, `oscOpacity`, `vibrateOsc`, `enablePerfOverlay`, `enablePerfOverlayBottom`, `enableLatencyToast`, etc.).
2. **Sync or pass `StreamSettings` on stream launch**:
   - In `StreamSessionLauncher.createGameActivityIntent` / `launchGameActivity`, resolve the current `StreamSettings` from `PreferencesRepository` (with active `Profile` overrides applied) and either:
     - Write/apply it via `PreferenceConfigurationBridge` so `Game.java` receives the exact `PreferenceConfiguration` from DataStore, or
     - Pass the serialized/bundled `StreamSettings` in the `Intent` and apply it in `StreamIntentParser.applyOverrides`.
3. **Write unit tests**:
   - In `StreamIntentParserTest.kt` and `StreamSessionLauncherTest.kt`, verify that toggling each setting in `FakePreferencesRepository` (e.g., `enableHdr = true`, `videoSuperResolution = true`, `flipFaceButtons = true`, `enablePerfOverlay = true`, `deadzonePercentage = 15`, `audioConfig = SURROUND_51`) produces a `PreferenceConfiguration` with the matching fields set.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew :app:testDebugUnitTest :core:preferences:testDebugUnitTest
./gradlew ktlintCheck detekt
```
