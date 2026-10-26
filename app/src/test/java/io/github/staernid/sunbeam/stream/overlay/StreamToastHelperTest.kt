package io.github.staernid.sunbeam.stream.overlay

import android.content.Context
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.shadows.ShadowMoonBridge
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowMoonBridge::class])
class StreamToastHelperTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun showToast_displaysExpectedText() {
        StreamToastHelper.showToast(context, "Hello Sunbeam", Toast.LENGTH_SHORT)
        assertEquals("Hello Sunbeam", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun showPanZoomToast_displaysEnabledAndDisabledStrings() {
        StreamToastHelper.showPanZoomToast(context, true)
        assertEquals(context.getString(R.string.pan_zoom_mode_enabled), ShadowToast.getTextOfLatestToast())

        StreamToastHelper.showPanZoomToast(context, false)
        assertEquals(context.getString(R.string.pan_zoom_mode_disabled), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun showLatencyToast_displaysLatencyMessage() {
        StreamToastHelper.showLatencyToast(context, "Latency: 8.5ms")
        assertEquals("Latency: 8.5ms", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun showQuitSuccessToast_displaysQuitMessage() {
        StreamToastHelper.showQuitSuccessToast(context, "Steam")
        val expected = "${context.getString(R.string.applist_quit_success)} Steam"
        assertEquals(expected, ShadowToast.getTextOfLatestToast())
    }
}
