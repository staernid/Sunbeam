@file:Suppress("LongParameterList")

package io.github.staernid.sunbeam.features.hosts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.designsystem.adaptive.rememberSunbeamAdaptiveInfo
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamCenteredLoadingScreen
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamTopAppBar
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.features.hosts.component.AddHostDialog
import io.github.staernid.sunbeam.features.hosts.component.DeleteConfirmDialog
import io.github.staernid.sunbeam.features.hosts.component.HelpDialog
import io.github.staernid.sunbeam.features.hosts.component.HostCard
import io.github.staernid.sunbeam.features.hosts.component.HostDetailsDialog
import io.github.staernid.sunbeam.features.hosts.component.PairingDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsScreen(
    uiState: HostsUiState,
    windowSizeClass: WindowSizeClass,
    onHostClick: (Host) -> Unit,
    onHostLongClick: (Host) -> Unit,
    onPairClick: (Host) -> Unit,
    onAddHostClick: () -> Unit,
    onAddHostSubmit: (address: String, name: String) -> Unit,
    onDeleteHostRequest: (Host) -> Unit,
    onDeleteHostConfirm: (uuid: String) -> Unit,
    onDismissDialog: () -> Unit,
    onUserMessageDismissed: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWakeClick: (Host) -> Unit = {},
    onResumeClick: (Host) -> Unit = {},
) {
    val adaptiveInfo = rememberSunbeamAdaptiveInfo(windowSizeClass)
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onUserMessageDismissed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SunbeamTopAppBar(
                title = "Sunbeam",
                actions = {
                    IconButton(onClick = onAddHostClick, modifier = Modifier.sunbeamFocusable()) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Host")
                    }
                    IconButton(onClick = onHelpClick, modifier = Modifier.sunbeamFocusable()) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help")
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.sunbeamFocusable()) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        HostsContent(
            uiState = uiState,
            columns = adaptiveInfo.preferredGridColumns,
            onHostClick = onHostClick,
            onHostLongClick = onHostLongClick,
            onPairClick = onPairClick,
            onAddHostClick = onAddHostClick,
            onWakeClick = onWakeClick,
            onResumeClick = onResumeClick,
            modifier = Modifier.padding(innerPadding),
        )
    }

    HostsDialogsHost(
        dialogState = uiState.dialogState,
        onDismissDialog = onDismissDialog,
        onAddHostSubmit = onAddHostSubmit,
        onDeleteHostRequest = onDeleteHostRequest,
        onDeleteHostConfirm = onDeleteHostConfirm,
    )
}

@Composable
private fun HostsContent(
    uiState: HostsUiState,
    columns: Int,
    onHostClick: (Host) -> Unit,
    onHostLongClick: (Host) -> Unit,
    onPairClick: (Host) -> Unit,
    onAddHostClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWakeClick: (Host) -> Unit = {},
    onResumeClick: (Host) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.hosts.isEmpty() -> {
                SunbeamCenteredLoadingScreen()
            }
            uiState.hosts.isEmpty() -> {
                EmptyHostsView(
                    onAddHostClick = onAddHostClick,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items = uiState.hosts, key = { it.uuid }) { host ->
                        HostCard(
                            host = host,
                            onHostClick = onHostClick,
                            onHostLongClick = onHostLongClick,
                            onPairClick = onPairClick,
                            onWakeClick = onWakeClick,
                            onResumeClick = onResumeClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HostsDialogsHost(
    dialogState: HostsDialogState,
    onDismissDialog: () -> Unit,
    onAddHostSubmit: (address: String, name: String) -> Unit,
    onDeleteHostRequest: (Host) -> Unit,
    onDeleteHostConfirm: (uuid: String) -> Unit,
) {
    when (dialogState) {
        is HostsDialogState.AddHost -> {
            AddHostDialog(onDismiss = onDismissDialog, onSubmit = onAddHostSubmit)
        }
        is HostsDialogState.Pairing -> {
            PairingDialog(host = dialogState.host, pin = dialogState.pin, onCancel = onDismissDialog)
        }
        is HostsDialogState.HostDetails -> {
            HostDetailsDialog(
                host = dialogState.host,
                onDismiss = onDismissDialog,
                onDeleteRequest = onDeleteHostRequest,
            )
        }
        is HostsDialogState.DeleteConfirm -> {
            DeleteConfirmDialog(
                host = dialogState.host,
                onDismiss = onDismissDialog,
                onConfirm = onDeleteHostConfirm,
            )
        }
        is HostsDialogState.Help -> {
            HelpDialog(onDismiss = onDismissDialog)
        }
        HostsDialogState.None -> Unit
    }
}

@Composable
fun EmptyHostsView(
    onAddHostClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.CastConnected,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Sunshine Servers Found",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Make sure Sunshine is running on your PC and connected to the same network," +
                " or add your host IP address manually.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddHostClick,
            modifier = Modifier.sunbeamFocusable(),
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Add Host Manually")
        }
    }
}
