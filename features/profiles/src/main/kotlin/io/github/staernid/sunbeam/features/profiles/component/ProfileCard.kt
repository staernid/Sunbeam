@file:Suppress(
    "LongMethod",
    "MagicNumber",
)

package io.github.staernid.sunbeam.features.profiles.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Profile
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.features.profiles.model.bitrateKbps
import io.github.staernid.sunbeam.features.profiles.model.fps
import io.github.staernid.sunbeam.features.profiles.model.height
import io.github.staernid.sunbeam.features.profiles.model.oscEnabled
import io.github.staernid.sunbeam.features.profiles.model.stretchMode
import io.github.staernid.sunbeam.features.profiles.model.width

/**
 * Card displaying profile details and actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColors = if (isActive) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        )
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    }

    val border = if (isActive) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .sunbeamFocusable(),
        colors = cardColors,
        border = border,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isActive) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("ACTIVE") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            labelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val bitrateMbps = profile.bitrateKbps / 1000f
            val bitrateStr = if (bitrateMbps % 1f == 0f) {
                "${bitrateMbps.toInt()} Mbps"
            } else {
                "%.1f Mbps".format(bitrateMbps)
            }

            Text(
                text = "${profile.width}x${profile.height} @ ${profile.fps} FPS • $bitrateStr",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (profile.stretchMode) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Stretch to fit") },
                    )
                }
                if (profile.oscEnabled) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("On-Screen Controls") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isActive) {
                    FilledTonalButton(
                        onClick = onActivate,
                        modifier = Modifier.sunbeamFocusable(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Activate")
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Currently Active",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.sunbeamFocusable(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.sunbeamFocusable(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Profile",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.sunbeamFocusable(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Profile",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
