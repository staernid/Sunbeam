package io.github.staernid.sunbeam.features.profiles

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.features.profiles.model.createNewProfile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfilesScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleProfile = createNewProfile(
        name = "Ultra 4K",
        width = 3840,
        height = 2160,
        fps = 60,
        bitrateKbps = 50_000,
        stretchMode = true,
        oscEnabled = true,
    )

    @Test
    fun displaysProfilesListAndCards() {
        composeTestRule.setContent {
            SunbeamTheme {
                ProfilesScreen(
                    uiState = ProfilesUiState(
                        profiles = listOf(sampleProfile),
                        activeProfileId = sampleProfile.uuid,
                        isLoading = false,
                        selectedTab = ProfilesTab.PROFILES,
                    ),
                    onTabSelected = {},
                    onAddProfileClick = {},
                    onActivateProfile = {},
                    onEditProfileClick = {},
                    onDuplicateProfileClick = {},
                    onDeleteProfileClick = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _ -> },
                    onConfirmDeleteProfile = {},
                    onDismissDialog = {},
                    onTestRumble = {},
                    onOscOpacityChange = {},
                    onOscScaleChange = {},
                    onFeedbackMessageShown = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Profiles & Controls").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ultra 4K").assertIsDisplayed()
        composeTestRule.onNodeWithText("ACTIVE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stretch to fit").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("On-Screen Controls").assertCountEquals(2)
    }

    @Test
    fun displaysEmptyStateWhenNoProfiles() {
        composeTestRule.setContent {
            SunbeamTheme {
                ProfilesScreen(
                    uiState = ProfilesUiState(
                        profiles = emptyList(),
                        isLoading = false,
                        selectedTab = ProfilesTab.PROFILES,
                    ),
                    onTabSelected = {},
                    onAddProfileClick = {},
                    onActivateProfile = {},
                    onEditProfileClick = {},
                    onDuplicateProfileClick = {},
                    onDeleteProfileClick = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _ -> },
                    onConfirmDeleteProfile = {},
                    onDismissDialog = {},
                    onTestRumble = {},
                    onOscOpacityChange = {},
                    onOscScaleChange = {},
                    onFeedbackMessageShown = {},
                )
            }
        }

        composeTestRule.onNodeWithText("No Custom Profiles").assertIsDisplayed()
    }

    @Test
    fun displaysGamepadTesterTab() {
        composeTestRule.setContent {
            SunbeamTheme {
                ProfilesScreen(
                    uiState = ProfilesUiState(
                        profiles = emptyList(),
                        isLoading = false,
                        selectedTab = ProfilesTab.GAMEPAD_TESTER,
                    ),
                    onTabSelected = {},
                    onAddProfileClick = {},
                    onActivateProfile = {},
                    onEditProfileClick = {},
                    onDuplicateProfileClick = {},
                    onDeleteProfileClick = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _ -> },
                    onConfirmDeleteProfile = {},
                    onDismissDialog = {},
                    onTestRumble = {},
                    onOscOpacityChange = {},
                    onOscScaleChange = {},
                    onFeedbackMessageShown = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Connected Controllers (0)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thumbsticks").assertIsDisplayed()
        composeTestRule.onNodeWithText("Left Stick (LS)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Right Stick (RS)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Triggers & Bumpers").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Rumble / Haptic Test").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun displaysOscPreviewTab() {
        composeTestRule.setContent {
            SunbeamTheme {
                ProfilesScreen(
                    uiState = ProfilesUiState(
                        profiles = emptyList(),
                        isLoading = false,
                        selectedTab = ProfilesTab.OSC_PREVIEW,
                    ),
                    onTabSelected = {},
                    onAddProfileClick = {},
                    onActivateProfile = {},
                    onEditProfileClick = {},
                    onDuplicateProfileClick = {},
                    onDeleteProfileClick = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _ -> },
                    onConfirmDeleteProfile = {},
                    onDismissDialog = {},
                    onTestRumble = {},
                    onOscOpacityChange = {},
                    onOscScaleChange = {},
                    onFeedbackMessageShown = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Virtual Controller Layout Preview").assertIsDisplayed()
    }

    @Test
    fun displaysCreateDialog() {
        composeTestRule.setContent {
            SunbeamTheme {
                ProfilesScreen(
                    uiState = ProfilesUiState(
                        profiles = emptyList(),
                        isLoading = false,
                        selectedTab = ProfilesTab.PROFILES,
                        dialogState = ProfileDialogState.Create,
                    ),
                    onTabSelected = {},
                    onAddProfileClick = {},
                    onActivateProfile = {},
                    onEditProfileClick = {},
                    onDuplicateProfileClick = {},
                    onDeleteProfileClick = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _ -> },
                    onConfirmDeleteProfile = {},
                    onDismissDialog = {},
                    onTestRumble = {},
                    onOscOpacityChange = {},
                    onOscScaleChange = {},
                    onFeedbackMessageShown = {},
                )
            }
        }

        composeTestRule.onNodeWithText("New Profile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profile Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stream Resolution").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun displaysDeleteConfirmationDialog() {
        composeTestRule.setContent {
            SunbeamTheme {
                ProfilesScreen(
                    uiState = ProfilesUiState(
                        profiles = listOf(sampleProfile),
                        isLoading = false,
                        selectedTab = ProfilesTab.PROFILES,
                        dialogState = ProfileDialogState.DeleteConfirmation(sampleProfile),
                    ),
                    onTabSelected = {},
                    onAddProfileClick = {},
                    onActivateProfile = {},
                    onEditProfileClick = {},
                    onDuplicateProfileClick = {},
                    onDeleteProfileClick = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _ -> },
                    onConfirmDeleteProfile = {},
                    onDismissDialog = {},
                    onTestRumble = {},
                    onOscOpacityChange = {},
                    onOscScaleChange = {},
                    onFeedbackMessageShown = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Delete Profile").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Are you sure you want to delete 'Ultra 4K'? This action cannot be undone.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }
}
