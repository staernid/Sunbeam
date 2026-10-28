@file:Suppress("EmptyFunctionBlock")

package io.github.staernid.sunbeam.ui

import android.app.Application
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.Game
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.data.model.Profile
import io.github.staernid.sunbeam.core.data.repository.AppRepository
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import io.github.staernid.sunbeam.core.data.repository.ProfileRepository
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.ui.navigation.SunbeamNavigator
import io.github.staernid.sunbeam.ui.navigation.SunbeamScreen
import io.github.staernid.sunbeam.ui.navigation.TopLevelTab
import io.github.staernid.sunbeam.ui.stream.StreamSessionLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
class SunbeamAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testHost =
        Host(
            uuid = "host-1",
            name = "Desktop Rig",
            localAddress = HostAddress("192.168.1.100"),
            pairState = PairState.PAIRED,
        )

    private val secondHost =
        Host(
            uuid = "host-2",
            name = "Living Room PC",
            localAddress = HostAddress("192.168.1.101"),
            pairState = PairState.PAIRED,
        )

    private val testApp =
        HostApp(
            id = 200,
            title = "Cyberpunk 2077",
        )

    private var currentHosts: List<Host> = listOf(testHost)

    private val fakeHostRepo =
        object : HostRepository {
            override val hostsFlow: Flow<List<Host>>
                get() = flowOf(currentHosts)

            override suspend fun getAllHosts(): List<Host> = currentHosts

            override suspend fun getHostByUuid(uuid: String): Host? = currentHosts.find { it.uuid == uuid }

            override suspend fun saveHost(host: Host) {}

            override suspend fun deleteHost(uuid: String) {}

            override fun notifyHostUpdated(host: Host) {}
        }

    private val fakeAppRepo =
        object : AppRepository {
            override fun getApps(
                hostUuid: String,
                showHidden: Boolean,
            ): Flow<List<HostApp>> =
                flowOf(listOf(testApp))

            override suspend fun refreshApps(host: Host): Result<List<HostApp>> = Result.success(listOf(testApp))

            override suspend fun setAppHidden(
                hostUuid: String,
                appId: Int,
                hidden: Boolean,
            ) {}

            override suspend fun quitCurrentApp(host: Host): Result<Unit> = Result.success(Unit)

            override fun getBoxArtFile(
                hostUuid: String,
                appId: Int,
            ): File? = null
        }

    private val fakePrefsRepo =
        object : PreferencesRepository {
            private var current = StreamSettings()
            override val settingsFlow: Flow<StreamSettings> = flowOf(current)

            override suspend fun getSettings(): StreamSettings = current

            override suspend fun updateSettings(transform: (StreamSettings) -> StreamSettings): StreamSettings {
                current = transform(current)
                return current
            }

            override suspend fun setSettings(settings: StreamSettings) {
                current = settings
            }
        }

    private val fakeProfileRepo =
        object : ProfileRepository {
            override val profilesFlow: Flow<List<Profile>> = flowOf(emptyList())
            override val activeProfileFlow: Flow<Profile?> = flowOf(null)

            override suspend fun getAllProfiles(): List<Profile> = emptyList()

            override suspend fun getProfile(uuid: UUID): Profile? = null

            override suspend fun saveProfile(profile: Profile) {}

            override suspend fun deleteProfile(uuid: UUID) {}

            override suspend fun setActiveProfile(uuid: UUID?) {}
        }

    private val streamLauncher = StreamSessionLauncher(fakeHostRepo, fakeAppRepo, fakePrefsRepo, fakeProfileRepo)

    @Test
    fun testTabSwitchingAndBackstackPreservation() {
        currentHosts = listOf(testHost.copy(state = HostState.ONLINE))
        val navigator = SunbeamNavigator()
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))

        composeTestRule.setContent {
            SunbeamTheme {
                SunbeamApp(
                    windowSizeClass = windowSizeClass,
                    hostRepository = fakeHostRepo,
                    appRepository = fakeAppRepo,
                    preferencesRepository = fakePrefsRepo,
                    profileRepository = fakeProfileRepo,
                    streamLauncher = streamLauncher,
                    navigator = navigator,
                )
            }
        }

        composeTestRule.onNodeWithText("Hosts").assertIsDisplayed()
        composeTestRule.onNodeWithText("Profiles").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Single paired online host auto-routes directly to AppsScreen on launch
        composeTestRule.waitForIdle()
        assertEquals(SunbeamScreen.Apps("host-1"), navigator.currentScreen)
        composeTestRule.onNodeWithText("Desktop Rig").assertIsDisplayed()

        // Switch to Profiles tab
        composeTestRule.onNodeWithText("Profiles").performClick()
        assertEquals(TopLevelTab.PROFILES, navigator.currentTab)
        assertEquals(SunbeamScreen.Profiles, navigator.currentScreen)

        // Switch to Settings tab
        composeTestRule.onNodeWithText("Settings").performClick()
        assertEquals(TopLevelTab.SETTINGS, navigator.currentTab)
        assertEquals(SunbeamScreen.Settings, navigator.currentScreen)

        // Switch back to Hosts tab — Apps("host-1") backstack entry is preserved
        composeTestRule.onNodeWithText("Hosts").performClick()
        assertEquals(TopLevelTab.HOSTS, navigator.currentTab)
        assertEquals(SunbeamScreen.Apps("host-1"), navigator.currentScreen)
        composeTestRule.onNodeWithText("Cyberpunk 2077").assertIsDisplayed()
    }

    @Test
    fun testHostToAppsToStreamNavigation() {
        currentHosts = listOf(testHost, secondHost)
        val navigator = SunbeamNavigator()
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))

        composeTestRule.setContent {
            SunbeamTheme {
                SunbeamApp(
                    windowSizeClass = windowSizeClass,
                    hostRepository = fakeHostRepo,
                    appRepository = fakeAppRepo,
                    preferencesRepository = fakePrefsRepo,
                    profileRepository = fakeProfileRepo,
                    streamLauncher = streamLauncher,
                    navigator = navigator,
                )
            }
        }

        // With 2 online hosts and no lastSelectedHostUuid, starts on HostsScreen
        assertEquals(SunbeamScreen.Hosts, navigator.currentScreen)

        // Click on the test host to navigate to AppsScreen
        composeTestRule.onNodeWithText("Desktop Rig").performClick()
        assertEquals(SunbeamScreen.Apps("host-1"), navigator.currentScreen)

        // Click on the test app to launch the streaming session
        composeTestRule.onNodeWithText("Cyberpunk 2077").performClick()
        val startedIntent = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Game::class.java.name, startedIntent.component?.className)
        assertEquals(200, startedIntent.getIntExtra(Game.EXTRA_APP_ID, 0))
        assertEquals("Cyberpunk 2077", startedIntent.getStringExtra(Game.EXTRA_APP_NAME))
        assertEquals(SunbeamScreen.Apps("host-1"), navigator.currentScreen)
    }

    @Test
    fun testHostSwitcherPillSwitchesHostAndNavigatesToManageHosts() {
        currentHosts = listOf(testHost, secondHost)
        val navigator = SunbeamNavigator(initialScreen = SunbeamScreen.Apps("host-1"))
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))

        composeTestRule.setContent {
            SunbeamTheme {
                SunbeamApp(
                    windowSizeClass = windowSizeClass,
                    hostRepository = fakeHostRepo,
                    appRepository = fakeAppRepo,
                    preferencesRepository = fakePrefsRepo,
                    profileRepository = fakeProfileRepo,
                    streamLauncher = streamLauncher,
                    navigator = navigator,
                )
            }
        }

        // Open Host Switcher menu in AppsTopBar and switch to Living Room PC
        composeTestRule.onNodeWithText("Desktop Rig").performClick()
        composeTestRule.onNodeWithText("Living Room PC").performClick()
        assertEquals(SunbeamScreen.Apps("host-2"), navigator.currentScreen)

        // Open Host Switcher menu again and tap Manage All Hosts
        composeTestRule.onNodeWithText("Living Room PC").performClick()
        composeTestRule.onNodeWithText("Manage All Hosts").performClick()
        assertEquals(SunbeamScreen.Hosts, navigator.currentScreen)
    }

    @Test
    fun testDirectStreamScreenNavigationLaunchesActivity() {
        val navigator = SunbeamNavigator(
            initialScreen = SunbeamScreen.Stream("host-1", 200, "Cyberpunk 2077"),
        )
        val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))

        composeTestRule.setContent {
            SunbeamTheme {
                SunbeamApp(
                    windowSizeClass = windowSizeClass,
                    hostRepository = fakeHostRepo,
                    appRepository = fakeAppRepo,
                    preferencesRepository = fakePrefsRepo,
                    profileRepository = fakeProfileRepo,
                    streamLauncher = streamLauncher,
                    navigator = navigator,
                )
            }
        }

        val startedIntent = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Game::class.java.name, startedIntent.component?.className)
        assertEquals(200, startedIntent.getIntExtra(Game.EXTRA_APP_ID, 0))
        assertEquals(SunbeamScreen.Hosts, navigator.currentScreen)
    }
}
