package io.github.staernid.sunbeam.features.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `all setting categories are displayed`() {
        composeTestRule.setContent {
            SunbeamTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        settings = StreamSettings(),
                        isLoading = false,
                    ),
                    onBackClick = {},
                    onOpenDialog = {},
                    onDismissDialog = {},
                    onBitrateChange = {},
                    onDeadzoneChange = {},
                    onOscOpacityChange = {},
                    onResolutionSelect = { _, _ -> },
                    onFpsSelect = {},
                    onVideoScaleSelect = {},
                    onVideoFormatSelect = {},
                    onAudioConfigSelect = {},
                    onFramePacingSelect = {},
                    onUpdateSetting = {},
                    onConfirmReset = {},
                    onUserMessageDismissed = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video & Display").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resolution").assertIsDisplayed()
        composeTestRule.onNodeWithText("Frame Rate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Video Bitrate").assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Gamepad & Input").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `resolution picker dialog displays resolution options`() {
        composeTestRule.setContent {
            SunbeamTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        settings = StreamSettings(),
                        dialogState = SettingsDialogState.ResolutionPicker,
                    ),
                    onBackClick = {},
                    onOpenDialog = {},
                    onDismissDialog = {},
                    onBitrateChange = {},
                    onDeadzoneChange = {},
                    onOscOpacityChange = {},
                    onResolutionSelect = { _, _ -> },
                    onFpsSelect = {},
                    onVideoScaleSelect = {},
                    onVideoFormatSelect = {},
                    onAudioConfigSelect = {},
                    onFramePacingSelect = {},
                    onUpdateSetting = {},
                    onConfirmReset = {},
                    onUserMessageDismissed = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Select Resolution").assertIsDisplayed()
        composeTestRule.onNodeWithText("1920x1080").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2560x1440").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `reset confirmation dialog displays prompt and reset button`() {
        composeTestRule.setContent {
            SunbeamTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        settings = StreamSettings(),
                        dialogState = SettingsDialogState.ResetConfirmation,
                    ),
                    onBackClick = {},
                    onOpenDialog = {},
                    onDismissDialog = {},
                    onBitrateChange = {},
                    onDeadzoneChange = {},
                    onOscOpacityChange = {},
                    onResolutionSelect = { _, _ -> },
                    onFpsSelect = {},
                    onVideoScaleSelect = {},
                    onVideoFormatSelect = {},
                    onAudioConfigSelect = {},
                    onFramePacingSelect = {},
                    onUpdateSetting = {},
                    onConfirmReset = {},
                    onUserMessageDismissed = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Reset All Settings?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
