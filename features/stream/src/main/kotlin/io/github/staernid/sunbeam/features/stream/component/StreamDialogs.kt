@file:Suppress(
    "MagicNumber",
    "LongMethod",
    "LongParameterList",
    "UnusedParameter",
)

package io.github.staernid.sunbeam.features.stream.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.features.stream.model.StreamShortcut

private val DrawerGlassSurface = Color(0xDC0B0F17)
private val DrawerGlassBorder = Color(0x22FFFFFF)
private val DrawerTileInactive = Color(0x14FFFFFF)
private val DrawerTileActive = Color(0x333B82F6)
private val DrawerTileActiveBorder = Color(0x8060A5FA)
private val DrawerTextPrimary = Color(0xFFF3F4F6)
private val DrawerTextSecondary = Color(0xFF9CA3AF)
private val DrawerAccent = Color(0xFF60A5FA)
private val DrawerDangerBg = Color(0x33EF4444)
private val DrawerDangerBorder = Color(0x66F87171)
private val DrawerDangerText = Color(0xFFFCA5A5)

/**
 * Compact, translucent dark-glass slide-in drawer providing in-stream controls,
 * quick toggles, mouse mode selection, and keyboard shortcut dispatch.
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
    isLocalCursorEnabled: Boolean = false,
    onToggleLocalCursor: () -> Unit = {},
    onEditVirtualController: () -> Unit = {},
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
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(onClick = onDismiss)
                .testTag("stream_drawer_scrim"),
        )

        // Compact slide-in edge panel
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .widthIn(min = 264.dp, max = 300.dp)
                .width(292.dp)
                .testTag("stream_control_drawer"),
            shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
            color = DrawerGlassSurface,
            contentColor = DrawerTextPrimary,
            border = BorderStroke(1.dp, DrawerGlassBorder),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Stream Controls",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = DrawerTextPrimary,
                        ),
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .sunbeamFocusable()
                            .testTag("close_drawer_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Controls",
                            tint = DrawerTextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Session Actions: Disconnect & Quit
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
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DrawerGlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DrawerTileInactive,
                            contentColor = DrawerTextPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Disconnect",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                        )
                    }
                    Button(
                        onClick = onOpenQuitConfirm,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DrawerDangerBorder),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DrawerDangerBg,
                            contentColor = DrawerDangerText,
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
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Quit Session",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                        )
                    }
                }

                HorizontalDivider(color = DrawerGlassBorder)

                // Quick Toggles
                DrawerSectionHeader("Quick Toggles")

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DrawerQuickToggle(
                            icon = Icons.Default.Gamepad,
                            title = "On-Screen Pad",
                            subtitle = if (isVirtualControllerEnabled) "Visible" else "Hidden",
                            active = isVirtualControllerEnabled,
                            onClick = onToggleVirtualController,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stream_toggle_virtual_controller"),
                        )
                        DrawerQuickToggle(
                            icon = if (isPanZoomMode) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                            title = "Pan / Zoom",
                            subtitle = if (isPanZoomMode) "Active" else "Off",
                            active = isPanZoomMode,
                            onClick = onToggleZoomMode,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stream_toggle_zoom"),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DrawerQuickToggle(
                            icon = Icons.Default.Keyboard,
                            title = "Keyboard",
                            subtitle = "Soft Input",
                            active = false,
                            onClick = onToggleKeyboard,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stream_toggle_keyboard"),
                        )
                        val hudLabel = when (perfOverlayMode) {
                            PerfOverlayMode.NONE -> "Off"
                            PerfOverlayMode.LITE -> "Lite"
                            PerfOverlayMode.EXPANDED -> "Full"
                        }
                        DrawerQuickToggle(
                            icon = Icons.Default.Speed,
                            title = "Stats HUD",
                            subtitle = hudLabel,
                            active = perfOverlayMode != PerfOverlayMode.NONE,
                            onClick = onTogglePerfHud,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stream_toggle_hud"),
                        )
                    }

                    if (isVirtualControllerEnabled) {
                        Surface(
                            onClick = onEditVirtualController,
                            shape = RoundedCornerShape(10.dp),
                            color = DrawerTileInactive,
                            border = BorderStroke(1.dp, DrawerGlassBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .sunbeamFocusable(shape = RoundedCornerShape(10.dp))
                                .testTag("stream_edit_virtual_controller"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = DrawerAccent,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Edit On-Screen Layout",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = DrawerAccent,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = DrawerGlassBorder)

                // Compact Mouse Mode Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DrawerSectionHeader("Mouse Mode")
                    Surface(
                        onClick = onToggleLocalCursor,
                        shape = RoundedCornerShape(50),
                        color = if (isLocalCursorEnabled) DrawerTileActive else DrawerTileInactive,
                        border = BorderStroke(
                            1.dp,
                            if (isLocalCursorEnabled) DrawerTileActiveBorder else DrawerGlassBorder,
                        ),
                        modifier = Modifier
                            .sunbeamFocusable(shape = RoundedCornerShape(50))
                            .testTag("stream_toggle_local_cursor"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mouse,
                                contentDescription = null,
                                tint = if (isLocalCursorEnabled) DrawerAccent else DrawerTextSecondary,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = "Local Cursor",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isLocalCursorEnabled) DrawerTextPrimary else DrawerTextSecondary,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StreamMouseMode.entries.forEach { mode ->
                        val isSelected = mode == selectedMouseMode
                        val itemShape = RoundedCornerShape(8.dp)
                        Surface(
                            onClick = { onSelectMouseMode(mode) },
                            shape = itemShape,
                            color = if (isSelected) DrawerTileActive else DrawerTileInactive,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) DrawerTileActiveBorder else Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .sunbeamFocusable(shape = itemShape),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) DrawerTextPrimary else DrawerTextSecondary,
                                    ),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (isSelected) DrawerAccent else Color(0x33FFFFFF),
                                            shape = CircleShape,
                                        ),
                                )
                            }
                        }
                    }
                    Text(
                        text = selectedMouseMode.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DrawerTextSecondary,
                            fontSize = 11.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
                    )
                }

                HorizontalDivider(color = DrawerGlassBorder)

                // Special Keys & Shortcuts (Clean Keycaps)
                DrawerSectionHeader("Special Keys & Shortcuts")

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    shortcuts.forEach { shortcut ->
                        val chipShape = RoundedCornerShape(8.dp)
                        Surface(
                            onClick = { onSendShortcut(shortcut) },
                            shape = chipShape,
                            color = DrawerTileInactive,
                            border = BorderStroke(1.dp, DrawerGlassBorder),
                            modifier = Modifier
                                .sunbeamFocusable(shape = chipShape)
                                .testTag("shortcut_${shortcut.id}"),
                        ) {
                            Text(
                                text = shortcut.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DrawerTextPrimary,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            color = DrawerAccent,
            letterSpacing = 0.3.sp,
        ),
    )
}

@Composable
private fun DrawerQuickToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tileShape = RoundedCornerShape(10.dp)
    Surface(
        onClick = onClick,
        shape = tileShape,
        color = if (active) DrawerTileActive else DrawerTileInactive,
        contentColor = DrawerTextPrimary,
        border = BorderStroke(
            width = 1.dp,
            color = if (active) DrawerTileActiveBorder else DrawerGlassBorder,
        ),
        modifier = modifier.sunbeamFocusable(shape = tileShape),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) DrawerAccent else DrawerTextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = DrawerTextPrimary,
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (active) DrawerAccent else DrawerTextSecondary,
                        fontSize = 10.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
        selectedMouseMode = StreamMouseMode.MULTI_TOUCH,
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
