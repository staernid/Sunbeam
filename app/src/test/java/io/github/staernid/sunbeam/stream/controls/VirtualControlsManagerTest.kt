@file:Suppress(
    "MagicNumber",
)

package io.github.staernid.sunbeam.stream.controls

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.binding.input.ControllerHandler
import io.github.staernid.sunbeam.binding.input.virtual_controller.VirtualController
import io.github.staernid.sunbeam.binding.input.virtual_controller.keyboard.KeyBoardController
import io.github.staernid.sunbeam.binding.input.virtual_controller.keyboard.KeyBoardLayoutController
import io.github.staernid.sunbeam.nvstream.NvConnection
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import io.github.staernid.sunbeam.shadows.ShadowMoonBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowMoonBridge::class])
class VirtualControlsManagerTest {
    private lateinit var context: Context
    private lateinit var rootLayout: FrameLayout
    private lateinit var prefConfig: PreferenceConfiguration
    private lateinit var mockControllerHandler: ControllerHandler
    private lateinit var mockNvConnection: NvConnection

    private lateinit var mockVirtualController: VirtualController
    private lateinit var mockKeyBoardController: KeyBoardController
    private lateinit var mockKeyBoardLayoutController: KeyBoardLayoutController

    private var externalDisplayActive = false
    private var externalKeyboardToggledCount = 0

    private lateinit var manager: VirtualControlsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        rootLayout = FrameLayout(context)
        prefConfig = PreferenceConfiguration.readPreferences(context)
        mockControllerHandler = mock(ControllerHandler::class.java)
        mockNvConnection = mock(NvConnection::class.java)

        mockVirtualController = mock(VirtualController::class.java)
        mockKeyBoardController = mock(KeyBoardController::class.java)
        mockKeyBoardLayoutController = mock(KeyBoardLayoutController::class.java)

        externalDisplayActive = false
        externalKeyboardToggledCount = 0

