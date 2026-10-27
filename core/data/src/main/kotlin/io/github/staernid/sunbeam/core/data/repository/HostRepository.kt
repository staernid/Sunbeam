package io.github.staernid.sunbeam.core.data.repository

import io.github.staernid.sunbeam.core.data.model.Host
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for streaming host persistence and reactive updates.
 */
interface HostRepository {
    /**
     * Flow of all persisted hosts, re-emitted on any database modification.
     */
    val hostsFlow: Flow<List<Host>>

    /**
     * Retrieves all persisted hosts.
     */
    suspend fun getAllHosts(): List<Host>

    /**
     * Finds a host by its unique identifier.
     */
    suspend fun getHostByUuid(uuid: String): Host?

    /**
     * Inserts or updates a host.
     */
    suspend fun saveHost(host: Host)

    /**
     * Deletes a host by its unique identifier.
     */
    suspend fun deleteHost(uuid: String)

    /**
     * Notifies the repository of an updated host (e.g. from mDNS auto-discovery or dynamic polling).
     */
    fun notifyHostUpdated(host: Host)
}
