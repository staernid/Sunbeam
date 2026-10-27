package io.github.staernid.sunbeam.stream

import android.content.Intent
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration

/**
 * Extracts and overrides streaming configuration from launching intent extras.
 */
object StreamIntentParser {
    const val EXTRA_STREAM_WIDTH = "StreamWidth"
    const val EXTRA_STREAM_HEIGHT = "StreamHeight"
    const val EXTRA_STREAM_FPS = "StreamFps"
    const val EXTRA_STREAM_BITRATE = "StreamBitrate"

    @JvmStatic
    fun applyStreamExtras(
        intent: Intent?,
        prefConfig: PreferenceConfiguration?,
    ) {
        if (intent == null || prefConfig == null) return

        val width = intent.getIntExtra(EXTRA_STREAM_WIDTH, 0)
        val height = intent.getIntExtra(EXTRA_STREAM_HEIGHT, 0)
        if (width > 0 && height > 0) {
            prefConfig.width = width
            prefConfig.height = height
        }

        val fps = intent.getIntExtra(EXTRA_STREAM_FPS, 0)
        if (fps > 0) {
            prefConfig.fps = fps.toFloat()
        }

        val bitrate = intent.getIntExtra(EXTRA_STREAM_BITRATE, 0)
        if (bitrate > 0) {
            prefConfig.bitrate = bitrate
        }
    }
}
