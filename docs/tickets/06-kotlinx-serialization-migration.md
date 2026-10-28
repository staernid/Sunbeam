# Ticket 06: Replace Gson with `kotlinx-serialization-json`

## Context

The project still depends on `com.google.code.gson:gson` in `:core:data` (`JsonProfileRepository.kt`) and `:app` (`ProfilesManager.java`, `SettingsProfile.java`, `KeyConfigHelper.java`, `PerformanceDataTracker.java`). Migrating to `kotlinx.serialization` eliminates reflection, ensures Kotlin null-safety and default value handling, and aligns with modern Kotlin 2.x conventions.

## Files to Modify

- `gradle/libs.versions.toml`
- `plugins/build.gradle.kts` (if adding convention support) or module `build.gradle.kts` files
- `core/data/build.gradle.kts`
- `core/data/src/main/kotlin/io/github/staernid/sunbeam/core/data/repository/JsonProfileRepository.kt`
- `core/data/src/test/kotlin/io/github/staernid/sunbeam/core/data/JsonProfileRepositoryTest.kt`
- `app/build.gradle.kts` and any remaining Gson usages in `:app`

## Steps

1. Add the `kotlin-serialization` plugin (`org.jetbrains.kotlin.plugin.serialization`) and `org.jetbrains.kotlinx:kotlinx-serialization-json` to `gradle/libs.versions.toml`.
2. In `:core:data`, apply the serialization plugin and replace `Gson` and `TypeToken` in `JsonProfileRepository.kt` with `@Serializable` DTOs and ` kotlinx.serialization.json.Json { ignoreUnknownKeys = true; prettyPrint = true }`.
3. Ensure backward-compatible JSON field names (`uuid`, `name`, `modifiedUtc`, `options`) so existing `profiles.json` files deserialize identically.
4. Replace or remove any remaining `com.google.gson` usages in `:app`, then remove `gson` from `gradle/libs.versions.toml`, `core/data/build.gradle.kts`, and `app/build.gradle.kts`.

## Verification & Completion Criteria

1. Confirm `grep -rn "com.google.gson" app/ core/ features/` returns no matches.
2. Run:

```bash
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
```
