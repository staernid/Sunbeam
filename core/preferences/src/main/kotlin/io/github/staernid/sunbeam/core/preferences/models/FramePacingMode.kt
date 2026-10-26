@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.core.preferences.models

/**
 * Frame pacing strategy balancing latency against frame presentation smoothness.
 */
enum class FramePacingMode(val prefValue: String, val modeId: Int) {
    MIN_LATENCY("latency", 0),
    BALANCED("balanced", 1),
    CAP_FPS("cap-fps", 2),
    MAX_SMOOTHNESS("smoothness", 3),
    ;

    companion object {
        fun fromPrefValue(value: String?): FramePacingMode {
            return entries.firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: MIN_LATENCY
        }

        fun fromModeId(id: Int): FramePacingMode {
            return entries.firstOrNull { it.modeId == id } ?: MIN_LATENCY
        }
    }
}
