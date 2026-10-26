@file:Suppress(
    "MagicNumber",
)

package io.github.staernid.sunbeam.stream.overlay

import android.app.AlertDialog
import android.content.Context
import android.os.Looper
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.Game
import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.binding.input.GameInputDevice
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import io.github.staernid.sunbeam.shadows.ShadowMoonBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowMoonBridge::class])
class StreamMenuCoordinatorTest {
    private lateinit var context: Context
    private lateinit var prefConfig: PreferenceConfiguration

    private class FakeGameMenuCallbacks : Game.GameMenuCallbacks {
        var showCount = 0
        var hideCount = 0
        var lastDevice: GameInputDevice? = null
        var menuOpen = false

        override fun showMenu(device: GameInputDevice?) {
            showCount++
            lastDevice = device
            menuOpen = true
        }

        override fun hideMenu() {
            hideCount++
            menuOpen = false
        }

        override fun isMenuOpen(): Boolean = menuOpen
    }

    private lateinit var fakeCallbacks: FakeGameMenuCallbacks
    private var isExternalDisplay = false

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        context = ContextThemeWrapper(app, R.style.AppTheme)
        prefConfig = PreferenceConfiguration.readPreferences(context)
        fakeCallbacks = FakeGameMenuCallbacks()
        isExternalDisplay = false
    }

    private fun createCoordinator(): StreamMenuCoordinator {
        return StreamMenuCoordinator(
            isOnExternalDisplay = { isExternalDisplay },
            getMenuCallbacks = { fakeCallbacks },
        )
    }

    @Test
    fun showGameMenu_onInternalDisplay_delegatesToCallbacks() {
        val coordinator = createCoordinator()

        coordinator.showGameMenu(null)
        assertEquals(1, fakeCallbacks.showCount)
        assertTrue(coordinator.isMenuOpen())

        coordinator.hideGameMenu()
        assertEquals(1, fakeCallbacks.hideCount)
        assertFalse(coordinator.isMenuOpen())
    }

    @Test
    fun showQuitConfirmDialog_confirmTriggersCallback() {
        val coordinator = createCoordinator()
        var quitConfirmed = false

        coordinator.showQuitConfirmDialog(context) {
            quitConfirmed = true
        }

        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(dialog)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(quitConfirmed)
    }

    @Test
    fun showSelectMouseModeDialog_selectsOptionAndApplies() {
        val coordinator = createCoordinator()
        var appliedMode = -1

        coordinator.showSelectMouseModeDialog(
            context = context,
            prefConfig = prefConfig,
            onApplyMouseMode = { appliedMode = it },
            onToggleLocalCursor = { appliedMode = 999 },
        )

        val dialog = ShadowAlertDialog.getLatestDialog() as? AlertDialog
        assertNotNull(dialog)

        // Click index 0
        dialog?.listView?.performItemClick(dialog.listView.getChildAt(0), 0, 0)
        assertEquals(0, appliedMode)
    }
}
