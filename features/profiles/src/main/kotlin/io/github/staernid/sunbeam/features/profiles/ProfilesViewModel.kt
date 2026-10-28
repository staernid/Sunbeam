@file:Suppress(
    "TooManyFunctions",
    "LongParameterList",
    "CyclomaticComplexMethod",
    "MagicNumber",
)

package io.github.staernid.sunbeam.features.profiles

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.staernid.sunbeam.core.data.model.Profile
import io.github.staernid.sunbeam.core.data.repository.ProfileRepository
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.features.profiles.component.reduceKeyEvent
import io.github.staernid.sunbeam.features.profiles.component.reduceMotionEvent
import io.github.staernid.sunbeam.features.profiles.model.GamepadButton
import io.github.staernid.sunbeam.features.profiles.model.GamepadDebugState
import io.github.staernid.sunbeam.features.profiles.model.GamepadDeviceInfo
import io.github.staernid.sunbeam.features.profiles.model.bitrateKbps
import io.github.staernid.sunbeam.features.profiles.model.copyWithOptions
import io.github.staernid.sunbeam.features.profiles.model.createNewProfile
import io.github.staernid.sunbeam.features.profiles.model.fps
import io.github.staernid.sunbeam.features.profiles.model.height
import io.github.staernid.sunbeam.features.profiles.model.oscEnabled
import io.github.staernid.sunbeam.features.profiles.model.oscOpacity
import io.github.staernid.sunbeam.features.profiles.model.stretchMode
import io.github.staernid.sunbeam.features.profiles.model.width
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Presenter for Profile management, Gamepad input testing, and On-Screen Controller preview.
 */
