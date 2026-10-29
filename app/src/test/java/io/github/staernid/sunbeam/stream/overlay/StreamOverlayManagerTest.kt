@file:Suppress(
    "MagicNumber",
)

package io.github.staernid.sunbeam.stream.overlay

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import io.github.staernid.sunbeam.shadows.ShadowMoonBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowMoonBridge::class])
class StreamOverlayManagerTest {
    private lateinit var context: Context
    private lateinit var prefConfig: PreferenceConfiguration
    private lateinit var perfOverlayView: View
    private lateinit var perfOverlayLite: TextView
    private lateinit var perfOverlayBig: TextView
    private lateinit var notificationOverlayView: TextView
    private lateinit var floatingMenuButton: ImageButton
    private lateinit var overlayZoomButton: ImageButton

    private var menuRequestedCount = 0
    private var zoomToggleRequestedCount = 0

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefConfig = PreferenceConfiguration.readPreferences(context)

        perfOverlayView =
            View(context).apply {
                layoutParams = FrameLayout.LayoutParams(100, 100)
            }
        perfOverlayLite = TextView(context)
        perfOverlayBig = TextView(context)
        notificationOverlayView = TextView(context)
        floatingMenuButton = ImageButton(context)
        overlayZoomButton = ImageButton(context)

        menuRequestedCount = 0
        zoomToggleRequestedCount = 0
    }

    private fun createManager(
        decorWidth: Int = 1920,
        decorHeight: Int = 1080,
    ): StreamOverlayManager {
        return StreamOverlayManager(
            prefConfig = prefConfig,
            decorBoundsProvider = { isWidth -> if (isWidth) decorWidth else decorHeight },
            perfOverlayView = perfOverlayView,
            perfOverlayLite = perfOverlayLite,
            perfOverlayBig = perfOverlayBig,
            notificationOverlayView = notificationOverlayView,
            floatingMenuButton = floatingMenuButton,
            overlayZoomButton = overlayZoomButton,
            onMenuRequested = { menuRequestedCount++ },
            onZoomToggleRequested = { zoomToggleRequestedCount++ },
        )
    }

    @Test
    fun setupPerformanceOverlay_whenEnabled_configuresVisibilityAndPlacement() {
        prefConfig.enablePerfOverlay = true
        prefConfig.enablePerfOverlayLite = true
        prefConfig.enablePerfOverlayBottom = true

        createManager()

        assertEquals(View.VISIBLE, perfOverlayView.visibility)
        assertEquals(View.VISIBLE, perfOverlayLite.visibility)
        assertEquals(View.GONE, perfOverlayBig.visibility)

        val params = perfOverlayView.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, params.gravity)
    }

    @Test
    fun onPerfUpdate_dispatchesToCorrectTextView() {
        prefConfig.enablePerfOverlay = true
        prefConfig.enablePerfOverlayLite = true
        val manager = createManager()

        manager.onPerfUpdate("60 FPS, 5ms")
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("60 FPS, 5ms", perfOverlayLite.text.toString())
    }

    @Test
    fun toggleHUD_flipsStateAndVisibilities() {
        prefConfig.enablePerfOverlay = false
        val manager = createManager()

        assertEquals(View.GONE, perfOverlayView.visibility)

        val newState = manager.toggleHUD()
        assertTrue(newState)
        assertEquals(View.VISIBLE, perfOverlayView.visibility)

        val revertedState = manager.toggleHUD()
        assertFalse(revertedState)
        assertEquals(View.GONE, perfOverlayView.visibility)
    }

    @Test
    fun onConnectionStatusUpdate_handlesPoorAndOkayStatus() {
        prefConfig.bitrate = 10000 // > 5000 -> slow_connection_msg
        val manager = createManager()

        manager.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_POOR)
        assertEquals(View.VISIBLE, manager.requestedNotificationOverlayVisibility)
        assertEquals(View.VISIBLE, notificationOverlayView.visibility)
        assertEquals(context.getString(R.string.slow_connection_msg), notificationOverlayView.text.toString())

        prefConfig.bitrate = 2000 // <= 5000 -> poor_connection_msg
        manager.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_POOR)
        assertEquals(context.getString(R.string.poor_connection_msg), notificationOverlayView.text.toString())

        manager.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_OKAY)
        assertEquals(View.GONE, manager.requestedNotificationOverlayVisibility)
        assertEquals(View.GONE, notificationOverlayView.visibility)
    }

    @Test
    fun pipLifecycle_hidesAndRestoresOverlays() {
        prefConfig.enablePerfOverlay = true
        prefConfig.enableBackMenu = true
        prefConfig.enableFloatingButton = true
        prefConfig.showOverlayZoomToggleButton = true

        val manager = createManager()

        assertEquals(View.VISIBLE, perfOverlayView.visibility)
        assertEquals(View.VISIBLE, floatingMenuButton.visibility)
        assertEquals(View.VISIBLE, overlayZoomButton.visibility)

        manager.onEnterPiP()
        assertEquals(View.GONE, perfOverlayView.visibility)
        assertEquals(View.GONE, floatingMenuButton.visibility)
        assertEquals(View.GONE, overlayZoomButton.visibility)

        manager.onExitPiP()
        assertEquals(View.VISIBLE, perfOverlayView.visibility)
        assertEquals(View.VISIBLE, floatingMenuButton.visibility)
        assertEquals(View.VISIBLE, overlayZoomButton.visibility)
    }

    @Test
    fun draggableTouchListener_tapTriggersCallback_dragMovesViewAndClamps() {
        var tapCount = 0
        val listener =
            DraggableTouchListener(
                decorBoundsProvider = { isWidth -> if (isWidth) 1000 else 800 },
                clickThresholdPx = 5f,
                onSingleTap = { tapCount++ },
            )

        val button =
            View(context).apply {
                layoutParams = FrameLayout.LayoutParams(100, 100)
                x = 200f
                y = 200f
            }

        // Tap test: ACTION_DOWN followed by ACTION_UP with no delta
        val now = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 250f, 250f, 0)
        listener.onTouch(button, downEvent)
        downEvent.recycle()

        val upEvent = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 250f, 250f, 0)
        listener.onTouch(button, upEvent)
        upEvent.recycle()

        assertEquals(1, tapCount)

        // Drag test: ACTION_DOWN, ACTION_MOVE exceeding threshold
        val down2 = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_DOWN, 200f, 200f, 0)
        listener.onTouch(button, down2)
        down2.recycle()

        val move2 = MotionEvent.obtain(now, now + 150, MotionEvent.ACTION_MOVE, 950f, 750f, 0)
        listener.onTouch(button, move2)
        move2.recycle()

        // Decor width 1000 - button width 0 (not laid out in test) -> clamped
        assertTrue(button.x > 200f)
        assertTrue(button.y > 200f)

        val up2 = MotionEvent.obtain(now, now + 200, MotionEvent.ACTION_UP, 950f, 750f, 0)
        listener.onTouch(button, up2)
        up2.recycle()

        // Since it was a drag, tapCount should remain 1
        assertEquals(1, tapCount)
    }

    @Test
    fun updateZoomButtonAppearance_setsExpectedResource() {
        val manager = createManager()
        manager.updateZoomButtonAppearance(isPanZoomMode = true)
        assertEquals(1.0f, overlayZoomButton.alpha, 0.01f)

        manager.updateZoomButtonAppearance(isPanZoomMode = false)
        assertEquals(1.0f, overlayZoomButton.alpha, 0.01f)
    }

    @Test
    fun performanceStatsParser_extractsMetricsAccurately() {
        val log = "Video stream: 1920x1080 60.00 FPS (AV1 Main)\n" +
            "Incoming frame rate: 60.00 FPS\n" +
            "Network latency: min 10ms, max 20ms, avg 15ms\n" +
            "Decode time: 3.5 ms\n" +
            "Frames dropped by your network: 0.5%"

        val stats = PerformanceStatsParser.parse(log)
        assertEquals(60.0f, stats.fps, 0.01f)
        assertEquals(15, stats.networkLatencyMs)
        assertEquals(3.5f, stats.decodeLatencyMs, 0.01f)
        assertEquals(0.5f, stats.packetLossPercentage, 0.01f)
        assertEquals("AV1 Main", stats.videoFormat)
    }

    @Test
    fun performanceStatsParser_extractsMediaCodecDecoderRendererOutput() {
        val rendererLog = "Video stream: 2560x1600 120.00 FPS (HEVC Main10)\n" +
            "Decoder: c2.qti.hevc.decoder\n" +
            "Rendering frame rate: 119.85 FPS\n" +
            "Average network latency: 9 ms\n" +
            "Average decoding time: 1.85 ms\n" +
            "Frames dropped by your network connection: 0.02%\n" +
            "Bandwidth: 65000 Kbps"

        val stats = PerformanceStatsParser.parse(rendererLog)
        assertEquals(119.85f, stats.fps, 0.01f)
        assertEquals(9, stats.networkLatencyMs)
        assertEquals(1.85f, stats.decodeLatencyMs, 0.01f)
        assertEquals(0.02f, stats.packetLossPercentage, 0.01f)
        assertEquals(65000, stats.bandwidthKbps)
        assertEquals("HEVC Main10", stats.videoFormat)
        assertEquals("c2.qti.hevc.decoder", stats.decoderName)
    }

    @Test
    fun performanceStatsParser_parsesLiteLogFormat() {
        // Realistic lite-format string produced by MediaCodecDecoderRenderer when enablePerfOverlayLite=true
        val liteLog = "60.00 FPS, avg 14ms"
        val stats = PerformanceStatsParser.parse(liteLog)
        assertEquals(60.0f, stats.fps, 0.01f)
        assertEquals(14, stats.networkLatencyMs)
    }

    @Test
    fun streamOverlayManager_withComposeView_dispatchesStateToViewModel() {
        prefConfig.enablePerfOverlay = true
        prefConfig.enablePerfOverlayLite = true
        val composeView = ComposeView(context)

        var disconnectRequested = false
        val manager = StreamOverlayManager(
            prefConfig = prefConfig,
            decorBoundsProvider = { 1920 },
            perfOverlayView = perfOverlayView,
            perfOverlayLite = perfOverlayLite,
            perfOverlayBig = perfOverlayBig,
            notificationOverlayView = notificationOverlayView,
            floatingMenuButton = floatingMenuButton,
            overlayZoomButton = overlayZoomButton,
            onMenuRequested = {},
            onZoomToggleRequested = {},
            composeView = composeView,
            onDisconnectRequested = { disconnectRequested = true },
        )

        // Initial state
        assertEquals(PerfOverlayMode.LITE, manager.viewModel.uiState.value.perfOverlayMode)
        assertTrue(manager.viewModel.uiState.value.isPerfOverlayEnabled)

        // Perf stats update
        manager.onPerfUpdate("120 FPS, avg 8ms, Decode time: 2.1 ms")
        assertEquals(120f, manager.viewModel.uiState.value.perfStats.fps, 0.01f)
        assertEquals(8, manager.viewModel.uiState.value.perfStats.networkLatencyMs)
        assertEquals(2.1f, manager.viewModel.uiState.value.perfStats.decodeLatencyMs, 0.01f)

        // Connection warning update
        manager.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_POOR)
        assertTrue(manager.viewModel.uiState.value.connectionWarning != null)

        manager.onConnectionStatusUpdate(MoonBridge.CONN_STATUS_OKAY)
        assertTrue(manager.viewModel.uiState.value.connectionWarning == null)

        // Menu presentation
        assertFalse(manager.isMenuOpen())
        manager.showMenu()
        assertTrue(manager.isMenuOpen())
        manager.hideMenu()
        assertFalse(manager.isMenuOpen())

        // Action hook
        manager.viewModel.requestDisconnect()
        assertTrue(disconnectRequested)

        // PiP transitions
        manager.onEnterPiP()
        assertTrue(manager.viewModel.uiState.value.isInPiP)
        manager.onExitPiP()
        assertFalse(manager.viewModel.uiState.value.isInPiP)
    }
}
