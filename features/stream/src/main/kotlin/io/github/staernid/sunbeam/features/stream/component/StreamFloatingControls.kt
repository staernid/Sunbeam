@file:Suppress("MagicNumber", "LongMethod")

package io.github.staernid.sunbeam.features.stream.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable

/**
 * Floating action controls for in-stream menu and pan/zoom toggling.
 */
@Composable
fun StreamFloatingControls(
    isMenuButtonVisible: Boolean,
    isZoomButtonVisible: Boolean,
    isPanZoomMode: Boolean,
    onMenuClick: () -> Unit,
    onZoomClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = isMenuButtonVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = onMenuClick,
                    shape = CircleShape,
                    containerColor = Color(0xCC2A2A2A),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .size(42.dp)
                        .sunbeamFocusable(shape = CircleShape)
                        .testTag("stream_floating_menu_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Stream Menu",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = isZoomButtonVisible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = onZoomClick,
                    shape = CircleShape,
                    containerColor = if (isPanZoomMode) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(0xCC2A2A2A)
                    },
                    contentColor = if (isPanZoomMode) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        Color.White
                    },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .size(42.dp)
                        .sunbeamFocusable(shape = CircleShape)
                        .testTag("stream_floating_zoom_button"),
                ) {
                    Icon(
                        imageVector = if (isPanZoomMode) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                        contentDescription = "Toggle Pan/Zoom Mode",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
