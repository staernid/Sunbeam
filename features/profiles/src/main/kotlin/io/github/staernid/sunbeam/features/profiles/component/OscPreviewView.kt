@file:Suppress("MagicNumber", "LongMethod", "LongParameterList")

package io.github.staernid.sunbeam.features.profiles.component

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import kotlin.math.roundToInt

/**
 * Shared preference file name used by `VirtualControllerConfigurationLoader` in `:app`.
 */
const val OSC_PREFERENCE_NAME = "OSC"
const val KEY_OSC_SCALE = "osc_scale"
const val KEY_LEFT_OFFSET_X = "osc_left_offset_x"
const val KEY_LEFT_OFFSET_Y = "osc_left_offset_y"
const val KEY_RIGHT_OFFSET_X = "osc_right_offset_x"
const val KEY_RIGHT_OFFSET_Y = "osc_right_offset_y"

private fun clearIndividualElementOverrides(editor: SharedPreferences.Editor, prefs: SharedPreferences) {
    prefs.all.keys
        .filter { it.toIntOrNull() != null }
        .forEach { editor.remove(it) }
}

/**
 * Interactive On-Screen Controls (OSC) studio and layout configurator.
 * Persists scale and cluster offsets to [OSC_PREFERENCE_NAME] so changes apply to the in-stream
 * virtual controller, and exposes core OSC behavior toggles in the same place.
 */
@Composable
fun OscPreviewView(
    opacity: Float,
    scale: Float,
    onOpacityChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    onResetLayout: () -> Unit,
    modifier: Modifier = Modifier,
    oscEnabled: Boolean = false,
    hideOscWhenHasGamepad: Boolean = true,
    onlyShowL3R3: Boolean = false,
    showGuideButton: Boolean = true,
    vibrateOsc: Boolean = true,
    onOscEnabledChange: (Boolean) -> Unit = {},
    onHideOscWhenHasGamepadChange: (Boolean) -> Unit = {},
    onOnlyShowL3R3Change: (Boolean) -> Unit = {},
    onShowGuideButtonChange: (Boolean) -> Unit = {},
    onVibrateOscChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val oscPrefs = remember(context) {
        context.getSharedPreferences(OSC_PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    var leftClusterNormalized by remember {
        mutableStateOf(
            Offset(
                oscPrefs.getFloat(KEY_LEFT_OFFSET_X, 0f),
                oscPrefs.getFloat(KEY_LEFT_OFFSET_Y, 0f),
            ),
        )
    }
    var rightClusterNormalized by remember {
        mutableStateOf(
            Offset(
                oscPrefs.getFloat(KEY_RIGHT_OFFSET_X, 0f),
                oscPrefs.getFloat(KEY_RIGHT_OFFSET_Y, 0f),
            ),
        )
    }
    var pressedPreviewButton by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(oscPrefs) {
        if (oscPrefs.contains(KEY_OSC_SCALE)) {
            val savedScale = oscPrefs.getFloat(KEY_OSC_SCALE, 1.0f).coerceIn(0.5f, 1.5f)
            if (savedScale != scale) {
                onScaleChange(savedScale)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Virtual Controller Layout Preview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Drag the left or right control clusters to reposition them, or adjust button scale and opacity. " +
                "During a stream, open Stream Controls → Edit On-Screen Layout to fine-tune individual buttons.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Simulated 16:9 Stream Viewport
        OscSimulatedScreen(
            opacity = opacity,
            scale = scale,
            onlyShowL3R3 = onlyShowL3R3,
            showGuideButton = showGuideButton,
            leftClusterNormalized = leftClusterNormalized,
            rightClusterNormalized = rightClusterNormalized,
            pressedButton = pressedPreviewButton,
            onLeftClusterNormalizedChange = { updated ->
                leftClusterNormalized = updated
                val editor = oscPrefs.edit()
                clearIndividualElementOverrides(editor, oscPrefs)
                editor
                    .putFloat(KEY_LEFT_OFFSET_X, updated.x)
                    .putFloat(KEY_LEFT_OFFSET_Y, updated.y)
                    .apply()
            },
            onRightClusterNormalizedChange = { updated ->
                rightClusterNormalized = updated
                val editor = oscPrefs.edit()
                clearIndividualElementOverrides(editor, oscPrefs)
                editor
                    .putFloat(KEY_RIGHT_OFFSET_X, updated.x)
                    .putFloat(KEY_RIGHT_OFFSET_Y, updated.y)
                    .apply()
            },
            onButtonPress = { pressedPreviewButton = it },
        )

        // Sliders & Reset Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Opacity",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "${(opacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.sunbeamFocusable(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Scale",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "${(scale * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = scale,
                    onValueChange = { newScale ->
                        val clamped = newScale.coerceIn(0.5f, 1.5f)
                        onScaleChange(clamped)
                        val editor = oscPrefs.edit()
                        clearIndividualElementOverrides(editor, oscPrefs)
                        editor.putFloat(KEY_OSC_SCALE, clamped).apply()
                    },
                    valueRange = 0.5f..1.5f,
                    modifier = Modifier.sunbeamFocusable(),
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = {
                        leftClusterNormalized = Offset.Zero
                        rightClusterNormalized = Offset.Zero
                        oscPrefs.edit().clear().commit()
                        onResetLayout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .sunbeamFocusable(),
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Layout to Default")
                }
            }
        }

        // On-Screen Controls Behavior Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "On-Screen Controls Behavior",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                OscToggleRow(
                    title = "Show On-Screen Controls",
                    subtitle = "Display virtual gamepad overlay when streaming",
                    checked = oscEnabled,
                    onCheckedChange = onOscEnabledChange,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                OscToggleRow(
                    title = "Hide When Gamepad Connected",
                    subtitle = "Automatically hide overlay when a physical controller is attached",
                    checked = hideOscWhenHasGamepad,
                    onCheckedChange = onHideOscWhenHasGamepadChange,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                OscToggleRow(
                    title = "Show Guide (Home) Button",
                    subtitle = "Include central Xbox/Home button on the overlay",
                    checked = showGuideButton,
                    onCheckedChange = onShowGuideButtonChange,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                OscToggleRow(
                    title = "Only Show L3 & R3",
                    subtitle = "Hide full virtual controller except for stick-click buttons",
                    checked = onlyShowL3R3,
                    onCheckedChange = onOnlyShowL3R3Change,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                OscToggleRow(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate device when pressing virtual buttons",
                    checked = vibrateOsc,
                    onCheckedChange = onVibrateOscChange,
                )
            }
        }
    }
}

@Composable
private fun OscToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.sunbeamFocusable(),
        )
    }
}

