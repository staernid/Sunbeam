package io.github.staernid.sunbeam.features.apps

import io.github.staernid.sunbeam.core.data.model.Host
import io.github.staernid.sunbeam.core.data.model.HostApp
import io.github.staernid.sunbeam.core.data.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File

class FakeAppRepository(
    initialApps: List<HostApp> = emptyList(),
) : AppRepository {
    private val appsState = MutableStateFlow(initialApps)
    var quitAppCalled: Boolean = false
    var refreshCalled: Boolean = false

    override fun getApps(
        hostUuid: String,
        showHidden: Boolean,
    ): Flow<List<HostApp>> {
        return appsState.map { apps ->
            if (showHidden) apps else apps.filterNot { it.isHidden }
        }
    }

    override suspend fun refreshApps(host: Host): Result<List<HostApp>> {
        refreshCalled = true
        return Result.success(appsState.value)
    }

    override suspend fun setAppHidden(
        hostUuid: String,
        appId: Int,
        hidden: Boolean,
    ) {
        appsState.update { current ->
            current.map { app ->
                if (app.id == appId) app.copy(isHidden = hidden) else app
            }
        }
    }

    override suspend fun quitCurrentApp(host: Host): Result<Unit> {
        quitAppCalled = true
        return Result.success(Unit)
    }

    override fun getBoxArtFile(
        hostUuid: String,
        appId: Int,
    ): File? = null
}
