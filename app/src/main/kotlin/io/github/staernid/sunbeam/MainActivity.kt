package io.github.staernid.sunbeam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.zacsweers.metro.Inject
import io.github.staernid.sunbeam.core.data.repository.JsonProfileRepository
import io.github.staernid.sunbeam.core.data.repository.SqliteHostRepository
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.discovery.HostDiscoveryManager
import io.github.staernid.sunbeam.ui.SunbeamApp
import kotlinx.coroutines.launch

/**
 * Modern Compose-based root activity linking all Sunbeam features and streaming session launcher.
 */
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var sqliteHostRepository: SqliteHostRepository

    @Inject
    lateinit var jsonProfileRepository: JsonProfileRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var hostDiscoveryManager: HostDiscoveryManager

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        (application as SunbeamApplication).appGraph.inject(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            sqliteHostRepository.initialize()
            jsonProfileRepository.initialize()
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settings by preferencesRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = StreamSettings(),
            )
            SunbeamTheme(
                dynamicColor = settings.dynamicColor,
                oledBlack = settings.oledBlackMode,
            ) {
                SunbeamApp(
                    windowSizeClass = windowSizeClass,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        hostDiscoveryManager.start()
    }

    override fun onStop() {
        super.onStop()
        hostDiscoveryManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        hostDiscoveryManager.destroy()
    }
}
