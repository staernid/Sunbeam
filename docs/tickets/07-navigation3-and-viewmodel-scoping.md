# Ticket 07: Proper ViewModel Scoping, Animated Transitions & Predictive Back

## Context

In `SunbeamApp.kt`, `ScreenContent` currently instantiates ViewModels (`HostsViewModel`, `AppsViewModel`, `SettingsViewModel`, `ProfilesViewModel`) using `remember { ...ViewModel(...) }` inside each `when (screen)` branch, and renders `screen` with no transition animation and no ` SaveableStateHolder`.
As a result:
- Navigating from `Hosts` -> `Apps` -> Back, or switching tabs, destroys and recreates the ViewModel and loses scroll/UI state.
- Screen transitions snap abruptly with zero motion.
- The dead `SunbeamScreen.Stream` branch in `SunbeamApp.kt` duplicates `Game` activity launching.

## Files to Modify

- `gradle/libs.versions.toml` (if adding `androidx.navigation3` or using `viewModel()` factory + `SaveableStateHolder`)
- `app/src/main/kotlin/io/github/staernid/sunbeam/ui/navigation/SunbeamScreen.kt`
- `app/src/main/kotlin/io/github/staernid/sunbeam/ui/SunbeamApp.kt`
- `app/src/test/kotlin/io/github/staernid/sunbeam/ui/navigation/SunbeamNavigatorTest.kt`
- `app/src/test/kotlin/io/github/staernid/sunbeam/ui/SunbeamAppTest.kt`

## Steps

1. **Preserve per-tab backstacks and UI state**:
   - Update `SunbeamNavigator` (or migrate to AndroidX Navigation 3 `NavDisplay` / `rememberNavBackStack`) so each top-level tab (`HOSTS`, `PROFILES`, `SETTINGS`) preserves its state, and navigating `Hosts -> Apps(hostUuid) -> Back` retains `HostsViewModel` and list scroll position via `rememberSaveableStateHolder()`.
2. **Scope ViewModels via `viewModel(key = ..., factory = ...)`**:
   - Replace `remember { HostsViewModel(...) }` etc. in `SunbeamApp.kt` with `viewModel(key = ...) { ... }` so ViewModels survive configuration changes and backstack pushes.
3. **Add `AnimatedContent` transitions**:
   - Wrap screen switching in `AnimatedContent` with clean Material 3 fade + slide/scale transitions (`fadeIn` + `slideInHorizontally` on push/pop, `fadeIn`/`fadeOut` on tab switch).
4. **Clean up `SunbeamScreen.Stream`**:
   - Ensure stream launching goes cleanly through `StreamSessionLauncher.launchGameActivity` without pushing a dead screen onto the backstack that immediately pops itself.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew :app:testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
```
