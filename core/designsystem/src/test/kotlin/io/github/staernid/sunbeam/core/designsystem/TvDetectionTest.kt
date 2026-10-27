package io.github.staernid.sunbeam.core.designsystem

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.core.designsystem.adaptive.isTvDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TvDetectionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `standard phone returns false for isTvDevice`() {
        assertFalse(context.isTvDevice())
    }

    @Test
    fun `device with leanback feature returns true`() {
        val pm = shadowOf(context.packageManager)
        pm.setSystemFeature(PackageManager.FEATURE_LEANBACK, true)

        assertTrue(context.isTvDevice())
    }

    @Suppress("DEPRECATION")
    @Test
    fun `device with television ui mode returns true`() {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val shadowUiMode = shadowOf(uiModeManager)
        shadowUiMode.currentModeType = Configuration.UI_MODE_TYPE_TELEVISION

        assertTrue(context.isTvDevice())
    }
}
