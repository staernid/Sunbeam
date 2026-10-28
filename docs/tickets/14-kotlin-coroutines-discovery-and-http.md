# Ticket 14: Replace Legacy Java `ComputerManagerService` with Coroutines `HostDiscoveryManager`

## Context

Currently, host discovery and polling goes through a convoluted legacy chain:
`MainActivity` -> `ComputerManagerBridge.kt` -> binds to `ComputerManagerService.java` -> binds to `DiscoveryService.java` -> spawns raw Java threads (`ThreadPoolExecutor`, `synchronized` locks) -> polls `NvHTTP` -> writes to `ComputerDatabaseManager.java` -> converts `ComputerDetails` back into `core.data.model.Host` and saves to `SqliteHostRepository`.

Replacing `ComputerManagerService.java`, `DiscoveryService.java`, and `ComputerDatabaseManager.java` with a coroutine-based `HostDiscoveryManager` directly updating `HostRepository` removes ~1,500 lines of legacy Java service/thread/database code.

## Files to Delete / Modify

- **Create / Modify**:
  - `app/src/main/kotlin/io/github/staernid/sunbeam/discovery/HostDiscoveryManager.kt` (or evolve `ComputerManagerBridge.kt`)
  - `app/src/main/AndroidManifest.xml`
- **Delete once replaced**:
  - `app/src/main/java/io/github/staernid/sunbeam/computers/ComputerManagerService.java`
  - `app/src/main/java/io/github/staernid/sunbeam/computers/ComputerManagerListener.java`
  - `app/src/main/java/io/github/staernid/sunbeam/computers/ComputerDatabaseManager.java`
  - `app/src/main/java/io/github/staernid/sunbeam/computers/LegacyDatabaseReader.java`
  - `app/src/main/java/io/github/staernid/sunbeam/computers/LegacyDatabaseReader2.java`
  - `app/src/main/java/io/github/staernid/sunbeam/computers/LegacyDatabaseReader3.java`
  - `app/src/main/java/io/github/staernid/sunbeam/discovery/DiscoveryService.java`

## Steps

1. Implement coroutine-based mDNS discovery (wrapping `MdnsDiscoveryAgent` / `NsdManager`) and periodic `NvHTTP` serverinfo polling inside `ComputerManagerBridge.kt` (or `HostDiscoveryManager.kt`) using structured concurrency (`CoroutineScope` + `Dispatchers.IO`).
2. Read and write hosts exclusively through `HostRepository` (`SqliteHostRepository`), eliminating the duplicate `ComputerDatabaseManager` instance.
3. Implement `HostOperationsHandler` (`addHostManually`, `pairHost`, `deleteHost`, `wakeHost`) directly via coroutines and `NvHTTP` / `PairingManager` without binding to `ComputerManagerService`.
4. Remove `ComputerManagerService` and `DiscoveryService` from `AndroidManifest.xml` and delete the obsolete Java classes listed above.
5. Add unit tests for the coroutine discovery and polling loop.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
./gradlew :app:assembleDebug
```
