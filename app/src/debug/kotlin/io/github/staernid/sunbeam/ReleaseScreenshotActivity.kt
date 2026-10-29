package io.github.staernid.sunbeam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.features.apps.AppsScreen
import io.github.staernid.sunbeam.features.apps.AppsUiState
import io.github.staernid.sunbeam.features.hosts.HostsScreen
import io.github.staernid.sunbeam.features.hosts.HostsUiState
import io.github.staernid.sunbeam.features.profiles.ProfilesScreen
import io.github.staernid.sunbeam.features.profiles.ProfilesTab
import io.github.staernid.sunbeam.features.profiles.ProfilesUiState
import io.github.staernid.sunbeam.features.profiles.model.createNewProfile
import io.github.staernid.sunbeam.features.settings.SettingsScreen
import io.github.staernid.sunbeam.features.settings.SettingsUiState

class ReleaseScreenshotActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val screen = requireNotNull(intent.getStringExtra(EXTRA_SCREEN)) {
            "Missing required screenshot screen"
        }
        require(screen in SCREEN_NAMES) { "Unknown screenshot screen: $screen" }
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(411.dp, 914.dp))

        setContent {
            SunbeamTheme(darkTheme = true, dynamicColor = false) {
                ScreenshotScreen(screen, windowSizeClass)
            }
        }
    }

    private companion object {
        const val EXTRA_SCREEN = "screen"
        val SCREEN_NAMES = setOf("hosts", "apps", "profiles", "settings")
    }
}

@Composable
private fun ScreenshotScreen(
    screen: String,
    windowSizeClass: WindowSizeClass,
) {
    when (screen) {
        "hosts" -> HostsScreen(
            uiState = HostsUiState(hosts = screenshotHosts),
            windowSizeClass = windowSizeClass,
            onHostClick = {},
            onHostLongClick = {},
            onPairClick = {},
            onAddHostClick = {},
            onAddHostSubmit = { _, _ -> },
            onDeleteHostRequest = {},
            onDeleteHostConfirm = {},
            onDismissDialog = {},
            onUserMessageDismissed = {},
            onSettingsClick = {},
            onHelpClick = {},
        )

        "apps" -> AppsScreen(
            uiState = AppsUiState(host = screenshotHosts.first(), apps = screenshotApps),
            windowSizeClass = windowSizeClass,
            onBackClick = {},
            onAppClick = {},
            onAppLongClick = {},
            onResumeClick = {},
            onQuitClick = {},
            onRefresh = {},
            onSearchQueryChange = {},
            onSearchActiveChange = {},
            onToggleShowHiddenApps = {},
            onDismissDialog = {},
            onConfirmQuitApp = {},
            onConfirmQuitAndLaunch = {},
            onLaunchApp = {},
            onHideApp = {},
            onUnhideApp = {},
            onUserMessageDismissed = {},
        )

        "profiles" -> ProfilesScreen(
            uiState = ProfilesUiState(
                profiles = screenshotProfiles,
                activeProfileId = screenshotProfiles.first().uuid,
                isLoading = false,
                selectedTab = ProfilesTab.PROFILES,
            ),
            onTabSelected = {},
            onAddProfileClick = {},
            onActivateProfile = {},
            onEditProfileClick = {},
            onDuplicateProfileClick = {},
            onDeleteProfileClick = {},
            onSaveProfile = { _, _, _, _, _, _, _, _ -> },
            onConfirmDeleteProfile = {},
            onDismissDialog = {},
            onTestRumble = {},
            onOscOpacityChange = {},
            onOscScaleChange = {},
            onFeedbackMessageShown = {},
        )

        "settings" -> SettingsScreen(
            uiState = SettingsUiState(),
            onBackClick = {},
            onOpenDialog = {},
            onDismissDialog = {},
            onBitrateChange = {},
            onDeadzoneChange = {},
            onOscOpacityChange = {},
            onResolutionSelect = { _, _ -> },
            onFpsSelect = {},
            onVideoScaleSelect = {},
            onVideoFormatSelect = {},
            onAudioConfigSelect = {},
            onFramePacingSelect = {},
            onUpdateSetting = {},
            onConfirmReset = {},
            onUserMessageDismissed = {},
        )
    }
}

private val screenshotHosts = listOf(
    Host(
        uuid = "screenshot-host-01",
        name = "Living Room PC",
        manualAddress = HostAddress("192.0.2.10"),
        state = HostState.ONLINE,
        pairState = PairState.PAIRED,
        currentGameId = 101,
        currentGameTitle = "Steam Big Picture",
    ),
    Host(
        uuid = "screenshot-host-02",
        name = "Studio Desktop",
        manualAddress = HostAddress("192.0.2.20"),
        state = HostState.ONLINE,
        pairState = PairState.PAIRED,
    ),
)

private val screenshotApps = listOf(
    HostApp(id = 101, title = "Steam Big Picture", isHdrSupported = true),
    HostApp(id = 102, title = "Cyberpunk 2077"),
    HostApp(id = 103, title = "Desktop"),
    HostApp(id = 104, title = "The Witcher 3"),
)

private val screenshotProfiles = listOf(
    createNewProfile(
        name = "Balanced 1440p",
        width = 2560,
        height = 1440,
        fps = 60,
        bitrateKbps = 35_000,
        oscEnabled = true,
    ),
    createNewProfile(
        name = "Low Latency",
        width = 1920,
        height = 1080,
        fps = 120,
        bitrateKbps = 25_000,
        oscEnabled = false,
    ),
)
