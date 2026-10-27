package io.github.staernid.sunbeam.core.designsystem.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.sunbeamFocusable(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.medium,
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor: Color = Color.Transparent,
    focusedBorderWidth: Dp = 2.dp,
    focusedScale: Float = 1.03f,
    enabled: Boolean = true,
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        label = "sunbeamFocusScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedBorderColor else unfocusedBorderColor,
        label = "sunbeamFocusBorderColor",
    )

    return this
        .scale(scale)
        .border(
            border = BorderStroke(if (isFocused) focusedBorderWidth else 0.dp, borderColor),
            shape = shape,
        )
        .focusable(enabled = enabled, interactionSource = interactionSource)
}
