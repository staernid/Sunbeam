package io.github.staernid.sunbeam.features.hosts

import io.github.staernid.sunbeam.core.data.model.Host

sealed interface HostsDialogState {
    data object None : HostsDialogState

    data object AddHost : HostsDialogState

    data class Pairing(val host: Host, val pin: String) : HostsDialogState

    data class HostDetails(val host: Host) : HostsDialogState

    data class DeleteConfirm(val host: Host) : HostsDialogState

    data object Help : HostsDialogState
}

data class HostsUiState(
    val isLoading: Boolean = false,
    val hosts: List<Host> = emptyList(),
    val dialogState: HostsDialogState = HostsDialogState.None,
    val userMessage: String? = null,
)
