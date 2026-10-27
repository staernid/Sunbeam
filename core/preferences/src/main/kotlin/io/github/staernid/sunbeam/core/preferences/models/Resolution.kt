package io.github.staernid.sunbeam.core.preferences.models

import kotlin.math.abs
import kotlin.math.round

private const val ASPECT_TOLERANCE = 0.05f
private const val RATIO_16_9 = 16f / 9f
private const val RATIO_16_10 = 16f / 10f
private const val RATIO_21_9 = 21f / 9f
private const val RATIO_20_9 = 20f / 9f
private const val RATIO_19_5_9 = 19.5f / 9f
private const val RATIO_4_3 = 4f / 3f
private const val ROUND_SCALE = 10f

/**
 * Represents display/stream resolution dimensions.
 */
data class Resolution(
    val width: Int,
    val height: Int,
) {
    override fun toString(): String = "${width}x$height"

    val aspectRatioLabel: String
        get() {
            if (width <= 0 || height <= 0) return ""
            val g = gcd(width, height)
            val rw = width / g
            val rh = height / g
            return when {
                rw == 16 && rh == 9 -> "16:9"
                rw == 16 && rh == 10 -> "16:10"
                rw == 21 && rh == 9 -> "21:9"
                rw == 4 && rh == 3 -> "4:3"
                rw == 3 && rh == 2 -> "3:2"
                rw == 1 && rh == 1 -> "1:1"
                else -> {
                    val ratio = width.toFloat() / height.toFloat()
                    when {
                        abs(ratio - RATIO_16_9) < ASPECT_TOLERANCE -> "16:9"
                        abs(ratio - RATIO_16_10) < ASPECT_TOLERANCE -> "16:10"
                        abs(ratio - RATIO_21_9) < ASPECT_TOLERANCE -> "21:9"
                        abs(ratio - RATIO_20_9) < ASPECT_TOLERANCE -> "20:9"
                        abs(ratio - RATIO_19_5_9) < ASPECT_TOLERANCE -> "19.5:9"
                        abs(ratio - RATIO_4_3) < ASPECT_TOLERANCE -> "4:3"
                        else -> {
                            val rounded = round(ratio * ROUND_SCALE) / ROUND_SCALE
                            "$rounded:1"
                        }
                    }
                }
            }
        }

    val standardDisplayName: String?
        get() = when (this) {
            RES_720P -> "720p HD"
            RES_1080P -> "1080p FHD"
            RES_1440P -> "1440p QHD"
            RES_4K -> "4K UHD"
            RES_800P -> "800p"
            RES_1200P -> "1200p"
            RES_1600P -> "1600p"
            RES_480P -> "480p SD"
            RES_360P -> "360p"
            else -> null
        }

    companion object {
        val RES_360P = Resolution(640, 360)
        val RES_480P = Resolution(854, 480)
        val RES_720P = Resolution(1280, 720)
        val RES_1080P = Resolution(1920, 1080)
        val RES_1440P = Resolution(2560, 1440)
        val RES_4K = Resolution(3840, 2160)

        val RES_800P = Resolution(1280, 800)
        val RES_1200P = Resolution(1920, 1200)
        val RES_1600P = Resolution(2560, 1600)

        const val NATIVE_KEY = "Native"

        fun fromString(resStr: String?): Resolution? {
            if (resStr.isNullOrBlank() || resStr.equals(NATIVE_KEY, ignoreCase = true)) {
                return null
            }
            val parts = resStr.split("x", "X")
            val w = parts.getOrNull(0)?.trim()?.toIntOrNull()
            val h = parts.getOrNull(1)?.trim()?.toIntOrNull()
            return if (parts.size == 2 && w != null && h != null) Resolution(w, h) else null
        }

        private tailrec fun gcd(
            a: Int,
            b: Int,
        ): Int = if (b == 0) a else gcd(b, a % b)
    }
}
