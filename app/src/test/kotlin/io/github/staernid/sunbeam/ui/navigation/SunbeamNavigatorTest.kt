package io.github.staernid.sunbeam.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SunbeamNavigatorTest {
    @Test
    fun testInitialState() {
        val navigator = SunbeamNavigator()
        assertEquals(SunbeamScreen.Hosts, navigator.currentScreen)
        assertEquals(TopLevelTab.HOSTS, navigator.currentTab)
        assertFalse(navigator.canNavigateBack)
        assertEquals(1, navigator.backstack.size)
    }

    @Test
    fun testNavigateToAndPopBack() {
        val navigator = SunbeamNavigator()
        val appsScreen = SunbeamScreen.Apps("host-uuid-123")
        navigator.navigateTo(appsScreen)

        assertEquals(appsScreen, navigator.currentScreen)
        assertEquals(TopLevelTab.HOSTS, navigator.currentTab)
        assertEquals(NavigationAction.PUSH, navigator.lastAction)
        assertTrue(navigator.canNavigateBack)
        assertEquals(2, navigator.backstack.size)

        val streamScreen = SunbeamScreen.Stream("host-uuid-123", 42, "Steam")
        navigator.navigateTo(streamScreen)

        assertEquals(streamScreen, navigator.currentScreen)
        assertEquals(3, navigator.backstack.size)

        assertTrue(navigator.popBack())
        assertEquals(appsScreen, navigator.currentScreen)
        assertEquals(NavigationAction.POP, navigator.lastAction)

        assertTrue(navigator.popBack())
        assertEquals(SunbeamScreen.Hosts, navigator.currentScreen)

        assertFalse(navigator.popBack())
        assertEquals(SunbeamScreen.Hosts, navigator.currentScreen)
    }

    @Test
    fun testSwitchTabPreservesPerTabBackstackAndResetsOnReselect() {
        val navigator = SunbeamNavigator()
        val appsScreen = SunbeamScreen.Apps("host-1")
        navigator.navigateTo(appsScreen)

        navigator.switchTab(TopLevelTab.SETTINGS)
        assertEquals(SunbeamScreen.Settings, navigator.currentScreen)
        assertEquals(TopLevelTab.SETTINGS, navigator.currentTab)
        assertEquals(NavigationAction.TAB_SWITCH, navigator.lastAction)
        assertFalse(navigator.canNavigateBack)

        navigator.switchTab(TopLevelTab.PROFILES)
        assertEquals(SunbeamScreen.Profiles, navigator.currentScreen)
        assertEquals(TopLevelTab.PROFILES, navigator.currentTab)
        assertFalse(navigator.canNavigateBack)

        // Switching back to HOSTS restores its nested Apps destination
        navigator.switchTab(TopLevelTab.HOSTS)
        assertEquals(appsScreen, navigator.currentScreen)
        assertEquals(TopLevelTab.HOSTS, navigator.currentTab)
        assertTrue(navigator.canNavigateBack)

        // Re-selecting the active HOSTS tab resets its stack to root
        navigator.switchTab(TopLevelTab.HOSTS)
        assertEquals(SunbeamScreen.Hosts, navigator.currentScreen)
        assertEquals(TopLevelTab.HOSTS, navigator.currentTab)
        assertFalse(navigator.canNavigateBack)
    }

    @Test
    fun testIgnoreDuplicateNavigation() {
        val navigator = SunbeamNavigator()
        navigator.navigateTo(SunbeamScreen.Hosts)
        assertEquals(1, navigator.backstack.size)

        val appsScreen = SunbeamScreen.Apps("host-1")
        navigator.navigateTo(appsScreen)
        navigator.navigateTo(appsScreen)
        assertEquals(2, navigator.backstack.size)
    }
}
