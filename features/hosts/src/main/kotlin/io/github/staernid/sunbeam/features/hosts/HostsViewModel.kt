package io.github.staernid.sunbeam.features.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostAddress
import io.github.staernid.sunbeam.core.data.model.HostState
import io.github.staernid.sunbeam.core.data.model.PairState
import io.github.staernid.sunbeam.core.data.repository.HostOperationsHandler
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.UUID

class HostsViewModel(
    private val hostRepository: HostRepository,
    private val hostOperationsHandler: HostOperationsHandler? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HostsUiState(isLoading = true))
    val uiState: StateFlow<HostsUiState> = _uiState.asStateFlow()

    private val secureRandom = SecureRandom()
    private var pairingJob: Job? = null

    init {
        observeHosts()
    }

    private fun observeHosts() {
        viewModelScope.launch {
            hostRepository.hostsFlow
                .catch { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            userMessage = error.message ?: "Failed to load hosts",
                        )
                    }
                }
                .collect { hostList ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            hosts = hostList,
                        )
                    }
                }
        }
    }

    fun onAddHostClicked() {
        _uiState.update { it.copy(dialogState = HostsDialogState.AddHost) }
    }

    fun onAddHostSubmit(
        addressInput: String,
        customName: String = "",
    ) {
        val trimmedAddress = addressInput.trim()
        if (trimmedAddress.isBlank()) {
            _uiState.update { it.copy(userMessage = "Host address cannot be empty") }
            return
        }

        viewModelScope.launch {
            if (hostOperationsHandler != null) {
                _uiState.update { it.copy(dialogState = HostsDialogState.None, isLoading = true) }
                val result = hostOperationsHandler.addHost(trimmedAddress, customName)
                result.onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userMessage = error.message ?: "Failed to connect to host",
                        )
                    }
                }
            } else {
                val name = customName.trim().ifBlank { trimmedAddress }
                val host = Host(
                    uuid = UUID.randomUUID().toString(),
                    name = name,
                    manualAddress = HostAddress(address = trimmedAddress),
                    state = HostState.UNKNOWN,
                    pairState = PairState.NOT_PAIRED,
                )
                hostRepository.saveHost(host)
                _uiState.update { it.copy(dialogState = HostsDialogState.None) }
            }
        }
    }

    fun onHostLongClicked(host: Host) {
        _uiState.update { it.copy(dialogState = HostsDialogState.HostDetails(host)) }
    }

    fun onStartPairingClicked(host: Host) {
        val pin = generatePairingPin()
        _uiState.update { it.copy(dialogState = HostsDialogState.Pairing(host, pin)) }

        if (hostOperationsHandler != null) {
            pairingJob?.cancel()
            pairingJob = viewModelScope.launch {
                val result = hostOperationsHandler.pairHost(host, pin)
                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            dialogState = HostsDialogState.None,
                            userMessage = "Paired successfully with ${host.name}",
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            dialogState = HostsDialogState.None,
                            userMessage = error.message ?: "Pairing failed",
                        )
                    }
                }
            }
        }
    }

    fun onDeleteHostRequested(host: Host) {
        _uiState.update { it.copy(dialogState = HostsDialogState.DeleteConfirm(host)) }
    }

    fun onDeleteHostConfirmed(uuid: String) {
        viewModelScope.launch {
            hostRepository.deleteHost(uuid)
            _uiState.update { it.copy(dialogState = HostsDialogState.None) }
        }
    }

    fun onDismissDialog() {
        pairingJob?.cancel()
        pairingJob = null
        _uiState.update { it.copy(dialogState = HostsDialogState.None) }
    }

    fun onUserMessageDismissed() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun generatePairingPin(): String {
        val pinNumber = secureRandom.nextInt(PIN_MAX - PIN_MIN + 1) + PIN_MIN
        return pinNumber.toString()
    }

    companion object {
        private const val PIN_MIN = 1000
        private const val PIN_MAX = 9999
    }
}
