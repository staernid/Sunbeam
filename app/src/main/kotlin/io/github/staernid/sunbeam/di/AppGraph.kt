@file:Suppress("TooManyFunctions")

package io.github.staernid.sunbeam.di

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.staernid.sunbeam.MainActivity
import io.github.staernid.sunbeam.core.data.repository.AppRepository
import io.github.staernid.sunbeam.core.data.repository.CachedAppRepository
import io.github.staernid.sunbeam.core.data.repository.HostOperationsHandler
import io.github.staernid.sunbeam.core.data.repository.HostRepository
import io.github.staernid.sunbeam.core.data.repository.JsonProfileRepository
import io.github.staernid.sunbeam.core.data.repository.ProfileRepository
import io.github.staernid.sunbeam.core.data.repository.SqliteHostRepository
import io.github.staernid.sunbeam.core.preferences.DataStorePreferencesRepository
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.data.NvAppNetworkDataSource
import io.github.staernid.sunbeam.discovery.HostDiscoveryManager
import io.github.staernid.sunbeam.ui.stream.StreamSessionLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface AppGraph {
    val hostRepository: HostRepository
    val profileRepository: ProfileRepository
    val sqliteHostRepository: SqliteHostRepository
    val jsonProfileRepository: JsonProfileRepository
    val preferencesRepository: PreferencesRepository
    val appRepository: AppRepository
    val streamLauncher: StreamSessionLauncher
    val hostOperationsHandler: HostOperationsHandler
    val hostDiscoveryManager: HostDiscoveryManager

    fun inject(activity: MainActivity)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
        ): AppGraph
    }

    @Provides
    fun provideContext(application: Application): Context = application

    @Provides
    @SingleIn(AppScope::class)
    fun provideSqliteHostRepository(context: Context): SqliteHostRepository =
        SqliteHostRepository(context)

    @Provides
    fun provideHostRepository(sqliteHostRepository: SqliteHostRepository): HostRepository =
        sqliteHostRepository

    @Provides
    @SingleIn(AppScope::class)
    fun provideJsonProfileRepository(application: Application): JsonProfileRepository =
        JsonProfileRepository(File(application.filesDir, "profiles/profiles.json"))

    @Provides
    fun provideProfileRepository(jsonProfileRepository: JsonProfileRepository): ProfileRepository =
        jsonProfileRepository

    @Provides
    @SingleIn(AppScope::class)
    fun providePreferencesRepository(context: Context): PreferencesRepository =
        DataStorePreferencesRepository.create(context)

    @Provides
    @SingleIn(AppScope::class)
    fun provideAppRepository(context: Context): AppRepository =
        CachedAppRepository(
            context = context,
            networkDataSource = NvAppNetworkDataSource(context),
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideStreamLauncher(
        hostRepository: HostRepository,
        appRepository: AppRepository,
        preferencesRepository: PreferencesRepository,
        profileRepository: ProfileRepository,
    ): StreamSessionLauncher =
        StreamSessionLauncher(
            hostRepository = hostRepository,
            appRepository = appRepository,
            preferencesRepository = preferencesRepository,
            profileRepository = profileRepository,
        )

    @Provides
    @SingleIn(AppScope::class)
    fun provideHostDiscoveryManager(
        context: Context,
        hostRepository: HostRepository,
    ): HostDiscoveryManager =
        HostDiscoveryManager(
            context = context,
            hostRepository = hostRepository,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )

    @Provides
    fun provideHostOperationsHandler(discoveryManager: HostDiscoveryManager): HostOperationsHandler = discoveryManager
}
