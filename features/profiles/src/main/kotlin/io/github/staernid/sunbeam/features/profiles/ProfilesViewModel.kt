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
import io.github.staernid.sunbeam.features.profiles.model.GamepadButton
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

/**
 * Presenter for Profile management, Gamepad input testing, and On-Screen Controller preview.
 */
class ProfilesViewModel(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
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

    fun setConnectedDevices(devices: List<GamepadDeviceInfo>) {
        _uiState.update { state ->
            val debug = state.gamepadDebugState.copy(
                devices = devices,
                selectedDeviceId = devices.firstOrNull()?.id,
            )
            state.copy(gamepadDebugState = debug)
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
        _uiState.update { state ->
            val debug = state.gamepadDebugState.copy(
                leftStickX = leftStickX,
                leftStickY = leftStickY,
                rightStickX = rightStickX,
                rightStickY = rightStickY,
                leftTrigger = leftTrigger,
                rightTrigger = rightTrigger,
            )
            state.copy(gamepadDebugState = debug)
        }
    }

    fun setButtonPressed(
        button: GamepadButton,
        pressed: Boolean,
    ) {
        _uiState.update { state ->
            val buttons = state.gamepadDebugState.pressedButtons.toMutableSet()
            if (pressed) buttons.add(button) else buttons.remove(button)
            val debug = state.gamepadDebugState.copy(pressedButtons = buttons)
            state.copy(gamepadDebugState = debug)
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
        _uiState.update { state ->
            val debug = state.gamepadDebugState.copy(
                gyroX = gyroX,
                gyroY = gyroY,
                gyroZ = gyroZ,
                accelX = accelX,
                accelY = accelY,
                accelZ = accelZ,
            )
            state.copy(gamepadDebugState = debug)
        }
    }

    fun setRumbleStrength(strength: Float) {
        _uiState.update { state ->
            val debug = state.gamepadDebugState.copy(rumbleStrength = strength.coerceIn(0f, 1f))
            state.copy(gamepadDebugState = debug)
        }
    }

    fun setRumbleActive(active: Boolean) {
        _uiState.update { state ->
            val debug = state.gamepadDebugState.copy(isRumbling = active)
            state.copy(gamepadDebugState = debug)
        }
    }

    fun setOscOpacity(opacity: Float) {
        _uiState.update { it.copy(oscOpacity = opacity.coerceIn(0.1f, 1.0f)) }
    }

    fun setOscScale(scale: Float) {
        _uiState.update { it.copy(oscScale = scale.coerceIn(0.5f, 2.0f)) }
    }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val isDown = event.action == KeyEvent.ACTION_DOWN
        val button = when (event.keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> GamepadButton.A
            KeyEvent.KEYCODE_BUTTON_B -> GamepadButton.B
            KeyEvent.KEYCODE_BUTTON_X -> GamepadButton.X
            KeyEvent.KEYCODE_BUTTON_Y -> GamepadButton.Y
            KeyEvent.KEYCODE_DPAD_UP -> GamepadButton.DPAD_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> GamepadButton.DPAD_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> GamepadButton.DPAD_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadButton.DPAD_RIGHT
            KeyEvent.KEYCODE_BUTTON_L1 -> GamepadButton.LB
            KeyEvent.KEYCODE_BUTTON_R1 -> GamepadButton.RB
            KeyEvent.KEYCODE_BUTTON_THUMBL -> GamepadButton.LSB
            KeyEvent.KEYCODE_BUTTON_THUMBR -> GamepadButton.RSB
            KeyEvent.KEYCODE_BUTTON_START -> GamepadButton.START
            KeyEvent.KEYCODE_BUTTON_SELECT -> GamepadButton.BACK
            KeyEvent.KEYCODE_BUTTON_MODE -> GamepadButton.GUIDE
            else -> null
        } ?: return false

        setButtonPressed(button, isDown)
        return true
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)
        val lt = maxOf(
            event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
            event.getAxisValue(MotionEvent.AXIS_BRAKE),
        )
        val rt = maxOf(
            event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
            event.getAxisValue(MotionEvent.AXIS_GAS),
        )

        updateAxes(
            leftStickX = lx,
            leftStickY = ly,
            rightStickX = rx,
            rightStickY = ry,
            leftTrigger = lt.coerceIn(0f, 1f),
            rightTrigger = rt.coerceIn(0f, 1f),
        )

        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        setButtonPressed(GamepadButton.DPAD_LEFT, hatX < -0.5f)
        setButtonPressed(GamepadButton.DPAD_RIGHT, hatX > 0.5f)
        setButtonPressed(GamepadButton.DPAD_UP, hatY < -0.5f)
        setButtonPressed(GamepadButton.DPAD_DOWN, hatY > 0.5f)

        return true
    }
}
