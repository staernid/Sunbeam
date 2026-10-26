package io.github.staernid.sunbeam.binding.input

import org.junit.Assert.assertEquals
import org.junit.Test

class ControllerLightsManagerTest {
    @Test
    fun packRgbToArgb_packsColorsWithFullAlpha() {
        // Red only
        val red =
            ControllerLightsManager.packRgbToArgb(
                0xFF.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            )
        assertEquals(0xFFFF0000.toInt(), red)

        // Green only
        val green =
            ControllerLightsManager.packRgbToArgb(
                0x00.toByte(),
                0xFF.toByte(),
                0x00.toByte(),
            )
        assertEquals(0xFF00FF00.toInt(), green)

        // Blue only
        val blue =
            ControllerLightsManager.packRgbToArgb(
                0x00.toByte(),
                0x00.toByte(),
                0xFF.toByte(),
            )
        assertEquals(0xFF0000FF.toInt(), blue)

        // Sunshine orange: #FFA500
        val orange =
            ControllerLightsManager.packRgbToArgb(
                0xFF.toByte(),
                0xA5.toByte(),
                0x00.toByte(),
            )
        assertEquals(0xFFFFA500.toInt(), orange)
    }

    @Test
    fun sonyVendorId_matchesExpectedConstant() {
        assertEquals(0x054c, ControllerLightsManager.SONY_VENDOR_ID)
    }
}
