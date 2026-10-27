@file:Suppress("MagicNumber", "LongMethod")

package io.github.staernid.sunbeam.features.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.features.settings.SettingsDialogState

private val BITRATE_RANGE = 1f..100f
private val OPACITY_RANGE = 10f..100f
private val DEADZONE_RANGE = 0f..50f

@Composable
fun VideoSettingsSection(
    settings: StreamSettings,
    bitrateMbps: Int,
    onOpenDialog: (SettingsDialogState) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingCategoryHeader(title = "Video & Display")

        SettingValueItem(
            title = "Resolution",
            value = "${settings.width}x${settings.height}",
            icon = Icons.Default.Tv,
            description = "Stream video resolution",
            onClick = { onOpenDialog(SettingsDialogState.ResolutionPicker) },
        )

        SettingValueItem(
            title = "Frame Rate",
            value = "${settings.fps} FPS",
            icon = Icons.Default.Speed,
            description = "Target frames per second",
            onClick = { onOpenDialog(SettingsDialogState.FpsPicker) },
        )

        SettingSliderItem(
            title = "Video Bitrate",
            value = bitrateMbps,
            valueSuffix = " Mbps",
            valueRange = BITRATE_RANGE,
            description = "Higher bitrates improve image clarity but require more bandwidth",
            onValueChange = onBitrateChange,
        )

        SettingValueItem(
            title = "Video Scale Mode",
            value = settings.videoScaleMode.name,
            icon = Icons.Default.AspectRatio,
            onClick = { onOpenDialog(SettingsDialogState.VideoScalePicker) },
        )

        SettingValueItem(
            title = "Video Codec",
            value = settings.videoFormat.name,
            icon = Icons.Default.Videocam,
            onClick = { onOpenDialog(SettingsDialogState.VideoFormatPicker) },
        )

        SettingValueItem(
            title = "Frame Pacing",
            value = settings.framePacing.name,
            onClick = { onOpenDialog(SettingsDialogState.FramePacingPicker) },
        )

        SettingSwitchItem(
            title = "Enable HDR",
            description = "10-bit high dynamic range color (requires Sunshine HDR host support)",
            checked = settings.enableHdr,
            onCheckedChange = { checked -> onUpdate { it.copy(enableHdr = checked) } },
        )

        SettingSwitchItem(
            title = "Full Range Color",
            description = "0-255 RGB dynamic range instead of 16-235 TV levels",
            checked = settings.fullRange,
            onCheckedChange = { checked -> onUpdate { it.copy(fullRange = checked) } },
        )

        SettingSwitchItem(
            title = "Video Super Resolution (SGSR)",
            description = "Edge-preserving spatial upscaling for high-DPI displays",
            checked = settings.videoSuperResolution,
            onCheckedChange = { checked -> onUpdate { it.copy(videoSuperResolution = checked) } },
        )

        SettingSwitchItem(
            title = "Ultra Low Latency Decoder",
            description = "Prioritize minimal frame delay over smoother pacing",
            checked = settings.enableUltraLowLatency,
            onCheckedChange = { checked -> onUpdate { it.copy(enableUltraLowLatency = checked) } },
        )
    }
}

@Composable
fun AudioSettingsSection(
    settings: StreamSettings,
    onOpenDialog: (SettingsDialogState) -> Unit,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingCategoryHeader(title = "Audio")

        SettingValueItem(
            title = "Audio Configuration",
            value = settings.audioConfig.name,
            description = "Surround sound channels",
            onClick = { onOpenDialog(SettingsDialogState.AudioConfigPicker) },
        )

        SettingSwitchItem(
            title = "Play Audio on Host PC",
            description = "Keep speakers playing on the computer while streaming",
            checked = settings.hostAudio,
            onCheckedChange = { checked -> onUpdate { it.copy(hostAudio = checked) } },
        )
    }
}

@Composable
fun InputSettingsSection(
    settings: StreamSettings,
    onDeadzoneChange: (Int) -> Unit,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingCategoryHeader(title = "Gamepad & Input")

        SettingSwitchItem(
            title = "Multi-Controller Support",
            description = "Allow up to 4 local gamepads to be forwarded as distinct players",
            checked = settings.multiController,
            onCheckedChange = { checked -> onUpdate { it.copy(multiController = checked) } },
        )

        SettingSwitchItem(
            title = "Gamepad Rumble",
            description = "Forward host vibration feedback to controller motors",
            checked = settings.enableRumble,
            onCheckedChange = { checked -> onUpdate { it.copy(enableRumble = checked) } },
        )

        SettingSliderItem(
            title = "Stick Deadzone",
            value = settings.deadzonePercentage,
            valueSuffix = "%",
            valueRange = DEADZONE_RANGE,
            description = "Inner stick threshold to prevent drift",
            onValueChange = onDeadzoneChange,
        )

        SettingSwitchItem(
            title = "Flip Face Buttons",
            description = "Swap A/B and X/Y button mappings (Nintendo style)",
            checked = settings.flipFaceButtons,
            onCheckedChange = { checked -> onUpdate { it.copy(flipFaceButtons = checked) } },
        )

        SettingSwitchItem(
            title = "Motion Sensors (Gyro)",
            description = "Send device gyroscope / accelerometer for motion aiming",
            checked = settings.gamepadMotionSensors,
            onCheckedChange = { checked -> onUpdate { it.copy(gamepadMotionSensors = checked) } },
        )
    }
}

@Composable
fun OscSettingsSection(
    settings: StreamSettings,
    onOpacityChange: (Int) -> Unit,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingCategoryHeader(title = "On-Screen Controls")

        SettingSwitchItem(
            title = "Show On-Screen Controls",
            description = "Touchscreen virtual gamepad overlay",
            checked = settings.onscreenController,
            onCheckedChange = { checked -> onUpdate { it.copy(onscreenController = checked) } },
        )

        SettingSwitchItem(
            title = "Auto-Hide When Gamepad Connected",
            description = "Hide touch controls when a physical controller is detected",
            checked = settings.hideOscWhenHasGamepad,
            onCheckedChange = { checked -> onUpdate { it.copy(hideOscWhenHasGamepad = checked) } },
        )

        SettingSliderItem(
            title = "Control Opacity",
            value = settings.oscOpacity,
            valueSuffix = "%",
            valueRange = OPACITY_RANGE,
            onValueChange = onOpacityChange,
        )

        SettingSwitchItem(
            title = "Haptic Vibration",
            description = "Vibrate on touch control presses",
            checked = settings.vibrateOsc,
            onCheckedChange = { checked -> onUpdate { it.copy(vibrateOsc = checked) } },
        )
    }
}

@Composable
fun PerfSettingsSection(
    settings: StreamSettings,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingCategoryHeader(title = "Performance Monitor")

        SettingSwitchItem(
            title = "Real-Time Performance Overlay",
            description = "Show stream resolution, FPS, decode latency, and packet loss",
            checked = settings.enablePerfOverlay,
            onCheckedChange = { checked -> onUpdate { it.copy(enablePerfOverlay = checked) } },
        )

        SettingSwitchItem(
            title = "Position Overlay at Bottom",
            description = "Move performance HUD to the bottom edge of the display",
            checked = settings.enablePerfOverlayBottom,
            onCheckedChange = { checked -> onUpdate { it.copy(enablePerfOverlayBottom = checked) } },
        )

        SettingSwitchItem(
            title = "Latency Toasts",
            description = "Display periodic stream latency notifications",
            checked = settings.enableLatencyToast,
            onCheckedChange = { checked -> onUpdate { it.copy(enableLatencyToast = checked) } },
        )
    }
}
