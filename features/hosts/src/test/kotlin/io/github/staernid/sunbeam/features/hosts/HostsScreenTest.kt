package io.github.staernid.sunbeam.features.hosts

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(RobolectricTestRunner::class)
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
}
