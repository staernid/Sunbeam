# Ticket 13: Wire Real Gamepad Input Tester & Interactive On-Screen Controls Editor

## Context

In `:features:profiles` (`ProfilesScreen.kt`):
1. `GamepadTesterView` renders sticks, triggers, buttons, and gyro, **but nothing feeds real Android `KeyEvent` or `MotionEvent` data into `ProfilesViewModel.updateGamepadState(...)`**, and `onTestRumble` only updates a float in state without vibrating any connected controller or device vibrator!
2. `OscPreviewView` draws static mock circles on a Canvas and never writes `oscOpacity` or `oscScale` back to `PreferencesRepository`.

## Files to Modify

- `features/profiles/src/main/kotlin/io/github/staernid/sunbeam/features/profiles/component/GamepadTesterView.kt`
- `features/profiles/src/main/kotlin/io/github/staernid/sunbeam/features/profiles/component/OscPreviewView.kt`
- `features/profiles/src/main/kotlin/io/github/staernid/sunbeam/features/profiles/ProfilesScreen.kt`
- `features/profiles/src/main/kotlin/io/github/staernid/sunbeam/features/profiles/ProfilesViewModel.kt`
- `app/src/main/kotlin/io/github/staernid/sunbeam/ui/SunbeamApp.kt`
- `features/profiles/src/test/kotlin/io/github/staernid/sunbeam/features/profiles/ProfilesScreenTest.kt`
- `features/profiles/src/test/kotlin/io/github/staernid/sunbeam/features/profiles/ProfilesViewModelTest.kt`

## Steps

1. **Capture Real Gamepad Events in `GamepadTesterView`**:
   - Attach an `onPreviewKeyEvent` / `View.OnGenericMotionListener` + `InputManager.InputDeviceListener` inside `GamepadTesterView` (when the `GAMEPAD_TESTER` tab is active) that reads connected `SOURCE_GAMEPAD` / `SOURCE_JOYSTICK` devices, maps `AXIS_X`, `AXIS_Y`, `AXIS_Z`, `AXIS_RZ`, `AXIS_LTRIGGER`, `AXIS_RTRIGGER`, `AXIS_HAT_X`, `AXIS_HAT_Y`, and `KEYCODE_BUTTON_*` to `GamepadState`, and forwards them to `ProfilesViewModel.updateGamepadState`.
2. **Real Rumble Test**:
   - When the user triggers `onTestRumble(strength)` in `GamepadTesterView`, vibrate the connected `InputDevice`'s `VibratorManager` (or fallback to system `Vibrator`) proportional to `strength`.
3. **Persist On-Screen Controls Adjustments & Reset**:
   - Inject `PreferencesRepository` into `ProfilesViewModel` (or pass callbacks) so changing opacity in `OscPreviewView` persists to `StreamSettings.oscOpacity` in DataStore, and add a **"Reset Layout to Default"** button that clears saved virtual controller element positions in SharedPreferences (`VirtualControllerConfigurationLoader`).
4. **Update tests** in `ProfilesViewModelTest.kt` and `ProfilesScreenTest.kt`.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew :features:profiles:testDebugUnitTest :app:testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
```
