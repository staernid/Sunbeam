# Ticket 03: In-Stream Slide-In Overlay Drawer & Compose HUD Wiring

## Context

Currently:
1. `:features:stream` (`StreamScreen.kt`, `StreamDialogs.kt`) uses basic modal `AlertDialog` popups (`StreamMenuDialog`, `StreamMouseModeDialog`, `StreamSpecialKeysDialog`) that block the middle of the video stream.
2. `SunbeamApp.kt` has a broken `SunbeamScreen.Stream` branch that starts a session and immediately launches `Game.java` and pops the backstack, while `Game.java` still uses legacy XML TextViews (`hud_overlay`, `notification_overlay`) and legacy `GameMenu.java` (`AlertDialog`).

## Files to Modify

- `features/stream/src/main/kotlin/io/github/staernid/sunbeam/features/stream/component/StreamDialogs.kt`
- `features/stream/src/main/kotlin/io/github/staernid/sunbeam/features/stream/StreamScreen.kt`
- `features/stream/src/test/kotlin/io/github/staernid/sunbeam/features/stream/StreamScreenTest.kt`
- `app/src/main/java/io/github/staernid/sunbeam/stream/overlay/StreamOverlayManager.kt`
- `app/src/main/java/io/github/staernid/sunbeam/stream/overlay/StreamMenuCoordinator.kt`
- `app/src/main/java/io/github/staernid/sunbeam/Game.java`

## Steps

1. **Redesign `StreamMenuDialog` into a Slide-In Edge Drawer (`StreamControlDrawer`)**:
   - In `:features:stream`, replace the center `AlertDialog` for `StreamDialog.MAIN_MENU` with an animated slide-in side sheet aligned to `Alignment.CenterEnd` (`widthIn(max = 360.dp)`, `fillMaxHeight()`, translucent `MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)` with `RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)` and scrim).
   - Inside the drawer, display:
     - Header with stream status and quick **Disconnect** / **Quit Game** actions.
     - Inline expandable or segmented controls for **Mouse Mode** and **Special Keys / Macros** (so users don't have to open nested modal dialogs just to send `Alt+Tab`, `Win+G`, `Ctrl+Shift+Esc`, or switch touch/trackpad mode).
     - Quick toggles for **Virtual Controller**, **Pan/Zoom Mode**, **Soft Keyboard**, and **Performance HUD**.
2. **Host Compose Overlay inside `Game` Activity**:
   - Add a `ComposeView` overlay layer in `Game` (managed via a Kotlin helper in `app/src/main/java/io/github/staernid/sunbeam/stream/overlay/`) that renders `StreamOverlayHud`, `StreamConnectionBanner`, `StreamFloatingControls`, and the new `StreamControlDrawer` backed by `StreamViewModel`.
   - Feed performance stats (`VideoStats` / decoder FPS / RTT / frame drop metrics) and menu triggers (`GameMenu` invocation) into `StreamViewModel` so the Compose HUD and slide-in drawer are used during real streams.
3. **Update tests**:
   - Update `StreamScreenTest.kt` to test the new slide-in drawer interactions, inline mouse mode selection, and shortcut triggers.

## Verification & Completion Criteria

Run the following commands and ensure every single one exits with code `0`:

```bash
./gradlew :features:stream:testDebugUnitTest :app:testDebugUnitTest
./gradlew ktlintCheck detekt :app:lintDebug
```
