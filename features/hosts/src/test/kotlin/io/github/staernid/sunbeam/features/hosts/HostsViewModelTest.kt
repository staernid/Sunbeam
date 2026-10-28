package io.github.staernid.sunbeam.features.hosts

import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.data.repository.HostOperationsHandler
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
class HostsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeHostRepository
    private lateinit var viewModel: HostsViewModel

    private val sampleHost = Host(
        uuid = "test-uuid-1",
        name = "Gaming Rig",
        manualAddress = HostAddress("192.168.1.100"),
        state = HostState.ONLINE,
        pairState = PairState.PAIRED,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHostRepository(listOf(sampleHost))
        viewModel = HostsViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads hosts from repository`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.hosts.size)
        assertEquals("Gaming Rig", state.hosts.first().name)
        assertEquals(HostsDialogState.None, state.dialogState)
    }

    @Test
    fun `onAddHostClicked sets dialog state to AddHost`() = runTest(testDispatcher) {
        viewModel.onAddHostClicked()
        assertEquals(HostsDialogState.AddHost, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `onAddHostSubmit adds host to repository and dismisses dialog`() = runTest(testDispatcher) {
        viewModel.onAddHostSubmit("192.168.1.50", "Living Room PC")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.hosts.size)
        assertTrue(state.hosts.any { it.name == "Living Room PC" })
        assertEquals(HostsDialogState.None, state.dialogState)
    }

    @Test
    fun `onAddHostSubmit rejects blank address with user message`() = runTest(testDispatcher) {
        viewModel.onAddHostSubmit("   ", "PC")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.hosts.size)
        assertNotNull(state.userMessage)
    }

    @Test
    fun `onStartPairingClicked generates 4-digit pin and sets Pairing dialog`() = runTest(testDispatcher) {
        viewModel.onStartPairingClicked(sampleHost)

        val dialog = viewModel.uiState.value.dialogState
        assertTrue(dialog is HostsDialogState.Pairing)
        val pairing = dialog as HostsDialogState.Pairing
        assertEquals(sampleHost.uuid, pairing.host.uuid)
        assertEquals(4, pairing.pin.length)
        assertTrue(pairing.pin.toInt() in 1000..9999)
    }

    @Test
    fun `onDeleteHostConfirmed removes host from repository`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.hosts.size)

        viewModel.onDeleteHostConfirmed(sampleHost.uuid)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hosts.isEmpty())
        assertEquals(HostsDialogState.None, state.dialogState)
    }

    @Test
    fun `onHostLongClicked shows host details dialog`() = runTest(testDispatcher) {
        viewModel.onHostLongClicked(sampleHost)

        val dialog = viewModel.uiState.value.dialogState
        assertTrue(dialog is HostsDialogState.HostDetails)
        assertEquals(sampleHost.uuid, (dialog as HostsDialogState.HostDetails).host.uuid)
    }

    @Test
    fun `onHelpClicked sets dialog state to Help`() = runTest(testDispatcher) {
        viewModel.onHelpClicked()
        assertEquals(HostsDialogState.Help, viewModel.uiState.value.dialogState)
    }

    @Test
    fun `onWakeHostClicked sends Wake-on-LAN and notifies user on success`() = runTest(testDispatcher) {
        var wokeHost: Host? = null
        val handler = object : HostOperationsHandler {
            override suspend fun addHost(
                address: String,
                name: String,
            ): Result<Host> = Result.failure(NotImplementedError())

            override suspend fun pairHost(
                host: Host,
                pin: String,
            ): Result<Unit> = Result.success(Unit)

            override suspend fun wakeHost(host: Host): Result<Unit> {
                wokeHost = host
                return Result.success(Unit)
            }
        }
        val vm = HostsViewModel(fakeRepository, handler)
        advanceUntilIdle()

        vm.onWakeHostClicked(sampleHost)
        advanceUntilIdle()

        assertEquals(sampleHost, wokeHost)
        assertEquals("Wake-on-LAN packet sent to ${sampleHost.name}", vm.uiState.value.userMessage)
    }

    @Test
    fun `onWakeHostClicked shows error message when wakeHost fails`() = runTest(testDispatcher) {
        val handler = object : HostOperationsHandler {
            override suspend fun addHost(
                address: String,
                name: String,
            ): Result<Host> = Result.failure(NotImplementedError())

            override suspend fun pairHost(
                host: Host,
                pin: String,
            ): Result<Unit> = Result.success(Unit)

            override suspend fun wakeHost(host: Host): Result<Unit> =
                Result.failure(Exception("Network unreachable"))
        }
        val vm = HostsViewModel(fakeRepository, handler)
        advanceUntilIdle()

        vm.onWakeHostClicked(sampleHost)
        advanceUntilIdle()

        assertEquals("Network unreachable", vm.uiState.value.userMessage)
    }

    @Test
    fun `onDismissDialog resets dialog to None`() = runTest(testDispatcher) {
        viewModel.onAddHostClicked()
        assertEquals(HostsDialogState.AddHost, viewModel.uiState.value.dialogState)

        viewModel.onDismissDialog()
        assertEquals(HostsDialogState.None, viewModel.uiState.value.dialogState)
    }
}
