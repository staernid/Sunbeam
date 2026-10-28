package io.github.staernid.sunbeam.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Screen destinations supported in Sunbeam's root navigation graph.
 */
sealed interface SunbeamScreen {
    val stateKey: String

    data object Hosts : SunbeamScreen {
        override val stateKey: String = "screen_hosts"
    }

    data class Apps(
        val hostUuid: String,
    ) : SunbeamScreen {
        override val stateKey: String = "screen_apps_$hostUuid"
    }

    data class Stream(
        val hostUuid: String,
        val appId: Int,
        val appName: String,
    ) : SunbeamScreen {
        override val stateKey: String = "screen_stream_${hostUuid}_$appId"
    }

    data object Settings : SunbeamScreen {
        override val stateKey: String = "screen_settings"
    }

    data object Profiles : SunbeamScreen {
        override val stateKey: String = "screen_profiles"
    }
}

/**
 * Top-level destinations displayed in the navigation bar/rail.
 */
enum class TopLevelTab(
    val title: String,
    val icon: ImageVector,
) {
    HOSTS("Hosts", Icons.Default.Computer),
    PROFILES("Profiles", Icons.Default.SportsEsports),
    SETTINGS("Settings", Icons.Default.Settings),
    ;

    val rootScreen: SunbeamScreen
        get() =
            when (this) {
                HOSTS -> SunbeamScreen.Hosts
                PROFILES -> SunbeamScreen.Profiles
                SETTINGS -> SunbeamScreen.Settings
            }
}

/**
 * Indicates the type of navigation transition for motion animations.
 */
enum class NavigationAction {
    PUSH,
    POP,
    TAB_SWITCH,
}

/**
 * State-backed multi-backstack navigator for Sunbeam that preserves per-tab backstacks.
 */
class SunbeamNavigator(
    initialScreen: SunbeamScreen = SunbeamScreen.Hosts,
) {
    private val tabBackstacks =
        mutableStateMapOf<TopLevelTab, List<SunbeamScreen>>(
            TopLevelTab.HOSTS to listOf(SunbeamScreen.Hosts),
            TopLevelTab.PROFILES to listOf(SunbeamScreen.Profiles),
            TopLevelTab.SETTINGS to listOf(SunbeamScreen.Settings),
        ).apply {
            val startTab = initialScreen.toTab()
            this[startTab] = listOf(initialScreen)
        }

    var currentTab by mutableStateOf(initialScreen.toTab())
        private set

    var lastAction by mutableStateOf(NavigationAction.TAB_SWITCH)
        private set

    val backstack: List<SunbeamScreen>
        get() = tabBackstacks[currentTab] ?: listOf(currentTab.rootScreen)

    val currentScreen: SunbeamScreen
        get() = backstack.last()

    val canNavigateBack: Boolean
        get() = backstack.size > 1

    fun navigateTo(screen: SunbeamScreen) {
        val targetTab = screen.toTab()
        currentTab = targetTab
        val currentStack = tabBackstacks[targetTab] ?: listOf(targetTab.rootScreen)
        if (currentStack.lastOrNull() == screen) return
        lastAction = NavigationAction.PUSH
        tabBackstacks[targetTab] = currentStack + screen
    }

    fun popBack(): Boolean {
        val currentStack = backstack
        if (currentStack.size <= 1) return false
        lastAction = NavigationAction.POP
        tabBackstacks[currentTab] = currentStack.dropLast(1)
        return true
    }

    fun switchTab(tab: TopLevelTab) {
        lastAction = NavigationAction.TAB_SWITCH
        if (currentTab == tab) {
            tabBackstacks[tab] = listOf(tab.rootScreen)
        } else {
            currentTab = tab
        }
    }
}

internal fun SunbeamScreen.toTab(): TopLevelTab =
    when (this) {
        is SunbeamScreen.Hosts,
        is SunbeamScreen.Apps,
        is SunbeamScreen.Stream,
        -> TopLevelTab.HOSTS
        is SunbeamScreen.Profiles -> TopLevelTab.PROFILES
        is SunbeamScreen.Settings -> TopLevelTab.SETTINGS
    }
