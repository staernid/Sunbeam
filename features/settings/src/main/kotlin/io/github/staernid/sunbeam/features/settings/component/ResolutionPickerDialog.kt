@file:Suppress("LongMethod")

package io.github.staernid.sunbeam.features.settings.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.preferences.display.DeviceDisplayHelper
import io.github.staernid.sunbeam.core.preferences.display.DeviceDisplayInfo
import io.github.staernid.sunbeam.core.preferences.display.ResolutionCategory
import io.github.staernid.sunbeam.core.preferences.display.ResolutionOption
import io.github.staernid.sunbeam.core.preferences.display.ResolutionProvider
import io.github.staernid.sunbeam.core.preferences.models.Resolution

private const val MIN_CUSTOM_DIMENSION = 240

@Composable
fun ResolutionPickerDialog(
    currentResolution: Resolution,
    onResolutionSelected: (width: Int, height: Int) -> Unit,
    onDismiss: () -> Unit,
    displayInfo: DeviceDisplayInfo = DeviceDisplayHelper.fromContext(LocalContext.current),
) {
    val nativeOptions = remember(displayInfo) {
        if (!displayInfo.isTv) ResolutionProvider.getNativeOptions(displayInfo) else emptyList()
    }

    val standardOptions = remember { ResolutionProvider.STANDARD_16_9 }
    val handheldOptions = remember { ResolutionProvider.HANDHELD_16_10 }

    val allPresetResolutions = remember(nativeOptions, standardOptions, handheldOptions) {
        (nativeOptions + standardOptions + handheldOptions).map { it.resolution }.toSet()
    }

    val isCurrentPreset = currentResolution in allPresetResolutions
    var isCustomExpanded by remember { mutableStateOf(!isCurrentPreset) }
    var customWidthText by remember { mutableStateOf(currentResolution.width.toString()) }
    var customHeightText by remember { mutableStateOf(currentResolution.height.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Resolution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (nativeOptions.isNotEmpty()) {
                    ResolutionSectionHeader(title = ResolutionCategory.NATIVE.headerTitle)
                    nativeOptions.forEach { option ->
                        ResolutionOptionRow(
                            option = option,
                            isSelected = !isCustomExpanded && currentResolution == option.resolution,
                            onClick = {
                                isCustomExpanded = false
                                onResolutionSelected(option.resolution.width, option.resolution.height)
                            },
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                ResolutionSectionHeader(title = ResolutionCategory.STANDARD_16_9.headerTitle)
                standardOptions.forEach { option ->
                    ResolutionOptionRow(
                        option = option,
                        isSelected = !isCustomExpanded && currentResolution == option.resolution,
                        onClick = {
                            isCustomExpanded = false
                            onResolutionSelected(option.resolution.width, option.resolution.height)
                        },
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ResolutionSectionHeader(title = ResolutionCategory.HANDHELD_16_10.headerTitle)
                handheldOptions.forEach { option ->
                    ResolutionOptionRow(
                        option = option,
                        isSelected = !isCustomExpanded && currentResolution == option.resolution,
                        onClick = {
                            isCustomExpanded = false
                            onResolutionSelected(option.resolution.width, option.resolution.height)
                        },
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ResolutionSectionHeader(title = ResolutionCategory.CUSTOM.headerTitle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustomExpanded = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isCustomExpanded,
                        onClick = { isCustomExpanded = true },
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "Custom Resolution",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "Specify exact streaming dimensions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AnimatedVisibility(visible = isCustomExpanded) {
                    CustomResolutionForm(
                        widthText = customWidthText,
                        heightText = customHeightText,
                        onWidthChange = { customWidthText = it },
                        onHeightChange = { customHeightText = it },
                        onApply = { w, h -> onResolutionSelected(w, h) },
                    )
                }

                if (nativeOptions.isNotEmpty() && currentResolution == displayInfo.halvedResolution) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "💡 Tip: Enable Video Super Resolution (SGSR) below " +
                                "to sharpen halved resolution on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ResolutionSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun ResolutionOptionRow(
    option: ResolutionOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = "${option.resolution.width}x${option.resolution.height}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            option.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            option.badge?.let { badge ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomResolutionForm(
    widthText: String,
    heightText: String,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onApply: (width: Int, height: Int) -> Unit,
) {
    val parsedW = widthText.toIntOrNull()
    val parsedH = heightText.toIntOrNull()
    val isValid = parsedW != null &&
        parsedH != null &&
        parsedW >= MIN_CUSTOM_DIMENSION &&
        parsedH >= MIN_CUSTOM_DIMENSION
    val customRes = if (isValid) Resolution(parsedW, parsedH) else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = widthText,
                onValueChange = { if (it.all { c -> c.isDigit() }) onWidthChange(it) },
                label = { Text("Width") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "×",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = heightText,
                onValueChange = { if (it.all { c -> c.isDigit() }) onHeightChange(it) },
                label = { Text("Height") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        if (customRes != null) {
            Text(
                text = "Aspect Ratio: ${customRes.aspectRatioLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Button(
            onClick = {
                if (customRes != null) {
                    val evenW = ((customRes.width / 2) * 2).coerceAtLeast(MIN_CUSTOM_DIMENSION)
                    val evenH = ((customRes.height / 2) * 2).coerceAtLeast(MIN_CUSTOM_DIMENSION)
                    onApply(evenW, evenH)
                }
            },
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apply Custom Resolution")
        }
    }
}
