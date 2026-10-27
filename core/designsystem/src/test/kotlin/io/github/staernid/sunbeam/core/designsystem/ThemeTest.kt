package io.github.staernid.sunbeam.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.staernid.sunbeam.core.designsystem.theme.LocalOledBlack
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamAmberPrimaryDark
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamAmberPrimaryLight
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamBackgroundDark
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dark theme without dynamic color provides sunbeam dark colors`() {
        var primary: Color? = null
        var background: Color? = null
        var isOled: Boolean? = null

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = true, dynamicColor = false, oledBlack = false) {
                primary = MaterialTheme.colorScheme.primary
                background = MaterialTheme.colorScheme.background
                isOled = LocalOledBlack.current
            }
        }

        assertEquals(SunbeamAmberPrimaryDark, primary)
        assertEquals(SunbeamBackgroundDark, background)
        assertEquals(false, isOled)
    }

    @Test
    fun `light theme without dynamic color provides sunbeam light colors`() {
        var primary: Color? = null
        var isOled: Boolean? = null

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = false, dynamicColor = false) {
                primary = MaterialTheme.colorScheme.primary
                isOled = LocalOledBlack.current
            }
        }

        assertEquals(SunbeamAmberPrimaryLight, primary)
        assertFalse(isOled == true)
    }

    @Test
    fun `oled mode sets background to pure black`() {
        var background: Color? = null
        var isOled: Boolean? = null

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = true, dynamicColor = false, oledBlack = true) {
                background = MaterialTheme.colorScheme.background
                isOled = LocalOledBlack.current
            }
        }

        assertEquals(Color.Black, background)
        assertTrue(isOled == true)
    }
}
