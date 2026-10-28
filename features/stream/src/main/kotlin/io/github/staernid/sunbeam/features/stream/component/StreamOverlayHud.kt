@file:Suppress("MagicNumber", "LongMethod")

package io.github.staernid.sunbeam.features.stream.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayPosition
import io.github.staernid.sunbeam.features.stream.model.PerformanceStats

/**
 * Performance HUD overlay displaying real-time stream diagnostics.
 */
@Composable
fun StreamOverlayHud(
    stats: PerformanceStats,
    mode: PerfOverlayMode,
    position: PerfOverlayPosition,
    onHudClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (mode == PerfOverlayMode.NONE) return

    val alignment = when (position) {
        PerfOverlayPosition.TOP_CENTER -> Alignment.TopCenter
        PerfOverlayPosition.TOP_START -> Alignment.TopStart
        PerfOverlayPosition.BOTTOM_CENTER -> Alignment.BottomCenter
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = alignment,
    ) {
        AnimatedVisibility(
            visible = mode == PerfOverlayMode.LITE,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LiteHud(
                stats = stats,
                onClick = onHudClick,
            )
        }

        AnimatedVisibility(
            visible = mode == PerfOverlayMode.EXPANDED,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ExpandedHud(
                stats = stats,
                onClose = onCloseClick,
            )
        }
    }
}

@Composable
private fun LiteHud(
    stats: PerformanceStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .sunbeamFocusable(shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Performance HUD" }
            .testTag("stream_hud_lite"),
        color = Color(0xCC1E1E1E),
        contentColor = Color.White,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${stats.formattedFps} FPS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
            )
            Text(text = "•", color = Color.Gray, fontSize = 10.sp)
            Text(
                text = "RTT: ${stats.networkLatencyMs}ms",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                ),
            )
            Text(text = "•", color = Color.Gray, fontSize = 10.sp)
            Text(
                text = "Dec: ${stats.formattedDecodeLatency}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                ),
            )
            Text(text = "•", color = Color.Gray, fontSize = 10.sp)
            Text(
                text = "Loss: ${stats.formattedPacketLoss}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (stats.packetLossPercentage > 1f) {
                        MaterialTheme.colorScheme.error
                    } else {
                        Color.White
                    },
                ),
            )
        }
    }
}

@Composable
private fun ExpandedHud(
    stats: PerformanceStats,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .widthIn(max = 380.dp)
            .testTag("stream_hud_expanded"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xEE1A1A1A),
            contentColor = Color.White,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Stream Performance",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(24.dp)
                        .sunbeamFocusable(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Collapse HUD",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            HudMetricRow(label = "Framerate", value = "${stats.formattedFps} FPS")
            HudMetricRow(label = "Network Latency (RTT)", value = "${stats.networkLatencyMs} ms")
            HudMetricRow(label = "Decode Time", value = stats.formattedDecodeLatency)
            HudMetricRow(
                label = "Packet Loss",
                value = stats.formattedPacketLoss,
                valueColor = if (stats.packetLossPercentage > 1f) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color.White
                },
            )
            HudMetricRow(label = "Bitrate / Bandwidth", value = stats.formattedBandwidth)
            if (stats.videoFormat.isNotEmpty()) {
                HudMetricRow(label = "Video Codec", value = stats.videoFormat)
            }
            if (stats.decoderName.isNotEmpty()) {
                HudMetricRow(label = "Hardware Decoder", value = stats.decoderName)
            }
            stats.customDetails?.let { details ->
                HudMetricRow(label = "Details", value = details)
            }
        }
    }
}

@Composable
private fun HudMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.LightGray,
                fontSize = 11.sp,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                fontSize = 11.sp,
            ),
        )
    }
}
