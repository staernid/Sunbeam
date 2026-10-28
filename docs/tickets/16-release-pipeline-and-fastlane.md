# Ticket 16: GitHub Actions Release Workflow, Fastlane Metadata & Accessibility Pass

## Context

Carry-over items from the former Roadmap Phase 6:
1. There is no GitHub Actions workflow to build signed release APKs and attach them to GitHub Releases for Obtainium / F-Droid users.
2. `fastlane/metadata/android/` still contains legacy Artemis/Moonlight descriptions or outdated screenshots/changelogs.
3. All interactive Compose icon buttons and custom cards need a final accessibility (`contentDescription`, minimum 48dp touch targets) and Android TV D-pad focus traversal check.

## Files to Create / Modify

- `.github/workflows/release.yml` (new)
- `fastlane/metadata/android/en-US/title.txt`
- `fastlane/metadata/android/en-US/short_description.txt`
- `fastlane/metadata/android/en-US/full_description.txt`
- `core/designsystem` and `features/*` (accessibility & D-pad focus audit)

## Steps

1. **Create `.github/workflows/release.yml`**:
   - Trigger on tag push `v*`.
   - Build release APKs (`./gradlew :app:assembleRelease`).
   - Sign APKs if `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` secrets are present (or produce unsigned release artifacts with SHA-256 checksums if not configured).
   - Attach the universal and ABI-split APKs and `SHA256SUMS.txt` to the GitHub Release.
2. **Update `fastlane/metadata/android/en-US/`**:
   - Update `title.txt` (`Sunbeam`), `short_description.txt`, and `full_description.txt` to accurately describe Sunbeam (modern Kotlin + Compose Android client for official Sunshine servers).
3. **Accessibility & TV Focus Pass**:
   - Audit all `IconButton`, `SunbeamCard`, `HostCard`, `AppCard`, and `SettingItem` composables to ensure `sunbeamFocusable()` is applied to every interactive element for Android TV D-pad navigation and semantic labels are present.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew ktlintCheck detekt lintDebug
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
```
