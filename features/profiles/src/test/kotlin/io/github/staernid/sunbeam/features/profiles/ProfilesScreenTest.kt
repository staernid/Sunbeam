package io.github.staernid.sunbeam.features.profiles

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.features.profiles.component.OSC_PREFERENCE_NAME
import io.github.staernid.sunbeam.features.profiles.model.createNewProfile
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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
    fun displaysOscPreviewTabAndResetsSavedLayoutPreferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(OSC_PREFERENCE_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("100", "{\"x\":10,\"y\":20}").commit()

        var resetInvoked = false
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
                    onResetOscLayout = { resetInvoked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Virtual Controller Layout Preview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset Layout to Default").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(resetInvoked)
        assertNull(prefs.getString("100", null))
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

    @Test
    fun screenshotEmptyPopulatedAndCreateDialogInLightAndDarkOled() {
        var darkTheme by mutableStateOf(false)
        var profiles by mutableStateOf(emptyList<io.github.staernid.sunbeam.core.data.model.Profile>())
        var dialogState by mutableStateOf<ProfileDialogState>(ProfileDialogState.None)

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = darkTheme, dynamicColor = false, oledBlack = darkTheme) {
                ProfilesScreen(
                    uiState = ProfilesUiState(
                        profiles = profiles,
                        activeProfileId = sampleProfile.uuid,
                        isLoading = false,
                        selectedTab = ProfilesTab.PROFILES,
                        dialogState = dialogState,
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

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/profiles_empty_light.png")
        darkTheme = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/profiles_empty_dark_oled.png")

        profiles = listOf(sampleProfile)
        darkTheme = false
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/profiles_populated_light.png")
        darkTheme = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/profiles_populated_dark_oled.png")

        dialogState = ProfileDialogState.Create
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isDialog()).captureRoboImage("src/test/screenshots/profiles_create_dialog_dark_oled.png")
    }
}
