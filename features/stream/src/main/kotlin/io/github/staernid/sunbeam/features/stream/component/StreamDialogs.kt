@file:Suppress(
    "MagicNumber",
    "LongMethod",
    "LongParameterList",
    "UnusedParameter",
)

package io.github.staernid.sunbeam.features.stream.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.features.stream.model.StreamShortcut

/**
 * Slide-in edge drawer providing in-stream controls, quick toggles, inline mouse mode,
 * and shortcut dispatch.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StreamControlDrawer(
    isPanZoomMode: Boolean,
    isVirtualControllerEnabled: Boolean,
    perfOverlayMode: PerfOverlayMode,
    selectedMouseMode: StreamMouseMode,
    shortcuts: List<StreamShortcut>,
    onDisconnect: () -> Unit,
    onOpenQuitConfirm: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleZoomMode: () -> Unit,
    onSelectMouseMode: (StreamMouseMode) -> Unit,
    onSendShortcut: (StreamShortcut) -> Unit,
    onTogglePerfHud: () -> Unit,
    onToggleVirtualController: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("stream_control_drawer_container"),
    ) {
        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
                .testTag("stream_drawer_scrim"),
        )

        // Slide-in drawer
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .widthIn(max = 360.dp)
                .testTag("stream_control_drawer"),
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stream Controls",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Active Session",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .sunbeamFocusable()
                            .testTag("close_drawer_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Controls",
                        )
                    }
                }

                // Quick Actions: Disconnect & Quit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier
                            .weight(1f)
                            .sunbeamFocusable()
                            .testTag("drawer_disconnect_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Disconnect", maxLines = 1)
                    }
                    Button(
                        onClick = onOpenQuitConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .sunbeamFocusable()
                            .testTag("drawer_quit_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = "Quit Session", maxLines = 1)
                    }
                }

                HorizontalDivider()

                // Quick Toggles
                Text(
                    text = "Quick Toggles",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DrawerQuickToggle(
                        icon = Icons.Default.Gamepad,
                        title = "Virtual Controller",
                        active = isVirtualControllerEnabled,
                        onClick = onToggleVirtualController,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stream_toggle_virtual_controller"),
                    )
                    DrawerQuickToggle(
                        icon = if (isPanZoomMode) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                        title = if (isPanZoomMode) "Exit Zoom" else "Pan / Zoom",
                        active = isPanZoomMode,
                        onClick = onToggleZoomMode,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stream_toggle_zoom"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DrawerQuickToggle(
                        icon = Icons.Default.Keyboard,
                        title = "Keyboard",
                        active = false,
                        onClick = onToggleKeyboard,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stream_toggle_keyboard"),
                    )
                    DrawerQuickToggle(
                        icon = Icons.Default.Speed,
                        title = "HUD: ${perfOverlayMode.name}",
                        active = perfOverlayMode != PerfOverlayMode.NONE,
                        onClick = onTogglePerfHud,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stream_toggle_hud"),
                    )
                }

                HorizontalDivider()

                // Inline Mouse Mode
                Text(
                    text = "Mouse Mode",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StreamMouseMode.entries.forEach { mode ->
                        val isSelected = mode == selectedMouseMode
                        Surface(
                            onClick = { onSelectMouseMode(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .sunbeamFocusable(shape = RoundedCornerShape(12.dp)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectMouseMode(mode) },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
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
                }

                HorizontalDivider()

                // Special Keys & Shortcuts
                Text(
                    text = "Special Keys & Shortcuts",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    shortcuts.forEach { shortcut ->
                        AssistChip(
                            onClick = { onSendShortcut(shortcut) },
                            label = {
                                Text(
                                    text = shortcut.label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardCommandKey,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            modifier = Modifier
                                .sunbeamFocusable()
                                .testTag("shortcut_${shortcut.id}"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerQuickToggle(
    icon: ImageVector,
    title: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.sunbeamFocusable(shape = RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
    }
}

/**
 * Backward-compatible wrapper delegating to [StreamControlDrawer].
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
    StreamControlDrawer(
        isPanZoomMode = isPanZoomMode,
        isVirtualControllerEnabled = isVirtualControllerEnabled,
        perfOverlayMode = PerfOverlayMode.LITE,
        selectedMouseMode = StreamMouseMode.TRACKPAD_NATURAL,
        shortcuts = StreamShortcut.defaultShortcuts,
        onDisconnect = onDisconnect,
        onOpenQuitConfirm = onOpenQuitConfirm,
        onToggleKeyboard = onToggleKeyboard,
        onToggleZoomMode = onToggleZoomMode,
        onSelectMouseMode = {},
        onSendShortcut = {},
        onTogglePerfHud = onTogglePerfHud,
        onToggleVirtualController = onToggleVirtualController,
        onDismiss = onDismiss,
        modifier = modifier,
    )
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
                StreamMouseMode.entries.forEach { mode ->
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
