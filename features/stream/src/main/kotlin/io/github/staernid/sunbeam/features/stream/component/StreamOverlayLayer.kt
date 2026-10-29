@file:Suppress(
    "LongParameterList",
    "LongMethod",
)

package io.github.staernid.sunbeam.features.stream.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.staernid.sunbeam.features.stream.StreamUiState
import io.github.staernid.sunbeam.features.stream.StreamViewModel
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.StreamDialog
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.features.stream.model.StreamShortcut

/**
 * Overlay layer containing HUD, alerts, floating buttons, and control drawer/dialogs.
 * Can be hosted in standalone Compose screens or overlaid over native SurfaceView activities.
 */
@Composable
fun StreamOverlayLayer(
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StreamOverlayLayer(
        uiState = uiState,
        onMenuClick = viewModel::toggleMenu,
        onZoomClick = viewModel::toggleZoomMode,
        onHudClick = viewModel::togglePerfHud,
        onCloseExpandedHud = { viewModel.setPerfOverlayMode(PerfOverlayMode.LITE) },
        onDismissWarning = viewModel::dismissConnectionWarning,
        onDisconnect = viewModel::requestDisconnect,
        onOpenQuitConfirm = viewModel::openQuitConfirm,
        onConfirmQuit = viewModel::confirmQuit,
        onToggleKeyboard = viewModel::toggleKeyboard,
        onToggleZoomMode = viewModel::toggleZoomMode,
        onTogglePerfHud = viewModel::togglePerfHud,
        onToggleVirtualController = viewModel::toggleVirtualController,
        onSelectMouseMode = viewModel::selectMouseMode,
        onSendShortcut = viewModel::sendShortcut,
        onDismissDialog = viewModel::dismissDialog,
        onToggleLocalCursor = viewModel::toggleLocalCursor,
        onEditVirtualController = viewModel::editVirtualController,
        modifier = modifier,
    )
}

@Composable
fun StreamOverlayLayer(
    uiState: StreamUiState,
    onMenuClick: () -> Unit,
    onZoomClick: () -> Unit,
    onHudClick: () -> Unit,
    onCloseExpandedHud: () -> Unit,
    onDismissWarning: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenQuitConfirm: () -> Unit,
    onConfirmQuit: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleZoomMode: () -> Unit,
    onTogglePerfHud: () -> Unit,
    onToggleVirtualController: () -> Unit,
    onSelectMouseMode: (StreamMouseMode) -> Unit,
    onSendShortcut: (StreamShortcut) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleLocalCursor: () -> Unit = {},
    onEditVirtualController: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (!uiState.isInPiP) {
            StreamOverlayHud(
                stats = uiState.perfStats,
                mode = uiState.perfOverlayMode,
                position = uiState.perfOverlayPosition,
                onHudClick = onHudClick,
                onCloseClick = onCloseExpandedHud,
            )

            StreamConnectionBanner(
                warningMessage = uiState.connectionWarning,
                onDismiss = onDismissWarning,
            )

            StreamFloatingControls(
                isMenuButtonVisible = uiState.isFloatingMenuButtonVisible,
                isZoomButtonVisible = uiState.isFloatingZoomButtonVisible,
                isPanZoomMode = uiState.isPanZoomMode,
                onMenuClick = onMenuClick,
                onZoomClick = onZoomClick,
            )
        }

        when (uiState.activeDialog) {
            StreamDialog.NONE -> Unit
            StreamDialog.MAIN_MENU -> {
                StreamControlDrawer(
                    isPanZoomMode = uiState.isPanZoomMode,
                    isVirtualControllerEnabled = uiState.isVirtualControllerEnabled,
                    perfOverlayMode = uiState.perfOverlayMode,
                    selectedMouseMode = uiState.selectedMouseMode,
                    shortcuts = uiState.shortcuts,
                    onDisconnect = onDisconnect,
                    onOpenQuitConfirm = onOpenQuitConfirm,
                    onToggleKeyboard = onToggleKeyboard,
                    onToggleZoomMode = onToggleZoomMode,
                    onSelectMouseMode = onSelectMouseMode,
                    onSendShortcut = onSendShortcut,
                    onTogglePerfHud = onTogglePerfHud,
                    onToggleVirtualController = onToggleVirtualController,
                    onDismiss = onDismissDialog,
                    isLocalCursorEnabled = uiState.isLocalCursorEnabled,
                    onToggleLocalCursor = onToggleLocalCursor,
                    onEditVirtualController = onEditVirtualController,
                )
            }
            StreamDialog.CONFIRM_QUIT -> {
                StreamQuitConfirmDialog(
                    onConfirm = onConfirmQuit,
                    onDismiss = onDismissDialog,
                )
            }
            StreamDialog.MOUSE_MODE_PICKER -> {
                StreamMouseModeDialog(
                    selectedMode = uiState.selectedMouseMode,
                    onSelectMode = onSelectMouseMode,
                    onDismiss = onDismissDialog,
                )
            }
            StreamDialog.SPECIAL_KEYS_PICKER -> {
                StreamSpecialKeysDialog(
                    shortcuts = uiState.shortcuts,
                    onSendShortcut = onSendShortcut,
                    onDismiss = onDismissDialog,
                )
            }
        }
    }
}
