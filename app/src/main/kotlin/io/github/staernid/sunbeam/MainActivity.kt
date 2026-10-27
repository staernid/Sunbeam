package io.github.staernid.sunbeam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.lifecycleScope
import io.github.staernid.sunbeam.core.data.repository.CachedAppRepository
import io.github.staernid.sunbeam.core.data.repository.JsonProfileRepository
import io.github.staernid.sunbeam.core.data.repository.SqliteHostRepository
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.core.preferences.DataStorePreferencesRepository
import io.github.staernid.sunbeam.core.stream.NvStreamSession
import io.github.staernid.sunbeam.data.NvAppNetworkDataSource
import io.github.staernid.sunbeam.ui.SunbeamApp
import io.github.staernid.sunbeam.ui.stream.StreamSessionLauncher
import kotlinx.coroutines.launch
import java.io.File

/**
 * Modern Compose-based root activity linking all Sunbeam features and streaming session launcher.
 */
class MainActivity : ComponentActivity() {
    private val hostRepository by lazy { SqliteHostRepository(this) }
    private val profileRepository by lazy {
        JsonProfileRepository(File(filesDir, "profiles/profiles.json"))
    }
    private val preferencesRepository by lazy { DataStorePreferencesRepository.create(this) }
    private val appRepository by lazy {
        CachedAppRepository(
            context = this,
            networkDataSource = NvAppNetworkDataSource(this),
        )
    }
    private val streamLauncher by lazy {
        StreamSessionLauncher(
            hostRepository = hostRepository,
            appRepository = appRepository,
            preferencesRepository = preferencesRepository,
            profileRepository = profileRepository,
        )
    }
    private val streamSession by lazy { NvStreamSession() }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            profileRepository.initialize()
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            SunbeamTheme {
                SunbeamApp(
                    windowSizeClass = windowSizeClass,
                    hostRepository = hostRepository,
                    appRepository = appRepository,
                    preferencesRepository = preferencesRepository,
                    profileRepository = profileRepository,
                    streamLauncher = streamLauncher,
                    streamSession = streamSession,
                )
            }
        }
    }
}
