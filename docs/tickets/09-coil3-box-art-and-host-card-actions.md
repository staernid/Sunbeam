# Ticket 09: Coil 3 Box Art Loading & Rich `HostCard` Actions (Resume, WoL, Help)

## Context

1. `AppCard.kt` loads box art using raw `BitmapFactory.decodeFile(path)` inside `produceState` with no memory cache, no downsampling, and no crossfade animation.
2. `HostCard.kt` shows `"Active Game (ID: 12345)"` instead of the running game's name, has no 1-tap **Resume** or **Quit** button when a game is running on that host, and has no **Wake** button when a paired host is `OFFLINE` with a MAC address.
3. In `SunbeamApp.kt`, `HostsScreen` passes `onHelpClick = {}` (a dead button).

## Files to Modify

- `gradle/libs.versions.toml`
- `features/apps/build.gradle.kts`
- `features/apps/src/main/kotlin/io/github/staernid/sunbeam/features/apps/component/AppCard.kt`
- `features/hosts/src/main/kotlin/io/github/staernid/sunbeam/features/hosts/component/HostCard.kt`
- `features/hosts/src/main/kotlin/io/github/staernid/sunbeam/features/hosts/component/HostDialogs.kt`
- `features/hosts/src/main/kotlin/io/github/staernid/sunbeam/features/hosts/HostsScreen.kt`
- `features/hosts/src/main/kotlin/io/github/staernid/sunbeam/features/hosts/HostsUiState.kt`
- `features/hosts/src/main/kotlin/io/github/staernid/sunbeam/features/hosts/HostsViewModel.kt`
- `core/data/src/main/kotlin/io/github/staernid/sunbeam/core/data/repository/HostOperationsHandler.kt`
- `app/src/main/kotlin/io/github/staernid/sunbeam/discovery/ComputerManagerBridge.kt`
- `app/src/main/kotlin/io/github/staernid/sunbeam/ui/SunbeamApp.kt`
- `features/hosts/src/test/kotlin/io/github/staernid/sunbeam/features/hosts/HostsScreenTest.kt`
- `features/hosts/src/test/kotlin/io/github/staernid/sunbeam/features/hosts/HostsViewModelTest.kt`

## Steps

1. **Adopt Coil 3 in `:features:apps`**:
   - Add `io.coil-kt.coil3:coil-compose` to `gradle/libs.versions.toml` and `features/apps/build.gradle.kts`.
   - Replace `BitmapFactory.decodeFile` in `AppCard.kt` (`BoxArtView`) with Coil's `SubcomposeAsyncImage` / `AsyncImage` with `crossfade(true)` and fallback to `AppPlaceholder`.
2. **Add Wake-on-LAN and 1-Tap Resume to `HostCard`**:
   - Add `suspend fun wakeHost(host: Host): Result<Unit>` to `HostOperationsHandler` and implement it in `ComputerManagerBridge` using `WakeOnLanSender`.
   - When `host.state == HostState.OFFLINE` and `host.pairState == PairState.PAIRED` and `!host.macAddress.isNullOrBlank()`, render a tonal **"Wake"** button on `HostCard` that triggers `onWakeClick(host)` and shows a snackbar (`"Wake-on-LAN packet sent to ${host.name}"`).
   - When `host.currentGameId != 0`, allow 1-tap **"Resume"** right from the `HostCard` footer.
3. **Wire the Help Button on `HostsScreen`**:
   - Add a `Troubleshooting / Setup Guide` bottom sheet or dialog (`HostsDialogState.Help`) or open the official Sunshine documentation URL when the Help icon is tapped, instead of `onHelpClick = {}`.
4. **Update unit tests**:
   - Add tests in `HostsScreenTest.kt` and `HostsViewModelTest.kt` covering the Wake button on offline paired hosts, Resume button on hosts with `currentGameId != 0`, and Help dialog/action.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew :features:hosts:testDebugUnitTest :features:apps:testDebugUnitTest :app:testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
```
