package io.github.staernid.sunbeam.core.preferences.display

import io.github.staernid.sunbeam.core.preferences.models.Resolution

/**
 * Encapsulates the physical characteristics and cutout insets of the host Android device display.
 */
data class DeviceDisplayInfo(
    val physicalWidth: Int,
    val physicalHeight: Int,
    val safeInsetLeft: Int = 0,
    val safeInsetRight: Int = 0,
    val safeInsetTop: Int = 0,
    val safeInsetBottom: Int = 0,
    val refreshRate: Float = 60f,
    val isTv: Boolean = false,
) {
    val landscapeWidth: Int = maxOf(physicalWidth, physicalHeight)
    val landscapeHeight: Int = minOf(physicalWidth, physicalHeight)

    val hasCutoutInsets: Boolean
        get() = safeInsetLeft > 0 || safeInsetRight > 0 || safeInsetTop > 0 || safeInsetBottom > 0

    val fullResolution: Resolution
        get() = Resolution(landscapeWidth, landscapeHeight)

    /**
     * Resolves the resolution fitting inside camera notches and display cutouts,
     * normalized to landscape orientation and rounded down to even pixel values for video decoders.
     */
    val avoidantResolution: Resolution
        get() {
            val isReportedPortrait = physicalWidth < physicalHeight
            val insetW = if (isReportedPortrait) safeInsetTop + safeInsetBottom else safeInsetLeft + safeInsetRight
            val insetH = if (isReportedPortrait) safeInsetLeft + safeInsetRight else safeInsetTop + safeInsetBottom
            val w = ((landscapeWidth - insetW) / 2) * 2
            val h = ((landscapeHeight - insetH) / 2) * 2
            return Resolution(maxOf(w, MIN_DIMENSION), maxOf(h, MIN_DIMENSION))
        }

    /**
     * Exact 50% scale of the full native resolution, preserving aspect ratio with 1/4th the pixel count.
     */
    val halvedResolution: Resolution
        get() {
            val w = ((landscapeWidth / 2) / 2) * 2
            val h = ((landscapeHeight / 2) / 2) * 2
            return Resolution(maxOf(w, MIN_DIMENSION), maxOf(h, MIN_DIMENSION))
        }

    private companion object {
        private const val MIN_DIMENSION = 240
    }
}
