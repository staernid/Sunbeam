@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.features.stream

import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayPosition
import io.github.staernid.sunbeam.features.stream.model.PerformanceStats
import io.github.staernid.sunbeam.features.stream.model.StreamDialog
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.features.stream.model.StreamShortcut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StreamViewModelTest {
    private lateinit var viewModel: StreamViewModel

    @Before
    fun setUp() {
        viewModel = StreamViewModel()
    }

    @Test
    fun initialState_hasDefaultValues() {
        val state = viewModel.uiState.value
        assertEquals(PerfOverlayMode.LITE, state.perfOverlayMode)
        assertEquals(PerfOverlayPosition.TOP_CENTER, state.perfOverlayPosition)
        assertTrue(state.isPerfOverlayEnabled)
        assertTrue(state.isFloatingMenuButtonVisible)
        assertFalse(state.isFloatingZoomButtonVisible)
        assertFalse(state.isPanZoomMode)
        assertFalse(state.isVirtualControllerEnabled)
        assertEquals(StreamDialog.NONE, state.activeDialog)
        assertNull(state.connectionWarning)
        assertEquals(StreamMouseMode.MULTI_TOUCH, state.selectedMouseMode)
        assertFalse(state.isLocalCursorEnabled)
    }

    @Test
    fun toggleMenu_cyclesBetweenMainMenuAndNone() {
        viewModel.toggleMenu()
        assertEquals(StreamDialog.MAIN_MENU, viewModel.uiState.value.activeDialog)

        viewModel.toggleMenu()
        assertEquals(StreamDialog.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun dismissDialog_resetsActiveDialogToNone() {
        viewModel.openQuitConfirm()
        assertEquals(StreamDialog.CONFIRM_QUIT, viewModel.uiState.value.activeDialog)

        viewModel.dismissDialog()
        assertEquals(StreamDialog.NONE, viewModel.uiState.value.activeDialog)
    }

    @Test
    fun requestDisconnect_dismissesDialogAndInvokesAction() {
        var disconnected = false
        viewModel.onDisconnectAction = { disconnected = true }
        viewModel.toggleMenu()

        viewModel.requestDisconnect()
        assertEquals(StreamDialog.NONE, viewModel.uiState.value.activeDialog)
        assertTrue(disconnected)
    }

    @Test
    fun confirmQuit_dismissesDialogAndInvokesAction() {
        var quit = false
        viewModel.onQuitAction = { quit = true }
        viewModel.openQuitConfirm()

        viewModel.confirmQuit()
        assertEquals(StreamDialog.NONE, viewModel.uiState.value.activeDialog)
        assertTrue(quit)
    }

    @Test
    fun togglePerfHud_cyclesLiteExpandedNone() {
        var notifiedMode: PerfOverlayMode? = null
        viewModel.onPerfOverlayModeChangedAction = { notifiedMode = it }

        // Initial state is LITE
        assertEquals(PerfOverlayMode.LITE, viewModel.uiState.value.perfOverlayMode)

        viewModel.togglePerfHud()
        assertEquals(PerfOverlayMode.EXPANDED, viewModel.uiState.value.perfOverlayMode)
        assertEquals(PerfOverlayMode.EXPANDED, notifiedMode)
        assertTrue(viewModel.uiState.value.isPerfOverlayEnabled)

        viewModel.togglePerfHud()
        assertEquals(PerfOverlayMode.NONE, viewModel.uiState.value.perfOverlayMode)
        assertEquals(PerfOverlayMode.NONE, notifiedMode)
        assertFalse(viewModel.uiState.value.isPerfOverlayEnabled)

        viewModel.togglePerfHud()
        assertEquals(PerfOverlayMode.LITE, viewModel.uiState.value.perfOverlayMode)
        assertEquals(PerfOverlayMode.LITE, notifiedMode)
        assertTrue(viewModel.uiState.value.isPerfOverlayEnabled)
    }

    @Test
    fun setPerfOverlayPosition_updatesPosition() {
        viewModel.setPerfOverlayPosition(PerfOverlayPosition.BOTTOM_CENTER)
        assertEquals(PerfOverlayPosition.BOTTOM_CENTER, viewModel.uiState.value.perfOverlayPosition)
    }

    @Test
    fun updatePerformanceStats_updatesFormattedMetrics() {
        val stats = PerformanceStats(
            fps = 60.0f,
            networkLatencyMs = 12,
            decodeLatencyMs = 4.32f,
            packetLossPercentage = 0.5f,
            bandwidthKbps = 50000,
            videoFormat = "HEVC Main 10",
            decoderName = "c2.qti.hevc.decoder",
        )
        viewModel.updatePerformanceStats(stats)

        val updated = viewModel.uiState.value.perfStats
        assertEquals(60.0f, updated.fps)
        assertEquals("60.0", updated.formattedFps)
        assertEquals(12, updated.networkLatencyMs)
        assertEquals("4.3 ms", updated.formattedDecodeLatency)
        assertEquals("0.5%", updated.formattedPacketLoss)
        assertEquals("50.0 Mbps", updated.formattedBandwidth)
        assertEquals("HEVC Main 10", updated.videoFormat)
        assertEquals("c2.qti.hevc.decoder", updated.decoderName)
    }

    @Test
    fun connectionWarning_setsAndDismissesProperly() {
        viewModel.setConnectionWarning("Poor connection: high packet loss detected")
        assertEquals("Poor connection: high packet loss detected", viewModel.uiState.value.connectionWarning)

        viewModel.dismissConnectionWarning()
        assertNull(viewModel.uiState.value.connectionWarning)
    }

    @Test
    fun toggleZoomMode_invokesActionAndUpdatesState() {
        var zoomState = false
        viewModel.onZoomModeToggledAction = { zoomState = it }

        viewModel.toggleZoomMode()
        assertTrue(viewModel.uiState.value.isPanZoomMode)
        assertTrue(zoomState)

        viewModel.toggleZoomMode()
        assertFalse(viewModel.uiState.value.isPanZoomMode)
        assertFalse(zoomState)
    }

    @Test
    fun toggleVirtualController_invokesActionAndUpdatesState() {
        var controllerState = false
        viewModel.onVirtualControllerToggledAction = { controllerState = it }

        viewModel.toggleVirtualController()
        assertTrue(viewModel.uiState.value.isVirtualControllerEnabled)
        assertTrue(controllerState)

        viewModel.toggleVirtualController()
        assertFalse(viewModel.uiState.value.isVirtualControllerEnabled)
        assertFalse(controllerState)
    }

    @Test
    fun selectMouseMode_updatesStateAndInvokesCallback() {
        var chosenMode: StreamMouseMode? = null
        viewModel.onMouseModeChangedAction = { chosenMode = it }
        viewModel.openMouseModePicker()

        viewModel.selectMouseMode(StreamMouseMode.TRACKPAD_GAMING)
        assertEquals(StreamMouseMode.TRACKPAD_GAMING, viewModel.uiState.value.selectedMouseMode)
        assertEquals(StreamDialog.NONE, viewModel.uiState.value.activeDialog)
        assertEquals(StreamMouseMode.TRACKPAD_GAMING, chosenMode)
    }

    @Test
    fun toggleLocalCursor_updatesStateAndInvokesCallback() {
        var localCursorState = false
        viewModel.onLocalCursorToggledAction = { localCursorState = it }

        viewModel.toggleLocalCursor()
        assertTrue(viewModel.uiState.value.isLocalCursorEnabled)
        assertTrue(localCursorState)

        viewModel.toggleLocalCursor()
        assertFalse(viewModel.uiState.value.isLocalCursorEnabled)
        assertFalse(localCursorState)
    }

    @Test
    fun sendShortcut_dismissesDialogAndInvokesCallback() {
        var sentShortcut: StreamShortcut? = null
        viewModel.onSendShortcutAction = { sentShortcut = it }
        viewModel.openSpecialKeysPicker()

        val shortcut = StreamShortcut.defaultShortcuts.first()
        viewModel.sendShortcut(shortcut)
        assertEquals(StreamDialog.NONE, viewModel.uiState.value.activeDialog)
        assertEquals(shortcut, sentShortcut)
    }

    @Test
    fun pipTransitions_hidesOverlaysAndRestoresOnExit() {
        viewModel.setFloatingButtonsVisibility(menuVisible = true, zoomVisible = true)
        viewModel.setPerfOverlayMode(PerfOverlayMode.LITE)

        viewModel.onEnterPiP()
        val pipState = viewModel.uiState.value
        assertTrue(pipState.isInPiP)
        assertFalse(pipState.isFloatingMenuButtonVisible)
        assertFalse(pipState.isFloatingZoomButtonVisible)
        assertEquals(PerfOverlayMode.NONE, pipState.perfOverlayMode)

        viewModel.onExitPiP()
        val restoredState = viewModel.uiState.value
        assertFalse(restoredState.isInPiP)
        assertTrue(restoredState.isFloatingMenuButtonVisible)
        assertTrue(restoredState.isFloatingZoomButtonVisible)
        assertEquals(PerfOverlayMode.LITE, restoredState.perfOverlayMode)
    }
}
