package io.github.staernid.sunbeam.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import dev.zacsweers.metro.createGraphFactory
import io.github.staernid.sunbeam.SunbeamApplication
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppGraphTest {
    @Test
    fun graphProvidesConfiguredDependenciesAndMaintainsSingletons() {
        val app: Application = ApplicationProvider.getApplicationContext()
        val graph = createGraphFactory<AppGraph.Factory>().create(app)

        assertNotNull(graph.hostRepository)
        assertNotNull(graph.profileRepository)
        assertNotNull(graph.preferencesRepository)
        assertNotNull(graph.appRepository)
        assertNotNull(graph.streamLauncher)
        assertNotNull(graph.hostOperationsHandler)
        assertNotNull(graph.hostDiscoveryManager)

        // Verify singleton scoping
        assertSame(graph.hostRepository, graph.hostRepository)
        assertSame(graph.profileRepository, graph.profileRepository)
        assertSame(graph.preferencesRepository, graph.preferencesRepository)
        assertSame(graph.appRepository, graph.appRepository)
        assertSame(graph.streamLauncher, graph.streamLauncher)
        assertSame(graph.hostDiscoveryManager, graph.hostOperationsHandler)
    }

    @Test
    fun applicationInitializesAppGraph() {
        val app: SunbeamApplication = ApplicationProvider.getApplicationContext()
        assertNotNull(app.appGraph)
    }
}
