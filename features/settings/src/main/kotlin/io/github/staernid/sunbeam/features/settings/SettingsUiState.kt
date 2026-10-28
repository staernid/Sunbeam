@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.features.settings

import io.github.staernid.sunbeam.core.preferences.models.AnalogStickScrolling
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.Resolution
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode

enum class StreamQualityPreset(val label: String) {
    LOW_LATENCY("Low Latency (1080p 120FPS)"),
    BALANCED("Balanced (1080p 60FPS 20Mbps)"),
    ULTRA_QUALITY("Ultra Quality (1440p/4K HDR 60Mbps)"),
    CUSTOM("Custom"),
    ;

    fun applyTo(settings: StreamSettings): StreamSettings =
        when (this) {
            LOW_LATENCY -> settings.copy(
                width = 1920,
                height = 1080,
                fps = 120,
                bitrateKbps = 25000,
                framePacing = FramePacingMode.MIN_LATENCY,
                enableHdr = false,
            )

            BALANCED -> settings.copy(
                width = 1920,
                height = 1080,
                fps = 60,
                bitrateKbps = 20000,
                framePacing = FramePacingMode.BALANCED,
                enableHdr = false,
            )

            ULTRA_QUALITY -> settings.copy(
                width = 3840,
                height = 2160,
                fps = 60,
                bitrateKbps = 60000,
                framePacing = FramePacingMode.MAX_SMOOTHNESS,
                enableHdr = true,
            )

            CUSTOM -> settings
        }

    companion object {
        fun fromSettings(settings: StreamSettings): StreamQualityPreset =
            entries.firstOrNull { preset ->
                preset != CUSTOM && preset.applyTo(settings) == settings
            } ?: CUSTOM
    }
}

val VideoScaleMode.displayLabel: String
    get() = when (this) {
        VideoScaleMode.FIT -> "Fit (Maintain Aspect)"
        VideoScaleMode.FILL -> "Fill (Crop to Screen)"
        VideoScaleMode.STRETCH -> "Stretch"
    }

val VideoFormatOption.displayLabel: String
    get() = when (this) {
        VideoFormatOption.AUTO -> "Automatic"
        VideoFormatOption.FORCE_H264 -> "H.264"
        VideoFormatOption.FORCE_HEVC -> "HEVC (H.265)"
        VideoFormatOption.FORCE_AV1 -> "AV1"
    }

val FramePacingMode.displayLabel: String
    get() = when (this) {
        FramePacingMode.MIN_LATENCY -> "Minimum Latency"
        FramePacingMode.BALANCED -> "Balanced"
        FramePacingMode.CAP_FPS -> "Cap FPS"
        FramePacingMode.MAX_SMOOTHNESS -> "Maximum Smoothness"
    }

val AudioConfiguration.displayLabel: String
    get() = when (this) {
        AudioConfiguration.STEREO -> "Stereo (2.0)"
        AudioConfiguration.SURROUND_51 -> "5.1 Surround"
        AudioConfiguration.SURROUND_71 -> "7.1 Surround"
    }

val AnalogStickScrolling.displayLabel: String
    get() = when (this) {
        AnalogStickScrolling.NONE -> "Disabled"
        AnalogStickScrolling.RIGHT -> "Right Stick"
        AnalogStickScrolling.LEFT -> "Left Stick"
    }

sealed interface SettingsDialogState {
    data object None : SettingsDialogState

    data object ResolutionPicker : SettingsDialogState

    data object FpsPicker : SettingsDialogState

    data object VideoScalePicker : SettingsDialogState

    data object VideoFormatPicker : SettingsDialogState

    data object AudioConfigPicker : SettingsDialogState

    data object FramePacingPicker : SettingsDialogState

    data object AnalogStickScrollingPicker : SettingsDialogState

    data object ResetConfirmation : SettingsDialogState
}

data class SettingsUiState(
    val settings: StreamSettings = StreamSettings(),
    val isLoading: Boolean = false,
    val dialogState: SettingsDialogState = SettingsDialogState.None,
    val userMessage: String? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
) {
    val activePreset: StreamQualityPreset
        get() = StreamQualityPreset.fromSettings(settings)

    val resolutionLabel: String
        get() {
            val res = Resolution(settings.width, settings.height)
            val name = res.standardDisplayName
            return if (name != null) {
                "$name (${res.width}x${res.height})"
            } else if (res.aspectRatioLabel.isNotEmpty()) {
                "${res.width}x${res.height} (${res.aspectRatioLabel})"
            } else {
                "${res.width}x${res.height}"
            }
        }

    val fpsLabel: String
        get() = "${settings.fps} FPS"

    val bitrateMbps: Int
        get() = (settings.bitrateKbps / 1000).coerceAtLeast(1)
}
