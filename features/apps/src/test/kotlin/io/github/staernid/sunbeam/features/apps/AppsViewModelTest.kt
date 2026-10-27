package io.github.staernid.sunbeam.features.apps

import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val host = Host(uuid = "host-1", name = "Desktop-PC", currentGameId = 0)
    private val steamApp = HostApp(id = 101, title = "Steam Big Picture")
    private val hadesApp = HostApp(id = 102, title = "Hades II", isHidden = true)
    private val cyberpunkApp = HostApp(id = 103, title = "Cyberpunk 2077")

    private lateinit var hostRepo: FakeHostRepository
    private lateinit var appRepo: FakeAppRepository
    private lateinit var viewModel: AppsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        hostRepo = FakeHostRepository(listOf(host))
        appRepo = FakeAppRepository(listOf(steamApp, hadesApp, cyberpunkApp))
        viewModel = AppsViewModel(
            hostUuid = "host-1",
            hostRepository = hostRepo,
            appRepository = appRepo,
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads host and apps from repositories`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Desktop-PC", state.host?.name)
        assertEquals(3, state.apps.size)
        // Hidden app filtered by default
        assertEquals(2, state.filteredApps.size)
    }

    @Test
    fun `search query filters apps correctly`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onSearchQueryChange("steam")
        val state = viewModel.uiState.value

        assertEquals("steam", state.searchQuery)
        assertEquals(1, state.filteredApps.size)
        assertEquals(101, state.filteredApps[0].id)
    }

    @Test
    fun `toggle show hidden apps reveals hidden games`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onToggleShowHiddenApps()
        val state = viewModel.uiState.value

        assertTrue(state.showHiddenApps)
        assertEquals(3, state.filteredApps.size)
    }

    @Test
    fun `onAppClick launches directly when host is idle`() = runTest(testDispatcher) {
        advanceUntilIdle()

        var launched = false
        viewModel.onAppClick(steamApp) { _, app ->
            if (app.id == steamApp.id) launched = true
        }

        assertTrue(launched)
        assertEquals(AppsDialogState.None, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `onAppClick shows QuitAndLaunch dialog when different game is streaming`() = runTest(testDispatcher) {
        val busyHost = host.copy(currentGameId = 103)
        hostRepo.saveHost(busyHost)
        advanceUntilIdle()

        var launched = false
        viewModel.onAppClick(steamApp) { _, _ -> launched = true }

        assertFalse(launched)
        val dialog = viewModel.uiState.value.dialogState
        assertTrue(dialog is AppsDialogState.QuitAndLaunchConfirmation)
        val quitAndLaunch = dialog as AppsDialogState.QuitAndLaunchConfirmation
        assertEquals(101, quitAndLaunch.targetApp.id)
        assertEquals(103, quitAndLaunch.runningApp.id)
    }

    @Test
    fun `confirm quit and launch terminates current session and launches target app`() = runTest(testDispatcher) {
        val busyHost = host.copy(currentGameId = 103)
        hostRepo.saveHost(busyHost)
        advanceUntilIdle()

        viewModel.onAppClick(steamApp) { _, _ -> }
        advanceUntilIdle()

        var launchedApp: HostApp? = null
        viewModel.onConfirmQuitAndLaunch { _, app ->
            launchedApp = app
        }
        advanceUntilIdle()

        assertTrue(appRepo.quitAppCalled)
        assertEquals(steamApp, launchedApp)
        assertEquals(AppsDialogState.None, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `onQuitRunningAppClick shows QuitConfirmation dialog`() = runTest(testDispatcher) {
        val busyHost = host.copy(currentGameId = 103)
        hostRepo.saveHost(busyHost)
        advanceUntilIdle()

        viewModel.onQuitRunningAppClick()
        val dialog = viewModel.uiState.value.dialogState

        assertTrue(dialog is AppsDialogState.QuitConfirmation)
        assertEquals(103, (dialog as AppsDialogState.QuitConfirmation).app.id)
    }

    @Test
    fun `onConfirmQuitApp calls repository to quit stream`() = runTest(testDispatcher) {
        val busyHost = host.copy(currentGameId = 103)
        hostRepo.saveHost(busyHost)
        advanceUntilIdle()

        viewModel.onQuitRunningAppClick()
        viewModel.onConfirmQuitApp()
        advanceUntilIdle()

        assertTrue(appRepo.quitAppCalled)
        assertEquals(AppsDialogState.None, viewModel.uiState.value.dialogState)
        assertNotNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `onAppLongClick shows AppDetails dialog`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onAppLongClick(steamApp)
        val dialog = viewModel.uiState.value.dialogState

        assertTrue(dialog is AppsDialogState.AppDetails)
        assertEquals(101, (dialog as AppsDialogState.AppDetails).app.id)
    }

    @Test
    fun `onHideApp and onUnhideApp update app visibility`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onHideApp(steamApp)
        advanceUntilIdle()
        val allApps = viewModel.uiState.value.apps
        assertTrue(allApps.first { it.id == 101 }.isHidden)

        viewModel.onUnhideApp(hadesApp)
        advanceUntilIdle()
        val visible = viewModel.uiState.value.filteredApps
        assertTrue(visible.any { it.id == 102 })
    }
}
