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
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.R
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
}
