@file:Suppress(
    "TooManyFunctions",
    "MagicNumber",
)

package io.github.staernid.sunbeam.features.stream

import androidx.lifecycle.ViewModel
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayPosition
import io.github.staernid.sunbeam.features.stream.model.PerformanceStats
import io.github.staernid.sunbeam.features.stream.model.StreamDialog
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.features.stream.model.StreamShortcut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Presenter/ViewModel managing stream session overlay UI state, menus, HUD modes, and dialogs.
 */
class StreamViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StreamUiState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    private var previousFloatingMenuState = true
    private var previousFloatingZoomState = false
    private var previousPerfMode = PerfOverlayMode.LITE

    // External action hooks for host stream integration
    var onDisconnectAction: (() -> Unit)? = null
    var onQuitAction: (() -> Unit)? = null
    var onSendShortcutAction: ((StreamShortcut) -> Unit)? = null
    var onMouseModeChangedAction: ((StreamMouseMode) -> Unit)? = null
    var onZoomModeToggledAction: ((Boolean) -> Unit)? = null
    var onKeyboardToggledAction: (() -> Unit)? = null
    var onVirtualControllerToggledAction: ((Boolean) -> Unit)? = null

    fun toggleMenu() {
        _uiState.update { current ->
            if (current.activeDialog == StreamDialog.MAIN_MENU) {
                current.copy(activeDialog = StreamDialog.NONE)
            } else {
                current.copy(activeDialog = StreamDialog.MAIN_MENU)
            }
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(activeDialog = StreamDialog.NONE) }
    }

    fun openQuitConfirm() {
        _uiState.update { it.copy(activeDialog = StreamDialog.CONFIRM_QUIT) }
    }

    fun openMouseModePicker() {
        _uiState.update { it.copy(activeDialog = StreamDialog.MOUSE_MODE_PICKER) }
    }

    fun openSpecialKeysPicker() {
        _uiState.update { it.copy(activeDialog = StreamDialog.SPECIAL_KEYS_PICKER) }
    }

    fun requestDisconnect() {
        dismissDialog()
        onDisconnectAction?.invoke()
    }

    fun confirmQuit() {
        dismissDialog()
        onQuitAction?.invoke()
    }

    fun togglePerfHud() {
        _uiState.update { current ->
            val nextMode = when (current.perfOverlayMode) {
                PerfOverlayMode.NONE -> PerfOverlayMode.LITE
                PerfOverlayMode.LITE -> PerfOverlayMode.EXPANDED
                PerfOverlayMode.EXPANDED -> PerfOverlayMode.NONE
            }
            current.copy(
                perfOverlayMode = nextMode,
                isPerfOverlayEnabled = nextMode != PerfOverlayMode.NONE,
            )
        }
    }

    fun setPerfOverlayMode(mode: PerfOverlayMode) {
        _uiState.update {
            it.copy(
                perfOverlayMode = mode,
                isPerfOverlayEnabled = mode != PerfOverlayMode.NONE,
            )
        }
    }

    fun setPerfOverlayPosition(position: PerfOverlayPosition) {
        _uiState.update { it.copy(perfOverlayPosition = position) }
    }

    fun updatePerformanceStats(stats: PerformanceStats) {
        _uiState.update { it.copy(perfStats = stats) }
    }

    fun setConnectionWarning(warning: String?) {
        _uiState.update { it.copy(connectionWarning = warning) }
    }

    fun dismissConnectionWarning() {
        _uiState.update { it.copy(connectionWarning = null) }
    }

    fun toggleZoomMode() {
        _uiState.update { current ->
            val newZoom = !current.isPanZoomMode
            onZoomModeToggledAction?.invoke(newZoom)
            current.copy(isPanZoomMode = newZoom)
        }
    }

    fun toggleVirtualController() {
        _uiState.update { current ->
            val newController = !current.isVirtualControllerEnabled
            onVirtualControllerToggledAction?.invoke(newController)
            current.copy(isVirtualControllerEnabled = newController)
        }
    }

    fun toggleKeyboard() {
        dismissDialog()
        onKeyboardToggledAction?.invoke()
        _uiState.update { it.copy(isKeyboardRequested = !it.isKeyboardRequested) }
    }

    fun selectMouseMode(mode: StreamMouseMode) {
        _uiState.update { it.copy(selectedMouseMode = mode, activeDialog = StreamDialog.NONE) }
        onMouseModeChangedAction?.invoke(mode)
    }

    fun sendShortcut(shortcut: StreamShortcut) {
        dismissDialog()
        onSendShortcutAction?.invoke(shortcut)
    }

    fun setFloatingButtonsVisibility(
        menuVisible: Boolean,
        zoomVisible: Boolean,
    ) {
        _uiState.update {
            it.copy(
                isFloatingMenuButtonVisible = menuVisible,
                isFloatingZoomButtonVisible = zoomVisible,
            )
        }
    }

    fun setCustomShortcuts(shortcuts: List<StreamShortcut>) {
        _uiState.update { it.copy(shortcuts = shortcuts) }
    }

    fun onEnterPiP() {
        val current = _uiState.value
        previousFloatingMenuState = current.isFloatingMenuButtonVisible
        previousFloatingZoomState = current.isFloatingZoomButtonVisible
        previousPerfMode = current.perfOverlayMode

        _uiState.update {
            it.copy(
                isInPiP = true,
                isFloatingMenuButtonVisible = false,
                isFloatingZoomButtonVisible = false,
                perfOverlayMode = PerfOverlayMode.NONE,
                activeDialog = StreamDialog.NONE,
                connectionWarning = null,
            )
        }
    }

    fun onExitPiP() {
        _uiState.update {
            it.copy(
                isInPiP = false,
                isFloatingMenuButtonVisible = previousFloatingMenuState,
                isFloatingZoomButtonVisible = previousFloatingZoomState,
                perfOverlayMode = previousPerfMode,
            )
        }
    }
}
