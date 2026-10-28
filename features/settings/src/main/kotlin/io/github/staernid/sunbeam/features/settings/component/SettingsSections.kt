@file:Suppress("MagicNumber", "LongMethod", "LongParameterList", "TooManyFunctions")

package io.github.staernid.sunbeam.features.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.features.settings.SettingsDialogState
import io.github.staernid.sunbeam.features.settings.StreamQualityPreset
import io.github.staernid.sunbeam.features.settings.displayLabel

private val BITRATE_RANGE = 1f..100f
private val OPACITY_RANGE = 10f..100f
private val DEADZONE_RANGE = 0f..50f
private val TRACKPAD_SENSITIVITY_RANGE = 10f..400f

internal fun matchesSearch(
    query: String,
    title: String,
    description: String? = null,
): Boolean {
    if (query.isBlank()) return true
    val trimmed = query.trim()
    return title.contains(trimmed, ignoreCase = true) ||
        description?.contains(trimmed, ignoreCase = true) == true
}

internal class FilteredSettingsBuilder(
    private val searchQuery: String,
) {
    private val matchingItems = mutableListOf<@Composable () -> Unit>()

    val hasMatches: Boolean
        get() = matchingItems.isNotEmpty()

    fun switchItem(
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        description: String? = null,
        icon: ImageVector? = null,
    ) {
        if (matchesSearch(searchQuery, title, description)) {
            matchingItems.add {
                SettingSwitchItem(
                    title = title,
                    description = description,
                    icon = icon,
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }
        }
    }

    fun valueItem(
        title: String,
        value: String,
        onClick: () -> Unit,
        description: String? = null,
        icon: ImageVector? = null,
    ) {
        if (matchesSearch(searchQuery, title, description)) {
            matchingItems.add {
                SettingValueItem(
                    title = title,
                    value = value,
                    description = description,
                    icon = icon,
                    onClick = onClick,
                )
            }
        }
    }

    fun sliderItem(
        title: String,
        value: Int,
        valueSuffix: String,
        valueRange: ClosedFloatingPointRange<Float>,
        onValueChange: (Int) -> Unit,
        description: String? = null,
    ) {
        if (matchesSearch(searchQuery, title, description)) {
            matchingItems.add {
                SettingSliderItem(
                    title = title,
                    value = value,
                    valueSuffix = valueSuffix,
                    valueRange = valueRange,
                    description = description,
                    onValueChange = onValueChange,
                )
            }
        }
    }

    @Composable
    fun RenderItems() {
        matchingItems.forEach { it() }
    }
}

@Composable
private fun FilteredSettingGroupCard(
    title: String,
    searchQuery: String,
    modifier: Modifier = Modifier,
    buildItems: FilteredSettingsBuilder.() -> Unit,
) {
    val builder = FilteredSettingsBuilder(searchQuery).apply(buildItems)
    if (!builder.hasMatches) return
    SettingGroupCard(
        title = title,
        modifier = modifier,
    ) {
        builder.RenderItems()
    }
}

@Composable
private fun CollapsibleSettingGroupCard(
    title: String,
    searchQuery: String,
    modifier: Modifier = Modifier,
    buildItems: FilteredSettingsBuilder.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val builder = FilteredSettingsBuilder(searchQuery).apply(buildItems)
    if (!builder.hasMatches) return
    val isExpanded = expanded || searchQuery.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sunbeamFocusable()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse $title" else "Expand $title",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                if (isExpanded) {
                    builder.RenderItems()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPresetSection(
    activePreset: StreamQualityPreset,
    onPresetSelected: (StreamQualityPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingGroupCard(
        title = "Quick Quality Preset",
        modifier = modifier,
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StreamQualityPreset.entries.forEach { preset ->
                FilterChip(
                    selected = activePreset == preset,
                    onClick = { onPresetSelected(preset) },
                    label = { Text(preset.label) },
                    modifier = Modifier.sunbeamFocusable(),
                )
            }
        }
    }
}

@Composable
fun AppearanceSettingsSection(
    settings: StreamSettings,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
) {
    FilteredSettingGroupCard(
        title = "Appearance",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        switchItem(
            title = "OLED Black Mode",
            description = "Pure black surfaces in dark theme for AMOLED displays",
            icon = Icons.Default.DarkMode,
            checked = settings.oledBlackMode,
            onCheckedChange = { checked -> onUpdate { it.copy(oledBlackMode = checked) } },
        )

        switchItem(
            title = "Dynamic Color (Material You)",
            description = "Adapt theme palette to your system wallpaper (Android 12+)",
            icon = Icons.Default.Palette,
            checked = settings.dynamicColor,
            onCheckedChange = { checked -> onUpdate { it.copy(dynamicColor = checked) } },
        )
    }
}

@Composable
fun VideoSettingsSection(
    settings: StreamSettings,
    bitrateMbps: Int,
    onOpenDialog: (SettingsDialogState) -> Unit,
    onBitrateChange: (Int) -> Unit,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
) {
    FilteredSettingGroupCard(
        title = "Video & Display",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        valueItem(
            title = "Resolution",
            value = "${settings.width}x${settings.height}",
            icon = Icons.Default.Tv,
            description = "Stream video resolution",
            onClick = { onOpenDialog(SettingsDialogState.ResolutionPicker) },
        )

        valueItem(
            title = "Frame Rate",
            value = "${settings.fps} FPS",
            icon = Icons.Default.Speed,
            description = "Target frames per second",
            onClick = { onOpenDialog(SettingsDialogState.FpsPicker) },
        )

        sliderItem(
            title = "Video Bitrate",
            value = bitrateMbps,
            valueSuffix = " Mbps",
            valueRange = BITRATE_RANGE,
            description = "Higher bitrates improve image clarity but require more bandwidth",
            onValueChange = onBitrateChange,
        )

        valueItem(
            title = "Video Scale Mode",
            value = settings.videoScaleMode.displayLabel,
            icon = Icons.Default.AspectRatio,
            onClick = { onOpenDialog(SettingsDialogState.VideoScalePicker) },
        )

        valueItem(
            title = "Video Codec",
            value = settings.videoFormat.displayLabel,
            icon = Icons.Default.Videocam,
            onClick = { onOpenDialog(SettingsDialogState.VideoFormatPicker) },
        )

        valueItem(
            title = "Frame Pacing",
            value = settings.framePacing.displayLabel,
            onClick = { onOpenDialog(SettingsDialogState.FramePacingPicker) },
        )

        switchItem(
            title = "Enable HDR",
            description = "10-bit high dynamic range color (requires Sunshine HDR host support)",
            checked = settings.enableHdr,
            onCheckedChange = { checked -> onUpdate { it.copy(enableHdr = checked) } },
        )

        switchItem(
            title = "Full Range Color",
            description = "0-255 RGB dynamic range instead of 16-235 TV levels",
            checked = settings.fullRange,
            onCheckedChange = { checked -> onUpdate { it.copy(fullRange = checked) } },
        )

        switchItem(
            title = "Video Super Resolution (SGSR)",
            description = "Edge-preserving spatial upscaling for high-DPI displays",
            checked = settings.videoSuperResolution,
            onCheckedChange = { checked -> onUpdate { it.copy(videoSuperResolution = checked) } },
        )

        switchItem(
            title = "Ultra Low Latency Decoder",
            description = "Prioritize minimal frame delay over smoother pacing",
            checked = settings.enableUltraLowLatency,
            onCheckedChange = { checked -> onUpdate { it.copy(enableUltraLowLatency = checked) } },
        )
    }
}

@Composable
fun AdvancedDisplaySettingsSection(
    settings: StreamSettings,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
) {
    CollapsibleSettingGroupCard(
        title = "Advanced Display & Window Behavior",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        switchItem(
            title = "Picture-in-Picture on Minimize",
            description = "Keep stream visible in a floating window when leaving the app",
            checked = settings.enablePip,
            onCheckedChange = { checked -> onUpdate { it.copy(enablePip = checked) } },
        )

        switchItem(
            title = "Unlock 90/120+ FPS Rates",
            description = "Allow high refresh rate streaming even if display reports 60 Hz",
            checked = settings.unlockFps,
            onCheckedChange = { checked -> onUpdate { it.copy(unlockFps = checked) } },
        )

        switchItem(
            title = "Reduce Refresh Rate",
            description = "Prefer lower display refresh rate to save battery",
            checked = settings.reduceRefreshRate,
            onCheckedChange = { checked -> onUpdate { it.copy(reduceRefreshRate = checked) } },
        )

        switchItem(
            title = "Enforce Display Mode",
            description = "Lock display refresh rate to match target stream frame rate",
            checked = settings.enforceDisplayMode,
            onCheckedChange = { checked -> onUpdate { it.copy(enforceDisplayMode = checked) } },
        )

        switchItem(
            title = "Resume Stream Without Confirmation",
            description = "Immediately reconnect to active host sessions without prompting",
            checked = settings.resumeWithoutConfirm,
            onCheckedChange = { checked -> onUpdate { it.copy(resumeWithoutConfirm = checked) } },
        )

        switchItem(
            title = "Disable Connection Warnings",
            description = "Suppress on-screen network and decoder warning messages",
            checked = settings.disableWarnings,
            onCheckedChange = { checked -> onUpdate { it.copy(disableWarnings = checked) } },
        )
    }
}

@Composable
fun AudioSettingsSection(
    settings: StreamSettings,
    onOpenDialog: (SettingsDialogState) -> Unit,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
) {
    FilteredSettingGroupCard(
        title = "Audio",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        valueItem(
            title = "Audio Configuration",
            value = settings.audioConfig.displayLabel,
            description = "Surround sound channels",
            onClick = { onOpenDialog(SettingsDialogState.AudioConfigPicker) },
        )

        switchItem(
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
    searchQuery: String = "",
) {
    FilteredSettingGroupCard(
        title = "Gamepad & Input",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        switchItem(
            title = "Multi-Controller Support",
            description = "Allow up to 4 local gamepads to be forwarded as distinct players",
            checked = settings.multiController,
            onCheckedChange = { checked -> onUpdate { it.copy(multiController = checked) } },
        )

        switchItem(
            title = "Gamepad Rumble",
            description = "Forward host vibration feedback to controller motors",
            checked = settings.enableRumble,
            onCheckedChange = { checked -> onUpdate { it.copy(enableRumble = checked) } },
        )

        sliderItem(
            title = "Stick Deadzone",
            value = settings.deadzonePercentage,
            valueSuffix = "%",
            valueRange = DEADZONE_RANGE,
            description = "Inner stick threshold to prevent drift",
            onValueChange = onDeadzoneChange,
        )

        switchItem(
            title = "Flip Face Buttons",
            description = "Swap A/B and X/Y button mappings (Nintendo style)",
            checked = settings.flipFaceButtons,
            onCheckedChange = { checked -> onUpdate { it.copy(flipFaceButtons = checked) } },
        )

        switchItem(
            title = "Motion Sensors (Gyro)",
            description = "Send device gyroscope / accelerometer for motion aiming",
            checked = settings.gamepadMotionSensors,
            onCheckedChange = { checked -> onUpdate { it.copy(gamepadMotionSensors = checked) } },
        )
    }
}

@Composable
fun AdvancedControllerSettingsSection(
    settings: StreamSettings,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
) {
    CollapsibleSettingGroupCard(
        title = "Advanced Controller",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        switchItem(
            title = "Custom USB Gamepad Driver",
            description = "Use built-in USB driver for controllers unsupported by Android",
            checked = settings.usbDriver,
            onCheckedChange = { checked -> onUpdate { it.copy(usbDriver = checked) } },
        )

        switchItem(
            title = "Bind All USB Devices",
            description = "Override default Android drivers for all connected USB gamepads",
            checked = settings.bindAllUsb,
            onCheckedChange = { checked -> onUpdate { it.copy(bindAllUsb = checked) } },
        )

        switchItem(
            title = "Gamepad Touchpad as Mouse",
            description = "Use DualShock / DualSense controller touchpad to control the mouse cursor",
            checked = settings.gamepadTouchpadAsMouse,
            onCheckedChange = { checked -> onUpdate { it.copy(gamepadTouchpadAsMouse = checked) } },
        )

        switchItem(
            title = "Device Motion Sensor Fallback",
            description = "Use phone/tablet gyroscope when controller lacks motion sensors",
            checked = settings.gamepadMotionFallback,
            onCheckedChange = { checked -> onUpdate { it.copy(gamepadMotionFallback = checked) } },
        )

        switchItem(
            title = "Use Device Vibration if Controller Lacks Rumble",
            description = "Emulate controller rumble using the Android device vibrator",
            checked = settings.vibrateFallback,
            onCheckedChange = { checked -> onUpdate { it.copy(vibrateFallback = checked) } },
        )
    }
}

@Composable
fun TouchMouseSettingsSection(
    settings: StreamSettings,
    onOpenDialog: (SettingsDialogState) -> Unit,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
) {
    CollapsibleSettingGroupCard(
        title = "Touch, Mouse & Trackpad",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        switchItem(
            title = "Touchscreen as Direct Touch vs Trackpad",
            description = "Use direct absolute touch coordinates instead of relative trackpad mode",
            checked = settings.absoluteMouseMode,
            onCheckedChange = { checked -> onUpdate { it.copy(absoluteMouseMode = checked) } },
        )

        sliderItem(
            title = "Trackpad Sensitivity X",
            value = settings.trackpadSensitivityX,
            valueSuffix = "%",
            valueRange = TRACKPAD_SENSITIVITY_RANGE,
            description = "Horizontal cursor speed in trackpad mode",
            onValueChange = { value -> onUpdate { it.copy(trackpadSensitivityX = value) } },
        )

        sliderItem(
            title = "Trackpad Sensitivity Y",
            value = settings.trackpadSensitivityY,
            valueSuffix = "%",
            valueRange = TRACKPAD_SENSITIVITY_RANGE,
            description = "Vertical cursor speed in trackpad mode",
            onValueChange = { value -> onUpdate { it.copy(trackpadSensitivityY = value) } },
        )

        switchItem(
            title = "Swap Trackpad X/Y Axes",
            description = "Invert horizontal and vertical trackpad movement",
            checked = settings.trackpadSwapAxis,
            onCheckedChange = { checked -> onUpdate { it.copy(trackpadSwapAxis = checked) } },
        )

        switchItem(
            title = "Gamepad Mouse Emulation",
            description = "Toggle mouse cursor control by long-pressing Start",
            checked = settings.mouseEmulation,
            onCheckedChange = { checked -> onUpdate { it.copy(mouseEmulation = checked) } },
        )

        switchItem(
            title = "Mouse Navigation Buttons",
            description = "Enable forward and back side mouse buttons",
            checked = settings.mouseNavButtons,
            onCheckedChange = { checked -> onUpdate { it.copy(mouseNavButtons = checked) } },
        )

        valueItem(
            title = "Analog Stick Scrolling",
            value = settings.analogStickScrolling.displayLabel,
            description = "Gamepad analog stick used for mouse scroll wheel",
            onClick = { onOpenDialog(SettingsDialogState.AnalogStickScrollingPicker) },
        )
    }
}

@Composable
fun KeyboardSettingsSection(
    settings: StreamSettings,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
) {
    CollapsibleSettingGroupCard(
        title = "Keyboard & Shortcuts",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        switchItem(
            title = "Force QWERTY Layout",
            description = "Translate non-QWERTY keyboard layouts to standard US key codes",
            checked = settings.forceQwerty,
            onCheckedChange = { checked -> onUpdate { it.copy(forceQwerty = checked) } },
        )

        switchItem(
            title = "Android Back button acts as Guide/Home",
            description = "Map Android system Back button to the Xbox Guide / Home button",
            checked = settings.backAsGuide,
            onCheckedChange = { checked -> onUpdate { it.copy(backAsGuide = checked) } },
        )

        switchItem(
            title = "Back Button as Windows Key",
            description = "Map Android Back button to the host Meta / Windows key",
            checked = settings.backAsMeta,
            onCheckedChange = { checked -> onUpdate { it.copy(backAsMeta = checked) } },
        )

        switchItem(
            title = "Right Alt as Windows Key",
            description = "Treat Right Alt key presses as the Meta / Windows key",
            checked = settings.rightAltAsMeta,
            onCheckedChange = { checked -> onUpdate { it.copy(rightAltAsMeta = checked) } },
        )
    }
}

@Composable
fun OscSettingsSection(
    settings: StreamSettings,
    onOpacityChange: (Int) -> Unit,
    onUpdate: ((StreamSettings) -> StreamSettings) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
) {
    FilteredSettingGroupCard(
        title = "On-Screen Controls",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        switchItem(
            title = "Show On-Screen Controls",
            description = "Touchscreen virtual gamepad overlay",
            checked = settings.onscreenController,
            onCheckedChange = { checked -> onUpdate { it.copy(onscreenController = checked) } },
        )

        switchItem(
            title = "Auto-Hide When Gamepad Connected",
            description = "Hide touch controls when a physical controller is detected",
            checked = settings.hideOscWhenHasGamepad,
            onCheckedChange = { checked -> onUpdate { it.copy(hideOscWhenHasGamepad = checked) } },
        )

        sliderItem(
            title = "Control Opacity",
            value = settings.oscOpacity,
            valueSuffix = "%",
            valueRange = OPACITY_RANGE,
            onValueChange = onOpacityChange,
        )

        switchItem(
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
    searchQuery: String = "",
) {
    FilteredSettingGroupCard(
        title = "Performance & Diagnostics",
        searchQuery = searchQuery,
        modifier = modifier,
    ) {
        switchItem(
            title = "Real-Time Performance Overlay",
            description = "Show stream resolution, FPS, decode latency, and packet loss",
            checked = settings.enablePerfOverlay,
            onCheckedChange = { checked -> onUpdate { it.copy(enablePerfOverlay = checked) } },
        )

        switchItem(
            title = "Position Overlay at Bottom",
            description = "Move performance HUD to the bottom edge of the display",
            checked = settings.enablePerfOverlayBottom,
            onCheckedChange = { checked -> onUpdate { it.copy(enablePerfOverlayBottom = checked) } },
        )

        switchItem(
            title = "Latency Toasts",
            description = "Display periodic stream latency notifications",
            checked = settings.enableLatencyToast,
            onCheckedChange = { checked -> onUpdate { it.copy(enableLatencyToast = checked) } },
        )
    }
}
