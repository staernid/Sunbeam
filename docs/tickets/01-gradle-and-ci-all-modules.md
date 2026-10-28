# Ticket 01: Gradle Build Performance & All-Module CI Checks

## Context

Currently:
1. `.github/workflows/ci.yml` only runs `:app:ktlintCheck`, `:app:detekt`, `:app:lintDebug`, and `:app:testDebugUnitTest`. All unit tests and static analysis in `:core:*`, `:features:*`, and `:tools:mock-sunshine` are skipped in CI.
2. `gradle.properties` leaves `org.gradle.parallel` disabled, has no `org.gradle.caching=true`, and sets a small 3GB heap.
3. `app/build.gradle.kts` contains a legacy `apkFilterRegEx` for `nonRoot` in `obtainiumUrl`, even though the `root`/`nonRoot` flavors were removed.

## Files to Modify

- `gradle.properties`
- `app/build.gradle.kts`
- `.github/workflows/ci.yml`
- `AGENTS.md`
- `README.md`

## Steps

1. **Update `gradle.properties`**:
   - Set `org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8`
   - Enable `org.gradle.parallel=true`
   - Enable `org.gradle.caching=true`
2. **Fix `obtainiumUrl` in `app/build.gradle.kts`**:
   - Update `apkFilterRegEx` inside `obtainiumUrl` from `nonRoot` to `release` (or remove the stale `nonRoot` filter so Obtainium matches standard release APKs).
3. **Update `README.md`**:
   - Replace `./gradlew :app:assembleNonRoot_gameDebug` with `./gradlew :app:assembleDebug`.
   - Update the pointer from `docs/ROADMAP.md` to `docs/tickets/README.md`.
4. **Update `.github/workflows/ci.yml` and `AGENTS.md`**:
   - Run `ktlintCheck`, `detekt`, `lintDebug`, and `testDebugUnitTest` across **all** subprojects (`./gradlew ktlintCheck detekt`, `./gradlew lintDebug`, `./gradlew testDebugUnitTest`), not only `:app`.
   - Fix any existing `ktlint` or `detekt` violations in `:core:*` and `:features:*` surfaced by running across all modules.
   - In `.github/workflows/ci.yml`, upload test and lint reports from `**/build/reports/`.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew ktlintCheck detekt
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
```
