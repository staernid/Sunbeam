@file:Suppress("MagicNumber", "LongMethod")

package io.github.staernid.sunbeam.features.stream.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.features.stream.model.StreamShortcut

/**
 * Main in-stream quick control menu.
 */
@Composable
fun StreamMenuDialog(
    isPanZoomMode: Boolean,
    isVirtualControllerEnabled: Boolean,
    onDisconnect: () -> Unit,
    onOpenQuitConfirm: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleZoomMode: () -> Unit,
    onOpenMouseModePicker: () -> Unit,
    onOpenSpecialKeysPicker: () -> Unit,
    onTogglePerfHud: () -> Unit,
    onToggleVirtualController: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Stream Controls",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StreamMenuItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Disconnect",
                    subtitle = "Leave stream running on host",
                    onClick = onDisconnect,
                )
                StreamMenuItem(
                    icon = Icons.Default.PowerSettingsNew,
                    title = "Quit Session",
                    subtitle = "Terminate active game on host",
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onOpenQuitConfirm,
                )
                StreamMenuItem(
                    icon = Icons.Default.Keyboard,
                    title = "On-Screen Keyboard",
                    subtitle = "Toggle software text input",
                    onClick = onToggleKeyboard,
                )
                StreamMenuItem(
                    icon = Icons.Default.ZoomIn,
                    title = if (isPanZoomMode) "Disable Pan / Zoom" else "Enable Pan / Zoom",
                    subtitle = "Pan and scale the video stream",
                    onClick = onToggleZoomMode,
                )
                StreamMenuItem(
                    icon = Icons.Default.Mouse,
                    title = "Mouse Mode",
                    subtitle = "Trackpad, gaming, or direct input",
                    onClick = onOpenMouseModePicker,
                )
                StreamMenuItem(
                    icon = Icons.Default.KeyboardCommandKey,
                    title = "Send Special Keys",
                    subtitle = "Esc, Alt+F4, Win+D, Task Manager",
                    onClick = onOpenSpecialKeysPicker,
                )
                StreamMenuItem(
                    icon = Icons.Default.Speed,
                    title = "Performance HUD",
                    subtitle = "Cycle off, compact, or expanded stats",
                    onClick = onTogglePerfHud,
                )
                StreamMenuItem(
                    icon = Icons.Default.Gamepad,
                    title = if (isVirtualControllerEnabled) "Hide Virtual Controls" else "Show Virtual Controls",
                    subtitle = "On-screen touch gamepad overlay",
                    onClick = onToggleVirtualController,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
        modifier = modifier.testTag("stream_menu_dialog"),
    )
}

@Composable
private fun StreamMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                ),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Confirmation dialog before terminating session on Sunshine host.
 */
@Composable
fun StreamQuitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Quit Stream Session?") },
        text = {
            Text(
                text = "This will terminate the active application on your host PC. " +
                    "Any unsaved progress in the game may be lost.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_quit_button"),
            ) {
                Text(text = "Quit Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        modifier = modifier.testTag("stream_quit_confirm_dialog"),
    )
}

/**
 * Dialog for selecting mouse / trackpad emulation mode.
 */
@Composable
fun StreamMouseModeDialog(
    selectedMode: StreamMouseMode,
    onSelectMode: (StreamMouseMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select Mouse Mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamMouseMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectMode(mode) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == selectedMode,
                            onClick = { onSelectMode(mode) },
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done")
            }
        },
        modifier = modifier.testTag("stream_mouse_mode_dialog"),
    )
}

/**
 * Dialog for sending special key shortcuts to the host.
 */
@Composable
fun StreamSpecialKeysDialog(
    shortcuts: List<StreamShortcut>,
    onSendShortcut: (StreamShortcut) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Send Keyboard Shortcut") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(shortcuts, key = { it.id }) { shortcut ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSendShortcut(shortcut) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = shortcut.label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardCommandKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        },
        modifier = modifier.testTag("stream_special_keys_dialog"),
    )
}
