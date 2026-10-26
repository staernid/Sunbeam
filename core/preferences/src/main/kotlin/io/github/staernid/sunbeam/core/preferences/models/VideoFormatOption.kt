package io.github.staernid.sunbeam.core.preferences.models

/**
 * Codec selection strategy for video streams.
 */
enum class VideoFormatOption(val prefValue: String) {
    AUTO("auto"),
    FORCE_AV1("av1"),
    FORCE_HEVC("hevc"),
    FORCE_H264("h264"),
    ;

    companion object {
        fun fromPrefValue(value: String?): VideoFormatOption {
            return entries.firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: AUTO
        }
    }
}
