@file:Suppress(
    "LongMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "TooGenericExceptionCaught",
    "TooManyFunctions",
    "SwallowedException",
    "MagicNumber",
    "LongParameterList",
    "CyclomaticComplexMethod",
)

package io.github.staernid.sunbeam.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.os.Build
import io.github.staernid.sunbeam.LimeLog
import io.github.staernid.sunbeam.binding.PlatformBinding
import io.github.staernid.sunbeam.computers.IdentityManager
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.data.model.SunshineServerCapabilities
import io.github.staernid.sunbeam.core.data.repository.HostOperationsHandler
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import io.github.staernid.sunbeam.nvstream.NvConnection
import io.github.staernid.sunbeam.nvstream.http.ComputerDetails
import io.github.staernid.sunbeam.nvstream.http.LimelightCryptoProvider
import io.github.staernid.sunbeam.nvstream.http.NvHTTP
import io.github.staernid.sunbeam.nvstream.http.PairingManager
import io.github.staernid.sunbeam.nvstream.mdns.JmDNSDiscoveryAgent
import io.github.staernid.sunbeam.nvstream.mdns.MdnsComputer
import io.github.staernid.sunbeam.nvstream.mdns.MdnsDiscoveryAgent
import io.github.staernid.sunbeam.nvstream.mdns.MdnsDiscoveryListener
import io.github.staernid.sunbeam.nvstream.mdns.NsdManagerDiscoveryAgent
import io.github.staernid.sunbeam.nvstream.wol.WakeOnLanSender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.Inet4Address
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap

/**
 * Modern coroutine- and Flow-based manager for host discovery (mDNS), periodic serverinfo polling,
 * and active host network operations (manual addition, pairing, WoL, deletion) directly backed
 * by [HostRepository].
 */
