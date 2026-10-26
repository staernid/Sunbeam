package io.github.staernid.sunbeam.binding.input

import android.view.InputDevice
import android.view.KeyEvent
import io.github.staernid.sunbeam.nvstream.input.KeyboardPacket
import io.github.staernid.sunbeam.nvstream.input.MouseButtonPacket
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@Suppress("LongParameterList", "MaxLineLength")
class KeyboardInputHandlerTest {
    private class RecordingKeyboardPacketSender : KeyboardPacketSender {
        val sentKeys = mutableListOf<String>()
        val mouseButtonsDown = mutableListOf<Byte>()
        val mouseButtonsUp = mutableListOf<Byte>()
        val sentText = mutableListOf<String>()

        override fun sendKeyboardInput(
            keyCode: Short,
            keyAction: Byte,
            modifiers: Byte,
            flags: Byte,
        ) {
            val actionStr = if (keyAction == KeyboardPacket.KEY_DOWN) "DOWN" else "UP"
            sentKeys.add(
                "KEY:0x${Integer.toHexString(keyCode.toInt())}:$actionStr:mod=0x${Integer.toHexString(modifiers.toInt())}:flag=$flags",
            )
        }

        override fun sendMouseButtonDown(button: Byte) {
            mouseButtonsDown.add(button)
        }

        override fun sendMouseButtonUp(button: Byte) {
            mouseButtonsUp.add(button)
        }

        override fun sendUtf8Text(text: String) {
            sentText.add(text)
        }
    }

    private class RecordingActionListener : KeyboardActionListener {
        var grabToggled = 0
        var exitRequested = 0
        var cursorToggled = 0

        override fun onToggleInputGrab() {
            grabToggled++
        }

        override fun onExitRequested() {
            exitRequested++
        }

        override fun onToggleCursorVisibility() {
            cursorToggled++
        }
    }

    private fun createHandler(
        packetSender: RecordingKeyboardPacketSender = RecordingKeyboardPacketSender(),
        actionListener: RecordingActionListener = RecordingActionListener(),
        prefConfig: PreferenceConfiguration = PreferenceConfiguration(),
        isGameController: (InputDevice?) -> Boolean = { false },
        isGrabbed: () -> Boolean = { true },
        delayedExecutor: (Long, () -> Unit) -> Unit = { _, action -> action() },
    ): Triple<KeyboardInputHandler, RecordingKeyboardPacketSender, RecordingActionListener> {
        val translator = KeyboardTranslator(prefConfig)
        val handler =
            KeyboardInputHandler(
                packetSender = packetSender,
                keyboardTranslator = translator,
                prefConfig = prefConfig,
                actionListener = actionListener,
                isGameControllerDevice = isGameController,
                handleControllerButtonDown = { false },
                handleControllerButtonUp = { false },
                isInputGrabbed = isGrabbed,
                postDelayedAction = delayedExecutor,
            )
        return Triple(handler, packetSender, actionListener)
    }

    @Test
    fun handleKeyDown_forwardsTranslatedKey() {
        val (handler, sender, _) = createHandler()
        val event = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, 0)

