@file:Suppress(
    "LongMethod",
    "MagicNumber",
    "TooManyFunctions",
)

package io.github.staernid.sunbeam.features.profiles.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.staernid.sunbeam.features.profiles.model.GamepadButton
import io.github.staernid.sunbeam.features.profiles.model.GamepadDebugState

@Composable
fun GamepadTesterView(
    state: GamepadDebugState,
    onTestRumble: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConnectedControllersCard(state)
        AnalogSticksCard(state)
        TriggersAndShouldersCard(state)
        ButtonsCard(state)
        MotionSensorsCard(state)
        RumbleTestCard(state, onTestRumble)
    }
}

@Composable
private fun ConnectedControllersCard(state: GamepadDebugState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gamepad,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connected Controllers (${state.devices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (state.devices.isEmpty()) {
                Text(
                    text = "No physical controller detected. Connect a Bluetooth or USB gamepad to test live inputs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.devices.forEach { dev ->
                    Text(
                        text = "• ${dev.name} [VID:${dev.vendorId.toString(16)} PID:${dev.productId.toString(16)}] " +
                            if (dev.hasVibrator) "• Rumble OK" else "",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalogSticksCard(state: GamepadDebugState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Thumbsticks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StickVisualizer(
                    label = "Left Stick (LS)",
                    x = state.leftStickX,
                    y = state.leftStickY,
                    isClicked = state.pressedButtons.contains(GamepadButton.LSB),
                )
                StickVisualizer(
                    label = "Right Stick (RS)",
                    x = state.rightStickX,
                    y = state.rightStickY,
                    isClicked = state.pressedButtons.contains(GamepadButton.RSB),
                )
            }
        }
    }
}

@Composable
private fun StickVisualizer(
    label: String,
    x: Float,
    y: Float,
    isClicked: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(6.dp))
        val primaryColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outline
        val clickBg = if (isClicked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(clickBg)
                .border(2.dp, outlineColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(110.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawLine(outlineColor.copy(alpha = 0.5f), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
                drawLine(outlineColor.copy(alpha = 0.5f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)

                val maxOffset = (size.width / 2f) - 16.dp.toPx()
                val dotX = cx + (x.coerceIn(-1f, 1f) * maxOffset)
                val dotY = cy + (y.coerceIn(-1f, 1f) * maxOffset)
                drawCircle(color = primaryColor, radius = 12.dp.toPx(), center = Offset(dotX, dotY))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "X: %+.2f  Y: %+.2f".format(x, y),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TriggersAndShouldersCard(state: GamepadDebugState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Triggers & Bumpers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TriggerGauge(
                    label = "LT / L2",
                    value = state.leftTrigger,
                    isBumperPressed = state.pressedButtons.contains(GamepadButton.LB),
                    bumperLabel = "LB",
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                TriggerGauge(
                    label = "RT / R2",
                    value = state.rightTrigger,
                    isBumperPressed = state.pressedButtons.contains(GamepadButton.RB),
                    bumperLabel = "RB",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TriggerGauge(
    label: String,
    value: Float,
    isBumperPressed: Boolean,
    bumperLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            ButtonPill(text = bumperLabel, isPressed = isBumperPressed)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            strokeCap = StrokeCap.Round,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ButtonsCard(state: GamepadDebugState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Buttons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DpadCluster(state.pressedButtons)
                CenterMenuCluster(state.pressedButtons)
                FaceButtonsCluster(state.pressedButtons)
            }
        }
    }
}

@Composable
private fun DpadCluster(pressed: Set<GamepadButton>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("D-Pad", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        ButtonPill("▲", pressed.contains(GamepadButton.DPAD_UP))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ButtonPill("◀", pressed.contains(GamepadButton.DPAD_LEFT))
            Spacer(modifier = Modifier.size(28.dp))
            ButtonPill("▶", pressed.contains(GamepadButton.DPAD_RIGHT))
        }
        ButtonPill("▼", pressed.contains(GamepadButton.DPAD_DOWN))
    }
}

@Composable
private fun CenterMenuCluster(pressed: Set<GamepadButton>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("System", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        ButtonPill("GUIDE", pressed.contains(GamepadButton.GUIDE))
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ButtonPill("BACK", pressed.contains(GamepadButton.BACK))
            ButtonPill("START", pressed.contains(GamepadButton.START))
        }
    }
}

@Composable
private fun FaceButtonsCluster(pressed: Set<GamepadButton>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Face", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        ButtonPill("Y", pressed.contains(GamepadButton.Y), activeColor = Color(0xFFF9A825))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ButtonPill("X", pressed.contains(GamepadButton.X), activeColor = Color(0xFF0288D1))
            Spacer(modifier = Modifier.size(28.dp))
            ButtonPill("B", pressed.contains(GamepadButton.B), activeColor = Color(0xFFD32F2F))
        }
        ButtonPill("A", pressed.contains(GamepadButton.A), activeColor = Color(0xFF388E3C))
    }
}

@Composable
private fun ButtonPill(
    text: String,
    isPressed: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    val bg = if (isPressed) activeColor else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isPressed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        modifier = Modifier.size(width = if (text.length > 2) 48.dp else 28.dp, height = 28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun MotionSensorsCard(state: GamepadDebugState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Motion Sensors (Gyro & Tilt)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Gyro: X: %+.2f Y: %+.2f Z: %+.2f".format(state.gyroX, state.gyroY, state.gyroZ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Accel: X: %+.2f Y: %+.2f Z: %+.2f".format(state.accelX, state.accelY, state.accelZ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun RumbleTestCard(
    state: GamepadDebugState,
    onTestRumble: (Float) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rumble / Haptic Test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Vibration Strength", style = MaterialTheme.typography.bodyMedium)
                Text("${(state.rumbleStrength * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = state.rumbleStrength,
                onValueChange = { /* strength updated via parent or internal state */ },
                valueRange = 0f..1f,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onTestRumble(state.rumbleStrength) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Test Vibration")
            }
        }
    }
}
