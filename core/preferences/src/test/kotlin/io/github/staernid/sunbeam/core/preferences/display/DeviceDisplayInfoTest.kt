package io.github.staernid.sunbeam.core.preferences.display

import io.github.staernid.sunbeam.core.preferences.models.Resolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceDisplayInfoTest {
    @Test
    fun `normal landscape phone without notch calculates full and halved resolutions`() {
        val display = DeviceDisplayInfo(
            physicalWidth = 2400,
            physicalHeight = 1080,
            safeInsetLeft = 0,
            safeInsetRight = 0,
            safeInsetTop = 0,
            safeInsetBottom = 0,
        )

        assertFalse(display.hasCutoutInsets)
        assertEquals(Resolution(2400, 1080), display.fullResolution)
        assertEquals(Resolution(2400, 1080), display.avoidantResolution)
        assertEquals(Resolution(1200, 540), display.halvedResolution)
    }

    @Test
    fun `portrait-reported phone with camera punchhole computes avoidant resolution correctly`() {
        // A phone reporting 1080x2400 in portrait with a punchhole of 80px at the top
        val display = DeviceDisplayInfo(
            physicalWidth = 1080,
            physicalHeight = 2400,
            safeInsetTop = 80,
            safeInsetBottom = 0,
            safeInsetLeft = 0,
            safeInsetRight = 0,
        )

        assertTrue(display.hasCutoutInsets)
        assertEquals(Resolution(2400, 1080), display.fullResolution)
        // In landscape, top inset (80px) is along the long width dimension -> (2400 - 80) = 2320
        assertEquals(Resolution(2320, 1080), display.avoidantResolution)
        assertEquals(Resolution(1200, 540), display.halvedResolution)
    }

    @Test
    fun `halved resolution rounds down to even pixel macroblocks`() {
        val display = DeviceDisplayInfo(
            physicalWidth = 2401,
            physicalHeight = 1081,
        )

        // Landscape dimensions: 2401x1081
        // Halved: (2401/2) = 1200, rounded to even -> 1200; (1081/2) = 540, rounded to even -> 540
        val half = display.halvedResolution
        assertEquals(0, half.width % 2)
        assertEquals(0, half.height % 2)
    }

    @Test
    fun `ResolutionProvider returns correct categories and options`() {
        val display = DeviceDisplayInfo(
            physicalWidth = 1080,
            physicalHeight = 2400,
            safeInsetTop = 80,
        )

        val nativeOptions = ResolutionProvider.getNativeOptions(display)
        assertEquals(3, nativeOptions.size)
        assertEquals("Native Fullscreen", nativeOptions[0].title)
        assertEquals(Resolution(2400, 1080), nativeOptions[0].resolution)

        assertEquals("Native Avoidant", nativeOptions[1].title)
        assertEquals(Resolution(2320, 1080), nativeOptions[1].resolution)

        assertEquals("Native Halved", nativeOptions[2].title)
        assertEquals(Resolution(1200, 540), nativeOptions[2].resolution)

        assertEquals(4, ResolutionProvider.STANDARD_16_9.size)
        assertEquals(3, ResolutionProvider.HANDHELD_16_10.size)
    }
}
