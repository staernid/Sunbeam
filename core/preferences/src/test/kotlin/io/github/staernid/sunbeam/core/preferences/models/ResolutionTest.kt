package io.github.staernid.sunbeam.core.preferences.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolutionTest {
    @Test
    fun `fromString parses valid resolution string`() {
        val res = Resolution.fromString("1920x1080")
        assertEquals(Resolution(1920, 1080), res)

        val resX = Resolution.fromString("2560X1440")
        assertEquals(Resolution(2560, 1440), resX)

        val resTrimmed = Resolution.fromString(" 3840 x 2160 ")
        assertEquals(Resolution(3840, 2160), resTrimmed)
    }

    @Test
    fun `fromString returns null on invalid or native input`() {
        assertNull(Resolution.fromString(null))
        assertNull(Resolution.fromString(""))
        assertNull(Resolution.fromString("Native"))
        assertNull(Resolution.fromString("invalid"))
        assertNull(Resolution.fromString("1920"))
        assertNull(Resolution.fromString("1920x"))
    }

    @Test
    fun `aspectRatioLabel computes known aspect ratios`() {
        assertEquals("16:9", Resolution(1920, 1080).aspectRatioLabel)
        assertEquals("16:9", Resolution(3840, 2160).aspectRatioLabel)
        assertEquals("16:9", Resolution(1280, 720).aspectRatioLabel)

        assertEquals("16:10", Resolution(1280, 800).aspectRatioLabel)
        assertEquals("16:10", Resolution(1920, 1200).aspectRatioLabel)
        assertEquals("16:10", Resolution(2560, 1600).aspectRatioLabel)

        assertEquals("21:9", Resolution(2560, 1080).aspectRatioLabel)
        assertEquals("20:9", Resolution(2400, 1080).aspectRatioLabel)
        assertEquals("4:3", Resolution(1024, 768).aspectRatioLabel)
        assertEquals("1:1", Resolution(1080, 1080).aspectRatioLabel)
    }

    @Test
    fun `standardDisplayName returns expected names`() {
        assertEquals("1080p FHD", Resolution.RES_1080P.standardDisplayName)
        assertEquals("4K UHD", Resolution.RES_4K.standardDisplayName)
        assertEquals("800p", Resolution.RES_800P.standardDisplayName)
        assertNull(Resolution(2400, 1080).standardDisplayName)
    }
}
