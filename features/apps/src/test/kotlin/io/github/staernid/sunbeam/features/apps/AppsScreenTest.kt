package io.github.staernid.sunbeam.features.apps

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
class AppsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))
    private val testHost = Host(uuid = "h1", name = "Gaming-Rig", currentGameId = 101)
    private val steamApp = HostApp(id = 101, title = "Steam Big Picture", isHdrSupported = true)
    private val cyberpunkApp = HostApp(id = 102, title = "Cyberpunk 2077")

    @Test
    fun `empty state is displayed when no apps exist`() {
        composeTestRule.setContent {
            SunbeamTheme {
                AppsScreen(
                    uiState = AppsUiState(host = testHost, apps = emptyList(), isLoading = false),
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
            }
        }

        composeTestRule.onNodeWithText("No Games or Apps Found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Refresh Apps").assertIsDisplayed()
    }

    @Test
    fun `app cards and running banner are displayed when apps exist`() {
        composeTestRule.setContent {
            SunbeamTheme {
                AppsScreen(
                    uiState = AppsUiState(
                        host = testHost,
                        apps = listOf(steamApp, cyberpunkApp),
                        isLoading = false,
                    ),
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
            }
        }

        // Running Banner
        composeTestRule.onNodeWithText("Now Streaming").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quit Session").assertIsDisplayed()

        // Game cards
        composeTestRule.onAllNodesWithText("Steam Big Picture")[1].assertIsDisplayed()
        composeTestRule.onNodeWithText("RUNNING").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cyberpunk 2077").assertIsDisplayed()
    }

    @Test
    fun `quit confirmation dialog displays warning message and buttons`() {
        composeTestRule.setContent {
            SunbeamTheme {
                AppsScreen(
                    uiState = AppsUiState(
                        host = testHost.copy(currentGameId = 0),
                        apps = listOf(steamApp),
                        dialogState = AppsDialogState.QuitConfirmation(steamApp),
                    ),
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
            }
        }

        composeTestRule.onNodeWithText("Quit Steam Big Picture?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quit Session").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun `app details dialog displays metadata and actions`() {
        composeTestRule.setContent {
            SunbeamTheme {
                AppsScreen(
                    uiState = AppsUiState(
                        host = testHost,
                        apps = listOf(steamApp),
                        dialogState = AppsDialogState.AppDetails(steamApp),
                    ),
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
            }
        }

        composeTestRule.onNodeWithText("App ID").assertIsDisplayed()
        composeTestRule.onNodeWithText("101").assertIsDisplayed()
        composeTestRule.onNodeWithText("HDR Supported").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hide App").assertIsDisplayed()
        composeTestRule.onNodeWithText("Launch").assertIsDisplayed()
    }
}