@Composable
private fun OscSimulatedScreen(
    opacity: Float,
    scale: Float,
    onlyShowL3R3: Boolean,
    showGuideButton: Boolean,
    leftClusterNormalized: Offset,
    rightClusterNormalized: Offset,
    pressedButton: String?,
    onLeftClusterNormalizedChange: (Offset) -> Unit,
    onRightClusterNormalizedChange: (Offset) -> Unit,
    onButtonPress: (String?) -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E1117))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        val density = LocalDensity.current
        val viewportW = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val viewportH = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val leftPixelOffset = Offset(
            x = leftClusterNormalized.x * viewportW,
            y = leftClusterNormalized.y * viewportH,
        )
        val rightPixelOffset = Offset(
            x = rightClusterNormalized.x * viewportW,
            y = rightClusterNormalized.y * viewportH,
        )

        // Subtle alignment grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 8
            val rows = 4
            val colStep = size.width / cols
            val rowStep = size.height / rows
            for (c in 1 until cols) {
                drawLine(
                    color = gridColor,
                    start = Offset(c * colStep, 0f),
                    end = Offset(c * colStep, size.height),
                    strokeWidth = 1f,
                )
            }
            for (r in 1 until rows) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, r * rowStep),
                    end = Offset(size.width, r * rowStep),
                    strokeWidth = 1f,
                )
            }
        }

        // Center HUD indicator
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "16:9 STREAM VIEWPORT",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.35f),
                fontFamily = FontFamily.Monospace,
            )
            if (pressedButton != null) {
                Text(
                    text = "Input: $pressedButton",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF60A5FA),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // Left Control Cluster (Triggers, D-Pad, Left Stick, L3)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
                .offset {
                    IntOffset(
                        leftPixelOffset.x.roundToInt(),
                        leftPixelOffset.y.roundToInt(),
                    )
                }
                .pointerInput(viewportW, viewportH) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val nx = (leftClusterNormalized.x + dragAmount.x / viewportW).coerceIn(-0.15f, 0.35f)
                        val ny = (leftClusterNormalized.y + dragAmount.y / viewportH).coerceIn(-0.35f, 0.15f)
                        onLeftClusterNormalizedChange(Offset(nx, ny))
                    }
                }
                .alpha(opacity),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((6 * scale).dp),
            ) {
                if (!onlyShowL3R3) {
                    Row(horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
                        PreviewPillButton("LT", scale, pressedButton == "LT", onButtonPress)
                        PreviewPillButton("LB", scale, pressedButton == "LB", onButtonPress)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((10 * scale).dp),
                    ) {
                        PreviewDpad(scale = scale)
                        PreviewThumbstick(label = "LS", scale = scale)
                    }
                }
                PreviewPillButton("L3", scale, pressedButton == "L3", onButtonPress)
            }
        }

        // Center Menu Buttons (Back / Guide / Start)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .alpha(opacity),
            horizontalArrangement = Arrangement.spacedBy((10 * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!onlyShowL3R3) {
                PreviewPillButton("SELECT", scale * 0.85f, pressedButton == "SELECT", onButtonPress)
            }
            if (showGuideButton) {
                PreviewCircleButton("HOME", scale * 0.9f, pressedButton == "HOME", onButtonPress)
            }
            if (!onlyShowL3R3) {
                PreviewPillButton("START", scale * 0.85f, pressedButton == "START", onButtonPress)
            }
        }

        // Right Control Cluster (Triggers, ABXY, Right Stick, R3)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .offset {
                    IntOffset(
                        rightPixelOffset.x.roundToInt(),
                        rightPixelOffset.y.roundToInt(),
                    )
                }
                .pointerInput(viewportW, viewportH) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val nx = (rightClusterNormalized.x + dragAmount.x / viewportW).coerceIn(-0.35f, 0.15f)
                        val ny = (rightClusterNormalized.y + dragAmount.y / viewportH).coerceIn(-0.35f, 0.15f)
                        onRightClusterNormalizedChange(Offset(nx, ny))
                    }
                }
                .alpha(opacity),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((6 * scale).dp),
            ) {
                if (!onlyShowL3R3) {
                    Row(horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
                        PreviewPillButton("RB", scale, pressedButton == "RB", onButtonPress)
                        PreviewPillButton("RT", scale, pressedButton == "RT", onButtonPress)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((10 * scale).dp),
                    ) {
                        PreviewThumbstick(label = "RS", scale = scale)
                        PreviewAbxyDiamond(scale, pressedButton, onButtonPress)
                    }
                }
                PreviewPillButton("R3", scale, pressedButton == "R3", onButtonPress)
            }
        }
    }
}

