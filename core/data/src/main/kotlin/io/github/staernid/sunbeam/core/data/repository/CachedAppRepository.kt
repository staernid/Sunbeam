@file:Suppress(
    "TooGenericExceptionCaught",
    "SwallowedException",
)

package io.github.staernid.sunbeam.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.parser.AppListParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Interface for remote network operations related to host applications.
 */
interface AppNetworkDataSource {
    suspend fun fetchAppListXml(host: Host): Result<String>

    suspend fun quitApp(host: Host): Result<Unit>
}

/**
 * Repository backed by Moonlight's backward-compatible disk cache:
 * - App list XML: `cacheDir/applist/<hostUuid>`
 * - Box art images: `cacheDir/boxart/<hostUuid>/<appId>.png`
 * - Hidden apps preferences: `HiddenApps` SharedPreferences
 */
class CachedAppRepository(
    private val context: Context,
    private val cacheDir: File = context.cacheDir,
    private val networkDataSource: AppNetworkDataSource? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AppRepository {
    private val mutex = Mutex()
    private val appFlows = mutableMapOf<String, MutableStateFlow<List<HostApp>>>()

    private val hiddenPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(HIDDEN_APPS_PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getApps(
        hostUuid: String,
        showHidden: Boolean,
    ): Flow<List<HostApp>> {
        val flow = getOrCreateFlow(hostUuid)
        return flow.map { apps ->
            if (showHidden) apps else apps.filterNot { it.isHidden }
        }
    }

    private fun getOrCreateFlow(hostUuid: String): MutableStateFlow<List<HostApp>> {
        synchronized(appFlows) {
            return appFlows.getOrPut(hostUuid) {
                MutableStateFlow(loadAppsFromDisk(hostUuid))
            }
        }
    }

    override suspend fun refreshApps(host: Host): Result<List<HostApp>> = withContext(ioDispatcher) {
        val netResult = networkDataSource?.fetchAppListXml(host)
        if (netResult != null && netResult.isSuccess) {
            val xml = netResult.getOrNull()
            if (!xml.isNullOrBlank()) {
                saveAppListXml(host.uuid, xml)
            }
        }

        val apps = loadAppsFromDisk(host.uuid)
        mutex.withLock {
            getOrCreateFlow(host.uuid).value = apps
        }

        if (netResult != null && netResult.isFailure && apps.isEmpty()) {
            Result.failure(netResult.exceptionOrNull() ?: IllegalStateException("Failed to refresh apps"))
        } else {
            Result.success(apps)
        }
    }

    override suspend fun setAppHidden(
        hostUuid: String,
        appId: Int,
        hidden: Boolean,
    ) = withContext(ioDispatcher) {
        mutex.withLock {
            val currentSet = hiddenPrefs.getStringSet(hostUuid, emptySet())?.toMutableSet() ?: mutableSetOf()
            val appIdStr = appId.toString()
            if (hidden) {
                currentSet.add(appIdStr)
            } else {
                currentSet.remove(appIdStr)
            }
            hiddenPrefs.edit().putStringSet(hostUuid, currentSet).commit()

            val updated = loadAppsFromDisk(hostUuid)
            getOrCreateFlow(hostUuid).value = updated
        }
    }

    override suspend fun quitCurrentApp(host: Host): Result<Unit> = withContext(ioDispatcher) {
        networkDataSource?.quitApp(host) ?: Result.success(Unit)
    }

    override fun getBoxArtFile(
        hostUuid: String,
        appId: Int,
    ): File? {
        val file = File(cacheDir, "boxart/$hostUuid/$appId.png")
        return if (file.exists()) file else null
    }

    private fun loadAppsFromDisk(hostUuid: String): List<HostApp> {
        val appListFile = File(cacheDir, "applist/$hostUuid")
        val rawXml = if (appListFile.exists()) {
            try {
                appListFile.readText()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        val parsed = if (!rawXml.isNullOrBlank()) {
            try {
                AppListParser.parse(rawXml)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val hiddenIds = hiddenPrefs.getStringSet(hostUuid, emptySet()) ?: emptySet()

        return parsed.map { app ->
            val boxArt = getBoxArtFile(hostUuid, app.id)
            app.copy(
                isHidden = hiddenIds.contains(app.id.toString()),
                boxArtPath = boxArt?.absolutePath,
            )
        }
    }

    private fun saveAppListXml(
        hostUuid: String,
        xml: String,
    ) {
        try {
            val appListDir = File(cacheDir, "applist")
            if (!appListDir.exists()) {
                appListDir.mkdirs()
            }
            File(appListDir, hostUuid).writeText(xml)
        } catch (_: Exception) {
            // Non-fatal disk write error
        }
    }

    companion object {
        const val HIDDEN_APPS_PREFS_NAME = "HiddenApps"
    }
}
