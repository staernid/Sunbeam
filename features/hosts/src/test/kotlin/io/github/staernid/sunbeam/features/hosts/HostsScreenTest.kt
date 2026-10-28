package io.github.staernid.sunbeam.features.hosts

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HostsScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))

    @Test
    fun `empty state is displayed when no hosts exist`() {
        composeTestRule.setContent {
            SunbeamTheme {
                HostsScreen(
                    uiState = HostsUiState(isLoading = false, hosts = emptyList()),
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
            }
        }

        composeTestRule.onNodeWithText("No Sunshine Servers Found").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Host Manually").assertIsDisplayed()
    }

    @Test
    fun `host card displays name, address and status badge`() {
        val host = Host(
            uuid = "h1",
            name = "Desktop Beast",
            manualAddress = HostAddress("192.168.1.15"),
            state = HostState.ONLINE,
            pairState = PairState.PAIRED,
        )

        composeTestRule.setContent {
            SunbeamTheme {
                HostsScreen(
                    uiState = HostsUiState(isLoading = false, hosts = listOf(host)),
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
            }
        }

        composeTestRule.onNodeWithText("Desktop Beast").assertIsDisplayed()
        composeTestRule.onNodeWithText("192.168.1.15").assertIsDisplayed()
        composeTestRule.onNodeWithText("Online").assertIsDisplayed()
    }

    @Test
    fun `unpaired host card displays Pair button`() {
        val host = Host(
            uuid = "h2",
            name = "Laptop",
            manualAddress = HostAddress("192.168.1.20"),
            state = HostState.ONLINE,
            pairState = PairState.NOT_PAIRED,
        )

        composeTestRule.setContent {
            SunbeamTheme {
                HostsScreen(
                    uiState = HostsUiState(isLoading = false, hosts = listOf(host)),
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
            }
        }

        composeTestRule.onNodeWithText("Pair").assertIsDisplayed()
    }

    @Test
    fun `pairing dialog displays pin correctly`() {
        val host = Host(
            uuid = "h3",
            name = "Workstation",
            manualAddress = HostAddress("10.0.0.5"),
            state = HostState.ONLINE,
            pairState = PairState.NOT_PAIRED,
        )

        composeTestRule.setContent {
            SunbeamTheme {
                HostsScreen(
                    uiState = HostsUiState(
                        isLoading = false,
                        hosts = listOf(host),
                        dialogState = HostsDialogState.Pairing(host, "7392"),
                    ),
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
            }
        }

        composeTestRule.onNodeWithText("Pair with Workstation").assertIsDisplayed()
        composeTestRule.onNodeWithText("7392").assertIsDisplayed()
    }

    @Test
    fun `offline paired host with MAC displays Wake button and clicking triggers onWakeClick`() {
        val host = Host(
            uuid = "h4",
            name = "Sleeping PC",
            manualAddress = HostAddress("192.168.1.30"),
            macAddress = "AA:BB:CC:DD:EE:FF",
            state = HostState.OFFLINE,
            pairState = PairState.PAIRED,
        )
        var wokenHost: Host? = null

        composeTestRule.setContent {
            SunbeamTheme {
                HostsScreen(
                    uiState = HostsUiState(isLoading = false, hosts = listOf(host)),
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
                    onWakeClick = { wokenHost = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Wake").assertIsDisplayed().performClick()
        assertEquals(host, wokenHost)
    }

    @Test
    fun `host with active game displays Resume button and title, clicking triggers onResumeClick`() {
        val host = Host(
            uuid = "h5",
            name = "Gaming Rig",
            manualAddress = HostAddress("192.168.1.40"),
            state = HostState.ONLINE,
            pairState = PairState.PAIRED,
            currentGameId = 1234,
            currentGameTitle = "Cyberpunk 2077",
        )
        var resumedHost: Host? = null

        composeTestRule.setContent {
            SunbeamTheme {
                HostsScreen(
                    uiState = HostsUiState(isLoading = false, hosts = listOf(host)),
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
                    onResumeClick = { resumedHost = it },
                )
            }
        }

        composeTestRule.onNodeWithText("Cyberpunk 2077").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume").assertIsDisplayed().performClick()
        assertEquals(host, resumedHost)
    }

    @Test
    fun `help button click in top app bar triggers onHelpClick`() {
        var helpClicked = false

        composeTestRule.setContent {
            SunbeamTheme {
                HostsScreen(
                    uiState = HostsUiState(isLoading = false, hosts = emptyList()),
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
                    onHelpClick = { helpClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Help").assertIsDisplayed().performClick()
        assertTrue(helpClicked)
    }

    @Test
    fun `Help dialog displays setup and troubleshooting information`() {
        composeTestRule.setContent {
            SunbeamTheme {
                HostsScreen(
                    uiState = HostsUiState(
                        isLoading = false,
                        dialogState = HostsDialogState.Help,
                    ),
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
            }
        }

        composeTestRule.onNodeWithText("Setup & Troubleshooting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Official Sunshine Documentation").assertIsDisplayed()
    }

    @Test
    fun `screenshot empty state light and dark oled`() {
        var darkTheme by mutableStateOf(false)
        composeTestRule.setContent {
            SunbeamTheme(darkTheme = darkTheme, dynamicColor = false, oledBlack = darkTheme) {
                HostsScreen(
                    uiState = HostsUiState(isLoading = false, hosts = emptyList()),
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
            }
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/hosts_empty_light.png")
        darkTheme = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/hosts_empty_dark_oled.png")
    }

    @Test
    fun `screenshot populated list and pairing dialog`() {
        val onlineHost = Host(
            uuid = "h1",
            name = "Desktop Beast",
            manualAddress = HostAddress("192.168.1.15"),
            state = HostState.ONLINE,
            pairState = PairState.PAIRED,
            currentGameId = 1234,
            currentGameTitle = "Cyberpunk 2077",
        )
        val unpairedHost = Host(
            uuid = "h2",
            name = "Living Room PC",
            manualAddress = HostAddress("192.168.1.20"),
            state = HostState.ONLINE,
            pairState = PairState.NOT_PAIRED,
        )
        var dialogState by mutableStateOf<HostsDialogState>(HostsDialogState.None)
        var darkTheme by mutableStateOf(false)

        composeTestRule.setContent {
            SunbeamTheme(darkTheme = darkTheme, dynamicColor = false, oledBlack = darkTheme) {
                HostsScreen(
                    uiState = HostsUiState(
                        isLoading = false,
                        hosts = listOf(onlineHost, unpairedHost),
                        dialogState = dialogState,
                    ),
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
            }
        }

        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/hosts_populated_light.png")
        darkTheme = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/hosts_populated_dark_oled.png")

        dialogState = HostsDialogState.Pairing(unpairedHost, "7392")
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isDialog()).captureRoboImage("src/test/screenshots/hosts_pairing_dialog_dark_oled.png")
    }
}
