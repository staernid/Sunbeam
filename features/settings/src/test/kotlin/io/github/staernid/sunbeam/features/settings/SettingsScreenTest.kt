package io.github.staernid.sunbeam.features.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderSettingsScreen(
        uiState: SettingsUiState = SettingsUiState(settings = StreamSettings(), isLoading = false),
        onUpdateSetting: ((StreamSettings) -> StreamSettings) -> Unit = {},
    ) {
        composeTestRule.setContent {
            SunbeamTheme {
                SettingsScreen(
                    uiState = uiState,
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
                    onUpdateSetting = onUpdateSetting,
                    onConfirmReset = {},
                    onUserMessageDismissed = {},
                )
            }
        }
    }

    @Test
    fun `all setting categories are displayed`() {
        renderSettingsScreen()

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Quality Preset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Low Latency (1080p 120FPS)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Balanced (1080p 60FPS 20Mbps)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Appearance").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("OLED Black Mode").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Dynamic Color (Material You)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Video & Display").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Resolution").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Frame Rate").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Video Bitrate").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Fit (Maintain Aspect)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Automatic").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Minimum Latency").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Advanced Display & Window Behavior").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Audio").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Stereo (2.0)").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Gamepad & Input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Advanced Controller").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Touch, Mouse & Trackpad").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Keyboard & Shortcuts").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `expanding advanced sections and toggling settings invokes callback`() {
        var currentSettings = StreamSettings()
        renderSettingsScreen(
            uiState = SettingsUiState(settings = currentSettings, isLoading = false),
            onUpdateSetting = { transform -> currentSettings = transform(currentSettings) },
        )

        composeTestRule.onNodeWithText("Touch, Mouse & Trackpad").performScrollTo().performClick()
        composeTestRule
            .onNodeWithText("Touchscreen as Direct Touch vs Trackpad")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(currentSettings.absoluteMouseMode)

        composeTestRule.onNodeWithText("Keyboard & Shortcuts").performScrollTo().performClick()
        composeTestRule
            .onNodeWithText("Android Back button acts as Guide/Home")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(currentSettings.backAsGuide)

        composeTestRule.onNodeWithText("Advanced Controller").performScrollTo().performClick()
        composeTestRule
            .onNodeWithText("Use Device Vibration if Controller Lacks Rumble")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(currentSettings.vibrateFallback)

        composeTestRule
            .onNodeWithText("Advanced Display & Window Behavior")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onNodeWithText("Picture-in-Picture on Minimize")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(currentSettings.enablePip)
    }

    @Test
    fun `search bar filters settings by title or description and auto-expands collapsible sections`() {
        renderSettingsScreen()

        composeTestRule.onNodeWithContentDescription("Search").performClick()
        composeTestRule.onNodeWithText("Search settings...").performTextInput("Picture-in-Picture")

        composeTestRule.onNodeWithText("Picture-in-Picture on Minimize").assertIsDisplayed()
        composeTestRule.onNodeWithText("OLED Black Mode").assertDoesNotExist()
        composeTestRule.onNodeWithText("Play Audio on Host PC").assertDoesNotExist()
    }

    @Test
    fun `resolution picker dialog displays resolution options`() {
        renderSettingsScreen(
            uiState = SettingsUiState(
                settings = StreamSettings(),
                dialogState = SettingsDialogState.ResolutionPicker,
            ),
        )

        composeTestRule.onNodeWithText("Select Resolution").assertIsDisplayed()
        composeTestRule.onNodeWithText("1920x1080").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2560x1440").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `reset confirmation dialog displays prompt and reset button`() {
        renderSettingsScreen(
            uiState = SettingsUiState(
                settings = StreamSettings(),
                dialogState = SettingsDialogState.ResetConfirmation,
            ),
        )

        composeTestRule.onNodeWithText("Reset All Settings?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `screenshot settings list light dark oled and resolution dialog`() {
        var darkTheme by mutableStateOf(false)
        var dialogState by mutableStateOf<SettingsDialogState>(SettingsDialogState.None)

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = darkTheme, dynamicColor = false, oledBlack = darkTheme) {
                SettingsScreen(
                    uiState = SettingsUiState(
                        settings = StreamSettings(oledBlackMode = darkTheme),
                        isLoading = false,
                        dialogState = dialogState,
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

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/settings_list_light.png")
        darkTheme = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/settings_list_dark_oled.png")

        dialogState = SettingsDialogState.ResolutionPicker
        composeTestRule.waitForIdle()
        composeTestRule
            .onNode(isDialog())
            .captureRoboImage("src/test/screenshots/settings_resolution_dialog_dark_oled.png")
    }
}
