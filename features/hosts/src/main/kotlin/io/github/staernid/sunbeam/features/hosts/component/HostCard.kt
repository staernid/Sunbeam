package io.github.staernid.sunbeam.features.hosts.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamCard
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable

private val StatusOnlineColor = Color(0xFF22C55E)
private val StatusOfflineColor = Color(0xFF64748B)

val Host.primaryAddress: HostAddress?
    get() = manualAddress ?: localAddress ?: remoteAddress ?: ipv6Address

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HostCard(
    host: Host,
    onHostClick: (Host) -> Unit,
    onHostLongClick: (Host) -> Unit,
    onPairClick: (Host) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    SunbeamCard(
        modifier = modifier
            .fillMaxWidth()
            .sunbeamFocusable(interactionSource = interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onHostClick(host) },
                onLongClick = { onHostLongClick(host) },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            HostCardHeader(host = host, onHostLongClick = onHostLongClick)
            Spacer(modifier = Modifier.height(12.dp))
            HostCardFooter(host = host, onPairClick = onPairClick)

            if (host.currentGameId != 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HostActiveGameBadge(gameId = host.currentGameId)
            }
        }
    }
}

@Composable
private fun HostCardHeader(
    host: Host,
    onHostLongClick: (Host) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = host.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val addressText = host.primaryAddress?.address ?: "No address"
                Text(
                    text = addressText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        IconButton(
            onClick = { onHostLongClick(host) },
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Host options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HostCardFooter(
    host: Host,
    onPairClick: (Host) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HostStatusBadge(state = host.state)

        if (host.pairState == PairState.NOT_PAIRED) {
            Button(
                onClick = { onPairClick(host) },
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.height(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pair",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun HostActiveGameBadge(gameId: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Active Game (ID: $gameId)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun HostStatusBadge(
    state: HostState,
    modifier: Modifier = Modifier,
) {
    val (dotColor, statusLabel) = when (state) {
        HostState.ONLINE -> StatusOnlineColor to "Online"
        HostState.OFFLINE -> StatusOfflineColor to "Offline"
        HostState.UNKNOWN -> StatusOfflineColor to "Unknown"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
