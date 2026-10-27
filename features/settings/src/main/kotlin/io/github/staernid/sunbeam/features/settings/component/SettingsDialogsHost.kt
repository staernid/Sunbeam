@file:Suppress("LongMethod")

package io.github.staernid.sunbeam.features.settings.component

import androidx.compose.runtime.Composable
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.Resolution
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import io.github.staernid.sunbeam.features.settings.SettingsDialogState
import io.github.staernid.sunbeam.features.settings.SettingsUiState

private val RESOLUTION_OPTIONS = listOf(
    Resolution.RES_720P,
    Resolution.RES_1080P,
    Resolution.RES_1440P,
    Resolution.RES_4K,
)

private val FPS_OPTIONS = listOf(30, 60, 90, 120)

@Composable
fun SettingsDialogsHost(
    uiState: SettingsUiState,
    onDismissDialog: () -> Unit,
    onResolutionSelect: (Int, Int) -> Unit,
    onFpsSelect: (Int) -> Unit,
    onVideoScaleSelect: (VideoScaleMode) -> Unit,
    onVideoFormatSelect: (VideoFormatOption) -> Unit,
    onAudioConfigSelect: (AudioConfiguration) -> Unit,
    onFramePacingSelect: (FramePacingMode) -> Unit,
    onConfirmReset: () -> Unit,
) {
    when (uiState.dialogState) {
        is SettingsDialogState.None -> Unit

        is SettingsDialogState.ResolutionPicker -> {
            val currentRes = Resolution(uiState.settings.width, uiState.settings.height)
            ListSelectionDialog(
                title = "Select Resolution",
                options = RESOLUTION_OPTIONS,
                selectedOption = currentRes,
                optionLabel = { "${it.width}x${it.height}" },
                onOptionSelected = { onResolutionSelect(it.width, it.height) },
                onDismiss = onDismissDialog,
            )
        }

        is SettingsDialogState.FpsPicker -> {
            ListSelectionDialog(
                title = "Select Frame Rate",
                options = FPS_OPTIONS,
                selectedOption = uiState.settings.fps,
                optionLabel = { "$it FPS" },
                onOptionSelected = onFpsSelect,
                onDismiss = onDismissDialog,
            )
        }

        is SettingsDialogState.VideoScalePicker -> {
            ListSelectionDialog(
                title = "Select Video Scale Mode",
                options = VideoScaleMode.entries,
                selectedOption = uiState.settings.videoScaleMode,
                optionLabel = { it.name },
                onOptionSelected = onVideoScaleSelect,
                onDismiss = onDismissDialog,
            )
        }

        is SettingsDialogState.VideoFormatPicker -> {
            ListSelectionDialog(
                title = "Select Video Codec",
                options = VideoFormatOption.entries,
                selectedOption = uiState.settings.videoFormat,
                optionLabel = { it.name },
                onOptionSelected = onVideoFormatSelect,
                onDismiss = onDismissDialog,
            )
        }

        is SettingsDialogState.AudioConfigPicker -> {
            ListSelectionDialog(
                title = "Select Audio Configuration",
                options = AudioConfiguration.entries,
                selectedOption = uiState.settings.audioConfig,
                optionLabel = { it.name },
                onOptionSelected = onAudioConfigSelect,
                onDismiss = onDismissDialog,
            )
        }

        is SettingsDialogState.FramePacingPicker -> {
            ListSelectionDialog(
                title = "Select Frame Pacing",
                options = FramePacingMode.entries,
                selectedOption = uiState.settings.framePacing,
                optionLabel = { it.name },
                onOptionSelected = onFramePacingSelect,
                onDismiss = onDismissDialog,
            )
        }

        is SettingsDialogState.ResetConfirmation -> {
            ResetConfirmationDialog(
                onConfirm = onConfirmReset,
                onDismiss = onDismissDialog,
            )
        }
    }
}
