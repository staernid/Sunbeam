@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.stream

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import io.github.staernid.sunbeam.shadows.ShadowMoonBridge
import io.github.staernid.sunbeam.ui.StreamContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowMoonBridge::class])
class StreamDisplayCoordinatorTest {
    private lateinit var activity: Activity
    private lateinit var prefConfig: PreferenceConfiguration
    private lateinit var streamContainer: StreamContainer
    private lateinit var coordinator: StreamDisplayCoordinator

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        prefConfig = PreferenceConfiguration()
        prefConfig.fps = 60f
        prefConfig.width = 1920
        prefConfig.height = 1080
        streamContainer = StreamContainer(activity, null)
        coordinator = StreamDisplayCoordinator(activity, prefConfig, streamContainer)
    }

    @Test
    fun isRefreshRateEqualMatch_withinTolerance() {
        assertTrue(coordinator.isRefreshRateEqualMatch(60f))
        assertTrue(coordinator.isRefreshRateEqualMatch(62.5f))
        assertFalse(coordinator.isRefreshRateEqualMatch(59f))
        assertFalse(coordinator.isRefreshRateEqualMatch(120f))
    }

    @Test
    fun isRefreshRateGoodMatch_matchesMultiples() {
        assertTrue(coordinator.isRefreshRateGoodMatch(60f))
        assertTrue(coordinator.isRefreshRateGoodMatch(120f))
        assertFalse(coordinator.isRefreshRateGoodMatch(90f))
    }

    @Test
    fun computeStreamFrameRate_capsFpsWhenRequested() {
        prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_CAP_FPS
        prefConfig.fps = 60f

        val adjusted = coordinator.computeStreamFrameRate(60f)
        assertEquals(59f, adjusted, 0.01f)
    }

    @Test
    fun computeStreamFrameRate_switchesToBalancedWhenFpsSignificantlyAboveDisplayHz() {
        prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_CAP_FPS
        prefConfig.fps = 120f

        val adjusted = coordinator.computeStreamFrameRate(60f)
        assertEquals(120f, adjusted, 0.01f)
        assertEquals(PreferenceConfiguration.FRAME_PACING_BALANCED, prefConfig.framePacing)
    }

    @Test
    fun rotateScreen_togglesOrientation() {
        val next = coordinator.rotateScreen(Configuration.ORIENTATION_LANDSCAPE)
        assertEquals(Configuration.ORIENTATION_PORTRAIT, next)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, activity.requestedOrientation)

        val back = coordinator.rotateScreen(next)
        assertEquals(Configuration.ORIENTATION_LANDSCAPE, back)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, activity.requestedOrientation)
    }
}
