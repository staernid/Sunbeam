@file:Suppress(
    "LongMethod",
    "CyclomaticComplexMethod",
)

package io.github.staernid.sunbeam.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.data.repository.AppRepository
import io.github.staernid.sunbeam.core.data.repository.HostOperationsHandler
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import io.github.staernid.sunbeam.core.data.repository.ProfileRepository
import io.github.staernid.sunbeam.core.designsystem.adaptive.SunbeamNavigationType
import io.github.staernid.sunbeam.core.designsystem.adaptive.rememberSunbeamAdaptiveInfo
import io.github.staernid.sunbeam.core.designsystem.focus.sunbeamFocusable
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.core.stream.StreamSession
import io.github.staernid.sunbeam.features.apps.AppsScreen
import io.github.staernid.sunbeam.features.apps.AppsViewModel
import io.github.staernid.sunbeam.features.hosts.HostsScreen
import io.github.staernid.sunbeam.features.hosts.HostsViewModel
import io.github.staernid.sunbeam.features.profiles.ProfileDialogState
import io.github.staernid.sunbeam.features.profiles.ProfilesScreen
import io.github.staernid.sunbeam.features.profiles.ProfilesViewModel
import io.github.staernid.sunbeam.features.settings.SettingsScreen
import io.github.staernid.sunbeam.features.settings.SettingsViewModel
import io.github.staernid.sunbeam.features.stream.StreamScreen
import io.github.staernid.sunbeam.features.stream.StreamViewModel
import io.github.staernid.sunbeam.ui.navigation.SunbeamNavigator
import io.github.staernid.sunbeam.ui.navigation.SunbeamScreen
import io.github.staernid.sunbeam.ui.navigation.TopLevelTab
import io.github.staernid.sunbeam.ui.stream.StreamSessionLauncher
import kotlinx.coroutines.launch

/**
 * Root composable shell for Sunbeam hosting adaptive navigation, screen switching,
 * and the streaming session lifecycle.
 */
