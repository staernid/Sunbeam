# Sunbeam Implementation Tickets

Self-contained, incremental tickets ordered so a coding agent can execute one ticket per session and leave the build and tests green after each step.

## Execution Rules for Agents

1. Read the assigned ticket file completely before editing any code.
2. Stay strictly within the scope and files listed in the ticket.
3. Follow all rules in [AGENTS.md](../../AGENTS.md).
4. A ticket is only complete when every check in its **Verification & Completion Criteria** section passes with exit code `0`.
5. When finished, mark the ticket `[x]` in the table below.

## Ticket Queue

| Status | Ticket | Summary |
| :---: | :--- | :--- |
| [x] | [01-gradle-and-ci-all-modules.md](01-gradle-and-ci-all-modules.md) | Enable Gradle caching/parallelism, fix Obtainium regex, and run lint/detekt/ktlint/tests across all modules in CI |
| [x] | [02-datastore-legacy-preference-bridge.md](02-datastore-legacy-preference-bridge.md) | Bridge `DataStorePreferencesRepository` settings into `Game.java` so Compose Settings actually apply during streams |
| [x] | [03-wire-compose-stream-overlay-and-drawer.md](03-wire-compose-stream-overlay-and-drawer.md) | Replace `AlertDialog` in-stream menu with a translucent slide-in overlay drawer and wire `:features:stream` HUD into `Game.java` |
| [x] | [04-remove-dead-legacy-activities.md](04-remove-dead-legacy-activities.md) | Delete superseded Java UI activities (`PcView`, `AppView`, `StreamSettings`, `ProfilesActivity`, etc.) and unused View dependencies |
| [x] | [05-remove-midas-sbs3d-bloat.md](05-remove-midas-sbs3d-bloat.md) | Remove legacy MiDaS SBS 3D renderer and ~50MB of `litert` + `opencv` dependencies |
| [x] | [06-kotlinx-serialization-migration.md](06-kotlinx-serialization-migration.md) | Replace `Gson` with `kotlinx-serialization-json` across `:core:data` and `:app` |
| [x] | [07-navigation3-and-viewmodel-scoping.md](07-navigation3-and-viewmodel-scoping.md) | Fix ViewModel lifecycle scoping across navigation backstack entries and add screen transitions + predictive back |
| [x] | [08-metro-di-setup.md](08-metro-di-setup.md) | Replace manual `by lazy` wiring in `MainActivity` with compile-time dependency injection |
| [x] | [09-coil3-box-art-and-host-card-actions.md](09-coil3-box-art-and-host-card-actions.md) | Adopt Coil 3 for box art loading, show active game title + 1-tap Resume and Wake-on-LAN on `HostCard` |
| [x] | [10-game-first-home-experience.md](10-game-first-home-experience.md) | Auto-open the last-used/single paired online host into the Apps library with a top-bar host switcher pill |
| [x] | [11-settings-screen-redesign.md](11-settings-screen-redesign.md) | Redesign Settings with a Quick Preset bar, grouped M3 cards, human-readable enum labels, and working Theme/OLED toggles |
| [x] | [12-settings-advanced-and-input-completeness.md](12-settings-advanced-and-input-completeness.md) | Expose remaining `StreamSettings` fields (touchpad sensitivity, mouse modes, keyboard options, display mode) in Settings sub-sheets |
| [x] | [13-real-gamepad-tester-and-osc-editor.md](13-real-gamepad-tester-and-osc-editor.md) | Wire real `InputDevice` key/motion/rumble events into `GamepadTesterView` and real layout editing into `OscPreviewView` |
| [x] | [14-kotlin-coroutines-discovery-and-http.md](14-kotlin-coroutines-discovery-and-http.md) | Replace legacy `ComputerManagerService` and `DiscoveryService` with a Kotlin Coroutines/Flow `HostDiscoveryManager` |
| [x] | [15-roborazzi-screenshot-tests.md](15-roborazzi-screenshot-tests.md) | Add Roborazzi JVM screenshot tests for all `:features:*` screens |
| [x] | [16-release-pipeline-and-fastlane.md](16-release-pipeline-and-fastlane.md) | Add GitHub Actions release APK workflow, verify Fastlane/Obtainium metadata, and Fork Detachment checklist |
