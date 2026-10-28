@file:Suppress(
    "LongMethod",
    "LongParameterList",
)

package io.github.staernid.sunbeam.features.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.designsystem.adaptive.rememberSunbeamAdaptiveInfo
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamCenteredLoadingScreen
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.features.apps.component.AppCard
import io.github.staernid.sunbeam.features.apps.component.AppsDialogsHost
import io.github.staernid.sunbeam.features.apps.component.RunningAppBanner

private val StatusOnlineColor = Color(0xFF22C55E)
private val StatusOfflineColor = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    uiState: AppsUiState,
    windowSizeClass: WindowSizeClass,
    onBackClick: () -> Unit,
    onAppClick: (HostApp) -> Unit,
    onAppLongClick: (HostApp) -> Unit,
    onResumeClick: (HostApp) -> Unit,
    onQuitClick: () -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onToggleShowHiddenApps: () -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmQuitApp: () -> Unit,
    onConfirmQuitAndLaunch: () -> Unit,
    onLaunchApp: (HostApp) -> Unit,
    onHideApp: (HostApp) -> Unit,
    onUnhideApp: (HostApp) -> Unit,
    onUserMessageDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    pairedHosts: List<Host> = emptyList(),
    onSwitchHostClick: (Host) -> Unit = {},
    onManageHostsClick: () -> Unit = onBackClick,
) {
    val adaptiveInfo = rememberSunbeamAdaptiveInfo(windowSizeClass)
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    uiState.userMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onUserMessageDismissed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppsTopBar(
                currentHost = uiState.host,
                title = uiState.host?.name ?: "Apps",
                pairedHosts = pairedHosts,
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                showHiddenApps = uiState.showHiddenApps,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onBackClick = onBackClick,
                onSwitchHostClick = onSwitchHostClick,
                onManageHostsClick = onManageHostsClick,
                onSearchToggle = { onSearchActiveChange(!uiState.isSearchActive) },
                onSearchQueryChange = onSearchQueryChange,
                onRefresh = onRefresh,
                onToggleShowHiddenApps = {
                    menuExpanded = false
                    onToggleShowHiddenApps()
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        AppsContent(
            uiState = uiState,
            columns = adaptiveInfo.preferredGridColumns,
            onAppClick = onAppClick,
            onAppLongClick = onAppLongClick,
            onResumeClick = onResumeClick,
            onQuitClick = onQuitClick,
            onRefresh = onRefresh,
            modifier = Modifier.padding(innerPadding),
        )
    }

    AppsDialogsHost(
        dialogState = uiState.dialogState,
        onDismissDialog = onDismissDialog,
        onConfirmQuitApp = onConfirmQuitApp,
        onConfirmQuitAndLaunch = onConfirmQuitAndLaunch,
        onLaunchApp = onLaunchApp,
        onHideApp = onHideApp,
        onUnhideApp = onUnhideApp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsTopBar(
    currentHost: Host?,
    title: String,
    pairedHosts: List<Host>,
    isSearchActive: Boolean,
    searchQuery: String,
    showHiddenApps: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onSwitchHostClick: (Host) -> Unit,
    onManageHostsClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleShowHiddenApps: () -> Unit,
) {
    var hostSwitcherExpanded by remember { mutableStateOf(false) }

    if (isSearchActive) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search games...") },
            leadingIcon = {
                IconButton(onClick = onSearchToggle, modifier = Modifier.sunbeamFocusable()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close search",
                    )
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.sunbeamFocusable()) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    } else {
        TopAppBar(
            title = {
                HostSwitcherPill(
                    title = title,
                    isOnline = currentHost?.state != HostState.OFFLINE,
                    expanded = hostSwitcherExpanded,
                    pairedHosts = pairedHosts,
                    currentHostUuid = currentHost?.uuid,
                    onExpandedChange = { hostSwitcherExpanded = it },
                    onSwitchHostClick = { host ->
                        hostSwitcherExpanded = false
                        onSwitchHostClick(host)
                    },
                    onManageHostsClick = {
                        hostSwitcherExpanded = false
                        onManageHostsClick()
                    },
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick, modifier = Modifier.sunbeamFocusable()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearchToggle, modifier = Modifier.sunbeamFocusable()) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = onRefresh, modifier = Modifier.sunbeamFocusable()) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { onMenuExpandedChange(true) }, modifier = Modifier.sunbeamFocusable()) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { onMenuExpandedChange(false) },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (showHiddenApps) "Hide hidden apps" else "Show hidden apps")
                        },
                        onClick = onToggleShowHiddenApps,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

@Composable
private fun HostSwitcherPill(
    title: String,
    isOnline: Boolean,
    expanded: Boolean,
    pairedHosts: List<Host>,
    currentHostUuid: String?,
    onExpandedChange: (Boolean) -> Unit,
    onSwitchHostClick: (Host) -> Unit,
    onManageHostsClick: () -> Unit,
) {
    Box {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .sunbeamFocusable(shape = CircleShape)
                .clip(CircleShape)
                .clickable { onExpandedChange(true) },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                HostStatusDot(isOnline = isOnline)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Switch host",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            val otherHosts = pairedHosts.filter { it.uuid != currentHostUuid }
            otherHosts.forEach { host ->
                DropdownMenuItem(
                    text = { Text(host.name) },
                    leadingIcon = {
                        HostStatusDot(isOnline = host.state == HostState.ONLINE)
                    },
                    onClick = { onSwitchHostClick(host) },
                )
            }
            if (otherHosts.isNotEmpty()) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Manage All Hosts") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                    )
                },
                onClick = onManageHostsClick,
            )
        }
    }
}

@Composable
private fun HostStatusDot(isOnline: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isOnline) StatusOnlineColor else StatusOfflineColor),
    )
}

@Composable
private fun AppsContent(
    uiState: AppsUiState,
    columns: Int,
    onAppClick: (HostApp) -> Unit,
    onAppLongClick: (HostApp) -> Unit,
    onResumeClick: (HostApp) -> Unit,
    onQuitClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.apps.isEmpty() -> {
                SunbeamCenteredLoadingScreen()
            }

            uiState.filteredApps.isEmpty() -> {
                EmptyAppsView(
                    onRefresh = onRefresh,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    uiState.runningApp?.let { running ->
                        RunningAppBanner(
                            runningApp = running,
                            onResumeClick = { onResumeClick(running) },
                            onQuitClick = onQuitClick,
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(
                            items = uiState.filteredApps,
                            key = { it.id },
                        ) { app ->
                            val isRunning = uiState.host?.currentGameId == app.id
                            AppCard(
                                app = app,
                                isRunning = isRunning,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAppsView(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.SportsEsports,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Games or Apps Found",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Make sure Sunshine is running on your PC with applications configured.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRefresh,
            modifier = Modifier.sunbeamFocusable(),
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Refresh Apps")
        }
    }
}
