package io.github.staernid.sunbeam.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable

@Composable
fun SunbeamCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        modifier
            .sunbeamFocusable(interactionSource = interactionSource, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
    } else {
        modifier
    }

    Card(
        modifier = clickableModifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
    ) {
        content()
    }
}

@Composable
fun SunbeamOutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    border: BorderStroke = BorderStroke(
        width = CardDefaults.outlinedCardBorder().width,
        color = MaterialTheme.colorScheme.outlineVariant,
    ),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        modifier
            .sunbeamFocusable(interactionSource = interactionSource, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
    } else {
        modifier
    }

    OutlinedCard(
        modifier = clickableModifier,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
    ) {
        content()
    }
}
