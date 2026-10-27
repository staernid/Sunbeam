@file:Suppress("EmptyFunctionBlock")

package io.github.staernid.sunbeam.ui

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostApp
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        )

    private val testApp =
        HostApp(
            id = 200,
            title = "Cyberpunk 2077",
        )

    private val fakeHostRepo =
        object : HostRepository {
            override val hostsFlow: Flow<List<Host>> = flowOf(listOf(testHost))

            override suspend fun getAllHosts(): List<Host> = listOf(testHost)

            override suspend fun getHostByUuid(uuid: String): Host? = if (uuid == testHost.uuid) testHost else null

            override suspend fun saveHost(host: Host) {}

            override suspend fun deleteHost(uuid: String) {}
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
    fun testTabSwitching() {
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

        // Switch to Profiles tab
        composeTestRule.onNodeWithText("Profiles").performClick()
        assertEquals(TopLevelTab.PROFILES, navigator.currentTab)
        assertEquals(SunbeamScreen.Profiles, navigator.currentScreen)

        // Switch to Settings tab
        composeTestRule.onNodeWithText("Settings").performClick()
        assertEquals(TopLevelTab.SETTINGS, navigator.currentTab)
        assertEquals(SunbeamScreen.Settings, navigator.currentScreen)
    }

    @Test
    fun testHostToAppsToStreamNavigation() {
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

        // Click on the test host to navigate to AppsScreen
        composeTestRule.onNodeWithText("Desktop Rig").performClick()
        assertEquals(SunbeamScreen.Apps("host-1"), navigator.currentScreen)

        // Click on the test app to navigate to StreamScreen
        composeTestRule.onNodeWithText("Cyberpunk 2077").performClick()
        assertTrue(navigator.currentScreen is SunbeamScreen.Stream)
        val stream = navigator.currentScreen as SunbeamScreen.Stream
        assertEquals("host-1", stream.hostUuid)
        assertEquals(200, stream.appId)
        assertEquals("Cyberpunk 2077", stream.appName)
    }
}
