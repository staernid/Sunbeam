@file:Suppress("MagicNumber", "MaxLineLength")

package io.github.staernid.sunbeam.features.stream

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.PerformanceStats
import io.github.staernid.sunbeam.features.stream.model.StreamDialog
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.features.stream.model.StreamShortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StreamScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun liteHud_displaysPerformanceMetrics() {
        val stats = PerformanceStats(
            fps = 60f,
            networkLatencyMs = 15,
            decodeLatencyMs = 3.5f,
            packetLossPercentage = 0.0f,
        )

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(
                    perfStats = stats,
                    perfOverlayMode = PerfOverlayMode.LITE,
                ),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = {},
                onZoomClick = {},
                onHudClick = {},
                onCloseExpandedHud = {},
                onDismissWarning = {},
                onDisconnect = {},
                onOpenQuitConfirm = {},
                onConfirmQuit = {},
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = {},
                onSendShortcut = {},
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_hud_lite").assertIsDisplayed()
        composeTestRule.onNodeWithText("60.0 FPS").assertIsDisplayed()
        composeTestRule.onNodeWithText("RTT: 15ms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dec: 3.5 ms").assertIsDisplayed()
        composeTestRule.onNodeWithText("Loss: 0.0%").assertIsDisplayed()
    }

    @Test
    fun clickingLiteHud_invokesHudClickCallback() {
        var hudClicked = false

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(perfOverlayMode = PerfOverlayMode.LITE),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = {},
                onZoomClick = {},
                onHudClick = { hudClicked = true },
                onCloseExpandedHud = {},
                onDismissWarning = {},
                onDisconnect = {},
                onOpenQuitConfirm = {},
                onConfirmQuit = {},
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = {},
                onSendShortcut = {},
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_hud_lite").performClick()
        assertTrue(hudClicked)
    }

    @Test
    fun expandedHud_displaysDiagnosticsAndCloseButton() {
        val stats = PerformanceStats(
            fps = 120f,
            networkLatencyMs = 8,
            decodeLatencyMs = 2.1f,
            packetLossPercentage = 0.0f,
            bandwidthKbps = 80000,
            videoFormat = "AV1 Main",
            decoderName = "c2.qti.av1.decoder",
        )
        var closed = false

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(
                    perfStats = stats,
                    perfOverlayMode = PerfOverlayMode.EXPANDED,
                ),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = {},
                onZoomClick = {},
                onHudClick = {},
                onCloseExpandedHud = { closed = true },
                onDismissWarning = {},
                onDisconnect = {},
                onOpenQuitConfirm = {},
                onConfirmQuit = {},
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = {},
                onSendShortcut = {},
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_hud_expanded").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stream Performance").assertIsDisplayed()
        composeTestRule.onNodeWithText("120.0 FPS").assertIsDisplayed()
        composeTestRule.onNodeWithText("8 ms").assertIsDisplayed()
        composeTestRule.onNodeWithText("80.0 Mbps").assertIsDisplayed()
        composeTestRule.onNodeWithText("AV1 Main").assertIsDisplayed()
        composeTestRule.onNodeWithText("c2.qti.av1.decoder").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Collapse HUD").performClick()
        assertTrue(closed)
    }

    @Test
    fun floatingMenuButton_isDisplayedAndClickable() {
        var menuClicked = false

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(isFloatingMenuButtonVisible = true),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = { menuClicked = true },
                onZoomClick = {},
                onHudClick = {},
                onCloseExpandedHud = {},
                onDismissWarning = {},
                onDisconnect = {},
                onOpenQuitConfirm = {},
                onConfirmQuit = {},
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = {},
                onSendShortcut = {},
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_floating_menu_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stream_floating_menu_button").performClick()
        assertTrue(menuClicked)
    }

    @Test
    fun streamMenuDialog_displaysOptionsAndHandlesClicks() {
        var disconnectClicked = false
        var quitConfirmOpened = false

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(activeDialog = StreamDialog.MAIN_MENU),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = {},
                onZoomClick = {},
                onHudClick = {},
                onCloseExpandedHud = {},
                onDismissWarning = {},
                onDisconnect = { disconnectClicked = true },
                onOpenQuitConfirm = { quitConfirmOpened = true },
                onConfirmQuit = {},
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = {},
                onSendShortcut = {},
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_menu_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stream Controls").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disconnect").assertIsDisplayed().performClick()
        assertTrue(disconnectClicked)

        composeTestRule.onNodeWithText("Quit Session").assertIsDisplayed().performClick()
        assertTrue(quitConfirmOpened)

        composeTestRule.onNodeWithText("On-Screen Keyboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mouse Mode").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Send Special Keys").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun quitConfirmDialog_displaysConfirmationAndInvokesQuit() {
        var quitConfirmed = false

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(activeDialog = StreamDialog.CONFIRM_QUIT),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = {},
                onZoomClick = {},
                onHudClick = {},
                onCloseExpandedHud = {},
                onDismissWarning = {},
                onDisconnect = {},
                onOpenQuitConfirm = {},
                onConfirmQuit = { quitConfirmed = true },
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = {},
                onSendShortcut = {},
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_quit_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quit Stream Session?").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_quit_button").performClick()
        assertTrue(quitConfirmed)
    }

    @Test
    fun mouseModeDialog_displaysOptionsAndHandlesSelection() {
        var selectedMode: StreamMouseMode? = null

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(activeDialog = StreamDialog.MOUSE_MODE_PICKER),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = {},
                onZoomClick = {},
                onHudClick = {},
                onCloseExpandedHud = {},
                onDismissWarning = {},
                onDisconnect = {},
                onOpenQuitConfirm = {},
                onConfirmQuit = {},
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = { selectedMode = it },
                onSendShortcut = {},
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_mouse_mode_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select Mouse Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trackpad (Gaming)").assertIsDisplayed()

        composeTestRule.onNodeWithText("Trackpad (Gaming)").performClick()
        assertEquals(StreamMouseMode.TRACKPAD_GAMING, selectedMode)
    }

    @Test
    fun specialKeysDialog_displaysShortcutsAndHandlesTap() {
        var sentShortcut: StreamShortcut? = null

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(activeDialog = StreamDialog.SPECIAL_KEYS_PICKER),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = {},
                onZoomClick = {},
                onHudClick = {},
                onCloseExpandedHud = {},
                onDismissWarning = {},
                onDisconnect = {},
                onOpenQuitConfirm = {},
                onConfirmQuit = {},
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = {},
                onSendShortcut = { sentShortcut = it },
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_special_keys_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send Keyboard Shortcut").assertIsDisplayed()
        composeTestRule.onNodeWithText("Escape").assertIsDisplayed()

        composeTestRule.onNodeWithText("Escape").performClick()
        assertEquals(StreamShortcut.defaultShortcuts.first { it.id == "esc" }, sentShortcut)
    }

    @Test
    fun connectionBanner_displaysWarningAndDismisses() {
        var dismissed = false

        composeTestRule.setContent {
            StreamContent(
                uiState = StreamUiState(connectionWarning = "Slow connection: reduce bitrate"),
                onSurfaceCreated = {},
                onSurfaceChanged = { _, _, _, _ -> },
                onSurfaceDestroyed = {},
                onMenuClick = {},
                onZoomClick = {},
                onHudClick = {},
                onCloseExpandedHud = {},
                onDismissWarning = { dismissed = true },
                onDisconnect = {},
                onOpenQuitConfirm = {},
                onConfirmQuit = {},
                onToggleKeyboard = {},
                onToggleZoomMode = {},
                onOpenMouseModePicker = {},
                onOpenSpecialKeysPicker = {},
                onTogglePerfHud = {},
                onToggleVirtualController = {},
                onSelectMouseMode = {},
                onSendShortcut = {},
                onDismissDialog = {},
            )
        }

        composeTestRule.onNodeWithTag("stream_connection_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Slow connection: reduce bitrate").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dismiss warning").performClick()
        assertTrue(dismissed)
    }
}
