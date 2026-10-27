@file:Suppress("TooManyFunctions")

package io.github.staernid.sunbeam.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KBPS_PER_MBPS = 1000

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.settingsFlow.collect { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun openDialog(dialog: SettingsDialogState) {
        _uiState.update { it.copy(dialogState = dialog) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = SettingsDialogState.None) }
    }

    fun updateResolution(
        width: Int,
        height: Int,
    ) {
        updateSetting { it.copy(width = width, height = height) }
        dismissDialog()
    }

    fun updateFps(fps: Int) {
        updateSetting { it.copy(fps = fps) }
        dismissDialog()
    }

    fun updateBitrateMbps(mbps: Int) {
        updateSetting { it.copy(bitrateKbps = mbps * KBPS_PER_MBPS) }
    }

    fun updateVideoScaleMode(mode: VideoScaleMode) {
        updateSetting { it.copy(videoScaleMode = mode) }
        dismissDialog()
    }

    fun updateVideoFormat(format: VideoFormatOption) {
        updateSetting { it.copy(videoFormat = format) }
        dismissDialog()
    }

    fun updateAudioConfig(config: AudioConfiguration) {
        updateSetting { it.copy(audioConfig = config) }
        dismissDialog()
    }

    fun updateFramePacing(mode: FramePacingMode) {
        updateSetting { it.copy(framePacing = mode) }
        dismissDialog()
    }

    fun updateOscOpacity(opacity: Int) {
        updateSetting { it.copy(oscOpacity = opacity) }
    }

    fun updateDeadzone(deadzone: Int) {
        updateSetting { it.copy(deadzonePercentage = deadzone) }
    }

    fun updateSetting(transform: (StreamSettings) -> StreamSettings) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.updateSettings(transform)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.setSettings(StreamSettings())
            _uiState.update {
                it.copy(
                    dialogState = SettingsDialogState.None,
                    userMessage = "Settings reset to defaults",
                )
            }
        }
    }

    fun dismissUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
