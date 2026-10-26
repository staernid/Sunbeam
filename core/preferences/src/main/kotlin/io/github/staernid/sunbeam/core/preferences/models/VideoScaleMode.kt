package io.github.staernid.sunbeam.core.preferences.models

/**
 * Strategy for scaling video stream to fit the screen.
 */
enum class VideoScaleMode(val prefValue: String) {
    FIT("fit"),
    FILL("fill"),
    STRETCH("stretch"),
    ;

    companion object {
        fun fromPrefValue(value: String?): VideoScaleMode {
            return entries.firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: FIT
        }
    }
}