        manager =
            VirtualControlsManager(
                context = context,
                rootLayout = rootLayout,
                prefConfig = prefConfig,
                controllerHandler = mockControllerHandler,
                connectionProvider = { mockNvConnection },
                isOnExternalDisplay = { externalDisplayActive },
                onToggleExternalKeyboard = { externalKeyboardToggledCount++ },
                virtualControllerFactory = { _, _, _ -> mockVirtualController },
                keyboardControllerFactory = { _, _, _ -> mockKeyBoardController },
                keyboardLayoutControllerFactory = { _, _, _ -> mockKeyBoardLayoutController },
            )
    }

    @Test
    fun setupInitialControls_whenOnscreenControllerEnabled_initializesVirtualController() {
        prefConfig.onscreenController = true
        prefConfig.hideOSCWhenHasGamepad = false
        prefConfig.enableKeyboard = false

        manager.setupInitialControls()

        assertNotNull(manager.virtualController)
        verify(mockVirtualController).refreshLayout()
        verify(mockVirtualController).show()
        assertNull(manager.keyBoardController)
    }

    @Test
    fun setupInitialControls_whenHideOscWithGamepad_skipsIfControllerConnected() {
        prefConfig.onscreenController = true
        prefConfig.hideOSCWhenHasGamepad = true
        `when`(mockControllerHandler.hasController()).thenReturn(true)

        manager.setupInitialControls()

        assertNull(manager.virtualController)
        verify(mockVirtualController, never()).show()
    }

    @Test
    fun setupInitialControls_whenHideOscWithGamepad_initializesIfNoControllerConnected() {
        prefConfig.onscreenController = true
        prefConfig.hideOSCWhenHasGamepad = true
        `when`(mockControllerHandler.hasController()).thenReturn(false)

        manager.setupInitialControls()

        assertNotNull(manager.virtualController)
        verify(mockVirtualController).show()
    }

    @Test
    fun setupInitialControls_whenKeyboardEnabled_initializesKeyboardController() {
        prefConfig.onscreenController = false
        prefConfig.enableKeyboard = true

        manager.setupInitialControls()

        assertNotNull(manager.keyBoardController)
        verify(mockKeyBoardController).refreshLayout()
        verify(mockKeyBoardController).show()
    }

    @Test
    fun toggleVirtualController_whenNull_initializesAndShows() {
        prefConfig.onscreenController = false

        manager.toggleVirtualController()

        assertNotNull(manager.virtualController)
        assertTrue(prefConfig.onscreenController)
        verify(mockVirtualController).show()
    }

    @Test
    fun toggleVirtualController_whenInitialized_switchesShowHide() {
        manager.initVirtualController()
        `when`(mockVirtualController.switchShowHide()).thenReturn(0)

        manager.toggleVirtualController()

        verify(mockVirtualController).switchShowHide()
        assertFalse(prefConfig.onscreenController)

        `when`(mockVirtualController.switchShowHide()).thenReturn(1)
        manager.toggleVirtualController()
        assertTrue(prefConfig.onscreenController)
    }

    @Test
    fun toggleKeyboardController_initializesOrTogglesVisibility() {
        manager.toggleKeyboardController()

        assertNotNull(manager.keyBoardController)
        verify(mockKeyBoardController).show()

        manager.toggleKeyboardController()
        verify(mockKeyBoardController).toggleVisibility()
    }

    @Test
    fun toggleFullKeyboard_whenOnExternalDisplay_routesToExternalHandler() {
        externalDisplayActive = true

        manager.toggleFullKeyboard()

        assertEquals(1, externalKeyboardToggledCount)
        assertNull(manager.keyBoardLayoutController)
    }

    @Test
    fun toggleFullKeyboard_whenLocal_initializesOrToggles() {
        externalDisplayActive = false

        manager.toggleFullKeyboard()

        assertNotNull(manager.keyBoardLayoutController)
        verify(mockKeyBoardLayoutController).show()

        manager.toggleFullKeyboard()
        verify(mockKeyBoardLayoutController).toggleVisibility()
    }

    @Test
    fun isKeyboardLayoutVisible_reflectsLayoutControllerShownState() {
        assertFalse(manager.isKeyboardLayoutVisible())

        manager.initKeyboardLayoutController()
        mockKeyBoardLayoutController.shown = false
        assertFalse(manager.isKeyboardLayoutVisible())

        mockKeyBoardLayoutController.shown = true
        assertTrue(manager.isKeyboardLayoutVisible())
    }

    @Test
    fun isVirtualControllerEditing_identifiesEditModes() {
        assertFalse(manager.isVirtualControllerEditing())

        manager.initVirtualController()

        `when`(mockVirtualController.controllerMode).thenReturn(VirtualController.ControllerMode.Active)
        assertFalse(manager.isVirtualControllerEditing())

        `when`(mockVirtualController.controllerMode).thenReturn(VirtualController.ControllerMode.MoveButtons)
        assertTrue(manager.isVirtualControllerEditing())

        `when`(mockVirtualController.controllerMode).thenReturn(VirtualController.ControllerMode.ResizeButtons)
        assertTrue(manager.isVirtualControllerEditing())
    }

    @Test
    fun onConfigurationChanged_refreshesAllActiveControllers() {
        manager.initVirtualController()
        manager.initKeyboardController()
        manager.initKeyboardLayoutController()

        clearInvocations(mockVirtualController, mockKeyBoardController, mockKeyBoardLayoutController)

        manager.onConfigurationChanged()

        verify(mockVirtualController).refreshLayout()
        verify(mockKeyBoardController).refreshLayout()
        verify(mockKeyBoardLayoutController).refreshLayout()
    }

    @Test
    fun pipTransitions_hidesAndRestoresControllers() {
        manager.initVirtualController()
        manager.initKeyboardController()
        manager.initKeyboardLayoutController()

        mockKeyBoardController.shown = true
        mockKeyBoardLayoutController.shown = true

        clearInvocations(mockVirtualController, mockKeyBoardController, mockKeyBoardLayoutController)

        manager.onEnterPiP()

        verify(mockVirtualController).hide()
        verify(mockKeyBoardController).hide(true)
        verify(mockKeyBoardLayoutController).hide(true)

        manager.onExitPiP()

        verify(mockVirtualController).show()
        verify(mockKeyBoardController).show()
        verify(mockKeyBoardLayoutController).show()
    }

    @Test
    fun onStopAndDestroy_cleansUpProperly() {
        manager.initVirtualController()
        manager.initKeyboardController()
        manager.initKeyboardLayoutController()

        clearInvocations(mockVirtualController, mockKeyBoardController, mockKeyBoardLayoutController)

        manager.onStop()

        verify(mockVirtualController).hide()
        verify(mockKeyBoardController).hide()
        verify(mockKeyBoardLayoutController).hide()

        manager.destroy()
        assertNull(manager.virtualController)
        assertNull(manager.keyBoardController)
        assertNull(manager.keyBoardLayoutController)
    }
}
