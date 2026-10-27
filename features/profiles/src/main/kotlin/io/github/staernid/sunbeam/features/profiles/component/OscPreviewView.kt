@file:Suppress(
    "LongMethod",
    "MagicNumber",
    "TooManyFunctions",
)

package io.github.staernid.sunbeam.features.profiles.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OscPreviewView(
    opacity: Float,
    scale: Float,
    onOpacityChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OscControlsBar(
            opacity = opacity,
            scale = scale,
            onOpacityChange = onOpacityChange,
            onScaleChange = onScaleChange,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121214)),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                OscSimulatedScreen(
                    opacity = opacity,
                    scale = scale,
                )
            }
        }
    }
}

@Composable
private fun OscControlsBar(
    opacity: Float,
    scale: Float,
    onOpacityChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Virtual Controller Layout Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Opacity: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.1f..1.0f,
                    modifier = Modifier.fillMaxWidth(0.8f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Scale: ${"%.1f".format(scale)}x", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = scale,
                    onValueChange = onScaleChange,
                    valueRange = 0.5f..1.5f,
                    modifier = Modifier.fillMaxWidth(0.8f),
                )
            }
        }
    }
}

@Composable
private fun OscSimulatedScreen(
    opacity: Float,
    scale: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(1.dp, Color(0xFF333338), RoundedCornerShape(8.dp)),
    ) {
        // Shoulder buttons
        OscShoulderBar(opacity = opacity, scale = scale)

        // Center menu buttons
        OscCenterMenu(opacity = opacity, scale = scale, modifier = Modifier.align(Alignment.TopCenter))

        // Left control cluster (D-pad & Left Stick)
        OscLeftControls(opacity = opacity, scale = scale, modifier = Modifier.align(Alignment.BottomStart))

        // Right control cluster (Face buttons & Right Stick)
        OscRightControls(opacity = opacity, scale = scale, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun OscShoulderBar(
    opacity: Float,
    scale: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (12 * scale).dp, vertical = (8 * scale).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
            OscVirtualButton("L2", opacity, (42 * scale).dp, (28 * scale).dp)
            OscVirtualButton("L1", opacity, (42 * scale).dp, (28 * scale).dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy((6 * scale).dp)) {
            OscVirtualButton("R1", opacity, (42 * scale).dp, (28 * scale).dp)
            OscVirtualButton("R2", opacity, (42 * scale).dp, (28 * scale).dp)
        }
    }
}

@Composable
private fun OscCenterMenu(
    opacity: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(top = (8 * scale).dp),
        horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
    ) {
        OscVirtualButton("BACK", opacity, (36 * scale).dp, (22 * scale).dp, isSmallText = true)
        OscVirtualButton("GUIDE", opacity, (36 * scale).dp, (22 * scale).dp, isSmallText = true)
        OscVirtualButton("START", opacity, (36 * scale).dp, (22 * scale).dp, isSmallText = true)
    }
}

@Composable
private fun OscLeftControls(
    opacity: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding((12 * scale).dp),
        horizontalArrangement = Arrangement.spacedBy((16 * scale).dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // D-Pad
        Box(modifier = Modifier.size((90 * scale).dp), contentAlignment = Alignment.Center) {
            OscVirtualButton("▲", opacity, (28 * scale).dp, (28 * scale).dp, Modifier.align(Alignment.TopCenter))
            OscVirtualButton("◀", opacity, (28 * scale).dp, (28 * scale).dp, Modifier.align(Alignment.CenterStart))
            OscVirtualButton("▶", opacity, (28 * scale).dp, (28 * scale).dp, Modifier.align(Alignment.CenterEnd))
            OscVirtualButton("▼", opacity, (28 * scale).dp, (28 * scale).dp, Modifier.align(Alignment.BottomCenter))
        }

        // Left Thumbstick
        OscVirtualStick(label = "LS", opacity = opacity, size = (70 * scale).dp)
    }
}

@Composable
private fun OscRightControls(
    opacity: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding((12 * scale).dp),
        horizontalArrangement = Arrangement.spacedBy((16 * scale).dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // Right Thumbstick
        OscVirtualStick(label = "RS", opacity = opacity, size = (70 * scale).dp)

        // Face Buttons (Y, X, B, A)
        Box(modifier = Modifier.size((90 * scale).dp), contentAlignment = Alignment.Center) {
            OscVirtualButton(
                "Y",
                opacity,
                (28 * scale).dp,
                (28 * scale).dp,
                Modifier.align(Alignment.TopCenter),
                tint = Color(0xFFF9A825),
            )
            OscVirtualButton(
                "X",
                opacity,
                (28 * scale).dp,
                (28 * scale).dp,
                Modifier.align(Alignment.CenterStart),
                tint = Color(0xFF0288D1),
            )
            OscVirtualButton(
                "B",
                opacity,
                (28 * scale).dp,
                (28 * scale).dp,
                Modifier.align(Alignment.CenterEnd),
                tint = Color(0xFFD32F2F),
            )
            OscVirtualButton(
                "A",
                opacity,
                (28 * scale).dp,
                (28 * scale).dp,
                Modifier.align(Alignment.BottomCenter),
                tint = Color(0xFF388E3C),
            )
        }
    }
}

@Composable
private fun OscVirtualButton(
    text: String,
    opacity: Float,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    isSmallText: Boolean = false,
) {
    var isPressed by remember { mutableStateOf(false) }
    val bg = if (isPressed) Color(0x99FFFFFF) else Color(0x33FFFFFF)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg,
        modifier = modifier
            .size(width, height)
            .alpha(opacity)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { isPressed = !isPressed },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = tint,
                fontSize = if (isSmallText) 8.sp else 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun OscVirtualStick(
    label: String,
    opacity: Float,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(opacity)
            .clip(CircleShape)
            .background(Color(0x22FFFFFF))
            .border(1.dp, Color(0x55FFFFFF), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size / 2.2f)
                .clip(CircleShape)
                .background(Color(0x55FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
