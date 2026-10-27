package io.github.staernid.sunbeam.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Screen destinations supported in Sunbeam's root navigation graph.
 */
sealed interface SunbeamScreen {
    data object Hosts : SunbeamScreen

    data class Apps(
        val hostUuid: String,
    ) : SunbeamScreen

    data class Stream(
        val hostUuid: String,
        val appId: Int,
        val appName: String,
    ) : SunbeamScreen

    data object Settings : SunbeamScreen

    data object Profiles : SunbeamScreen
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
}

/**
 * Lightweight, state-backed backstack navigator for Sunbeam.
 */
class SunbeamNavigator(
    initialScreen: SunbeamScreen = SunbeamScreen.Hosts,
) {
    var backstack by mutableStateOf(listOf(initialScreen))
        private set

    val currentScreen: SunbeamScreen
        get() = backstack.last()

    val canNavigateBack: Boolean
        get() = backstack.size > 1

    val currentTab: TopLevelTab
        get() =
            when (currentScreen) {
                is SunbeamScreen.Hosts,
                is SunbeamScreen.Apps,
                is SunbeamScreen.Stream,
                -> TopLevelTab.HOSTS
                is SunbeamScreen.Profiles -> TopLevelTab.PROFILES
                is SunbeamScreen.Settings -> TopLevelTab.SETTINGS
            }

    fun navigateTo(screen: SunbeamScreen) {
        if (backstack.lastOrNull() == screen) return
        backstack = backstack + screen
    }

    fun popBack(): Boolean {
        if (backstack.size <= 1) return false
        backstack = backstack.dropLast(1)
        return true
    }

    fun switchTab(tab: TopLevelTab) {
        val targetScreen: SunbeamScreen =
            when (tab) {
                TopLevelTab.HOSTS -> SunbeamScreen.Hosts
                TopLevelTab.PROFILES -> SunbeamScreen.Profiles
                TopLevelTab.SETTINGS -> SunbeamScreen.Settings
            }
        // If already on the target tab, reset backstack to its root
        backstack = listOf(targetScreen)
    }
}
