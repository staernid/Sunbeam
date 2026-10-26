@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.core.preferences.models

/**
 * Audio output configuration and channel layout.
 */
enum class AudioConfiguration(val prefValue: String, val channelCount: Int) {
    STEREO("2", 2),
    SURROUND_51("51", 6),
    SURROUND_71("71", 8),
    ;

    companion object {
        fun fromPrefValue(value: String?): AudioConfiguration {
            return entries.firstOrNull { it.prefValue == value } ?: STEREO
        }
    }
}
