# Ticket 10: Game-First Home Experience & Quick Host Switcher

## Context

Most users have 1 primary Sunshine PC. Currently, every time the user opens Sunbeam, they land on `HostsScreen`, wait for discovery, and must tap their PC card to reach `AppsScreen`.
When the user has an online paired host (either a single paired host or their last-selected paired host), Sunbeam should take them straight into their Games Library (`AppsScreen`) with a sleek Host Switcher pill in the top app bar to switch hosts or manage hosts at any time.

## Files to Modify

- `core/preferences/src/main/kotlin/io/github/staernid/sunbeam/core/preferences/datastore/StreamPreferenceKeys.kt`
- `core/preferences/src/main/kotlin/io/github/staernid/sunbeam/core/preferences/models/StreamSettings.kt`
- `core/preferences/src/main/kotlin/io/github/staernid/sunbeam/core/preferences/datastore/StreamSettingsMapper.kt`
- `features/apps/src/main/kotlin/io/github/staernid/sunbeam/features/apps/AppsScreen.kt`
- `app/src/main/kotlin/io/github/staernid/sunbeam/ui/SunbeamApp.kt`
- `app/src/test/kotlin/io/github/staernid/sunbeam/ui/SunbeamAppTest.kt`

## Steps

1. **Persist `lastSelectedHostUuid` in `StreamSettings` / DataStore**:
   - Add `val lastSelectedHostUuid: String? = null` to `StreamSettings`, `StreamPreferenceKeys`, and `StreamSettingsMapper`.
   - Whenever a user selects a paired host in `HostsScreen`, update `lastSelectedHostUuid`.
2. **Add a Host Switcher Chip/Menu to `AppsTopBar`**:
   - In `AppsScreen.kt`, display the current host name with anonline status dot and a dropdown/chevron (`onSwitchHostClick`) so the user can switch to another paired host or tap **"Manage All Hosts"** to view the full `HostsScreen`.
3. **Auto-Route on Launch**:
   - In `SunbeamApp.kt`, when launching on the initial screen, if `lastSelectedHostUuid` matches a paired host (or if there is exactly 1 paired `ONLINE` host), automatically navigate to `SunbeamScreen.Apps(host.uuid)`.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew :core:preferences:testDebugUnitTest :features:apps:testDebugUnitTest :app:testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
```
