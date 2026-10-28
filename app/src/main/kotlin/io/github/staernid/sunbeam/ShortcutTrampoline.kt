@file:Suppress(
    "LongMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "TooGenericExceptionCaught",
    "CyclomaticComplexMethod",
)

package io.github.staernid.sunbeam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.staernid.sunbeam.binding.PlatformBinding
import io.github.staernid.sunbeam.computers.IdentityManager
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.repository.SqliteHostRepository
import io.github.staernid.sunbeam.nvstream.http.ComputerDetails
import io.github.staernid.sunbeam.nvstream.http.NvApp
import io.github.staernid.sunbeam.nvstream.http.NvHTTP
import io.github.staernid.sunbeam.nvstream.http.PairingManager
import io.github.staernid.sunbeam.nvstream.wol.WakeOnLanSender
import io.github.staernid.sunbeam.utils.CacheHelper
import io.github.staernid.sunbeam.utils.Dialog
import io.github.staernid.sunbeam.utils.ServerHelper
import io.github.staernid.sunbeam.utils.ShortcutHelper
import io.github.staernid.sunbeam.utils.SpinnerDialog
import io.github.staernid.sunbeam.utils.UiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Objects
import java.util.UUID

open class ShortcutTrampoline : AppCompatActivity() {
    private var uuidString: String? = null
    private var app: NvApp? = null
    private val intentStack = ArrayList<Intent>()
    private var blockingLoadSpinner: SpinnerDialog? = null

    open fun validateHostInput(
        hostUUID: String?,
        hostName: String?,
    ): Boolean {
        if (hostUUID == null && hostName == null) {
            Dialog.displayDialog(
                this,
                getString(R.string.conn_error_title),
                getString(R.string.scut_invalid_uuid),
                true,
            )
            return false
        }

        if (!hostUUID.isNullOrEmpty()) {
            try {
                UUID.fromString(hostUUID)
            } catch (_: IllegalArgumentException) {
                Dialog.displayDialog(
                    this,
                    getString(R.string.conn_error_title),
                    getString(R.string.scut_invalid_uuid),
                    true,
                )
                return false
            }
        } else {
            if (hostName.isNullOrEmpty()) {
                Dialog.displayDialog(
                    this,
                    getString(R.string.conn_error_title),
                    getString(R.string.scut_invalid_uuid),
                    true,
                )
                return false
            }
        }
        return true
    }

    open fun validateAppInput(
        appUUID: String?,
        appIDStr: String?,
        appName: String?,
    ): Boolean {
        if (appUUID == null && appIDStr == null && appName == null) {
            return false
        }

        if (!appUUID.isNullOrEmpty()) {
            try {
                UUID.fromString(appUUID)
            } catch (_: IllegalArgumentException) {
                Dialog.displayDialog(
                    this,
                    getString(R.string.conn_error_title),
                    getString(R.string.scut_invalid_app_id),
                    true,
                )
                return false
            }
        } else if (!appIDStr.isNullOrEmpty()) {
            try {
                appIDStr.toInt()
            } catch (_: NumberFormatException) {
                Dialog.displayDialog(
                    this,
                    getString(R.string.conn_error_title),
                    getString(R.string.scut_invalid_app_id),
                    true,
                )
                return false
            }
        }
        return true
    }

