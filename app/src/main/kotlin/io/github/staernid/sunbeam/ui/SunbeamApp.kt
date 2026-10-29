@file:Suppress(
    "LongMethod",
    "CyclomaticComplexMethod",
    "LongParameterList",
)

package io.github.staernid.sunbeam.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.staernid.sunbeam.SunbeamApplication
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.model.HostState
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
import io.github.staernid.sunbeam.di.AppGraph
import io.github.staernid.sunbeam.features.apps.AppsScreen
import io.github.staernid.sunbeam.features.apps.AppsViewModel
import io.github.staernid.sunbeam.features.hosts.HostsScreen
import io.github.staernid.sunbeam.features.hosts.HostsViewModel
import io.github.staernid.sunbeam.features.profiles.ProfileDialogState
import io.github.staernid.sunbeam.features.profiles.ProfilesScreen
import io.github.staernid.sunbeam.features.profiles.ProfilesViewModel
import io.github.staernid.sunbeam.features.settings.SettingsScreen
import io.github.staernid.sunbeam.features.settings.SettingsViewModel
import io.github.staernid.sunbeam.ui.navigation.NavigationAction
import io.github.staernid.sunbeam.ui.navigation.SunbeamNavigator
import io.github.staernid.sunbeam.ui.navigation.SunbeamScreen
import io.github.staernid.sunbeam.ui.navigation.TopLevelTab
import io.github.staernid.sunbeam.ui.stream.StreamSessionLauncher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TRANSITION_DURATION_MS = 220
private const val SLIDE_OFFSET_DIVISOR = 6

/**
 * Root composable shell for Sunbeam hosting adaptive navigation, screen switching,
 * and the streaming session lifecycle.
 */
