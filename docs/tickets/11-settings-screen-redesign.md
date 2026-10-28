# Ticket 11: Redesign `SettingsScreen` (Quick Presets, Grouped M3 Cards, Human Labels & Theme Controls)

## Context

Currently, `SettingsScreen.kt` and `SettingsSections.kt` have several UX and visual problems:
1. It renders 21 settings in a single flat edge-to-edge list separated only by thin `HorizontalDivider`s.
2. `SettingValueItem` displays raw Kotlin enum `.name` values (`FIT`, `AUTO`, `MIN_LATENCY`, `STEREO`) instead of human-readable labels (`Fit to Screen`, `Automatic`, `Minimum Latency`, `Stereo (2.0)`).
3. There is no quick way to pick a streaming preset (e.g., *Competitive 120Hz*, *Balanced 1080p60*, *4K HDR Quality*) without opening 4 separate dialogs and dragging sliders.
4. `SunbeamTheme` supports `oledBlack` and `dynamicColor`, but `MainActivity` never passes settings to `SunbeamTheme`, and users cannot toggle OLED Black or Dynamic Color in `SettingsScreen`.

## Files to Modify

- `core/preferences/src/main/kotlin/io/github/staernid/sunbeam/core/preferences/models/StreamSettings.kt`
- `core/preferences/src/main/kotlin/io/github/staernid/sunbeam/core/preferences/datastore/StreamPreferenceKeys.kt`
- `core/preferences/src/main/kotlin/io/github/staernid/sunbeam/core/preferences/datastore/StreamSettingsMapper.kt`
- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/component/SettingItem.kt`
- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/component/SettingsSections.kt`
- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/SettingsScreen.kt`
- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/SettingsUiState.kt`
- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/io/github/staernid/sunbeam/MainActivity.kt`
- `features/settings/src/test/kotlin/io/github/staernid/sunbeam/features/settings/SettingsScreenTest.kt`
- `features/settings/src/test/kotlin/io/github/staernid/sunbeam/features/settings/SettingsViewModelTest.kt`

## Steps

1. **Add Theme Preferences to `StreamSettings`**:
   - Add `val oledBlackMode: Boolean = false` and `val dynamicColor: Boolean = true` to `StreamSettings`, `StreamPreferenceKeys`, and `StreamSettingsMapper`.
   - Observe `preferencesRepository.settingsFlow` in `MainActivity.kt` and pass `oledBlack = settings.oledBlackMode` and `dynamicColor = settings.dynamicColor` into `SunbeamTheme`.
2. **Human-Readable Labels for Enums**:
   - Create display label mappers for `VideoScaleMode` (`Fit (Maintain Aspect)`, `Fill (Crop to Screen)`, `Stretch`), `VideoFormatOption` (`Automatic`, `H.264`, `HEVC (H.265)`, `AV1`), `FramePacingMode` (`Minimum Latency`, `Balanced`, `Cap FPS`, `Maximum Smoothness`), and `AudioConfiguration` (`Stereo (2.0)`, `5.1 Surround`, `7.1 Surround`). Never render `.name` directly in the UI.
3. **Add a Top "Quick Quality Preset" Card**:
   - At the top of `SettingsScreen`, render a card with FilterChips/SegmentedButtons for **Low Latency (1080p 120FPS)**, **Balanced (1080p 60FPS 20Mbps)**, **Ultra Quality (1440p/4K HDR 60Mbps)**, and **Custom** that updates resolution, FPS, bitrate, and frame pacing in one tap.
4. **Group Settings into Rounded Material 3 Cards (`SettingGroupCard`)**:
   - Wrap each section (`Appearance`, `Video & Display`, `Audio`, `Gamepad & Input`, `On-Screen Controls`, `Performance & Diagnostics`) inside a rounded `Surface` / `Card` (`shape = RoundedCornerShape(20.dp)`, `color = MaterialTheme.colorScheme.surfaceContainerLow`, `padding(horizontal = 16.dp)`) instead of flat full-bleed rows and `HorizontalDivider`s.
5. **Update tests**:
   - Update `SettingsScreenTest.kt` and `SettingsViewModelTest.kt` for the human-readable labels, preset chips, and Appearance toggles.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew :core:preferences:testDebugUnitTest :features:settings:testDebugUnitTest :app:testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
```