class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
    private val preferencesRepository: PreferencesRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
        observePreferences()
    }

    private fun observeProfiles() {
        viewModelScope.launch {
            combine(
                profileRepository.profilesFlow,
                profileRepository.activeProfileFlow,
            ) { profiles, activeProfile ->
                profiles to activeProfile?.uuid
            }.collect { (profiles, activeId) ->
                _uiState.update { current ->
                    current.copy(
                        profiles = profiles,
                        activeProfileId = activeId,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun observePreferences() {
        val repo = preferencesRepository ?: return
        viewModelScope.launch {
            repo.settingsFlow.collect { settings ->
                val normalizedOpacity = (settings.oscOpacity / 100f).coerceIn(0.1f, 1.0f)
                _uiState.update { it.copy(oscOpacity = normalizedOpacity) }
            }
        }
    }

    fun selectTab(tab: ProfilesTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(dialogState = ProfileDialogState.Create) }
    }

    fun showEditDialog(profile: Profile) {
        _uiState.update { it.copy(dialogState = ProfileDialogState.Edit(profile)) }
    }

    fun showDeleteDialog(profile: Profile) {
        _uiState.update { it.copy(dialogState = ProfileDialogState.DeleteConfirmation(profile)) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = ProfileDialogState.None) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(userFeedbackMessage = null) }
    }

    fun createProfile(
        name: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        stretchMode: Boolean,
        oscEnabled: Boolean,
        oscOpacity: Float,
    ) {
        viewModelScope.launch {
            val newProfile = createNewProfile(
                name = name,
                width = width,
                height = height,
                fps = fps,
                bitrateKbps = bitrateKbps,
                stretchMode = stretchMode,
                oscEnabled = oscEnabled,
                oscOpacity = oscOpacity,
            )
            profileRepository.saveProfile(newProfile)
            _uiState.update {
                it.copy(
                    dialogState = ProfileDialogState.None,
                    userFeedbackMessage = "Profile '${newProfile.name}' created",
                )
            }
        }
    }

    fun updateProfile(
        uuid: UUID,
        name: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        stretchMode: Boolean,
        oscEnabled: Boolean,
        oscOpacity: Float,
    ) {
        viewModelScope.launch {
            val existing = profileRepository.getProfile(uuid) ?: return@launch
            val updated = existing.copyWithOptions(
                name = name,
                width = width,
                height = height,
                fps = fps,
                bitrateKbps = bitrateKbps,
                stretchMode = stretchMode,
                oscEnabled = oscEnabled,
                oscOpacity = oscOpacity,
            )
            profileRepository.saveProfile(updated)
            _uiState.update {
                it.copy(
                    dialogState = ProfileDialogState.None,
                    userFeedbackMessage = "Profile '${updated.name}' updated",
                )
            }
        }
    }

    fun deleteProfile(uuid: UUID) {
        viewModelScope.launch {
            val target = profileRepository.getProfile(uuid)
            profileRepository.deleteProfile(uuid)
            _uiState.update {
                it.copy(
                    dialogState = ProfileDialogState.None,
                    userFeedbackMessage = "Profile '${target?.name ?: ""}' deleted",
                )
            }
        }
    }

    fun setActiveProfile(uuid: UUID?) {
        viewModelScope.launch {
            profileRepository.setActiveProfile(uuid)
            val msg = if (uuid == null) "Switched to default settings" else "Profile activated"
            _uiState.update { it.copy(userFeedbackMessage = msg) }
        }
    }

    fun duplicateProfile(profile: Profile) {
        viewModelScope.launch {
            val duplicate = createNewProfile(
                name = "${profile.name} (Copy)",
                width = profile.width,
                height = profile.height,
                fps = profile.fps,
                bitrateKbps = profile.bitrateKbps,
                stretchMode = profile.stretchMode,
                oscEnabled = profile.oscEnabled,
                oscOpacity = profile.oscOpacity,
            )
            profileRepository.saveProfile(duplicate)
            _uiState.update {
                it.copy(userFeedbackMessage = "Duplicated to '${duplicate.name}'")
            }
        }
    }

    // --- Gamepad Debug Operations ---

    fun updateGamepadState(newState: GamepadDebugState) {
        _uiState.update { it.copy(gamepadDebugState = newState) }
    }

    fun updateGamepadState(transform: (GamepadDebugState) -> GamepadDebugState) {
        _uiState.update { state ->
            state.copy(gamepadDebugState = transform(state.gamepadDebugState))
        }
    }

    fun setConnectedDevices(devices: List<GamepadDeviceInfo>) {
        updateGamepadState { debug ->
            debug.copy(
                devices = devices,
                selectedDeviceId = devices.firstOrNull()?.id,
            )
        }
    }

    fun updateAxes(
        leftStickX: Float,
        leftStickY: Float,
        rightStickX: Float,
        rightStickY: Float,
        leftTrigger: Float,
        rightTrigger: Float,
    ) {
        updateGamepadState { debug ->
            debug.copy(
                leftStickX = leftStickX,
                leftStickY = leftStickY,
                rightStickX = rightStickX,
                rightStickY = rightStickY,
                leftTrigger = leftTrigger,
                rightTrigger = rightTrigger,
            )
        }
    }

    fun setButtonPressed(
        button: GamepadButton,
        pressed: Boolean,
    ) {
        updateGamepadState { debug ->
            val buttons = debug.pressedButtons.toMutableSet()
            if (pressed) buttons.add(button) else buttons.remove(button)
            debug.copy(pressedButtons = buttons)
        }
    }

    fun updateSensors(
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
        accelX: Float,
        accelY: Float,
        accelZ: Float,
    ) {
        updateGamepadState { debug ->
            debug.copy(
                gyroX = gyroX,
                gyroY = gyroY,
                gyroZ = gyroZ,
                accelX = accelX,
                accelY = accelY,
                accelZ = accelZ,
            )
        }
    }

    fun setRumbleStrength(strength: Float) {
        updateGamepadState { debug ->
            debug.copy(rumbleStrength = strength.coerceIn(0f, 1f))
        }
    }

    fun setRumbleActive(active: Boolean) {
        updateGamepadState { debug ->
            debug.copy(isRumbling = active)
        }
    }

    fun setOscOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.1f, 1.0f)
        _uiState.update { it.copy(oscOpacity = clamped) }
        preferencesRepository?.let { repo ->
            viewModelScope.launch {
                repo.updateSettings { it.copy(oscOpacity = (clamped * 100f).roundToInt()) }
            }
        }
    }

    fun setOscScale(scale: Float) {
        _uiState.update { it.copy(oscScale = scale.coerceIn(0.5f, 2.0f)) }
    }

    fun resetOscLayout() {
        _uiState.update {
            it.copy(
                oscOpacity = 0.9f,
                oscScale = 1.0f,
                userFeedbackMessage = "On-screen controls layout reset to default",
            )
        }
        preferencesRepository?.let { repo ->
            viewModelScope.launch {
                repo.updateSettings { it.copy(oscOpacity = 90) }
            }
        }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val updated = reduceKeyEvent(_uiState.value.gamepadDebugState, event) ?: return false
        updateGamepadState(updated)
        return true
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        val updated = reduceMotionEvent(_uiState.value.gamepadDebugState, event)
        updateGamepadState(updated)
        return true
    }
}