@Composable
fun SunbeamApp(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    navigator: SunbeamNavigator = remember { SunbeamNavigator() },
    appGraph: AppGraph? = (LocalContext.current.applicationContext as? SunbeamApplication)?.appGraph,
    hostRepository: HostRepository = appGraph?.hostRepository
        ?: error("HostRepository must be provided via AppGraph or parameter"),
    appRepository: AppRepository = appGraph?.appRepository
        ?: error("AppRepository must be provided via AppGraph or parameter"),
    preferencesRepository: PreferencesRepository = appGraph?.preferencesRepository
        ?: error("PreferencesRepository must be provided via AppGraph or parameter"),
    profileRepository: ProfileRepository = appGraph?.profileRepository
        ?: error("ProfileRepository must be provided via AppGraph or parameter"),
    streamLauncher: StreamSessionLauncher = appGraph?.streamLauncher
        ?: error("StreamSessionLauncher must be provided via AppGraph or parameter"),
    streamSession: StreamSession? = null,
    hostOperationsHandler: HostOperationsHandler? = appGraph?.hostOperationsHandler,
) {
    val adaptiveInfo = rememberSunbeamAdaptiveInfo(windowSizeClass)
    val saveableStateHolder = rememberSaveableStateHolder()
    val hosts by hostRepository.hostsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val pairedHosts = remember(hosts) { hosts.filter { it.pairState == PairState.PAIRED } }
    var hasAutoRouted by rememberSaveable {
        mutableStateOf(navigator.currentScreen != SunbeamScreen.Hosts)
    }

    LaunchedEffect(pairedHosts) {
        if (!hasAutoRouted && navigator.currentScreen == SunbeamScreen.Hosts && pairedHosts.isNotEmpty()) {
            val settings = preferencesRepository.getSettings()
            val lastSelectedHost = settings.lastSelectedHostUuid?.let { lastUuid ->
                pairedHosts.find { it.uuid == lastUuid }
            }
            val onlinePairedHosts = pairedHosts.filter { it.state == HostState.ONLINE }
            val targetHost = lastSelectedHost ?: onlinePairedHosts.singleOrNull()
            if (targetHost != null) {
                hasAutoRouted = true
                navigator.navigateTo(SunbeamScreen.Apps(targetHost.uuid))
            }
        }
    }

    // Handle system back navigation
    BackHandler(enabled = navigator.canNavigateBack) {
        val poppedKey = navigator.currentScreen.stateKey
        if (navigator.popBack()) {
            saveableStateHolder.removeState(poppedKey)
        }
    }

    val currentScreen = navigator.currentScreen

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
                    streamLauncher = streamLauncher,
                    navigator = navigator,
                    saveableStateHolder = saveableStateHolder,
                    streamSession = streamSession,
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
                    streamLauncher = streamLauncher,
                    navigator = navigator,
                    saveableStateHolder = saveableStateHolder,
                    streamSession = streamSession,
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
    streamLauncher: StreamSessionLauncher,
    navigator: SunbeamNavigator,
    saveableStateHolder: SaveableStateHolder,
    streamSession: StreamSession? = null,
    hostOperationsHandler: HostOperationsHandler? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val hosts by hostRepository.hostsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val pairedHosts = remember(hosts) { hosts.filter { it.pairState == PairState.PAIRED } }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            when (navigator.lastAction) {
                NavigationAction.PUSH -> {
                    (
                        fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) +
                            slideInHorizontally(animationSpec = tween(TRANSITION_DURATION_MS)) {
                                it / SLIDE_OFFSET_DIVISOR
                            }
                    ).togetherWith(
                        fadeOut(animationSpec = tween(TRANSITION_DURATION_MS)) +
                            slideOutHorizontally(animationSpec = tween(TRANSITION_DURATION_MS)) {
                                -it / SLIDE_OFFSET_DIVISOR
                            },
                    )
                }

                NavigationAction.POP -> {
                    (
                        fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) +
                            slideInHorizontally(animationSpec = tween(TRANSITION_DURATION_MS)) {
                                -it / SLIDE_OFFSET_DIVISOR
                            }
                    ).togetherWith(
                        fadeOut(animationSpec = tween(TRANSITION_DURATION_MS)) +
                            slideOutHorizontally(animationSpec = tween(TRANSITION_DURATION_MS)) {
                                it / SLIDE_OFFSET_DIVISOR
                            },
                    )
                }

                NavigationAction.TAB_SWITCH -> {
                    fadeIn(animationSpec = tween(TRANSITION_DURATION_MS))
                        .togetherWith(fadeOut(animationSpec = tween(TRANSITION_DURATION_MS)))
                }
            }
        },
        label = "SunbeamScreenTransition",
    ) { targetScreen ->
        saveableStateHolder.SaveableStateProvider(key = targetScreen.stateKey) {
            when (targetScreen) {
                is SunbeamScreen.Hosts -> {
                    val hostsViewModel: HostsViewModel =
                        viewModel(key = "HostsViewModel") {
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
                                coroutineScope.launch {
                                    preferencesRepository.updateSettings {
                                        it.copy(lastSelectedHostUuid = host.uuid)
                                    }
                                }
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
                        onHelpClick = hostsViewModel::onHelpClicked,
                        onWakeClick = hostsViewModel::onWakeHostClicked,
                        onResumeClick = { host ->
                            navigator.navigateTo(
                                SunbeamScreen.Stream(
                                    hostUuid = host.uuid,
                                    appId = host.currentGameId,
                                    appName = host.currentGameTitle ?: "Active Game",
                                ),
                            )
                        },
                    )
                }

                is SunbeamScreen.Apps -> {
                    val appsViewModel: AppsViewModel =
                        viewModel(key = "AppsViewModel_${targetScreen.hostUuid}") {
                            AppsViewModel(targetScreen.hostUuid, hostRepository, appRepository)
                        }
                    val appsUiState by appsViewModel.uiState.collectAsStateWithLifecycle()
                    val context = LocalContext.current

                    val launchStream: (HostApp) -> Unit = { app ->
                        val host = appsUiState.host
                        coroutineScope.launch {
                            val targetHost = host ?: hostRepository.getHostByUuid(targetScreen.hostUuid)
                            if (targetHost != null) {
                                streamLauncher.launchGameActivity(context, targetHost, app)
                            }
                        }
                    }

                    AppsScreen(
                        uiState = appsUiState,
                        windowSizeClass = windowSizeClass,
                        onBackClick = {
                            val poppedKey = targetScreen.stateKey
                            if (navigator.popBack()) {
                                saveableStateHolder.removeState(poppedKey)
                            }
                        },
                        onAppClick = { app ->
                            appsViewModel.onAppClick(app) { _, targetApp ->
                                launchStream(targetApp)
                            }
                        },
                        onAppLongClick = appsViewModel::onAppLongClick,
                        onResumeClick = launchStream,
                        onQuitClick = appsViewModel::onQuitRunningAppClick,
                        onRefresh = appsViewModel::onRefresh,
                        onSearchQueryChange = appsViewModel::onSearchQueryChange,
                        onSearchActiveChange = appsViewModel::onSearchActiveChange,
                        onToggleShowHiddenApps = appsViewModel::onToggleShowHiddenApps,
                        onDismissDialog = appsViewModel::onDismissDialog,
                        onConfirmQuitApp = appsViewModel::onConfirmQuitApp,
                        onConfirmQuitAndLaunch = {
                            appsViewModel.onConfirmQuitAndLaunch { _, targetApp ->
                                launchStream(targetApp)
                            }
                        },
                        onLaunchApp = launchStream,
                        onHideApp = appsViewModel::onHideApp,
                        onUnhideApp = appsViewModel::onUnhideApp,
                        onUserMessageDismissed = appsViewModel::onUserMessageDismissed,
                        pairedHosts = pairedHosts,
                        onSwitchHostClick = { targetHost ->
                            coroutineScope.launch {
                                preferencesRepository.updateSettings {
                                    it.copy(lastSelectedHostUuid = targetHost.uuid)
                                }
                            }
                            navigator.navigateTo(SunbeamScreen.Apps(targetHost.uuid))
                        },
                        onManageHostsClick = {
                            navigator.switchTab(TopLevelTab.HOSTS)
                        },
                    )
                }

                is SunbeamScreen.Settings -> {
                    val settingsViewModel: SettingsViewModel =
                        viewModel(key = "SettingsViewModel") {
                            SettingsViewModel(preferencesRepository)
                        }
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
                    val profilesViewModel: ProfilesViewModel =
                        viewModel(key = "ProfilesViewModel") {
                            ProfilesViewModel(profileRepository, preferencesRepository)
                        }
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
                        onUpdateGamepadState = profilesViewModel::updateGamepadState,
                        onResetOscLayout = profilesViewModel::resetOscLayout,
                        onToggleOscEnabled = profilesViewModel::setOscEnabled,
                        onToggleHideOscWhenHasGamepad = profilesViewModel::setHideOscWhenHasGamepad,
                        onToggleOnlyShowL3R3 = profilesViewModel::setOnlyShowL3R3,
                        onToggleShowGuideButton = profilesViewModel::setShowGuideButton,
                        onToggleVibrateOsc = profilesViewModel::setVibrateOsc,
                    )
                }

                is SunbeamScreen.Stream -> {
                    val context = LocalContext.current
                    LaunchedEffect(targetScreen.hostUuid, targetScreen.appId) {
                        val configResult =
                            streamLauncher.resolveStreamConfig(
                                targetScreen.hostUuid,
                                targetScreen.appId,
                            )
                        configResult.onSuccess { config ->
                            streamSession?.start(config)
                        }
                        val host = hostRepository.getHostByUuid(targetScreen.hostUuid)
                        val apps = appRepository.getApps(targetScreen.hostUuid, showHidden = true).first()
                        val app = apps.find { it.id == targetScreen.appId }
                            ?: HostApp(
                                id = targetScreen.appId,
                                title = targetScreen.appName.ifBlank { host?.currentGameTitle ?: "Active Game" },
                            )
                        if (host != null) {
                            streamLauncher.launchGameActivity(context, host, app)
                            if (!navigator.popBack()) {
                                navigator.switchTab(TopLevelTab.HOSTS)
                            }
                        }
                    }
                }
            }
        }
    }
}
