package io.github.staernid.sunbeam.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.staernid.sunbeam.core.designsystem.theme.LocalOledBlack
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamBackgroundDark
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamNeutralSecondaryDark
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamNeutralSecondaryLight
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamOrangePrimaryDark
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamOrangePrimaryLight
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
        var secondary: Color? = null
        var tertiary: Color? = null
        var background: Color? = null
        var isOled: Boolean? = null

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = true, dynamicColor = false, oledBlack = false) {
                primary = MaterialTheme.colorScheme.primary
                secondary = MaterialTheme.colorScheme.secondary
                tertiary = MaterialTheme.colorScheme.tertiary
                background = MaterialTheme.colorScheme.background
                isOled = LocalOledBlack.current
            }
        }

        assertEquals(SunbeamOrangePrimaryDark, primary)
        assertEquals(SunbeamNeutralSecondaryDark, secondary)
        assertEquals(SunbeamNeutralSecondaryDark, tertiary)
        assertEquals(SunbeamBackgroundDark, background)
        assertEquals(false, isOled)
    }

    @Test
    fun `light theme without dynamic color provides sunbeam light colors`() {
        var primary: Color? = null
        var secondary: Color? = null
        var tertiary: Color? = null
        var isOled: Boolean? = null

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = false, dynamicColor = false) {
                primary = MaterialTheme.colorScheme.primary
                secondary = MaterialTheme.colorScheme.secondary
                tertiary = MaterialTheme.colorScheme.tertiary
                isOled = LocalOledBlack.current
            }
        }

        assertEquals(SunbeamOrangePrimaryLight, primary)
        assertEquals(SunbeamNeutralSecondaryLight, secondary)
        assertEquals(SunbeamNeutralSecondaryLight, tertiary)
        assertFalse(isOled == true)
    }

    @Test
    fun `default theme uses sunbeam colors instead of dynamic colors`() {
        var primary: Color? = null
        var background: Color? = null

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
                background = MaterialTheme.colorScheme.background
            }
        }

        assertEquals(SunbeamOrangePrimaryDark, primary)
        assertEquals(SunbeamBackgroundDark, background)
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
