@file:Suppress(
    "LongMethod",
    "LongParameterList",
)

package io.github.staernid.sunbeam.features.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.designsystem.adaptive.rememberSunbeamAdaptiveInfo
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamCenteredLoadingScreen
import io.github.staernid.sunbeam.core.designsystem.component.SunbeamTopAppBar
import io.github.staernid.sunbeam.features.apps.component.AppCard
import io.github.staernid.sunbeam.features.apps.component.AppsDialogsHost
import io.github.staernid.sunbeam.features.apps.component.RunningAppBanner

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
                title = uiState.host?.name ?: "Apps",
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                showHiddenApps = uiState.showHiddenApps,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onBackClick = onBackClick,
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
    title: String,
    isSearchActive: Boolean,
    searchQuery: String,
    showHiddenApps: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleShowHiddenApps: () -> Unit,
) {
    if (isSearchActive) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search games...") },
            leadingIcon = {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close search",
                    )
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
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
        SunbeamTopAppBar(
            title = title,
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearchToggle) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { onMenuExpandedChange(true) }) {
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
        )
    }
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
        Button(onClick = onRefresh) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Refresh Apps")
        }
    }
}