        val handled = handler.handleKeyDown(event)
        assertTrue(handled)
        assertEquals(1, sender.sentKeys.size)
        assertTrue(sender.sentKeys.first().contains("DOWN"))
    }

    @Test
    fun handleKeyDown_addsShiftModifierForPlusKey() {
        val (handler, sender, _) = createHandler()
        val event = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PLUS, 0)

        val handled = handler.handleKeyDown(event)
        assertTrue(handled)
        assertEquals(1, sender.sentKeys.size)
        // MODIFIER_SHIFT is 0x01
        assertTrue(sender.sentKeys.first().contains("mod=0x1"))
    }

    @Test
    fun handleKeyDown_remapsRightAltToMeta_whenConfigured() {
        val prefConfig = PreferenceConfiguration()
        prefConfig.rightAltAsMeta = true
        val (handler, sender, _) = createHandler(prefConfig = prefConfig)

        val downEvent = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_RIGHT, 0)
        val downHandled = handler.handleKeyDown(downEvent)
        assertTrue(downHandled)
        assertEquals(1, sender.sentKeys.size)
        // VK_LWIN is 0x5b
        assertTrue(sender.sentKeys.first().contains("KEY:0x5b:DOWN"))

        val upEvent = KeyEvent(0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_RIGHT, 0)
        val upHandled = handler.handleKeyUp(upEvent)
        assertTrue(upHandled)
        assertEquals(2, sender.sentKeys.size)
        assertTrue(sender.sentKeys[1].contains("KEY:0x5b:UP"))
    }

    @Test
    fun handleKeyDown_shortcutExit_triggeredOnAllModifiersUp() {
        val (handler, _, listener) = createHandler()

        // Press Ctrl + Alt + Shift
        handler.handleSpecialKeys(KeyEvent.KEYCODE_CTRL_LEFT, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_ALT_LEFT, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_SHIFT_LEFT, down = true)

        // Press Q (quit shortcut)
        val consumed = handler.handleSpecialKeys(KeyEvent.KEYCODE_Q, down = true)
        assertTrue(consumed)
        assertEquals(0, listener.exitRequested)

        // Release Q
        handler.handleSpecialKeys(KeyEvent.KEYCODE_Q, down = false)
        assertEquals(0, listener.exitRequested)

        // Release Ctrl, Alt, Shift
        handler.handleSpecialKeys(KeyEvent.KEYCODE_CTRL_LEFT, down = false)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_ALT_LEFT, down = false)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_SHIFT_LEFT, down = false)

        assertEquals(1, listener.exitRequested)
    }

    @Test
    fun handleKeyDown_shortcutToggleGrab_triggeredOnAllModifiersUp() {
        val (handler, _, listener) = createHandler()

        // Press Ctrl + Alt + Shift + Z
        handler.handleSpecialKeys(KeyEvent.KEYCODE_CTRL_LEFT, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_ALT_LEFT, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_SHIFT_LEFT, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_Z, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_Z, down = false)

        // Release modifiers
        handler.handleSpecialKeys(KeyEvent.KEYCODE_CTRL_LEFT, down = false)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_ALT_LEFT, down = false)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_SHIFT_LEFT, down = false)

        assertEquals(1, listener.grabToggled)
    }

    @Test
    fun handleKeyDown_shortcutToggleCursor_triggeredOnAllModifiersUp() {
        val (handler, _, listener) = createHandler()

        // Press Ctrl + Alt + Shift + C
        handler.handleSpecialKeys(KeyEvent.KEYCODE_CTRL_LEFT, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_ALT_LEFT, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_SHIFT_LEFT, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_C, down = true)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_C, down = false)

        // Release modifiers
        handler.handleSpecialKeys(KeyEvent.KEYCODE_CTRL_LEFT, down = false)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_ALT_LEFT, down = false)
        handler.handleSpecialKeys(KeyEvent.KEYCODE_SHIFT_LEFT, down = false)

        assertEquals(1, listener.cursorToggled)
    }

    @Test
    fun handleKeyDown_ignoresVirtualHardKeys() {
        val (handler, sender, _) = createHandler()
        val event =
            KeyEvent(
                0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME, 0,
                0, 0, 0, KeyEvent.FLAG_VIRTUAL_HARD_KEY,
            )

        val handled = handler.handleKeyDown(event)
        assertFalse(handled)
        assertEquals(0, sender.sentKeys.size)
    }

    @Test
    fun handleKeyDown_ignoresSyntheticEvents_whenConfigured() {
        val prefConfig = PreferenceConfiguration()
        prefConfig.ignoreSynthEvents = true
        val (handler, sender, _) = createHandler(prefConfig = prefConfig)
        // Device id -1 is a synthetic event
        val event =
            KeyEvent(
                0L,
                0L,
                KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_A,
                0,
                0,
                -1,
                0,
            )

        val handled = handler.handleKeyDown(event)
        assertFalse(handled)
        assertEquals(0, sender.sentKeys.size)
    }

    @Test
    fun handleKeyDown_mouseBackSendsRightClick_whenMouseNavDisabled() {
        val prefConfig = PreferenceConfiguration()
        prefConfig.mouseNavButtons = false
        val (handler, sender, _) = createHandler(prefConfig = prefConfig)

        val downEvent =
            KeyEvent(
                0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0,
                0, 1, 0, 0, InputDevice.SOURCE_MOUSE,
            )
        val downHandled = handler.handleKeyDown(downEvent)
        assertTrue(downHandled)
        assertEquals(listOf(MouseButtonPacket.BUTTON_RIGHT), sender.mouseButtonsDown)

        val upEvent =
            KeyEvent(
                0L, 0L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0,
                0, 1, 0, 0, InputDevice.SOURCE_MOUSE,
            )
        val upHandled = handler.handleKeyUp(upEvent)
        assertTrue(upHandled)
        assertEquals(listOf(MouseButtonPacket.BUTTON_RIGHT), sender.mouseButtonsUp)
    }

    @Test
    fun handleKeyMultiple_sendsUtf8Text() {
        val (handler, sender, _) = createHandler()
        val event = KeyEvent(0L, "Hello", 0, 0)

        val handled = handler.handleKeyMultiple(event)
        assertTrue(handled)
        assertEquals(listOf("Hello"), sender.sentText)
    }

    @Test
    fun sendKeys_sendsSequenceWithModifiersAndDelayedRelease() {
        val (handler, sender, _) = createHandler()
        // VK_CONTROL = 0x11, VK_C = 0x43
        val keys = shortArrayOf(0x11, 0x43)

        handler.sendKeys(keys)
        // 2 key downs + 2 key ups = 4 events sent
        assertEquals(4, sender.sentKeys.size)
        assertTrue(sender.sentKeys[0].contains("DOWN"))
        assertTrue(sender.sentKeys[1].contains("DOWN"))
        assertTrue(sender.sentKeys[2].contains("UP"))
        assertTrue(sender.sentKeys[3].contains("UP"))
    }
}
