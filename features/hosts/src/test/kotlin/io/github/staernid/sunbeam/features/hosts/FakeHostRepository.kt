package io.github.staernid.sunbeam.features.hosts

import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeHostRepository(
    initialHosts: List<Host> = emptyList(),
) : HostRepository {
    private val hostsState = MutableStateFlow(initialHosts)

    override val hostsFlow: Flow<List<Host>> = hostsState.asStateFlow()

    override suspend fun getAllHosts(): List<Host> = hostsState.value

    override suspend fun getHostByUuid(uuid: String): Host? {
        return hostsState.value.firstOrNull { it.uuid == uuid }
    }

    override suspend fun saveHost(host: Host) {
        hostsState.update { current ->
            val index = current.indexOfFirst { it.uuid == host.uuid }
            if (index >= 0) {
                current.toMutableList().apply { set(index, host) }
            } else {
                current + host
            }
        }
    }

    override suspend fun deleteHost(uuid: String) {
        hostsState.update { current -> current.filterNot { it.uuid == uuid } }
    }

    override fun notifyHostUpdated(host: Host) {
        hostsState.update { current ->
            val index = current.indexOfFirst { it.uuid == host.uuid }
            if (index >= 0) {
                current.toMutableList().apply { set(index, host) }
            } else {
                current + host
            }
        }
    }
}
