package io.github.staernid.sunbeam.stream

import android.content.Intent
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StreamIntentParserTest {
    @Test
    fun applyStreamExtras_withNoExtras_doesNotModifyConfig() {
        val intent = Intent()
        val prefConfig = PreferenceConfiguration().apply {
            width = 1280
            height = 720
            fps = 60f
            bitrate = 10000
        }

        StreamIntentParser.applyStreamExtras(intent, prefConfig)

        assertEquals(1280, prefConfig.width)
        assertEquals(720, prefConfig.height)
        assertEquals(60f, prefConfig.fps, 0.01f)
        assertEquals(10000, prefConfig.bitrate)
    }

    @Test
    fun applyStreamExtras_withValidExtras_updatesAllFields() {
        val intent = Intent().apply {
            putExtra(StreamIntentParser.EXTRA_STREAM_WIDTH, 2560)
            putExtra(StreamIntentParser.EXTRA_STREAM_HEIGHT, 1440)
            putExtra(StreamIntentParser.EXTRA_STREAM_FPS, 120)
            putExtra(StreamIntentParser.EXTRA_STREAM_BITRATE, 40000)
        }
        val prefConfig = PreferenceConfiguration().apply {
            width = 1280
            height = 720
            fps = 60f
            bitrate = 10000
        }

        StreamIntentParser.applyStreamExtras(intent, prefConfig)

        assertEquals(2560, prefConfig.width)
        assertEquals(1440, prefConfig.height)
        assertEquals(120f, prefConfig.fps, 0.01f)
        assertEquals(40000, prefConfig.bitrate)
    }

    @Test
    fun applyStreamExtras_withInvalidValues_ignoresThem() {
        val intent = Intent().apply {
            putExtra(StreamIntentParser.EXTRA_STREAM_WIDTH, -1)
            putExtra(StreamIntentParser.EXTRA_STREAM_HEIGHT, 0)
            putExtra(StreamIntentParser.EXTRA_STREAM_FPS, -60)
            putExtra(StreamIntentParser.EXTRA_STREAM_BITRATE, 0)
        }
        val prefConfig = PreferenceConfiguration().apply {
            width = 1920
            height = 1080
            fps = 60f
            bitrate = 20000
        }

        StreamIntentParser.applyStreamExtras(intent, prefConfig)

        assertEquals(1920, prefConfig.width)
        assertEquals(1080, prefConfig.height)
        assertEquals(60f, prefConfig.fps, 0.01f)
        assertEquals(20000, prefConfig.bitrate)
    }
}