    private fun parseArtFileData(fileUri: Uri?): Map<String, String>? {
        if (fileUri == null) return null
        val artData = HashMap<String, String>()
        try {
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        line = line.trim()
                        if (line.isNotEmpty() && !line.startsWith("#")) {
                            if (!line.startsWith("[")) {
                                throw IOException("Invalid .art file format")
                            }
                            val separatorIndex = line.indexOf(' ')
                            if (separatorIndex in 1 until line.length - 1) {
                                val key = line.substring(0, separatorIndex).trim()
                                val value = line.substring(separatorIndex + 1).trim()
                                if (key.endsWith("]")) {
                                    artData[key.substring(1, key.length - 1)] = value
                                } else {
                                    throw IOException("Invalid .art file format")
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading .art file", e)
            Dialog.displayDialog(
                this,
                getString(R.string.conn_error_title),
                "Error reading .art file: ${e.message}",
                true,
            )
        }
        return artData
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiHelper.notifyNewRootView(this)

        val intent = intent ?: return
        val action = intent.action
        val dataUri = intent.data

        var hostUUID: String? = null
        var hostName: String? = null
        var appName: String? = null
        var appUUID: String? = null
        var appIDStr: String? = null

        if (Intent.ACTION_VIEW == action && dataUri != null) {
            val artData = parseArtFileData(dataUri)
            if (artData != null) {
                hostUUID = artData[ShortcutHelper.KEY_HOST_UUID]
                hostName = artData[ShortcutHelper.KEY_HOST_NAME]
                appName = artData[ShortcutHelper.KEY_APP_NAME]
                appUUID = artData[ShortcutHelper.KEY_APP_UUID]
                appIDStr = artData[ShortcutHelper.KEY_APP_ID]
            }
        }

        if (hostUUID == null) hostUUID = intent.getStringExtra(EXTRA_UUID)
        if (hostName == null) hostName = intent.getStringExtra(EXTRA_NAME)
        if (appUUID == null) appUUID = intent.getStringExtra(Game.EXTRA_APP_UUID)
        if (appIDStr == null) appIDStr = intent.getStringExtra(Game.EXTRA_APP_ID)
        if (appName == null) appName = intent.getStringExtra(Game.EXTRA_APP_NAME)

        if (!validateHostInput(hostUUID, hostName)) {
            return
        }

        val repository = (application as? SunbeamApplication)?.appGraph?.sqliteHostRepository
            ?: SqliteHostRepository(this)

        lifecycleScope.launch {
            val host = withContext(Dispatchers.IO) {
                if (!hostUUID.isNullOrEmpty()) {
                    repository.getHostByUuid(hostUUID)
                } else if (!hostName.isNullOrEmpty()) {
                    repository.getAllHosts().find { it.name.equals(hostName, ignoreCase = true) }
                } else {
                    null
                }
            }

            if (host == null) {
                Dialog.displayDialog(
                    this@ShortcutTrampoline,
                    getString(R.string.conn_error_title),
                    getString(R.string.scut_pc_not_found),
                    true,
                )
                return@launch
            }

            uuidString = host.uuid
            setIntent(Intent(getIntent()).putExtra(EXTRA_UUID, host.uuid))

            if (validateAppInput(appUUID, appIDStr, appName)) {
                if (!appUUID.isNullOrEmpty()) {
                    app = NvApp(appName, appUUID, -1, intent.getBooleanExtra(Game.EXTRA_APP_HDR, false))
                } else if (!appIDStr.isNullOrEmpty()) {
                    val appID = appIDStr.toInt()
                    app = NvApp(appName, null, appID, intent.getBooleanExtra(Game.EXTRA_APP_HDR, false))
                } else if (!appName.isNullOrEmpty()) {
                    try {
                        val rawAppList = withContext(Dispatchers.IO) {
                            CacheHelper.readInputStreamToString(
                                CacheHelper.openCacheFileForInput(cacheDir, "applist", host.uuid),
                            )
                        }
                        if (rawAppList.isEmpty()) {
                            Dialog.displayDialog(
                                this@ShortcutTrampoline,
                                getString(R.string.conn_error_title),
                                "${getString(R.string.scut_invalid_app_id)} (applist cache empty or unreadable)",
                                true,
                            )
                            return@launch
                        }
                        val appList = NvHTTP.getAppListByReader(StringReader(rawAppList))
                        val matched = appList.firstOrNull { it.appName.equals(appName, ignoreCase = true) }
                        if (matched == null) {
                            Dialog.displayDialog(
                                this@ShortcutTrampoline,
                                getString(R.string.conn_error_title),
                                "${getString(R.string.scut_invalid_app_id)} (app not found in cache)",
                                true,
                            )
                            return@launch
                        }
                        if (intent.getStringExtra(Game.EXTRA_APP_ID) == null) {
                            intent.putExtra(Game.EXTRA_APP_ID, matched.appId.toString())
                        }
                        if (intent.getStringExtra(Game.EXTRA_APP_UUID) == null && matched.appUUID != null) {
                            intent.putExtra(Game.EXTRA_APP_UUID, matched.appUUID)
                        }
                        app = NvApp(
                            appName,
                            matched.appUUID,
                            matched.appId,
                            intent.getBooleanExtra(Game.EXTRA_APP_HDR, false),
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing app list from cache", e)
                        Dialog.displayDialog(
                            this@ShortcutTrampoline,
                            getString(R.string.conn_error_title),
                            "${getString(R.string.scut_invalid_app_id)} (error parsing applist cache)",
                            true,
                        )
                        return@launch
                    }
                }
            }

            blockingLoadSpinner = SpinnerDialog.displayDialog(
                this@ShortcutTrampoline,
                getString(R.string.conn_establishing_title),
                getString(R.string.applist_connect_msg),
                true,
            )

            pollAndLaunch(host)
        }
    }

    private suspend fun pollAndLaunch(host: Host) {
        var wakeHostTries = WAKE_HOST_MAX_TRIES
        var targetDetails: ComputerDetails? = null

        withContext(Dispatchers.IO) {
            val addresses = listOfNotNull(
                host.localAddress,
                host.manualAddress,
                host.remoteAddress,
                host.ipv6Address,
            ).distinct()

            val cert = parseCert(host.serverCertDer)
            val uniqueId = IdentityManager(this@ShortcutTrampoline).uniqueId
            val crypto = PlatformBinding.getCryptoProvider(this@ShortcutTrampoline)

            while (targetDetails == null && wakeHostTries >= 0) {
                for (addr in addresses) {
                    try {
                        val http = NvHTTP(
                            ComputerDetails.AddressTuple(addr.address, addr.port),
                            host.httpsPort,
                            uniqueId,
                            cert,
                            crypto,
                        )
                        val polled = http.getComputerDetails(false)
                        if (polled != null && (polled.uuid == host.uuid || host.uuid.isEmpty())) {
                            polled.activeAddress = ComputerDetails.AddressTuple(addr.address, addr.port)
                            targetDetails = polled
                            break
                        }
                    } catch (_: Exception) {
                    }
                }

                if (targetDetails == null) {
                    if (!host.macAddress.isNullOrEmpty() && wakeHostTries > 0) {
                        try {
                            val details = ComputerDetails().apply { macAddress = host.macAddress }
                            WakeOnLanSender.sendWolPacket(details)
                        } catch (e: IOException) {
                            Log.w(TAG, "Failed sending WoL packet", e)
                        }
                    }
                    wakeHostTries--
                    if (wakeHostTries >= 0) {
                        delay(POLL_RETRY_DELAY_MS)
                    }
                }
            }
        }

        blockingLoadSpinner?.dismiss()
        blockingLoadSpinner = null

        val details = targetDetails
        if (details == null || details.state == ComputerDetails.State.OFFLINE) {
            Dialog.displayDialog(
                this,
                getString(R.string.conn_error_title),
                getString(R.string.error_pc_offline),
                true,
            )
            return
        }

        if (details.pairState != PairingManager.PairState.PAIRED) {
            Dialog.displayDialog(
                this,
                getString(R.string.conn_error_title),
                getString(R.string.scut_not_paired),
                true,
            )
            return
        }

        val targetApp = app
        if (targetApp != null) {
            val isCurrentAppRunning = details.runningGameId == 0 ||
                details.runningGameId == targetApp.appId ||
                Objects.equals(details.runningGameUUID, targetApp.appUUID)

            if (isCurrentAppRunning) {
                intentStack.add(ServerHelper.createStartIntent(this, targetApp, details))
                finish()
                startActivities(intentStack.toTypedArray())
            } else {
                val startIntent = ServerHelper.createStartIntent(this, targetApp, details)
                UiHelper.displayQuitConfirmationDialog(
                    this,
                    {
                        intentStack.add(startIntent)
                        finish()
                        startActivities(intentStack.toTypedArray())
                    },
                    {
                        finish()
                    },
                )
            }
        } else {
            finish()
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_UUID, uuidString)
            }
            intentStack.add(mainIntent)

            if (details.runningGameId != 0) {
                intentStack.add(
                    ServerHelper.createStartIntent(
                        this,
                        NvApp(null, null, details.runningGameId, false),
                        details,
                    ),
                )
            }
            startActivities(intentStack.toTypedArray())
        }
    }

    private fun parseCert(der: ByteArray?): X509Certificate? {
        if (der == null || der.isEmpty()) return null
        return try {
            val factory = CertificateFactory.getInstance("X.509")
            factory.generateCertificate(ByteArrayInputStream(der)) as? X509Certificate
        } catch (_: Exception) {
            null
        }
    }

    override fun onStop() {
        super.onStop()
        blockingLoadSpinner?.dismiss()
        blockingLoadSpinner = null
        Dialog.closeDialogs()
        finish()
    }

    companion object {
        const val EXTRA_NAME = "Name"
        const val EXTRA_UUID = "UUID"
        private const val TAG = "ShortcutTrampoline"
        private const val WAKE_HOST_MAX_TRIES = 10
        private const val POLL_RETRY_DELAY_MS = 1000L
    }
}