class HostDiscoveryManager(
    private val context: Context,
    private val hostRepository: HostRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pollingPeriodMs: Long = DEFAULT_POLLING_PERIOD_MS,
    private val discoveryAgentFactory: ((Context, MdnsDiscoveryListener) -> MdnsDiscoveryAgent)? = null,
    private val detailsPoller: (
        suspend (
            address: HostAddress,
            httpsPort: Int,
            serverCert: X509Certificate?,
            likelyOnline: Boolean,
        ) -> ComputerDetails?
    )? = null,
    private val pairingExecutor: (
        suspend (
            host: Host,
            pin: String,
        ) -> Result<X509Certificate>
    )? = null,
    private val externalAddressResolver: ((host: String, port: Int) -> String?)? = null,
) : HostOperationsHandler {
    private var isRunning = false
    private var pollingJob: Job? = null
    private var discoveryAgent: MdnsDiscoveryAgent? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val offlineCounts = ConcurrentHashMap<String, Int>()

    private val uniqueId: String by lazy {
        IdentityManager(context).uniqueId
    }

    private val cryptoProvider: LimelightCryptoProvider by lazy {
        PlatformBinding.getCryptoProvider(context)
    }

    fun start() {
        if (isRunning) return
        isRunning = true

        startMdnsDiscovery()
        registerNetworkCallback()
        startPollingLoop()
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false

        pollingJob?.cancel()
        pollingJob = null

        discoveryAgent?.stopDiscovery()
        discoveryAgent = null

        unregisterNetworkCallback()
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    private fun startMdnsDiscovery() {
        val listener = object : MdnsDiscoveryListener {
            override fun notifyComputerAdded(computer: MdnsComputer) {
                scope.launch(ioDispatcher) {
                    handleMdnsComputerDiscovered(computer)
                }
            }

            override fun notifyDiscoveryFailure(e: Exception) {
                LimeLog.warning("mDNS auto-discovery error: ${e.message}")
            }
        }

        val agent = discoveryAgentFactory?.invoke(context, listener) ?: run {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                JmDNSDiscoveryAgent(context.applicationContext, listener)
            } else {
                NsdManagerDiscoveryAgent(context.applicationContext, listener)
            }
        }

        discoveryAgent = agent
        try {
            agent.startDiscovery(MDNS_QUERY_PERIOD_MS)
        } catch (e: Exception) {
            LimeLog.warning("Failed to start mDNS discovery agent: ${e.message}")
        }
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = scope.launch(ioDispatcher) {
            while (isActive && isRunning) {
                try {
                    val hosts = hostRepository.getAllHosts()
                    for (host in hosts) {
                        if (!isActive || !isRunning) break
                        pollHost(host)
                    }
                } catch (e: Exception) {
                    LimeLog.warning("Error during host polling loop: ${e.message}")
                }
                delay(pollingPeriodMs)
            }
        }
    }

    suspend fun pollAllHostsOnce() = withContext(ioDispatcher) {
        val hosts = hostRepository.getAllHosts()
        for (host in hosts) {
            pollHost(host)
        }
    }

    private suspend fun pollHost(host: Host) {
        val addresses = listOfNotNull(
            host.localAddress,
            host.manualAddress,
            host.remoteAddress,
            host.ipv6Address,
        ).distinct()

        if (addresses.isEmpty()) {
            recordOffline(host)
            return
        }

        val cert = parseDerCert(host.serverCertDer)
        val isLikelyOnline = host.state == HostState.ONLINE

        val deferredResults = addresses.map { addr ->
            scope.async(ioDispatcher) {
                addr to tryPollAddress(addr, host.httpsPort, cert, isLikelyOnline)
            }
        }

        val results = deferredResults.awaitAll()
        val successfulResult = results.firstOrNull { (_, details) ->
            details != null && (details.uuid == host.uuid || host.uuid.isEmpty())
        }

        if (successfulResult != null) {
            val (activeAddr, details) = successfulResult
            offlineCounts[host.uuid] = 0

            val pairState = when (details?.pairState) {
                PairingManager.PairState.PAIRED -> PairState.PAIRED
                PairingManager.PairState.NOT_PAIRED -> PairState.NOT_PAIRED
                else -> if (host.serverCertDer != null) PairState.PAIRED else PairState.NOT_PAIRED
            }

            val resolvedHttpsPort = if (details?.httpsPort != null && details.httpsPort != 0) {
                details.httpsPort
            } else {
                host.httpsPort
            }

            val updated = host.copy(
                name = details?.name ?: host.name,
                state = HostState.ONLINE,
                pairState = pairState,
                currentGameId = details?.runningGameId ?: 0,
                currentGameUuid = details?.runningGameUUID,
                httpsPort = resolvedHttpsPort,
                macAddress = details?.macAddress ?: host.macAddress,
            )

            hostRepository.notifyHostUpdated(updated)

            if (updated.name != host.name ||
                updated.httpsPort != host.httpsPort ||
                updated.macAddress != host.macAddress
            ) {
                hostRepository.saveHost(updated)
            }
        } else {
            recordOffline(host)
        }
    }

    private fun recordOffline(host: Host) {
        val currentFails = (offlineCounts[host.uuid] ?: 0) + 1
        offlineCounts[host.uuid] = currentFails

        if (currentFails >= OFFLINE_POLL_TRIES && host.state != HostState.OFFLINE) {
            val offlineHost = host.copy(
                state = HostState.OFFLINE,
                currentGameId = 0,
                currentGameUuid = null,
            )
            hostRepository.notifyHostUpdated(offlineHost)
        }
    }

    private suspend fun tryPollAddress(
        address: HostAddress,
        httpsPort: Int,
        serverCert: X509Certificate?,
        likelyOnline: Boolean,
    ): ComputerDetails? {
        if (detailsPoller != null) {
            return detailsPoller.invoke(address, httpsPort, serverCert, likelyOnline)
        }
        return try {
            val http = createNvHttp(address, httpsPort, serverCert)
            http.getComputerDetails(likelyOnline)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun handleMdnsComputerDiscovered(computer: MdnsComputer) {
        val localAddr = computer.localAddress?.let { HostAddress(it.hostAddress, computer.port) }
        val ipv6Addr = computer.ipv6Address?.let { HostAddress(it.hostAddress, computer.port) }

        var remoteAddr: HostAddress? = null
        if (computer.localAddress is Inet4Address) {
            try {
                val stunHost = if (externalAddressResolver != null) {
                    externalAddressResolver.invoke("stun.moonlight-stream.org", 3478)
                } else {
                    NvConnection.findExternalAddressForMdns("stun.moonlight-stream.org", 3478)
                }
                if (stunHost != null) {
                    remoteAddr = HostAddress(stunHost, NvHTTP.DEFAULT_HTTP_PORT)
                }
            } catch (_: Exception) {
            }
        }

        val candidateAddresses = listOfNotNull(localAddr, ipv6Addr)
        var polledDetails: ComputerDetails? = null

        for (addr in candidateAddresses) {
            polledDetails = tryPollAddress(addr, httpsPort = 0, serverCert = null, likelyOnline = false)
            if (polledDetails?.uuid != null) break
        }

        val details = polledDetails ?: return
        val uuid = details.uuid ?: return

        val existing = hostRepository.getHostByUuid(uuid)
        val cert = parseDerCert(existing?.serverCertDer)
        val pairState = when (details.pairState) {
            PairingManager.PairState.PAIRED -> PairState.PAIRED
            PairingManager.PairState.NOT_PAIRED -> PairState.NOT_PAIRED
            else -> if (cert != null) PairState.PAIRED else PairState.NOT_PAIRED
        }

        val hostToSave = if (existing != null) {
            existing.copy(
                name = details.name ?: existing.name,
                localAddress = localAddr ?: existing.localAddress,
                ipv6Address = ipv6Addr ?: existing.ipv6Address,
                remoteAddress = remoteAddr ?: existing.remoteAddress,
                macAddress = details.macAddress ?: existing.macAddress,
                state = HostState.ONLINE,
                pairState = pairState,
                currentGameId = details.runningGameId,
                currentGameUuid = details.runningGameUUID,
                httpsPort = if (details.httpsPort != 0) details.httpsPort else existing.httpsPort,
            )
        } else {
            Host(
                uuid = uuid,
                name = details.name ?: computer.name ?: "Sunshine",
                localAddress = localAddr,
                remoteAddress = remoteAddr,
                ipv6Address = ipv6Addr,
                macAddress = details.macAddress,
                state = HostState.ONLINE,
                pairState = pairState,
                currentGameId = details.runningGameId,
                currentGameUuid = details.runningGameUUID,
                capabilities = SunshineServerCapabilities.DEFAULT,
                httpsPort = details.httpsPort,
            )
        }

        hostRepository.saveHost(hostToSave)
        hostRepository.notifyHostUpdated(hostToSave)
    }

    override suspend fun addHost(
        address: String,
        name: String,
    ): Result<Host> = withContext(ioDispatcher) {
        val parsed = parseHostAddress(address) ?: return@withContext Result.failure(
            IllegalArgumentException("Invalid host address: $address"),
        )
        val (hostStr, port) = parsed
        val manualAddr = HostAddress(hostStr, port)

        val details = if (detailsPoller != null) {
            detailsPoller.invoke(manualAddr, 0, null, false)
                ?: return@withContext Result.failure(
                    IOException("Could not connect to Sunshine server at $address"),
                )
        } else {
            try {
                val http = createNvHttp(manualAddr, 0, null)
                http.getComputerDetails(false)
                    ?: return@withContext Result.failure(
                        IOException("Could not connect to Sunshine server at $address"),
                    )
            } catch (e: Exception) {
                return@withContext Result.failure(
                    IOException("Could not connect to Sunshine server at $address: ${e.message}", e),
                )
            }
        }

        val uuid = details.uuid ?: return@withContext Result.failure(
            IOException("Sunshine server at $address did not return a UUID"),
        )

        val existing = hostRepository.getHostByUuid(uuid)
        val cert = parseDerCert(existing?.serverCertDer)

        val finalDetails = if (cert != null) {
            tryPollAddress(manualAddr, details.httpsPort, cert, likelyOnline = true) ?: details
        } else {
            details
        }

        val pairState = when (finalDetails.pairState) {
            PairingManager.PairState.PAIRED -> PairState.PAIRED
            PairingManager.PairState.NOT_PAIRED -> PairState.NOT_PAIRED
            else -> if (cert != null) PairState.PAIRED else PairState.NOT_PAIRED
        }

        val hostName = if (name.isNotBlank()) name else (finalDetails.name ?: "Sunshine")
        val host = Host(
            uuid = uuid,
            name = hostName,
            manualAddress = manualAddr,
            localAddress = existing?.localAddress,
            remoteAddress = existing?.remoteAddress,
            ipv6Address = existing?.ipv6Address,
            macAddress = finalDetails.macAddress ?: existing?.macAddress,
            serverCertDer = existing?.serverCertDer,
            state = HostState.ONLINE,
            pairState = pairState,
            currentGameId = finalDetails.runningGameId,
            currentGameUuid = finalDetails.runningGameUUID,
            httpsPort = finalDetails.httpsPort,
        )

        hostRepository.saveHost(host)
        hostRepository.notifyHostUpdated(host)
        Result.success(host)
    }

    override suspend fun pairHost(
        host: Host,
        pin: String,
    ): Result<Unit> = withContext(ioDispatcher) {
        if (pairingExecutor != null) {
            val result = pairingExecutor.invoke(host, pin)
            return@withContext result.map { pairedCert ->
                val updated = host.copy(
                    pairState = PairState.PAIRED,
                    serverCertDer = pairedCert.encoded,
                )
                hostRepository.saveHost(updated)
                hostRepository.notifyHostUpdated(updated)
            }
        }

        val targetAddress = host.manualAddress ?: host.localAddress ?: host.remoteAddress ?: host.ipv6Address
            ?: return@withContext Result.failure(IOException("Host has no reachable address"))

        val cert = parseDerCert(host.serverCertDer)
        val httpConn = try {
            createNvHttp(targetAddress, host.httpsPort, cert)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        if (httpConn.pairState == PairingManager.PairState.PAIRED) {
            val updated = host.copy(
                pairState = PairState.PAIRED,
                httpsPort = if (httpConn.resolvedHttpsPort != 0) httpConn.resolvedHttpsPort else host.httpsPort,
            )
            hostRepository.notifyHostUpdated(updated)
            return@withContext Result.success(Unit)
        }

        val pm = httpConn.pairingManager
        val result = try {
            pm.pair(httpConn.getServerInfo(true), pin)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }

        when (result) {
            PairingManager.PairState.PAIRED -> {
                val pairedCert = pm.pairedCert
                val updated = host.copy(
                    pairState = PairState.PAIRED,
                    serverCertDer = pairedCert?.encoded,
                    httpsPort = if (httpConn.resolvedHttpsPort != 0) httpConn.resolvedHttpsPort else host.httpsPort,
                )
                hostRepository.saveHost(updated)
                hostRepository.notifyHostUpdated(updated)
                Result.success(Unit)
            }
            PairingManager.PairState.PIN_WRONG -> {
                Result.failure(IOException("Incorrect PIN entered on server"))
            }
            PairingManager.PairState.ALREADY_IN_PROGRESS -> {
                Result.failure(IOException("Pairing already in progress on server"))
            }
            PairingManager.PairState.FAILED -> {
                val msg = if (host.currentGameId != 0) {
                    "Cannot pair while Sunshine is actively streaming a game"
                } else {
                    "Pairing failed"
                }
                Result.failure(IOException(msg))
            }
            else -> Result.failure(IOException("Pairing ended with status $result"))
        }
    }

    override suspend fun wakeHost(host: Host): Result<Unit> = withContext(ioDispatcher) {
        val mac = host.macAddress ?: return@withContext Result.failure(
            IllegalArgumentException("No MAC address available for host"),
        )
        try {
            val details = ComputerDetails().apply { macAddress = mac }
            WakeOnLanSender.sendWolPacket(details)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeHost(uuid: String) = withContext(ioDispatcher) {
        hostRepository.deleteHost(uuid)
    }

    private fun createNvHttp(
        address: HostAddress,
        httpsPort: Int,
        serverCert: X509Certificate?,
    ): NvHTTP {
        return NvHTTP(
            ComputerDetails.AddressTuple(address.address, address.port),
            httpsPort,
            uniqueId,
            serverCert,
            cryptoProvider,
        )
    }

    private fun registerNetworkCallback() {
        try {
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch(ioDispatcher) {
                        pollAllHostsOnce()
                    }
                }

                override fun onLost(network: Network) {
                    scope.launch(ioDispatcher) {
                        val hosts = hostRepository.getAllHosts()
                        for (h in hosts) {
                            val off = h.copy(state = HostState.OFFLINE)
                            hostRepository.notifyHostUpdated(off)
                        }
                    }
                }
            }
            networkCallback = callback
            connMgr.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            LimeLog.warning("Failed to register ConnectivityManager callback: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        networkCallback = null
        try {
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connMgr?.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
    }

    private fun parseHostAddress(rawInput: String): Pair<String, Int>? {
        val trimmed = rawInput.trim()
        val uri = Uri.parse("sunbeam://$trimmed").takeIf { !it.host.isNullOrEmpty() }
            ?: Uri.parse("sunbeam://[$trimmed]").takeIf { !it.host.isNullOrEmpty() }
        val host = uri?.host ?: return null
        val port = if (uri.port != -1) uri.port else NvHTTP.DEFAULT_HTTP_PORT
        return host to port
    }

    private fun parseDerCert(der: ByteArray?): X509Certificate? {
        if (der == null || der.isEmpty()) return null
        return try {
            val cf = CertificateFactory.getInstance("X.509")
            cf.generateCertificate(ByteArrayInputStream(der)) as? X509Certificate
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val DEFAULT_POLLING_PERIOD_MS = 1500L
        private const val MDNS_QUERY_PERIOD_MS = 1000
        private const val OFFLINE_POLL_TRIES = 3
    }
}
