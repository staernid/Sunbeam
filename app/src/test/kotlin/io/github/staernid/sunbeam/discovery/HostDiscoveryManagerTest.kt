package io.github.staernid.sunbeam.discovery

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import io.github.staernid.sunbeam.nvstream.http.ComputerDetails
import io.github.staernid.sunbeam.nvstream.http.PairingManager
import io.github.staernid.sunbeam.nvstream.mdns.MdnsComputer
import io.github.staernid.sunbeam.nvstream.mdns.MdnsDiscoveryAgent
import io.github.staernid.sunbeam.nvstream.mdns.MdnsDiscoveryListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetAddress
import java.security.cert.X509Certificate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HostDiscoveryManagerTest {
    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeHostRepository : HostRepository {
        val hosts = mutableMapOf<String, Host>()
        private val mutableFlow = MutableStateFlow<List<Host>>(emptyList())
        override val hostsFlow: Flow<List<Host>> = mutableFlow.asStateFlow()

        override suspend fun getAllHosts(): List<Host> = hosts.values.toList()

        override suspend fun getHostByUuid(uuid: String): Host? = hosts[uuid]

        override suspend fun saveHost(host: Host) {
            hosts[host.uuid] = host
            mutableFlow.value = hosts.values.toList()
        }

        override suspend fun deleteHost(uuid: String) {
            hosts.remove(uuid)
            mutableFlow.value = hosts.values.toList()
        }

        override fun notifyHostUpdated(host: Host) {
            hosts[host.uuid] = host
            mutableFlow.value = hosts.values.toList()
        }
    }

    private class FakeDiscoveryAgent(listener: MdnsDiscoveryListener) : MdnsDiscoveryAgent(listener) {
        var isDiscovering = false
        var lastIntervalMs = 0

        override fun startDiscovery(discoveryIntervalMs: Int) {
            isDiscovering = true
            lastIntervalMs = discoveryIntervalMs
        }

        override fun stopDiscovery() {
            isDiscovering = false
        }

        fun emitComputer(computer: MdnsComputer) {
            listener.notifyComputerAdded(computer)
        }
    }

    private lateinit var repository: FakeHostRepository
    private var agentInstance: FakeDiscoveryAgent? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = FakeHostRepository()
        agentInstance = null
    }

    @Test
    fun startAndStop_togglesDiscoveryAgentLifecycle() = testScope.runTest {
        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            pollingPeriodMs = 5000L,
            discoveryAgentFactory = { _, listener ->
                FakeDiscoveryAgent(listener).also { agentInstance = it }
            },
            externalAddressResolver = { _, _ -> null },
        )

        manager.start()
        testDispatcher.scheduler.runCurrent()
        assertNotNull(agentInstance)
        assertTrue(agentInstance!!.isDiscovering)

        manager.stop()
        testDispatcher.scheduler.runCurrent()
        assertFalse(agentInstance!!.isDiscovering)
    }

    @Test
    fun handleMdnsComputerDiscovered_addsDiscoveredHostToRepository() = testScope.runTest {
        val details = ComputerDetails().apply {
            uuid = "discovered-host-uuid"
            name = "Discovered PC"
            pairState = PairingManager.PairState.NOT_PAIRED
            httpsPort = 47990
        }

        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            discoveryAgentFactory = { _, listener ->
                FakeDiscoveryAgent(listener).also { agentInstance = it }
            },
            detailsPoller = { _, _, _, _ -> details },
            externalAddressResolver = { _, _ -> null },
        )

        manager.start()
        testDispatcher.scheduler.runCurrent()

        val mdnsComputer = MdnsComputer(
            "Discovered PC",
            InetAddress.getByName("192.168.1.100"),
            null,
            47989,
        )
        agentInstance?.emitComputer(mdnsComputer)
        testDispatcher.scheduler.runCurrent()

        val savedHost = repository.getHostByUuid("discovered-host-uuid")
        assertNotNull(savedHost)
        assertEquals("Discovered PC", savedHost?.name)
        assertEquals("192.168.1.100", savedHost?.localAddress?.address)
        assertEquals(HostState.ONLINE, savedHost?.state)

        manager.stop()
        testDispatcher.scheduler.runCurrent()
    }

    @Test
    fun pollAllHostsOnce_updatesHostOnlineState() = testScope.runTest {
        val initialHost = Host(
            uuid = "poll-host-uuid",
            name = "My Rig",
            localAddress = HostAddress("192.168.1.50", 47989),
            state = HostState.UNKNOWN,
        )
        repository.saveHost(initialHost)

        val details = ComputerDetails().apply {
            uuid = "poll-host-uuid"
            name = "My Rig Renamed"
            pairState = PairingManager.PairState.PAIRED
            runningGameId = 42
            httpsPort = 47990
        }

        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            detailsPoller = { _, _, _, _ -> details },
            externalAddressResolver = { _, _ -> null },
        )

        manager.pollAllHostsOnce()
        testDispatcher.scheduler.runCurrent()

        val updated = repository.getHostByUuid("poll-host-uuid")
        assertNotNull(updated)
        assertEquals(HostState.ONLINE, updated?.state)
        assertEquals(PairState.PAIRED, updated?.pairState)
        assertEquals(42, updated?.currentGameId)
        assertEquals("My Rig Renamed", updated?.name)
    }

    @Test
    fun pollAllHostsOnce_marksHostOfflineAfterConsecutiveFailures() = testScope.runTest {
        val initialHost = Host(
            uuid = "offline-test-uuid",
            name = "Offline PC",
            localAddress = HostAddress("192.168.1.50", 47989),
            state = HostState.ONLINE,
        )
        repository.saveHost(initialHost)

        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            // Simulates network failure
            detailsPoller = { _, _, _, _ -> null },
            externalAddressResolver = { _, _ -> null },
        )

        // OFFLINE_POLL_TRIES is 3
        manager.pollAllHostsOnce()
        testDispatcher.scheduler.runCurrent()
        assertEquals(HostState.ONLINE, repository.getHostByUuid("offline-test-uuid")?.state)

        manager.pollAllHostsOnce()
        testDispatcher.scheduler.runCurrent()
        assertEquals(HostState.ONLINE, repository.getHostByUuid("offline-test-uuid")?.state)

        manager.pollAllHostsOnce()
        testDispatcher.scheduler.runCurrent()
        assertEquals(HostState.OFFLINE, repository.getHostByUuid("offline-test-uuid")?.state)
    }

    @Test
    fun addHost_success() = testScope.runTest {
        val details = ComputerDetails().apply {
            uuid = "manual-host-uuid"
            name = "Manual PC"
            pairState = PairingManager.PairState.NOT_PAIRED
            httpsPort = 47990
        }

        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            detailsPoller = { _, _, _, _ -> details },
            externalAddressResolver = { _, _ -> null },
        )

        val result = manager.addHost("192.168.1.120:47989")
        assertTrue(result.isSuccess)
        val host = result.getOrNull()
        assertNotNull(host)
        assertEquals("manual-host-uuid", host?.uuid)
        assertEquals("Manual PC", host?.name)
        assertEquals("192.168.1.120", host?.manualAddress?.address)
        assertEquals(47989, host?.manualAddress?.port)
    }

    @Test
    fun addHost_failsOnInvalidAddress() = testScope.runTest {
        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            externalAddressResolver = { _, _ -> null },
        )

        val result = manager.addHost("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun addHost_failsWhenUnreachable() = testScope.runTest {
        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            detailsPoller = { _, _, _, _ -> null },
            externalAddressResolver = { _, _ -> null },
        )

        val result = manager.addHost("192.168.1.200")
        assertTrue(result.isFailure)
    }

    @Test
    fun pairHost_successWithPairingExecutor() = testScope.runTest {
        val host = Host(
            uuid = "pair-uuid",
            name = "Pairing Host",
            manualAddress = HostAddress("192.168.1.10", 47989),
            pairState = PairState.NOT_PAIRED,
        )
        repository.saveHost(host)

        val fakeCert = generateDummyCert()

        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            pairingExecutor = { _, _ -> Result.success(fakeCert) },
            externalAddressResolver = { _, _ -> null },
        )

        val result = manager.pairHost(host, "1234")
        assertTrue(result.isSuccess)

        val updated = repository.getHostByUuid("pair-uuid")
        assertNotNull(updated)
        assertEquals(PairState.PAIRED, updated?.pairState)
        assertNotNull(updated?.serverCertDer)
    }

    @Test
    fun wakeHost_succeedsWithMacAddressAndFailsWithoutMac() = testScope.runTest {
        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            externalAddressResolver = { _, _ -> null },
        )

        val hostWithMac = Host(
            uuid = "mac-host",
            name = "Host with MAC",
            macAddress = "00:11:22:33:44:55",
        )
        val successResult = manager.wakeHost(hostWithMac)
        assertTrue(successResult.isSuccess)

        val hostWithoutMac = Host(
            uuid = "no-mac-host",
            name = "Host without MAC",
            macAddress = null,
        )
        val failResult = manager.wakeHost(hostWithoutMac)
        assertTrue(failResult.isFailure)
    }

    @Test
    fun removeHost_deletesFromRepository() = testScope.runTest {
        val host = Host(uuid = "delete-uuid", name = "To Delete")
        repository.saveHost(host)
        assertNotNull(repository.getHostByUuid("delete-uuid"))

        val manager = HostDiscoveryManager(
            context = context,
            hostRepository = repository,
            scope = testScope,
            ioDispatcher = testDispatcher,
            externalAddressResolver = { _, _ -> null },
        )

        manager.removeHost("delete-uuid")
        testDispatcher.scheduler.runCurrent()

        assertEquals(null, repository.getHostByUuid("delete-uuid"))
    }

    private fun generateDummyCert(): X509Certificate {
        val crypto = io.github.staernid.sunbeam.binding.PlatformBinding.getCryptoProvider(context)
        return crypto.clientCertificate
    }
}
