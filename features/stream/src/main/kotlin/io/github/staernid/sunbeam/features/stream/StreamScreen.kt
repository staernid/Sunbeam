@file:Suppress("LongParameterList", "LongMethod")

package io.github.staernid.sunbeam.features.stream

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.staernid.sunbeam.features.stream.component.StreamConnectionBanner
import io.github.staernid.sunbeam.features.stream.component.StreamFloatingControls
import io.github.staernid.sunbeam.features.stream.component.StreamMenuDialog
import io.github.staernid.sunbeam.features.stream.component.StreamMouseModeDialog
import io.github.staernid.sunbeam.features.stream.component.StreamOverlayHud
import io.github.staernid.sunbeam.features.stream.component.StreamQuitConfirmDialog
import io.github.staernid.sunbeam.features.stream.component.StreamSpecialKeysDialog
import io.github.staernid.sunbeam.features.stream.component.StreamSurface
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.StreamDialog

/**
 * Main stream composable integrating the video [SurfaceView] layer, performance HUD,
 * transient alerts, floating action buttons, and in-stream control menus.
 */
@Composable
fun StreamScreen(
    onSurfaceCreated: (SurfaceHolder) -> Unit,
    onSurfaceChanged: (holder: SurfaceHolder, format: Int, width: Int, height: Int) -> Unit,
    onSurfaceDestroyed: (SurfaceHolder) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StreamViewModel = viewModel(),
    aspectRatio: Float? = null,
    onSurfaceViewCreated: ((SurfaceView) -> Unit)? = null,
    virtualControlsContent: (@Composable () -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StreamContent(
        uiState = uiState,
        onSurfaceCreated = onSurfaceCreated,
        onSurfaceChanged = onSurfaceChanged,
        onSurfaceDestroyed = onSurfaceDestroyed,
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
        onOpenMouseModePicker = viewModel::openMouseModePicker,
        onOpenSpecialKeysPicker = viewModel::openSpecialKeysPicker,
        onTogglePerfHud = viewModel::togglePerfHud,
        onToggleVirtualController = viewModel::toggleVirtualController,
        onSelectMouseMode = viewModel::selectMouseMode,
        onSendShortcut = viewModel::sendShortcut,
        onDismissDialog = viewModel::dismissDialog,
        modifier = modifier,
        aspectRatio = aspectRatio,
        onSurfaceViewCreated = onSurfaceViewCreated,
        virtualControlsContent = virtualControlsContent,
    )
}

/**
 * Stateless stream UI presentation layer.
 */
@Composable
fun StreamContent(
    uiState: StreamUiState,
    onSurfaceCreated: (SurfaceHolder) -> Unit,
    onSurfaceChanged: (holder: SurfaceHolder, format: Int, width: Int, height: Int) -> Unit,
    onSurfaceDestroyed: (SurfaceHolder) -> Unit,
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
    onOpenMouseModePicker: () -> Unit,
    onOpenSpecialKeysPicker: () -> Unit,
    onTogglePerfHud: () -> Unit,
    onToggleVirtualController: () -> Unit,
    onSelectMouseMode: (io.github.staernid.sunbeam.features.stream.model.StreamMouseMode) -> Unit,
    onSendShortcut: (io.github.staernid.sunbeam.features.stream.model.StreamShortcut) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float? = null,
    onSurfaceViewCreated: ((SurfaceView) -> Unit)? = null,
    virtualControlsContent: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Base hardware surface layer
        StreamSurface(
            onSurfaceCreated = onSurfaceCreated,
            onSurfaceChanged = onSurfaceChanged,
            onSurfaceDestroyed = onSurfaceDestroyed,
            aspectRatio = aspectRatio,
            onSurfaceViewCreated = onSurfaceViewCreated,
        )

        // Virtual controls layer
        if (uiState.isVirtualControllerEnabled && virtualControlsContent != null && !uiState.isInPiP) {
            virtualControlsContent()
        }

        // Performance diagnostics HUD
        if (!uiState.isInPiP) {
            StreamOverlayHud(
                stats = uiState.perfStats,
                mode = uiState.perfOverlayMode,
                position = uiState.perfOverlayPosition,
                onHudClick = onHudClick,
                onCloseClick = onCloseExpandedHud,
            )

            // Connection warning banner
            StreamConnectionBanner(
                warningMessage = uiState.connectionWarning,
                onDismiss = onDismissWarning,
            )

            // Floating action controls (Menu button & Zoom toggle)
            StreamFloatingControls(
                isMenuButtonVisible = uiState.isFloatingMenuButtonVisible,
                isZoomButtonVisible = uiState.isFloatingZoomButtonVisible,
                isPanZoomMode = uiState.isPanZoomMode,
                onMenuClick = onMenuClick,
                onZoomClick = onZoomClick,
            )
        }

        // In-stream active dialogs
        when (uiState.activeDialog) {
            StreamDialog.NONE -> Unit
            StreamDialog.MAIN_MENU -> {
                StreamMenuDialog(
                    isPanZoomMode = uiState.isPanZoomMode,
                    isVirtualControllerEnabled = uiState.isVirtualControllerEnabled,
                    onDisconnect = onDisconnect,
                    onOpenQuitConfirm = onOpenQuitConfirm,
                    onToggleKeyboard = onToggleKeyboard,
                    onToggleZoomMode = onToggleZoomMode,
                    onOpenMouseModePicker = onOpenMouseModePicker,
                    onOpenSpecialKeysPicker = onOpenSpecialKeysPicker,
                    onTogglePerfHud = onTogglePerfHud,
                    onToggleVirtualController = onToggleVirtualController,
                    onDismiss = onDismissDialog,
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
