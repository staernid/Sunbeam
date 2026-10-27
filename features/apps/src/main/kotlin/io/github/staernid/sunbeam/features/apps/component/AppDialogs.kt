package io.github.staernid.sunbeam.features.apps.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.features.apps.AppsDialogState

@Composable
fun AppsDialogsHost(
    dialogState: AppsDialogState,
    onDismissDialog: () -> Unit,
    onConfirmQuitApp: () -> Unit,
    onConfirmQuitAndLaunch: () -> Unit,
    onLaunchApp: (HostApp) -> Unit,
    onHideApp: (HostApp) -> Unit,
    onUnhideApp: (HostApp) -> Unit,
) {
    when (dialogState) {
        is AppsDialogState.None -> Unit
        is AppsDialogState.QuitConfirmation -> {
            QuitConfirmationDialog(
                app = dialogState.app,
                onConfirm = onConfirmQuitApp,
                onDismiss = onDismissDialog,
            )
        }
        is AppsDialogState.QuitAndLaunchConfirmation -> {
            QuitAndLaunchDialog(
                targetApp = dialogState.targetApp,
                runningApp = dialogState.runningApp,
                onConfirm = onConfirmQuitAndLaunch,
                onDismiss = onDismissDialog,
            )
        }
        is AppsDialogState.AppDetails -> {
            AppDetailsDialog(
                app = dialogState.app,
                onDismiss = onDismissDialog,
                onLaunch = {
                    onDismissDialog()
                    onLaunchApp(dialogState.app)
                },
                onToggleHidden = {
                    if (dialogState.app.isHidden) {
                        onUnhideApp(dialogState.app)
                    } else {
                        onHideApp(dialogState.app)
                    }
                    onDismissDialog()
                },
            )
        }
    }
}

@Composable
fun QuitConfirmationDialog(
    app: HostApp,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Quit ${app.title}?") },
        text = {
            Text(text = "Are you sure you want to quit the running game? Any unsaved progress will be lost.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Quit Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun QuitAndLaunchDialog(
    targetApp: HostApp,
    runningApp: HostApp,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Quit ${runningApp.title} and launch ${targetApp.title}?") },
        text = {
            Text(
                text = "${runningApp.title} is already streaming on this host. " +
                    "Ending that session will allow ${targetApp.title} to start.",
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("Quit & Launch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun AppDetailsDialog(
    app: HostApp,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onToggleHidden: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = app.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppDetailRow(label = "App ID", value = app.id.toString())
                if (app.uuid.isNotBlank()) {
                    AppDetailRow(label = "UUID", value = app.uuid)
                }
                AppDetailRow(
                    label = "HDR Supported",
                    value = if (app.isHdrSupported) "Yes" else "No",
                )
                AppDetailRow(
                    label = "Visibility",
                    value = if (app.isHidden) "Hidden" else "Visible",
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onToggleHidden,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (app.isHidden) "Unhide App" else "Hide App")
                }
            }
        },
        confirmButton = {
            Button(onClick = onLaunch) {
                Text("Launch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun AppDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
