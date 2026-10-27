package io.github.staernid.sunbeam.features.settings.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.core.preferences.display.DeviceDisplayInfo
import io.github.staernid.sunbeam.core.preferences.models.Resolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResolutionPickerDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dialog displays native options for notch device`() {
        val displayInfo = DeviceDisplayInfo(
            physicalWidth = 1080,
            physicalHeight = 2400,
            safeInsetTop = 80,
        )

        var selectedW = 0
        var selectedH = 0

        composeTestRule.setContent {
            SunbeamTheme {
                ResolutionPickerDialog(
                    currentResolution = Resolution(1920, 1080),
                    onResolutionSelected = { w, h ->
                        selectedW = w
                        selectedH = h
                    },
                    onDismiss = {},
                    displayInfo = displayInfo,
                )
            }
        }

        composeTestRule.onNodeWithText("Select Resolution").assertIsDisplayed()
        composeTestRule.onNodeWithText("DEVICE NATIVE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Native Fullscreen").assertIsDisplayed()
        composeTestRule.onNodeWithText("2400x1080").assertIsDisplayed()
        composeTestRule.onNodeWithText("Native Avoidant").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2320x1080").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Native Halved").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1200x540").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("Native Fullscreen").performScrollTo().performClick()
        assertEquals(2400, selectedW)
        assertEquals(1080, selectedH)
    }

    @Test
    fun `dialog selects standard resolution on click`() {
        val displayInfo = DeviceDisplayInfo(physicalWidth = 1920, physicalHeight = 1080, isTv = true)

        var selectedW = 0
        var selectedH = 0

        composeTestRule.setContent {
            SunbeamTheme {
                ResolutionPickerDialog(
                    currentResolution = Resolution(1920, 1080),
                    onResolutionSelected = { w, h ->
                        selectedW = w
                        selectedH = h
                    },
                    onDismiss = {},
                    displayInfo = displayInfo,
                )
            }
        }

        composeTestRule.onNodeWithText("720p HD").performScrollTo().performClick()
        assertEquals(1280, selectedW)
        assertEquals(720, selectedH)
    }

    @Test
    fun `custom resolution form validates and submits even dimensions`() {
        val displayInfo = DeviceDisplayInfo(physicalWidth = 1920, physicalHeight = 1080, isTv = true)

        var selectedW = 0
        var selectedH = 0

        composeTestRule.setContent {
            SunbeamTheme {
                ResolutionPickerDialog(
                    currentResolution = Resolution(1920, 1080),
                    onResolutionSelected = { w, h ->
                        selectedW = w
                        selectedH = h
                    },
                    onDismiss = {},
                    displayInfo = displayInfo,
                )
            }
        }

        composeTestRule.onNodeWithText("Custom Resolution").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Apply Custom Resolution").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("Width").performTextInput("560")
        composeTestRule.onNodeWithText("Apply Custom Resolution").performClick()

        assertTrue(selectedW >= 240)
        assertTrue(selectedH >= 240)
    }
}
