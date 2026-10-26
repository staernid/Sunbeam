@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.core.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.github.staernid.sunbeam.core.preferences.datastore.StreamPreferenceKeys
import io.github.staernid.sunbeam.core.preferences.datastore.StreamSettingsMapper
import io.github.staernid.sunbeam.core.preferences.models.AnalogStickScrolling
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamSettingsMapperTest {
    @Test
    fun fromEmptyPreferences_returnsDefaultSettings() {
        val prefs = emptyPreferences()
        val settings = StreamSettingsMapper.fromPreferences(prefs)

        assertEquals(1280, settings.width)
        assertEquals(720, settings.height)
        assertEquals(60, settings.fps)
        assertEquals(10000, settings.bitrateKbps)
        assertEquals(VideoScaleMode.FIT, settings.videoScaleMode)
        assertEquals(VideoFormatOption.AUTO, settings.videoFormat)
        assertEquals(AudioConfiguration.STEREO, settings.audioConfig)
        assertEquals(FramePacingMode.MIN_LATENCY, settings.framePacing)
        assertFalse(settings.enableHdr)
        assertFalse(settings.onscreenController)
        assertTrue(settings.multiController)
    }

    @Test
    fun fromPreferences_parsesCustomConfig() {
        val prefs =
            mutablePreferencesOf(
                StreamPreferenceKeys.RESOLUTION to "1920x1080",
                StreamPreferenceKeys.FPS to "120",
                StreamPreferenceKeys.BITRATE_KBPS to 35000,
                StreamPreferenceKeys.VIDEO_SCALE_MODE to "stretch",
                StreamPreferenceKeys.VIDEO_FORMAT to "av1",
                StreamPreferenceKeys.AUDIO_CONFIG to "51",
                StreamPreferenceKeys.FRAME_PACING to "balanced",
                StreamPreferenceKeys.ANALOG_SCROLLING to "left",
                StreamPreferenceKeys.ENABLE_HDR to true,
                StreamPreferenceKeys.FULL_RANGE to true,
                StreamPreferenceKeys.ONSCREEN_CONTROLLER to true,
                StreamPreferenceKeys.ENABLE_RUMBLE to false,
                StreamPreferenceKeys.DEADZONE to 12,
                StreamPreferenceKeys.OSC_OPACITY to 75,
            )

        val settings = StreamSettingsMapper.fromPreferences(prefs)

        assertEquals(1920, settings.width)
        assertEquals(1080, settings.height)
        assertEquals(120, settings.fps)
        assertEquals(35000, settings.bitrateKbps)
        assertEquals(VideoScaleMode.STRETCH, settings.videoScaleMode)
        assertEquals(VideoFormatOption.FORCE_AV1, settings.videoFormat)
        assertEquals(AudioConfiguration.SURROUND_51, settings.audioConfig)
        assertEquals(FramePacingMode.BALANCED, settings.framePacing)
        assertEquals(AnalogStickScrolling.LEFT, settings.analogStickScrolling)
        assertTrue(settings.enableHdr)
        assertTrue(settings.fullRange)
        assertTrue(settings.onscreenController)
        assertFalse(settings.enableRumble)
        assertEquals(12, settings.deadzonePercentage)
        assertEquals(75, settings.oscOpacity)
    }

    @Test
    fun toPreferences_serializesSettings() {
        val settings =
            StreamSettings(
                width = 2560,
                height = 1440,
                fps = 90,
                bitrateKbps = 45000,
                videoScaleMode = VideoScaleMode.FILL,
                videoFormat = VideoFormatOption.FORCE_HEVC,
                audioConfig = AudioConfiguration.SURROUND_71,
                framePacing = FramePacingMode.MAX_SMOOTHNESS,
                analogStickScrolling = AnalogStickScrolling.NONE,
                enableHdr = true,
                onscreenController = true,
                enableRumble = false,
                deadzonePercentage = 8,
                oscOpacity = 85,
            )

        val prefs = mutablePreferencesOf()
        StreamSettingsMapper.toPreferences(settings, prefs)

        assertEquals("2560x1440", prefs[StreamPreferenceKeys.RESOLUTION])
        assertEquals("90", prefs[StreamPreferenceKeys.FPS])
        assertEquals(45000, prefs[StreamPreferenceKeys.BITRATE_KBPS])
        assertEquals("fill", prefs[StreamPreferenceKeys.VIDEO_SCALE_MODE])
        assertEquals("hevc", prefs[StreamPreferenceKeys.VIDEO_FORMAT])
        assertEquals("71", prefs[StreamPreferenceKeys.AUDIO_CONFIG])
        assertEquals("smoothness", prefs[StreamPreferenceKeys.FRAME_PACING])
        assertEquals("none", prefs[StreamPreferenceKeys.ANALOG_SCROLLING])
        assertEquals(true, prefs[StreamPreferenceKeys.ENABLE_HDR])
        assertEquals(true, prefs[StreamPreferenceKeys.ONSCREEN_CONTROLLER])
        assertEquals(false, prefs[StreamPreferenceKeys.ENABLE_RUMBLE])
        assertEquals(8, prefs[StreamPreferenceKeys.DEADZONE])
        assertEquals(85, prefs[StreamPreferenceKeys.OSC_OPACITY])
    }
}