@Composable
fun SunbeamApp(
    windowSizeClass: WindowSizeClass,
    hostRepository: HostRepository,
    appRepository: AppRepository,
    preferencesRepository: PreferencesRepository,
    profileRepository: ProfileRepository,
    streamLauncher: StreamSessionLauncher,
    modifier: Modifier = Modifier,
    navigator: SunbeamNavigator = remember { SunbeamNavigator() },
    streamSession: StreamSession? = null,
    hostOperationsHandler: HostOperationsHandler? = null,
) {
    val adaptiveInfo = rememberSunbeamAdaptiveInfo(windowSizeClass)
    val coroutineScope = rememberCoroutineScope()

    // Handle system back navigation
    BackHandler(enabled = navigator.canNavigateBack) {
        navigator.popBack()
    }

    val currentScreen = navigator.currentScreen

    // Fullscreen streaming screen bypasses navigation chrome
    if (currentScreen is SunbeamScreen.Stream) {
        val streamViewModel = remember { StreamViewModel() }

        streamViewModel.onDisconnectAction = {
            coroutineScope.launch {
                streamSession?.stop()
                navigator.popBack()
            }
        }
        streamViewModel.onQuitAction = {
            coroutineScope.launch {
                streamSession?.stop()
                navigator.popBack()
            }
        }

        LaunchedEffect(currentScreen.hostUuid, currentScreen.appId) {
            val configResult = streamLauncher.resolveStreamConfig(currentScreen.hostUuid, currentScreen.appId)
            configResult.onSuccess { config ->
                streamSession?.start(config)
            }
        }

        StreamScreen(
            onSurfaceCreated = {},
            onSurfaceChanged = { _, _, _, _ -> },
            onSurfaceDestroyed = {},
            viewModel = streamViewModel,
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    // Adaptive navigation chrome (Rail vs Bottom Bar)
    val isRail = adaptiveInfo.navigationType == SunbeamNavigationType.NavigationRail ||
        adaptiveInfo.navigationType == SunbeamNavigationType.PermanentNavigationDrawer

    if (isRail) {
        Row(modifier = modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
            ) {
                TopLevelTab.entries.forEach { tab ->
                    val selected = navigator.currentTab == tab
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navigator.switchTab(tab) },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        modifier = Modifier.sunbeamFocusable(),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                ScreenContent(
                    screen = currentScreen,
                    windowSizeClass = windowSizeClass,
                    hostRepository = hostRepository,
                    appRepository = appRepository,
                    preferencesRepository = preferencesRepository,
                    profileRepository = profileRepository,
                    navigator = navigator,
                    hostOperationsHandler = hostOperationsHandler,
                )
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        val selected = navigator.currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigator.switchTab(tab) },
                            icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            modifier = Modifier.sunbeamFocusable(),
                        )
                    }
                }
            },
            modifier = modifier.fillMaxSize(),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                ScreenContent(
                    screen = currentScreen,
                    windowSizeClass = windowSizeClass,
                    hostRepository = hostRepository,
                    appRepository = appRepository,
                    preferencesRepository = preferencesRepository,
                    profileRepository = profileRepository,
                    navigator = navigator,
                    hostOperationsHandler = hostOperationsHandler,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ScreenContent(
    screen: SunbeamScreen,
    windowSizeClass: WindowSizeClass,
    hostRepository: HostRepository,
    appRepository: AppRepository,
    preferencesRepository: PreferencesRepository,
    profileRepository: ProfileRepository,
    navigator: SunbeamNavigator,
    hostOperationsHandler: HostOperationsHandler? = null,
) {
    when (screen) {
        is SunbeamScreen.Hosts -> {
            val hostsViewModel = remember(hostRepository, hostOperationsHandler) {
                HostsViewModel(hostRepository, hostOperationsHandler)
            }
            val hostsUiState by hostsViewModel.uiState.collectAsStateWithLifecycle()

            HostsScreen(
                uiState = hostsUiState,
                windowSizeClass = windowSizeClass,
                onHostClick = { host ->
                    if (host.pairState == PairState.NOT_PAIRED) {
                        hostsViewModel.onStartPairingClicked(host)
                    } else {
                        navigator.navigateTo(SunbeamScreen.Apps(host.uuid))
                    }
                },
                onHostLongClick = hostsViewModel::onHostLongClicked,
                onPairClick = hostsViewModel::onStartPairingClicked,
                onAddHostClick = hostsViewModel::onAddHostClicked,
                onAddHostSubmit = hostsViewModel::onAddHostSubmit,
                onDeleteHostRequest = hostsViewModel::onDeleteHostRequested,
                onDeleteHostConfirm = hostsViewModel::onDeleteHostConfirmed,
                onDismissDialog = hostsViewModel::onDismissDialog,
                onUserMessageDismissed = hostsViewModel::onUserMessageDismissed,
                onSettingsClick = { navigator.switchTab(TopLevelTab.SETTINGS) },
                onHelpClick = {},
            )
        }

        is SunbeamScreen.Apps -> {
            val appsViewModel = remember(screen.hostUuid, hostRepository, appRepository) {
                AppsViewModel(screen.hostUuid, hostRepository, appRepository)
            }
            val appsUiState by appsViewModel.uiState.collectAsStateWithLifecycle()

            val navigateToStream: (HostApp) -> Unit = { app ->
                navigator.navigateTo(
                    SunbeamScreen.Stream(
                        hostUuid = screen.hostUuid,
                        appId = app.id,
                        appName = app.title,
                    ),
                )
            }

            AppsScreen(
                uiState = appsUiState,
                windowSizeClass = windowSizeClass,
                onBackClick = { navigator.popBack() },
                onAppClick = { app ->
                    appsViewModel.onAppClick(app) { _, targetApp ->
                        navigateToStream(targetApp)
                    }
                },
                onAppLongClick = appsViewModel::onAppLongClick,
                onResumeClick = navigateToStream,
                onQuitClick = appsViewModel::onQuitRunningAppClick,
                onRefresh = appsViewModel::onRefresh,
                onSearchQueryChange = appsViewModel::onSearchQueryChange,
                onSearchActiveChange = appsViewModel::onSearchActiveChange,
                onToggleShowHiddenApps = appsViewModel::onToggleShowHiddenApps,
                onDismissDialog = appsViewModel::onDismissDialog,
                onConfirmQuitApp = appsViewModel::onConfirmQuitApp,
                onConfirmQuitAndLaunch = {
                    appsViewModel.onConfirmQuitAndLaunch { _, targetApp ->
                        navigateToStream(targetApp)
                    }
                },
                onLaunchApp = navigateToStream,
                onHideApp = appsViewModel::onHideApp,
                onUnhideApp = appsViewModel::onUnhideApp,
                onUserMessageDismissed = appsViewModel::onUserMessageDismissed,
            )
        }

        is SunbeamScreen.Settings -> {
            val settingsViewModel = remember(preferencesRepository) { SettingsViewModel(preferencesRepository) }
            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            SettingsScreen(
                uiState = settingsUiState,
                onBackClick = { navigator.popBack() },
                onOpenDialog = settingsViewModel::openDialog,
                onDismissDialog = settingsViewModel::dismissDialog,
                onBitrateChange = settingsViewModel::updateBitrateMbps,
                onDeadzoneChange = settingsViewModel::updateDeadzone,
                onOscOpacityChange = settingsViewModel::updateOscOpacity,
                onResolutionSelect = settingsViewModel::updateResolution,
                onFpsSelect = settingsViewModel::updateFps,
                onVideoScaleSelect = settingsViewModel::updateVideoScaleMode,
                onVideoFormatSelect = settingsViewModel::updateVideoFormat,
                onAudioConfigSelect = settingsViewModel::updateAudioConfig,
                onFramePacingSelect = settingsViewModel::updateFramePacing,
                onUpdateSetting = settingsViewModel::updateSetting,
                onConfirmReset = settingsViewModel::resetToDefaults,
                onUserMessageDismissed = settingsViewModel::dismissUserMessage,
            )
        }

        is SunbeamScreen.Profiles -> {
            val profilesViewModel = remember(profileRepository) { ProfilesViewModel(profileRepository) }
            val profilesUiState by profilesViewModel.uiState.collectAsStateWithLifecycle()

            ProfilesScreen(
                uiState = profilesUiState,
                onTabSelected = profilesViewModel::selectTab,
                onAddProfileClick = profilesViewModel::showCreateDialog,
                onActivateProfile = profilesViewModel::setActiveProfile,
                onEditProfileClick = profilesViewModel::showEditDialog,
                onDuplicateProfileClick = profilesViewModel::duplicateProfile,
                onDeleteProfileClick = profilesViewModel::showDeleteDialog,
                onSaveProfile = { name, width, height, fps, bitrateKbps, stretchMode, oscEnabled, oscOpacity ->
                    val activeDialog = profilesUiState.dialogState
                    if (activeDialog is ProfileDialogState.Edit) {
                        profilesViewModel.updateProfile(
                            uuid = activeDialog.profile.uuid,
                            name = name,
                            width = width,
                            height = height,
                            fps = fps,
                            bitrateKbps = bitrateKbps,
                            stretchMode = stretchMode,
                            oscEnabled = oscEnabled,
                            oscOpacity = oscOpacity,
                        )
                    } else {
                        profilesViewModel.createProfile(
                            name = name,
                            width = width,
                            height = height,
                            fps = fps,
                            bitrateKbps = bitrateKbps,
                            stretchMode = stretchMode,
                            oscEnabled = oscEnabled,
                            oscOpacity = oscOpacity,
                        )
                    }
                },
                onConfirmDeleteProfile = profilesViewModel::deleteProfile,
                onDismissDialog = profilesViewModel::dismissDialog,
                onTestRumble = profilesViewModel::setRumbleStrength,
                onOscOpacityChange = profilesViewModel::setOscOpacity,
                onOscScaleChange = profilesViewModel::setOscScale,
                onFeedbackMessageShown = profilesViewModel::clearFeedbackMessage,
            )
        }

        is SunbeamScreen.Stream -> Unit
    }
}
