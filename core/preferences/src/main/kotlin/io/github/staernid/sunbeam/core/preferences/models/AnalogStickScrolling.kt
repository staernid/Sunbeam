package io.github.staernid.sunbeam.core.preferences.models

/**
 * Gamepad analog stick designated for mouse scroll wheel emulation.
 */
enum class AnalogStickScrolling(val prefValue: String) {
    NONE("none"),
    RIGHT("right"),
    LEFT("left"),
    ;

    companion object {
        fun fromPrefValue(value: String?): AnalogStickScrolling {
            return entries.firstOrNull { it.prefValue.equals(value, ignoreCase = true) } ?: RIGHT
        }
    }
}
