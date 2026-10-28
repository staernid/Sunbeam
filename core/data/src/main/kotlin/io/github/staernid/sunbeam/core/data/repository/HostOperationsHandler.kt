package io.github.staernid.sunbeam.core.data.repository

import io.github.staernid.sunbeam.core.data.model.Host

/**
 * Interface for active host network operations: auto-discovery/manual addition, pairing, and Wake-on-LAN.
 */
interface HostOperationsHandler {
    /**
     * Resolves and adds a host at [address] by querying Sunshine's serverinfo.
     */
    suspend fun addHost(
        address: String,
        name: String = "",
    ): Result<Host>

    /**
     * Executes the pairing handshake with [host] using the 4-digit [pin].
     */
    suspend fun pairHost(
        host: Host,
        pin: String,
    ): Result<Unit>

    /**
     * Sends a Wake-on-LAN magic packet to [host].
     */
    suspend fun wakeHost(host: Host): Result<Unit>
}
