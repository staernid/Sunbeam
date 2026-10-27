package io.github.staernid.sunbeam.features.settings

import io.github.staernid.sunbeam.core.preferences.models.StreamSettings

sealed interface SettingsDialogState {
    data object None : SettingsDialogState

    data object ResolutionPicker : SettingsDialogState

    data object FpsPicker : SettingsDialogState

    data object VideoScalePicker : SettingsDialogState

    data object VideoFormatPicker : SettingsDialogState

    data object AudioConfigPicker : SettingsDialogState

    data object FramePacingPicker : SettingsDialogState

    data object ResetConfirmation : SettingsDialogState
}

data class SettingsUiState(
    val settings: StreamSettings = StreamSettings(),
    val isLoading: Boolean = false,
    val dialogState: SettingsDialogState = SettingsDialogState.None,
    val userMessage: String? = null,
) {
    val resolutionLabel: String
        get() = "${settings.width}x${settings.height}"

    val fpsLabel: String
        get() = "${settings.fps} FPS"

    val bitrateMbps: Int
        get() = (settings.bitrateKbps / 1000).coerceAtLeast(1)
}
