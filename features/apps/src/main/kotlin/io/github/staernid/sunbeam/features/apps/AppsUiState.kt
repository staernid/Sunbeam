package io.github.staernid.sunbeam.features.apps

import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp

sealed interface AppsDialogState {
    data object None : AppsDialogState

    data class AppDetails(val app: HostApp) : AppsDialogState

    data class QuitConfirmation(val app: HostApp) : AppsDialogState

    data class QuitAndLaunchConfirmation(
        val targetApp: HostApp,
        val runningApp: HostApp,
    ) : AppsDialogState
}

data class AppsUiState(
    val host: Host? = null,
    val apps: List<HostApp> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showHiddenApps: Boolean = false,
    val userMessage: String? = null,
    val dialogState: AppsDialogState = AppsDialogState.None,
) {
    val runningApp: HostApp?
        get() = if (host?.currentGameId != null && host.currentGameId != 0) {
            apps.find { it.id == host.currentGameId } ?: HostApp(
                id = host.currentGameId,
                title = "Running Session",
                uuid = host.currentGameUuid ?: "",
            )
        } else {
            null
        }

    val filteredApps: List<HostApp>
        get() {
            val base = if (showHiddenApps) apps else apps.filterNot { it.isHidden }
            if (searchQuery.isBlank()) return base
            return base.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
        }
}
