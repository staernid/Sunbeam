@file:Suppress("TooGenericExceptionCaught")

package io.github.staernid.sunbeam.data

import android.content.Context
import io.github.staernid.sunbeam.LimeLog
import io.github.staernid.sunbeam.binding.PlatformBinding
import io.github.staernid.sunbeam.computers.IdentityManager
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.repository.AppNetworkDataSource
import io.github.staernid.sunbeam.nvstream.http.ComputerDetails
import io.github.staernid.sunbeam.nvstream.http.NvHTTP
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Network data source backed by [NvHTTP] to fetch application lists and quit running sessions.
 */
class NvAppNetworkDataSource(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AppNetworkDataSource {
    private val identityManager = IdentityManager(context)

    override suspend fun fetchAppListXml(host: Host): Result<String> =
        withContext(ioDispatcher) {
            val address = host.localAddress ?: host.remoteAddress ?: host.manualAddress ?: host.ipv6Address
            if (address == null) {
                return@withContext Result.failure(IllegalStateException("No reachable address for host ${host.name}"))
            }

            try {
                val cert = parseCertificate(host.serverCertDer)
                val httpConn =
                    NvHTTP(
                        ComputerDetails.AddressTuple(address.address, address.port),
                        address.port,
                        identityManager.uniqueId,
                        cert,
                        PlatformBinding.getCryptoProvider(context),
                    )
                val xml = httpConn.appListRaw
                Result.success(xml)
            } catch (e: Exception) {
                LimeLog.warning("Failed to fetch app list from host: ${e.message}")
                Result.failure(e)
            }
        }

    override suspend fun quitApp(host: Host): Result<Unit> =
        withContext(ioDispatcher) {
            val address = host.localAddress ?: host.remoteAddress ?: host.manualAddress ?: host.ipv6Address
            if (address == null) {
                return@withContext Result.failure(IllegalStateException("No reachable address for host ${host.name}"))
            }

            try {
                val cert = parseCertificate(host.serverCertDer)
                val httpConn =
                    NvHTTP(
                        ComputerDetails.AddressTuple(address.address, address.port),
                        address.port,
                        identityManager.uniqueId,
                        cert,
                        PlatformBinding.getCryptoProvider(context),
                    )
                val success = httpConn.quitApp()
                if (success) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Server refused quitApp command"))
                }
            } catch (e: Exception) {
                LimeLog.warning("Failed to quit app on host: ${e.message}")
                Result.failure(e)
            }
        }

    private fun parseCertificate(serverCertDer: ByteArray?): X509Certificate? {
        if (serverCertDer == null || serverCertDer.isEmpty()) return null
        return try {
            val certFactory = CertificateFactory.getInstance("X.509")
            certFactory.generateCertificate(ByteArrayInputStream(serverCertDer)) as? X509Certificate
        } catch (e: Exception) {
            LimeLog.warning("Failed to parse host server certificate: ${e.message}")
            null
        }
    }
}
