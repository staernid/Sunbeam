@file:Suppress("LongParameterList", "LongMethod", "UnusedParameter")

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
import io.github.staernid.sunbeam.features.stream.component.StreamOverlayLayer
import io.github.staernid.sunbeam.features.stream.component.StreamSurface
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode

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
    onOpenMouseModePicker: () -> Unit = {},
    onOpenSpecialKeysPicker: () -> Unit = {},
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

        // Performance diagnostics HUD, warning banners, floating controls, and dialogs / drawer
        StreamOverlayLayer(
            uiState = uiState,
            onMenuClick = onMenuClick,
            onZoomClick = onZoomClick,
            onHudClick = onHudClick,
            onCloseExpandedHud = onCloseExpandedHud,
            onDismissWarning = onDismissWarning,
            onDisconnect = onDisconnect,
            onOpenQuitConfirm = onOpenQuitConfirm,
            onConfirmQuit = onConfirmQuit,
            onToggleKeyboard = onToggleKeyboard,
            onToggleZoomMode = onToggleZoomMode,
            onTogglePerfHud = onTogglePerfHud,
            onToggleVirtualController = onToggleVirtualController,
            onSelectMouseMode = onSelectMouseMode,
            onSendShortcut = onSendShortcut,
            onDismissDialog = onDismissDialog,
        )
    }
}
