@file:Suppress(
    "LongParameterList",
    "TooManyFunctions",
)

package io.github.staernid.sunbeam.stream.controls

import android.content.Context
import android.widget.FrameLayout
import io.github.staernid.sunbeam.binding.input.ControllerHandler
import io.github.staernid.sunbeam.binding.input.virtual_controller.VirtualController
import io.github.staernid.sunbeam.binding.input.virtual_controller.keyboard.KeyBoardController
import io.github.staernid.sunbeam.binding.input.virtual_controller.keyboard.KeyBoardLayoutController
import io.github.staernid.sunbeam.nvstream.NvConnection
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import io.github.staernid.sunbeam.utils.ExternalDisplayControlActivity

typealias VirtualControllerFactory = (ControllerHandler?, FrameLayout, Context) -> VirtualController
typealias KeyboardControllerFactory = (NvConnection?, FrameLayout, Context) -> KeyBoardController
typealias KeyboardLayoutFactory = (FrameLayout, Context, PreferenceConfiguration) -> KeyBoardLayoutController

/**
 * Manages the lifecycle, visibility, and layout updates of on-screen virtual controllers,
 * special key floating layouts, and the full on-screen keyboard.
 */
class VirtualControlsManager
    @JvmOverloads
    constructor(
        private val context: Context,
        private val rootLayout: FrameLayout,
        private val prefConfig: PreferenceConfiguration,
        private val controllerHandler: ControllerHandler?,
        private val connectionProvider: () -> NvConnection?,
        private val isOnExternalDisplay: () -> Boolean = { false },
        private val onToggleExternalKeyboard: Runnable =
            Runnable {
                ExternalDisplayControlActivity.toggleFullKeyboard()
            },
        private val virtualControllerFactory: VirtualControllerFactory = { ch, layout, ctx ->
            VirtualController(ch, layout, ctx)
        },
        private val keyboardControllerFactory: KeyboardControllerFactory = { conn, layout, ctx ->
            KeyBoardController(conn, layout, ctx)
        },
        private val keyboardLayoutControllerFactory: KeyboardLayoutFactory = { layout, ctx, prefs ->
            KeyBoardLayoutController(layout, ctx, prefs)
        },
    ) {
        var virtualController: VirtualController? = null
            private set

        var keyBoardController: KeyBoardController? = null
            private set

        var keyBoardLayoutController: KeyBoardLayoutController? = null
            private set

        /**
         * Initializes controllers configured to show on stream launch according to preferences.
         */
        fun setupInitialControls() {
            if (prefConfig.onscreenController) {
                if (prefConfig.hideOSCWhenHasGamepad) {
                    if (controllerHandler == null || !controllerHandler.hasController()) {
                        initVirtualController()
                    }
                } else {
                    initVirtualController()
                }
            }

            if (prefConfig.enableKeyboard) {
                initKeyboardController()
            }
        }

        /**
         * Instantiates and displays the virtual gamepad controller overlay.
         */
        fun initVirtualController() {
            val controller = virtualControllerFactory(controllerHandler, rootLayout, context)
            controller.refreshLayout()
            controller.show()
            virtualController = controller
        }

        /**
         * Instantiates and displays the special keys keyboard controller overlay.
         */
        fun initKeyboardController() {
            val connection = connectionProvider()
            val controller = keyboardControllerFactory(connection, rootLayout, context)
            controller.refreshLayout()
            controller.show()
            keyBoardController = controller
        }

        /**
         * Instantiates and displays the full on-screen keyboard layout overlay.
         */
        fun initKeyboardLayoutController() {
            val controller = keyboardLayoutControllerFactory(rootLayout, context, prefConfig)
            controller.refreshLayout()
            controller.show()
            keyBoardLayoutController = controller
        }

        /**
         * Toggles visibility of the virtual gamepad controller overlay and updates preferences.
         */
        fun toggleVirtualController() {
            val controller = virtualController
            if (controller == null) {
                initVirtualController()
                prefConfig.onscreenController = true
                return
            }
            prefConfig.onscreenController = controller.switchShowHide() != 0
        }

        /**
         * Toggles visibility of the special keys keyboard controller overlay.
         */
        fun toggleKeyboardController() {
            val controller = keyBoardController
            if (controller == null) {
                initKeyboardController()
                return
            }
            controller.toggleVisibility()
        }

        /**
         * Toggles visibility of the full virtual keyboard overlay, routing to external display if active.
         */
        fun toggleFullKeyboard() {
            if (isOnExternalDisplay()) {
                onToggleExternalKeyboard.run()
                return
            }
            val controller = keyBoardLayoutController
            if (controller == null) {
                initKeyboardLayoutController()
                return
            }
            controller.toggleVisibility()
        }

        /**
         * Checks if the full on-screen keyboard layout is currently visible.
         */
        fun isKeyboardLayoutVisible(): Boolean = keyBoardLayoutController?.shown == true

        /**
         * Checks if the virtual gamepad controller is actively being reconfigured (moved, resized, or toggled).
         */
        fun isVirtualControllerEditing(): Boolean {
            val controller = virtualController ?: return false
            val mode = controller.controllerMode
            return mode == VirtualController.ControllerMode.MoveButtons ||
                mode == VirtualController.ControllerMode.ResizeButtons ||
                mode == VirtualController.ControllerMode.DisableEnableButtons
        }

        /**
         * Starts or advances the interactive layout configuration mode on the virtual controller.
         */
        fun cycleVirtualControllerEditMode() {
            if (virtualController == null) {
                initVirtualController()
                prefConfig.onscreenController = true
            }
            virtualController?.cycleConfigurationMode()
        }

        /**
         * Refreshes the layout of all instantiated controllers following configuration / orientation changes.
         */
        fun onConfigurationChanged() {
            virtualController?.refreshLayout()
            keyBoardController?.refreshLayout()
            keyBoardLayoutController?.refreshLayout()
        }

        /**
         * Hides all active on-screen controls when entering Picture-in-Picture mode.
         */
        fun onEnterPiP() {
            virtualController?.hide()
            keyBoardController?.let {
                if (it.shown) {
                    it.hide(true)
                }
            }
            keyBoardLayoutController?.let {
                if (it.shown) {
                    it.hide(true)
                }
            }
        }

        /**
         * Restores on-screen controls to their previous visible state when leaving Picture-in-Picture mode.
         */
        fun onExitPiP() {
            virtualController?.show()
            keyBoardController?.let {
                if (it.shown) {
                    it.show()
                }
            }
            keyBoardLayoutController?.let {
                if (it.shown) {
                    it.show()
                }
            }
        }

        /**
         * Hides all controllers when the stream is paused or stopped.
         */
        fun onStop() {
            virtualController?.hide()
            keyBoardController?.hide()
            keyBoardLayoutController?.hide()
        }

        /**
         * Cleans up references when the stream activity is destroyed.
         */
        fun destroy() {
            virtualController = null
            keyBoardController = null
            keyBoardLayoutController = null
        }
    }
