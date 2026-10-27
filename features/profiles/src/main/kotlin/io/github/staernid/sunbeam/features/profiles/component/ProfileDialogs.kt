@file:Suppress(
    "LongMethod",
    "MagicNumber",
    "LongParameterList",
)

package io.github.staernid.sunbeam.features.profiles.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Profile
import io.github.staernid.sunbeam.features.profiles.model.bitrateKbps
import io.github.staernid.sunbeam.features.profiles.model.fps
import io.github.staernid.sunbeam.features.profiles.model.height
import io.github.staernid.sunbeam.features.profiles.model.oscEnabled
import io.github.staernid.sunbeam.features.profiles.model.oscOpacity
import io.github.staernid.sunbeam.features.profiles.model.stretchMode
import io.github.staernid.sunbeam.features.profiles.model.width

private data class ResOption(val label: String, val width: Int, val height: Int)

private val STANDARD_RESOLUTIONS = listOf(
    ResOption("720p", 1280, 720),
    ResOption("1080p", 1920, 1080),
    ResOption("1440p", 2560, 1440),
    ResOption("4K", 3840, 2160),
)

private val STANDARD_FPS = listOf(30, 60, 90, 120)

@Composable
fun ProfileFormDialog(
    initialProfile: Profile?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        stretchMode: Boolean,
        oscEnabled: Boolean,
        oscOpacity: Float,
    ) -> Unit,
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var selectedWidth by remember { mutableIntStateOf(initialProfile?.width ?: 1920) }
    var selectedHeight by remember { mutableIntStateOf(initialProfile?.height ?: 1080) }
    var selectedFps by remember { mutableIntStateOf(initialProfile?.fps ?: 60) }
    var bitrateKbps by remember { mutableIntStateOf(initialProfile?.bitrateKbps ?: 20_000) }
    var stretchMode by remember { mutableStateOf(initialProfile?.stretchMode ?: false) }
    var oscEnabled by remember { mutableStateOf(initialProfile?.oscEnabled ?: false) }
    var oscOpacity by remember { mutableFloatStateOf(initialProfile?.oscOpacity ?: 0.7f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialProfile == null) "New Profile" else "Edit Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
                ResolutionSelector(selectedWidth, selectedHeight) { w, h ->
                    selectedWidth = w
                    selectedHeight = h
                }

                Spacer(modifier = Modifier.height(16.dp))
                FpsSelector(selectedFps) { selectedFps = it }

                Spacer(modifier = Modifier.height(16.dp))
                BitrateSelector(bitrateKbps) { bitrateKbps = it }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Stretch to Fill Screen", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = stretchMode, onCheckedChange = { stretchMode = it })
                }

                Spacer(modifier = Modifier.height(8.dp))
                OscSettingsSection(
                    oscEnabled = oscEnabled,
                    oscOpacity = oscOpacity,
                    onOscEnabledChange = { oscEnabled = it },
                    onOscOpacityChange = { oscOpacity = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalName = name.trim().ifEmpty { "Custom Profile" }
                    onSave(
                        finalName,
                        selectedWidth,
                        selectedHeight,
                        selectedFps,
                        bitrateKbps,
                        stretchMode,
                        oscEnabled,
                        oscOpacity,
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResolutionSelector(
    currentWidth: Int,
    currentHeight: Int,
    onSelect: (Int, Int) -> Unit,
) {
    Text("Stream Resolution", style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        STANDARD_RESOLUTIONS.forEach { res ->
            val selected = currentWidth == res.width && currentHeight == res.height
            FilterChip(
                selected = selected,
                onClick = { onSelect(res.width, res.height) },
                label = { Text(res.label) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FpsSelector(
    currentFps: Int,
    onSelect: (Int) -> Unit,
) {
    Text("Framerate (FPS)", style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        STANDARD_FPS.forEach { fps ->
            FilterChip(
                selected = currentFps == fps,
                onClick = { onSelect(fps) },
                label = { Text("$fps FPS") },
            )
        }
    }
}

@Composable
private fun BitrateSelector(
    currentKbps: Int,
    onSelect: (Int) -> Unit,
) {
    val mbps = currentKbps / 1000
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Target Bitrate", style = MaterialTheme.typography.labelMedium)
        Text("$mbps Mbps", style = MaterialTheme.typography.bodySmall)
    }
    Slider(
        value = mbps.toFloat(),
        onValueChange = { onSelect((it.toInt()) * 1000) },
        valueRange = 5f..100f,
        steps = 18,
    )
}

@Composable
private fun OscSettingsSection(
    oscEnabled: Boolean,
    oscOpacity: Float,
    onOscEnabledChange: (Boolean) -> Unit,
    onOscOpacityChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("On-Screen Controls", style = MaterialTheme.typography.bodyMedium)
        Switch(checked = oscEnabled, onCheckedChange = onOscEnabledChange)
    }

    if (oscEnabled) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Control Opacity", style = MaterialTheme.typography.labelSmall)
            Text("${(oscOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = oscOpacity,
            onValueChange = onOscOpacityChange,
            valueRange = 0.1f..1.0f,
        )
    }
}

@Composable
fun DeleteProfileConfirmationDialog(
    profile: Profile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Profile") },
        text = { Text("Are you sure you want to delete '${profile.name}'? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
