@file:Suppress("TooGenericExceptionCaught")

package io.github.staernid.sunbeam.binding.video

import android.os.Build
import android.view.Surface
import io.github.staernid.sunbeam.LimeLog
import java.lang.reflect.Method

/**
 * Utility helpers for configuring underlying [Surface] properties (frame-rate, producer throttling).
 */
object SurfaceUtils {
    private const val TAG = "SurfaceUtils"

    private val producerThrottlingMethod: Method? by lazy {
        try {
            Surface::class.java.getMethod("setProducerThrottlingEnabled", Boolean::class.javaPrimitiveType)
        } catch (_: NoSuchMethodException) {
            null
        } catch (t: Throwable) {
            LimeLog.warning("$TAG: Failed to reflect setProducerThrottlingEnabled: ${t.message}")
            null
        }
    }

    /**
     * Disables or enables producer throttling on the surface (Android 15+ / Baklava / API 35+).
     * Disabling producer throttling prevents SurfaceFlinger from blocking/throttling the video
     * decoder or render thread, significantly lowering presentation latency.
     */
    @JvmStatic
    fun setProducerThrottlingEnabled(
        surface: Surface?,
        enabled: Boolean,
    ) {
        if (surface == null || !surface.isValid) return
        val method = producerThrottlingMethod ?: return
        try {
            method.invoke(surface, enabled)
            LimeLog.info("$TAG: Surface producer throttling set to $enabled")
        } catch (t: Throwable) {
            LimeLog.warning("$TAG: Could not set producer throttling: ${t.message}")
        }
    }

    /**
     * Safely applies the target frame rate to the display surface if supported by the Android version.
     */
    @JvmStatic
    fun applySurfaceFrameRate(
        surface: Surface?,
        fps: Float,
    ) {
        if (surface == null || !surface.isValid || fps <= 0f) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                surface.setFrameRate(fps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
            } catch (t: Throwable) {
                LimeLog.warning("$TAG: Could not apply surface frame rate: ${t.message}")
            }
        }
    }
}