@Composable
private fun PreviewAbxyDiamond(
    scale: Float,
    pressedButton: String?,
    onButtonPress: (String?) -> Unit,
) {
    val sizeDp = (68 * scale).dp
    Box(modifier = Modifier.size(sizeDp)) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            PreviewCircleButton("Y", scale, pressedButton == "Y", onButtonPress, Color(0xFFFDE68A))
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            PreviewCircleButton("A", scale, pressedButton == "A", onButtonPress, Color(0xFF6EE7B7))
        }
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            PreviewCircleButton("X", scale, pressedButton == "X", onButtonPress, Color(0xFF93C5FD))
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            PreviewCircleButton("B", scale, pressedButton == "B", onButtonPress, Color(0xFFFCA5A5))
        }
    }
}

@Composable
private fun PreviewCircleButton(
    label: String,
    scale: Float,
    isPressed: Boolean,
    onButtonPress: (String?) -> Unit,
    ringColor: Color = Color(0xFFD1D9E6),
) {
    val sizeDp = (22 * scale).dp
    val bgColor = if (isPressed) Color(0xFF2563EB) else Color(0xCC1A2230)
    val borderColor = if (isPressed) Color(0xFF93C5FD) else ringColor.copy(alpha = 0.85f)

    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.2.dp, borderColor, CircleShape)
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        onButtonPress(label)
                        tryAwaitRelease()
                        onButtonPress(null)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = (8 * scale).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun PreviewPillButton(
    label: String,
    scale: Float,
    isPressed: Boolean,
    onButtonPress: (String?) -> Unit,
) {
    val bgColor = if (isPressed) Color(0xFF2563EB) else Color(0xCC1A2230)
    val borderColor = if (isPressed) Color(0xFF93C5FD) else Color(0xFFD1D9E6).copy(alpha = 0.8f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .pointerInput(label) {
                detectTapGestures(
                    onPress = {
                        onButtonPress(label)
                        tryAwaitRelease()
                        onButtonPress(null)
                    },
                )
            }
            .padding(horizontal = (8 * scale).dp, vertical = (3 * scale).dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = (8 * scale).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun PreviewDpad(scale: Float) {
    val sizeDp = (52 * scale).dp
    val armSize = (16 * scale).dp
    Box(modifier = Modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        // Vertical bar
        Box(
            modifier = Modifier
                .width(armSize)
                .height(sizeDp)
                .clip(RoundedCornerShape((4 * scale).dp))
                .background(Color(0xCC1A2230))
                .border(1.dp, Color(0xFFD1D9E6).copy(alpha = 0.75f), RoundedCornerShape((4 * scale).dp)),
        )
        // Horizontal bar
        Box(
            modifier = Modifier
                .width(sizeDp)
                .height(armSize)
                .clip(RoundedCornerShape((4 * scale).dp))
                .background(Color(0xCC1A2230))
                .border(1.dp, Color(0xFFD1D9E6).copy(alpha = 0.75f), RoundedCornerShape((4 * scale).dp)),
        )
    }
}

@Composable
private fun PreviewThumbstick(
    label: String,
    scale: Float,
) {
    val sizeDp = (46 * scale).dp
    Box(
        modifier = Modifier.size(sizeDp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0x99111722),
                radius = size.minDimension / 2f,
            )
            drawCircle(
                color = Color(0xFFD1D9E6).copy(alpha = 0.6f),
                radius = size.minDimension / 2f,
                style = Stroke(width = 2f),
            )
            drawCircle(
                color = Color(0xDD1A2230),
                radius = size.minDimension / 3.4f,
            )
            drawCircle(
                color = Color(0xFFD1D9E6).copy(alpha = 0.85f),
                radius = size.minDimension / 3.4f,
                style = Stroke(width = 1.8f),
            )
        }
        Text(
            text = label,
            fontSize = (7 * scale).sp,
            color = Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold,
        )
    }
}
