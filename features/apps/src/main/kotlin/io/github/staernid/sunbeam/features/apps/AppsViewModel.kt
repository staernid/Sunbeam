@file:Suppress("TooManyFunctions")

package io.github.staernid.sunbeam.features.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.repository.AppRepository
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppsViewModel(
    private val hostUuid: String,
    private val hostRepository: HostRepository,
    private val appRepository: AppRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppsUiState(isLoading = true))
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    private var initialRefreshAttempted = false

    init {
        observeHost()
        observeApps()
    }

    private fun observeHost() {
        viewModelScope.launch {
            hostRepository.hostsFlow.collect { hosts ->
                val matchedHost = hosts.find { it.uuid == hostUuid }
                _uiState.update { it.copy(host = matchedHost) }
                if (matchedHost != null && !initialRefreshAttempted) {
                    initialRefreshAttempted = true
                    onRefresh()
                }
            }
        }
    }

    private fun observeApps() {
        viewModelScope.launch {
            appRepository.getApps(hostUuid, showHidden = true).collect { appList ->
                _uiState.update {
                    it.copy(
                        apps = appList,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchActiveChange(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery,
            )
        }
    }

    fun onToggleShowHiddenApps() {
        _uiState.update { it.copy(showHiddenApps = !it.showHiddenApps) }
    }

    fun onRefresh() {
        val host = _uiState.value.host ?: return
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = appRepository.refreshApps(host)
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    userMessage = if (result.isFailure) "Failed to refresh apps from PC" else null,
                )
            }
        }
    }

    fun onAppClick(
        app: HostApp,
        onLaunch: (Host, HostApp) -> Unit,
    ) {
        val host = _uiState.value.host ?: return
        val runningId = host.currentGameId

        if (runningId != 0 && runningId != app.id) {
            val runningApp = _uiState.value.runningApp ?: HostApp(
                id = runningId,
                title = "Current Game",
            )
            _uiState.update {
                it.copy(
                    dialogState = AppsDialogState.QuitAndLaunchConfirmation(
                        targetApp = app,
                        runningApp = runningApp,
                    ),
                )
            }
        } else {
            onLaunch(host, app)
        }
    }

    fun onAppLongClick(app: HostApp) {
        _uiState.update { it.copy(dialogState = AppsDialogState.AppDetails(app)) }
    }

    fun onHideApp(app: HostApp) {
        viewModelScope.launch(ioDispatcher) {
            appRepository.setAppHidden(hostUuid, app.id, hidden = true)
        }
    }

    fun onUnhideApp(app: HostApp) {
        viewModelScope.launch(ioDispatcher) {
            appRepository.setAppHidden(hostUuid, app.id, hidden = false)
        }
    }

    fun onQuitRunningAppClick() {
        val running = _uiState.value.runningApp ?: return
        _uiState.update { it.copy(dialogState = AppsDialogState.QuitConfirmation(running)) }
    }

    fun onConfirmQuitApp() {
        val host = _uiState.value.host ?: return
        viewModelScope.launch(ioDispatcher) {
            val result = appRepository.quitCurrentApp(host)
            _uiState.update {
                it.copy(
                    dialogState = AppsDialogState.None,
                    userMessage = if (result.isSuccess) "Streaming session ended" else "Failed to end session",
                )
            }
        }
    }

    fun onConfirmQuitAndLaunch(onLaunch: (Host, HostApp) -> Unit) {
        val host = _uiState.value.host ?: return
        val dialog = _uiState.value.dialogState as? AppsDialogState.QuitAndLaunchConfirmation ?: return
        val targetApp = dialog.targetApp

        viewModelScope.launch(ioDispatcher) {
            appRepository.quitCurrentApp(host)
            _uiState.update { it.copy(dialogState = AppsDialogState.None) }
            onLaunch(host, targetApp)
        }
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(dialogState = AppsDialogState.None) }
    }

    fun onUserMessageDismissed() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
