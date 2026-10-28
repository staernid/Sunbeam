# Ticket 08: Compile-Time Dependency Injection (Metro DI)

## Context

`MainActivity.kt` currently constructs all repositories and bridges manually using `by lazy { ... }` and passes 8 parameters down through `SunbeamApp(...)` and `ScreenContent(...)`. Adopting compile-time DI (Metro `dev.zacsweers.metro`, matching the Voice reference architecture) eliminates parameter drilling and scopes singletons cleanly to `SunbeamApplication`.

## Files to Modify

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/main/java/io/github/staernid/sunbeam/SunbeamApplication.java` (convert to `SunbeamApplication.kt` or attach graph holder)
- `app/src/main/kotlin/io/github/staernid/sunbeam/di/AppGraph.kt` (new)
- `app/src/main/kotlin/io/github/staernid/sunbeam/MainActivity.kt`
- `app/src/main/kotlin/io/github/staernid/sunbeam/ui/SunbeamApp.kt`

## Steps

1. Add Metro (`dev.zacsweers.metro`) plugin to `gradle/libs.versions.toml` and `:app` (aligned with the project's Kotlin version in `libs.versions.toml`).
2. Define `@DependencyGraph(AppScope::class) interface AppGraph` in `app/src/main/kotlin/io/github/staernid/sunbeam/di/AppGraph.kt` providing:
   - `HostRepository` (`SqliteHostRepository`)
   - `ProfileRepository` (`JsonProfileRepository`)
   - `PreferencesRepository` (`DataStorePreferencesRepository`)
   - `AppRepository` (`CachedAppRepository`)
   - `StreamSessionLauncher`
   - `HostOperationsHandler` (`ComputerManagerBridge`)
3. Initialize `AppGraph` in `SunbeamApplication` and inject/provide dependencies into `MainActivity` and `SunbeamApp` while keeping default/overridable parameters on `SunbeamApp` so `SunbeamAppTest` can still pass fake repositories in unit tests.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
./gradlew :app:assembleDebug
```
