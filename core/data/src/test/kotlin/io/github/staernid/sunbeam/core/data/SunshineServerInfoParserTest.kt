package io.github.staernid.sunbeam.core.data

import io.github.staernid.sunbeam.core.data.model.SunshineServerCapabilities
import io.github.staernid.sunbeam.core.data.parser.SunshineServerInfoParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SunshineServerInfoParserTest {
    @Test
    fun parse_sunshineServerInfo_extractsCapabilities() {
        val xml =
            """
            <root status_code="200">
                <hostname>DESKTOP-GAMING</hostname>
                <appversion>0.23.1</appversion>
                <GfeVersion>3.28.0.417</GfeVersion>
                <gputype>NVIDIA GeForce RTX 4090</gputype>
                <uniqueid>01234567-89ab-cdef-0123-456789abcdef</uniqueid>
                <HttpsPort>47984</HttpsPort>
                <MaxLumaPixelsHEVC>186977280</MaxLumaPixelsHEVC>
                <ServerCodecModeSupport>${
                SunshineServerCapabilities.SCM_H264 or
                    SunshineServerCapabilities.SCM_HEVC_MAIN10 or
                    SunshineServerCapabilities.SCM_AV1_MAIN10
            }</ServerCodecModeSupport>
                <state>SUNSHINE_SERVER_FREE</state>
            </root>
            """.trimIndent()

        val capabilities = SunshineServerInfoParser.parse(xml)

        assertTrue(capabilities.isSunshine)
        assertEquals("0.23.1", capabilities.appVersion)
        assertEquals("3.28.0.417", capabilities.gfeVersion)
        assertEquals("NVIDIA GeForce RTX 4090", capabilities.gpuType)
        assertEquals(186977280L, capabilities.maxLumaPixelsHevc)
        assertTrue(capabilities.supportsH264)
        assertTrue(capabilities.supportsHevc)
        assertTrue(capabilities.supportsAv1)
        assertTrue(capabilities.supports10Bit)
        assertTrue(capabilities.supportsAv1Main10)
        assertTrue(capabilities.supportsHevcMain10)
    }

    @Test
    fun parse_gfeServerInfo_detectsNonSunshine() {
        val xml =
            """
            <root status_code="200">
                <hostname>OFFICE-PC</hostname>
                <appversion>7.1.431.0</appversion>
                <GfeVersion>3.20.5.70</GfeVersion>
                <ServerCodecModeSupport>1</ServerCodecModeSupport>
                <state>MJOLNIR_SERVER_FREE</state>
            </root>
            """.trimIndent()

        val capabilities = SunshineServerInfoParser.parse(xml)

        assertFalse(capabilities.isSunshine)
        assertTrue(capabilities.supportsH264)
        assertFalse(capabilities.supportsAv1)
        assertFalse(capabilities.supports10Bit)
    }

    @Test
    fun parse_emptyOrInvalid_returnsDefault() {
        val capabilities = SunshineServerInfoParser.parse("")
        assertEquals(SunshineServerCapabilities.DEFAULT, capabilities)
    }
}
