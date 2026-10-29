package io.github.staernid.sunbeam.features.stream

import io.github.staernid.sunbeam.core.stream.model.StreamState
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayPosition
import io.github.staernid.sunbeam.features.stream.model.PerformanceStats
import io.github.staernid.sunbeam.features.stream.model.StreamDialog
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.features.stream.model.StreamShortcut

/**
 * UI state for the active stream session screen.
 */
data class StreamUiState(
    val streamState: StreamState = StreamState.Idle,
    val perfStats: PerformanceStats = PerformanceStats(),
    val perfOverlayMode: PerfOverlayMode = PerfOverlayMode.LITE,
    val perfOverlayPosition: PerfOverlayPosition = PerfOverlayPosition.TOP_CENTER,
    val isPerfOverlayEnabled: Boolean = true,
    val isFloatingMenuButtonVisible: Boolean = true,
    val isFloatingZoomButtonVisible: Boolean = false,
    val isPanZoomMode: Boolean = false,
    val isVirtualControllerEnabled: Boolean = false,
    val isLocalCursorEnabled: Boolean = false,
    val isKeyboardRequested: Boolean = false,
    val connectionWarning: String? = null,
    val selectedMouseMode: StreamMouseMode = StreamMouseMode.MULTI_TOUCH,
    val activeDialog: StreamDialog = StreamDialog.NONE,
    val shortcuts: List<StreamShortcut> = StreamShortcut.defaultShortcuts,
    val isInPiP: Boolean = false,
)
