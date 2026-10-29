package io.github.staernid.sunbeam.features.profiles

import io.github.staernid.sunbeam.core.data.model.Profile
import io.github.staernid.sunbeam.features.profiles.model.GamepadDebugState
import java.util.UUID

/**
 * Top-level tabs in the Profiles & Input debugging feature.
 */
enum class ProfilesTab {
    PROFILES,
    GAMEPAD_TESTER,
    OSC_PREVIEW,
}

/**
 * Dialog states for profile CRUD actions.
 */
sealed interface ProfileDialogState {
    data object None : ProfileDialogState

    data object Create : ProfileDialogState

    data class Edit(val profile: Profile) : ProfileDialogState

    data class DeleteConfirmation(val profile: Profile) : ProfileDialogState
}

/**
 * Combined UI state for the Profiles feature.
 */
data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: UUID? = null,
    val isLoading: Boolean = true,
    val selectedTab: ProfilesTab = ProfilesTab.PROFILES,
    val dialogState: ProfileDialogState = ProfileDialogState.None,
    val gamepadDebugState: GamepadDebugState = GamepadDebugState(),
    val oscEnabled: Boolean = false,
    val hideOscWhenHasGamepad: Boolean = true,
    val onlyShowL3R3: Boolean = false,
    val showGuideButton: Boolean = true,
    val vibrateOsc: Boolean = true,
    val oscOpacity: Float = 0.7f,
    val oscScale: Float = 1.0f,
    val userFeedbackMessage: String? = null,
)
