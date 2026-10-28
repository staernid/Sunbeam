# Ticket 04: Delete Superseded Legacy Java Activities & View Dependencies

## Context

`MainActivity` (`SunbeamApp`) has replaced the legacy Java screens with `:features:hosts`, `:features:apps`, `:features:settings`, and `:features:profiles`. However, `AndroidManifest.xml` and `app/src/main/java/` still carry 8 obsolete Activities, their RecyclerView/GridView adapters, XML layouts, and legacy View libraries (`androidx.preference`, `SearchPreference`, `MPAndroidChart`, `cardview`, `recyclerview`).

## Files to Delete / Modify

- **Delete legacy UI classes**:
  - `app/src/main/java/io/github/staernid/sunbeam/PcView.java`
  - `app/src/main/java/io/github/staernid/sunbeam/AppView.java`
  - `app/src/main/java/io/github/staernid/sunbeam/ProfilesActivity.java`
  - `app/src/main/java/io/github/staernid/sunbeam/EditProfileActivity.java`
  - `app/src/main/java/io/github/staernid/sunbeam/HelpActivity.java`
  - `app/src/main/java/io/github/staernid/sunbeam/DebugInfoActivity.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/StreamSettings.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/AddComputerManually.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/SeekBarPreference.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/SmallIconCheckboxPreference.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/WebLauncherPreference.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/LanguagePreference.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/ConfirmDeleteOscPreference.java`
  - `app/src/main/java/io/github/staernid/sunbeam/preferences/ConfirmDeleteKeyboardPreference.java`
  - `app/src/main/java/io/github/staernid/sunbeam/grid/PcGridAdapter.java`
  - `app/src/main/java/io/github/staernid/sunbeam/grid/AppGridAdapter.java`
  - `app/src/main/java/io/github/staernid/sunbeam/grid/GenericGridAdapter.java`
  - `app/src/main/java/io/github/staernid/sunbeam/profiles/ProfilesAdapter.java`
  - `app/src/main/java/io/github/staernid/sunbeam/ui/AdapterFragment.java`
  - `app/src/main/java/io/github/staernid/sunbeam/ui/AdapterFragmentCallbacks.java`
- **Delete obsolete legacy tests** that only tested deleted Java activities (`LayoutInflationTest.java`, `SimpleStartupTest.java`, `StartupCrashTest.java`, `StartupTest.java`, `ProfilesActivityUiTest.java`, `ProfilesNavigationTest.java`, `ProfilesOverlayTest.java`, `OverlayPreferencesTest.java`).
- **Modify**:
  - `app/src/main/java/io/github/staernid/sunbeam/ShortcutTrampoline.java` (route fallback to `MainActivity` instead of `PcView`/`AppView`)
  - `app/src/main/java/io/github/staernid/sunbeam/utils/ShortcutHelper.java` (update any `PcView`/`AppView` references to `MainActivity`)
  - `app/src/main/AndroidManifest.xml` (remove the deleted `<activity>` tags)
  - `app/build.gradle.kts` & `gradle/libs.versions.toml` (remove `androidx-preference`, `androidx-cardview`, `androidx-recyclerview`, `search-preference`, `mp-android-chart` if no longer referenced)

## Steps

1. Update `ShortcutTrampoline.java` and `ShortcutHelper.java` so launcher shortcuts launch `Game` or `MainActivity` without referencing `PcView` or `AppView`.
2. Delete the legacy Java Activity and Adapter files listed above and their unused XML layouts/menus in `app/src/main/res/`.
3. Remove the corresponding `<activity>` entries from `app/src/main/AndroidManifest.xml`.
4. Remove unused dependencies (`search-preference`, `mp-android-chart`, `androidx-preference`, `androidx-cardview`, `androidx-recyclerview`) from `app/build.gradle.kts` and `gradle/libs.versions.toml`.
5. Delete the obsolete legacy Activity tests and verify all remaining tests compile and pass.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
./gradlew :app:assembleDebug
```
