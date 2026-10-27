package io.github.staernid.sunbeam.core.data.repository

import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for managing and observing apps available on a streaming host.
 */
interface AppRepository {
    /**
     * Emits the reactive list of apps for the specified [hostUuid].
     * If [showHidden] is false, hidden apps are excluded.
     */
    fun getApps(
        hostUuid: String,
        showHidden: Boolean = false,
    ): Flow<List<HostApp>>

    /**
     * Fetches the latest app list for [host] from the network and updates the local cache.
     */
    suspend fun refreshApps(host: Host): Result<List<HostApp>>

    /**
     * Marks an app as hidden or visible for the given [hostUuid].
     */
    suspend fun setAppHidden(
        hostUuid: String,
        appId: Int,
        hidden: Boolean,
    )

    /**
     * Terminate the currently running stream session on [host].
     */
    suspend fun quitCurrentApp(host: Host): Result<Unit>

    /**
     * Returns the cached box art image [File] for the given [hostUuid] and [appId] if present.
     */
    fun getBoxArtFile(
        hostUuid: String,
        appId: Int,
    ): File?
}
