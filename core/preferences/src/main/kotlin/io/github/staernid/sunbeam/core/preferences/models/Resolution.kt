package io.github.staernid.sunbeam.core.preferences.models

/**
 * Represents display/stream resolution dimensions.
 */
data class Resolution(
    val width: Int,
    val height: Int,
) {
    override fun toString(): String = "${width}x$height"

    companion object {
        val RES_360P = Resolution(640, 360)
        val RES_480P = Resolution(854, 480)
        val RES_720P = Resolution(1280, 720)
        val RES_1080P = Resolution(1920, 1080)
        val RES_1440P = Resolution(2560, 1440)
        val RES_4K = Resolution(3840, 2160)

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
    }
}
