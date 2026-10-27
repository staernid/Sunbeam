package io.github.staernid.sunbeam.features.hosts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamLoadingIndicator

@Composable
fun AddHostDialog(
    onDismiss: () -> Unit,
    onSubmit: (address: String, name: String) -> Unit,
) {
    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add Host Manually", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(
                    text = "Enter the IP address or hostname of your Sunshine server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("IP Address / Hostname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(address, name) },
                enabled = address.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun PairingDialog(
    host: Host,
    pin: String,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(text = "Pair with ${host.name}", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Open the Sunshine Web UI in your browser and enter this PIN:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                PinDisplayBox(pin = pin)
                Spacer(modifier = Modifier.height(20.dp))
                PairingWaitingIndicator()
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}

@Composable
private fun PinDisplayBox(pin: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            text = pin,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }
}

@Composable
private fun PairingWaitingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SunbeamLoadingIndicator(size = 20.dp, strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Waiting for Sunshine server...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun HostDetailsDialog(
    host: Host,
    onDismiss: () -> Unit,
    onDeleteRequest: (Host) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = host.name, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                DetailRow(label = "State", value = host.state.name)
                DetailRow(label = "Pairing", value = host.pairState.name)

                val addresses = listOfNotNull(
                    host.manualAddress?.address,
                    host.localAddress?.address,
                    host.remoteAddress?.address,
                    host.ipv6Address?.address,
                ).joinToString(", ")
                DetailRow(label = "Addresses", value = addresses.ifEmpty { "None" })

                host.macAddress?.let { mac -> DetailRow(label = "MAC Address", value = mac) }
                if (host.currentGameId != 0) {
                    DetailRow(label = "Active Game ID", value = host.currentGameId.toString())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDeleteRequest(host) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Delete Host")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    host: Host,
    onDismiss: () -> Unit,
    onConfirm: (uuid: String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Host") },
        text = {
            Text("Are you sure you want to forget '${host.name}'? You will need to pair again to stream from this PC.")
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(host.uuid) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
