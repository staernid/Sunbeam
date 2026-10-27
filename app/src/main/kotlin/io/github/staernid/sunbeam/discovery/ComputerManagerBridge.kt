@file:Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException",
    "TooManyFunctions",
    "LongMethod",
)

package io.github.staernid.sunbeam.discovery

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import io.github.staernid.sunbeam.LimeLog
import io.github.staernid.sunbeam.binding.PlatformBinding
import io.github.staernid.sunbeam.computers.ComputerManagerListener
import io.github.staernid.sunbeam.computers.ComputerManagerService
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.data.model.SunshineServerCapabilities
import io.github.staernid.sunbeam.core.data.repository.HostOperationsHandler
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import io.github.staernid.sunbeam.nvstream.http.ComputerDetails
import io.github.staernid.sunbeam.nvstream.http.NvHTTP
import io.github.staernid.sunbeam.nvstream.http.PairingManager
import io.github.staernid.sunbeam.nvstream.wol.WakeOnLanSender
import io.github.staernid.sunbeam.utils.ServerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Bridge between [ComputerManagerService] (mDNS discovery, periodic state polling, pairing)
 * and the modern Compose host architecture ([HostRepository], [HostOperationsHandler]).
 */
class ComputerManagerBridge(
    private val context: Context,
    private val hostRepository: HostRepository,
    private val scope: CoroutineScope,
) : HostOperationsHandler {
    private var managerBinder: ComputerManagerService.ComputerManagerBinder? = null
    private var isBound = false
    private var isPolling = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            binder: IBinder?,
        ) {
            val localBinder = binder as? ComputerManagerService.ComputerManagerBinder ?: return
            scope.launch(Dispatchers.IO) {
                try {
                    localBinder.waitForReady()
                    managerBinder = localBinder
                    PlatformBinding.getCryptoProvider(context).clientCertificate
                    startPollingInternal()
                } catch (e: Exception) {
                    LimeLog.warning("Failed during ComputerManagerService setup: ${e.message}")
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            managerBinder = null
            isPolling = false
        }
    }

    fun start() {
        if (!isBound) {
            val intent = Intent(context, ComputerManagerService::class.java)
            isBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            startPollingInternal()
        }
    }

    private fun startPollingInternal() {
        val binder = managerBinder ?: return
        if (isPolling) return
        isPolling = true
        binder.startPolling(object : ComputerManagerListener {
            override fun notifyComputerUpdated(details: ComputerDetails) {
                val host = details.toHost()
                hostRepository.notifyHostUpdated(host)
            }
        })
    }

    fun stop() {
        if (isPolling) {
            managerBinder?.stopPolling()
            isPolling = false
        }
    }

    fun destroy() {
        stop()
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                // Service may have already been unbound
            }
            isBound = false
        }
    }

    override suspend fun addHost(
        address: String,
        name: String,
    ): Result<Host> = withContext(Dispatchers.IO) {
        val binder = managerBinder ?: return@withContext Result.failure(
            IllegalStateException("Discovery service is not ready yet"),
        )
        val parsed = parseHostAddress(address) ?: return@withContext Result.failure(
            IllegalArgumentException("Invalid host address: $address"),
        )
        val (hostStr, port) = parsed
        val details = ComputerDetails().apply {
            manualAddress = ComputerDetails.AddressTuple(hostStr, port)
        }
        val success = try {
            binder.addComputerBlocking(details)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        if (success && details.uuid != null) {
            val host = details.toHost()
            hostRepository.notifyHostUpdated(host)
            Result.success(host)
        } else {
            Result.failure(IOException("Could not connect to Sunshine server at $address"))
        }
    }

    override suspend fun pairHost(
        host: Host,
        pin: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val binder = managerBinder ?: return@withContext Result.failure(
            IllegalStateException("Discovery service is not ready yet"),
        )
        val computer = binder.getComputer(host.uuid) ?: return@withContext Result.failure(
            IllegalArgumentException("Host ${host.name} not found in manager"),
        )
        val activeAddr = ServerHelper.getCurrentAddressFromComputer(computer) ?: return@withContext Result.failure(
            IOException("Host is offline or has no reachable address"),
        )

        try {
            binder.stopPolling()
            isPolling = false

            val crypto = PlatformBinding.getCryptoProvider(context)
            val httpConn = NvHTTP(
                activeAddr,
                computer.httpsPort,
                binder.uniqueId,
                computer.serverCert,
                crypto,
            )

            if (httpConn.pairState == PairingManager.PairState.PAIRED) {
                val updated = host.copy(pairState = PairState.PAIRED)
                hostRepository.notifyHostUpdated(updated)
                startPollingInternal()
                return@withContext Result.success(Unit)
            }

            val pm = httpConn.pairingManager
            val result = pm.pair(httpConn.getServerInfo(true), pin)

            startPollingInternal()

            when (result) {
                PairingManager.PairState.PAIRED -> {
                    val pairedCert = pm.pairedCert
                    computer.serverCert = pairedCert
                    binder.invalidateStateForComputer(computer.uuid)
                    val updated = host.copy(
                        pairState = PairState.PAIRED,
                        serverCertDer = pairedCert?.encoded,
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
                    val msg = if (computer.runningGameId != 0) {
                        "Cannot pair while Sunshine is actively streaming a game"
                    } else {
                        "Pairing failed"
                    }
                    Result.failure(IOException(msg))
                }
                else -> Result.failure(IOException("Pairing ended with status $result"))
            }
        } catch (e: Exception) {
            startPollingInternal()
            Result.failure(e)
        }
    }

    suspend fun removeHost(uuid: String) = withContext(Dispatchers.IO) {
        val binder = managerBinder
        val computer = binder?.getComputer(uuid)
        if (binder != null && computer != null) {
            binder.removeComputer(computer)
        }
        hostRepository.deleteHost(uuid)
    }

    suspend fun wakeOnLan(host: Host): Result<Unit> = withContext(Dispatchers.IO) {
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

    private fun parseHostAddress(rawInput: String): Pair<String, Int>? {
        val trimmed = rawInput.trim()
        val uri = Uri.parse("sunbeam://$trimmed").takeIf { !it.host.isNullOrEmpty() }
            ?: Uri.parse("sunbeam://[$trimmed]").takeIf { !it.host.isNullOrEmpty() }
        val host = uri?.host ?: return null
        val port = if (uri.port != -1) uri.port else NvHTTP.DEFAULT_HTTP_PORT
        return host to port
    }

    private fun ComputerDetails.toHost(): Host {
        val hostState = when (state) {
            ComputerDetails.State.ONLINE -> HostState.ONLINE
            ComputerDetails.State.OFFLINE -> HostState.OFFLINE
            else -> HostState.UNKNOWN
        }
        val hostPairState = when (pairState) {
            PairingManager.PairState.PAIRED -> PairState.PAIRED
            PairingManager.PairState.NOT_PAIRED -> PairState.NOT_PAIRED
            else -> if (serverCert != null) PairState.PAIRED else PairState.NOT_PAIRED
        }
        val caps = SunshineServerCapabilities.DEFAULT
        return Host(
            uuid = uuid ?: "",
            name = name ?: "Sunshine",
            localAddress = localAddress?.toHostAddress(),
            remoteAddress = remoteAddress?.toHostAddress(),
            manualAddress = manualAddress?.toHostAddress(),
            ipv6Address = ipv6Address?.toHostAddress(),
            macAddress = macAddress,
            serverCertDer = try {
                serverCert?.encoded
            } catch (e: Exception) {
                null
            },
            state = hostState,
            pairState = hostPairState,
            currentGameId = runningGameId,
            currentGameUuid = runningGameUUID,
            capabilities = caps,
        )
    }

    private fun ComputerDetails.AddressTuple.toHostAddress(): HostAddress =
        HostAddress(address = address, port = port)
}
