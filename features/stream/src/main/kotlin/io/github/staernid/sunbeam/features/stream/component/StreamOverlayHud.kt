@file:Suppress("MagicNumber", "LongMethod")

package io.github.staernid.sunbeam.features.stream.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
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

private val HudGlassSurfaceLite = Color(0x800B0F17)
private val HudGlassSurfaceExpanded = Color(0xA60B0F17)
private val HudGlassBorder = Color(0x24FFFFFF)
private val HudTextPrimary = Color(0xFFF3F4F6)
private val HudTextSecondary = Color(0xFF9CA3AF)
private val HudStatusGood = Color(0xFF34D399)
private val HudStatusWarn = Color(0xFFFBBF24)
private val HudStatusBad = Color(0xFFF87171)

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
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
    val pillShape = RoundedCornerShape(50)
    val statusColor = when {
        stats.packetLossPercentage > 2f || stats.networkLatencyMs > 50 -> HudStatusBad
        stats.packetLossPercentage > 0.5f || stats.networkLatencyMs > 25 -> HudStatusWarn
        else -> HudStatusGood
    }

    Surface(
        modifier = modifier
            .sunbeamFocusable(shape = pillShape)
            .clip(pillShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Performance HUD" }
            .testTag("stream_hud_lite"),
        color = HudGlassSurfaceLite,
        contentColor = HudTextPrimary,
        shape = pillShape,
        border = BorderStroke(1.dp, HudGlassBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(statusColor, CircleShape),
            )
            Text(
                text = "${stats.formattedFps} FPS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = HudTextPrimary,
                    fontSize = 11.sp,
                ),
            )
            HudPillDivider()
            Text(
                text = "RTT: ${stats.networkLatencyMs}ms",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = HudTextPrimary,
                    fontSize = 11.sp,
                ),
            )
            HudPillDivider()
            Text(
                text = "Dec: ${stats.formattedDecodeLatency}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = HudTextPrimary,
                    fontSize = 11.sp,
                ),
            )
            HudPillDivider()
            Text(
                text = "Loss: ${stats.formattedPacketLoss}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (stats.packetLossPercentage > 1f) {
                        HudStatusBad
                    } else {
                        HudTextPrimary
                    },
                ),
            )
        }
    }
}

@Composable
private fun HudPillDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(10.dp)
            .background(HudGlassBorder),
    )
}

@Composable
private fun ExpandedHud(
    stats: PerformanceStats,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .widthIn(min = 230.dp, max = 280.dp)
            .testTag("stream_hud_expanded"),
        color = HudGlassSurfaceExpanded,
        contentColor = HudTextPrimary,
        shape = cardShape,
        border = BorderStroke(1.dp, HudGlassBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "Stream Performance",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = HudTextPrimary,
                        fontSize = 12.sp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(22.dp)
                        .sunbeamFocusable(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Collapse HUD",
                        tint = HudTextSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            HorizontalDivider(color = HudGlassBorder, thickness = 0.5.dp)

            HudMetricRow(label = "Framerate", value = "${stats.formattedFps} FPS")
            HudMetricRow(
                label = "Network RTT",
                value = "${stats.networkLatencyMs} ms",
                valueColor = if (stats.networkLatencyMs > 40) HudStatusWarn else HudTextPrimary,
            )
            HudMetricRow(label = "Decode Time", value = stats.formattedDecodeLatency)
            HudMetricRow(
                label = "Packet Loss",
                value = stats.formattedPacketLoss,
                valueColor = if (stats.packetLossPercentage > 1f) {
                    HudStatusBad
                } else {
                    HudTextPrimary
                },
            )
            HudMetricRow(label = "Bandwidth", value = stats.formattedBandwidth)
            if (stats.videoFormat.isNotEmpty()) {
                HudMetricRow(label = "Stream", value = stats.videoFormat)
            }
            if (stats.decoderName.isNotEmpty()) {
                HudMetricRow(label = "Decoder", value = stats.decoderName)
            }
            stats.customDetails?.takeIf { it.isNotBlank() }?.let { details ->
                HudMetricRow(label = "Host Latency", value = details)
            }
        }
    }
}

@Composable
private fun HudMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = HudTextPrimary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = HudTextSecondary,
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
