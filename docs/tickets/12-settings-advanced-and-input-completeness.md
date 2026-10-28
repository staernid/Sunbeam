# Ticket 12: Expose Remaining Touch, Mouse, Keyboard & Advanced Settings

## Context

`StreamSettings.kt` defines 66 settings migrated from legacy preferences, but `SettingsSections.kt` currently only exposes 21 of them. Users cannot configure:
- Touch / Mouse mode (`mouseEmulation`, `absoluteMouseMode`, `trackpadSensitivityX`, `trackpadSensitivityY`, `trackpadSwapAxis`, `mouseNavButtons`, `analogStickScrolling`)
- Physical / Soft Keyboard options (`forceQwerty`, `backAsMeta`, `rightAltAsMeta`, `backAsGuide`)
- Controller USB & Motion fallbacks (`usbDriver`, `bindAllUsb`, `gamepadTouchpadAsMouse`, `gamepadMotionFallback`, `vibrateFallback`)
- Display & Behavior (`reduceRefreshRate`, `enforceDisplayMode`, `unlockFps`, `enablePip`, `resumeWithoutConfirm`, `disableWarnings`)

Rather than bloating the main Settings screen, these should live in clean expandable "Advanced..." cards or modal bottom sheets per category.

## Files to Modify

- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/component/SettingsSections.kt`
- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/component/SettingsDialogsHost.kt`
- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/SettingsUiState.kt`
- `features/settings/src/main/kotlin/io/github/staernid/sunbeam/features/settings/SettingsScreen.kt`
- `features/settings/src/test/kotlin/io/github/staernid/sunbeam/features/settings/SettingsScreenTest.kt`

## Steps

1. **Add Collapsible / Bottom-Sheet Subsections**:
   - **Touch, Mouse & Trackpad Sheet/Section**: `absoluteMouseMode` ("Touchscreen as Direct Touch vs Trackpad"), `trackpadSensitivityX` / `trackpadSensitivityY` slider, `mouseEmulation`, `mouseNavButtons`, `analogStickScrolling`.
   - **Keyboard & Shortcuts Sheet/Section**: `forceQwerty`, `backAsGuide` ("Android Back button acts as Guide/Home"), `backAsMeta`, `rightAltAsMeta`.
   - **Advanced Controller Sheet/Section**: `usbDriver` ("Custom USB Gamepad Driver"), `gamepadTouchpadAsMouse`, `vibrateFallback` ("Use Device Vibration if Controller Lacks Rumble").
   - **Advanced Display & Window Behavior**: `enablePip` ("Picture-in-Picture on Minimize"), `unlockFps` ("Unlock 90/120+ FPS Rates"), `reduceRefreshRate`, `resumeWithoutConfirm`.
2. **Add Search / Filter Bar in `SettingsScreen`**:
   - Add a collapsible search icon/bar in `SettingsScreen` top bar that filters visible setting items by title or description so users can find any setting immediately.
3. **Add unit tests** in `SettingsScreenTest.kt` verifying search filtering and toggling advanced settings.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew :features:settings:testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
```
