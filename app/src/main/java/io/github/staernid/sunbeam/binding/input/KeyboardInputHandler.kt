package io.github.staernid.sunbeam.binding.input

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import io.github.staernid.sunbeam.binding.input.KeyboardTranslator.VK_LWIN
import io.github.staernid.sunbeam.binding.input.KeyboardTranslator.getModifier
import io.github.staernid.sunbeam.nvstream.NvConnection
import io.github.staernid.sunbeam.nvstream.input.KeyboardPacket
import io.github.staernid.sunbeam.nvstream.input.MouseButtonPacket
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration

interface KeyboardPacketSender {
    fun sendKeyboardInput(
        keyCode: Short,
        keyAction: Byte,
        modifiers: Byte,
        flags: Byte,
    )

    fun sendMouseButtonDown(button: Byte)

    fun sendMouseButtonUp(button: Byte)

    fun sendUtf8Text(text: String)
}

class NvKeyboardPacketSender(private val connection: NvConnection) : KeyboardPacketSender {
    override fun sendKeyboardInput(
        keyCode: Short,
        keyAction: Byte,
        modifiers: Byte,
        flags: Byte,
    ) {
        connection.sendKeyboardInput(keyCode, keyAction, modifiers, flags)
    }

    override fun sendMouseButtonDown(button: Byte) {
        connection.sendMouseButtonDown(button)
    }

    override fun sendMouseButtonUp(button: Byte) {
        connection.sendMouseButtonUp(button)
    }

    override fun sendUtf8Text(text: String) {
        connection.sendUtf8Text(text)
    }
}

interface KeyboardActionListener {
    fun onToggleInputGrab()

    fun onExitRequested()

    fun onToggleCursorVisibility()
}

@Suppress(
    "TooManyFunctions",
    "LongParameterList",
    "LongMethod",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
)
class KeyboardInputHandler(
    private val packetSender: KeyboardPacketSender,
    private val keyboardTranslator: KeyboardTranslator,
    private val prefConfig: PreferenceConfiguration,
    private val actionListener: KeyboardActionListener,
    private val isGameControllerDevice: (InputDevice?) -> Boolean = { ControllerHandler.isGameControllerDevice(it) },
    private val handleControllerButtonDown: (KeyEvent) -> Boolean = { false },
    private val handleControllerButtonUp: (KeyEvent) -> Boolean = { false },
    private val isInputGrabbed: () -> Boolean = { true },
    private val postDelayedAction: (delayMs: Long, action: () -> Unit) -> Unit = { delayMs, action ->
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(action, delayMs)
    },
) {
    constructor(
        connection: NvConnection,
        keyboardTranslator: KeyboardTranslator,
        prefConfig: PreferenceConfiguration,
        actionListener: KeyboardActionListener,
        controllerHandler: ControllerHandler?,
        isInputGrabbed: () -> Boolean,
    ) : this(
        packetSender = NvKeyboardPacketSender(connection),
        keyboardTranslator = keyboardTranslator,
        prefConfig = prefConfig,
        actionListener = actionListener,
        isGameControllerDevice = { ControllerHandler.isGameControllerDevice(it) },
        handleControllerButtonDown = { controllerHandler?.handleButtonDown(it) ?: false },
        handleControllerButtonUp = { controllerHandler?.handleButtonUp(it) ?: false },
        isInputGrabbed = isInputGrabbed,
    )

    companion object {
        const val KEY_UP_DELAY_MS = 100L
        private const val META_KEY_FALLBACK_CODE: Short = 0x5b
        private const val REQUIRED_SHORTCUT_MODIFIERS =
            KeyboardPacket.MODIFIER_CTRL.toInt() or
                KeyboardPacket.MODIFIER_ALT.toInt() or
                KeyboardPacket.MODIFIER_SHIFT.toInt()
    }

    var modifierFlags: Int = 0
        private set

    var specialKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN
        private set

    var waitingForAllModifiersUp: Boolean = false
        private set

    fun resetState() {
        modifierFlags = 0
        specialKeyCode = KeyEvent.KEYCODE_UNKNOWN
        waitingForAllModifiersUp = false
    }

    fun applyKeySpecificModifiers(
        keyCode: Int,
        modifier: Byte,
    ): Byte {
        var result = modifier
        if (keyCode == KeyEvent.KEYCODE_PLUS) {
            result = (result.toInt() or KeyboardPacket.MODIFIER_SHIFT.toInt()).toByte()
        }
        return result
    }

    fun getModifierState(event: KeyEvent): Byte {
        var modifier = modifierFlags.toByte()
        if (event.isShiftPressed) {
            modifier = (modifier.toInt() or KeyboardPacket.MODIFIER_SHIFT.toInt()).toByte()
        }
        if (event.isCtrlPressed) {
            modifier = (modifier.toInt() or KeyboardPacket.MODIFIER_CTRL.toInt()).toByte()
        }
        if (event.isAltPressed) {
            if (!prefConfig.rightAltAsMeta || (event.metaState and KeyEvent.META_ALT_LEFT_ON) != 0) {
                modifier = (modifier.toInt() or KeyboardPacket.MODIFIER_ALT.toInt()).toByte()
            }
        }
        if (event.isMetaPressed) {
            modifier = (modifier.toInt() or KeyboardPacket.MODIFIER_META.toInt()).toByte()
        }
        return applyKeySpecificModifiers(event.keyCode, modifier)
    }

    fun handleKeyDown(event: KeyEvent): Boolean {
        if ((event.flags and KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false
        }

        val deviceId = event.deviceId
        if (prefConfig.ignoreSynthEvents && deviceId <= 0) {
            return false
        }

        val eventSource = event.source
        if ((eventSource == InputDevice.SOURCE_MOUSE || eventSource == InputDevice.SOURCE_MOUSE_RELATIVE) &&
            event.keyCode == KeyEvent.KEYCODE_BACK
        ) {
            if (!prefConfig.mouseNavButtons) {
                packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT)
            }
            return true
        }

        var handled = false
        if (isGameControllerDevice(event.device)) {
            handled = handleControllerButtonDown(event)
        }

        if (!handled) {
            if (handleSpecialKeys(event.keyCode, down = true)) {
                return true
            }

            if (!isInputGrabbed()) {
                return false
            }

            var translated = keyboardTranslator.translate(event.keyCode, event.scanCode, deviceId)
            if (translated.toInt() == 0) {
                if (prefConfig.backAsMeta && event.keyCode == KeyEvent.KEYCODE_BACK) {
                    translated = META_KEY_FALLBACK_CODE
                } else {
                    val unicodeChar = event.unicodeChar
                    if ((unicodeChar and KeyCharacterMap.COMBINING_ACCENT) == 0 &&
                        (unicodeChar and KeyCharacterMap.COMBINING_ACCENT_MASK) != 0
                    ) {
                        packetSender.sendUtf8Text(unicodeChar.toChar().toString())
                        return true
                    }
                    return false
                }
            }

            if (event.repeatCount > 0) {
                return true
            }

            val flags =
                if (keyboardTranslator.hasNormalizedMapping(event.keyCode, deviceId)) {
                    0.toByte()
                } else {
                    MoonBridge.SS_KBE_FLAG_NON_NORMALIZED.toByte()
                }

            packetSender.sendKeyboardInput(
                translated,
                KeyboardPacket.KEY_DOWN,
                getModifierState(event),
                flags,
            )
        }

        return true
    }

    fun handleKeyUp(event: KeyEvent): Boolean {
        if ((event.flags and KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false
        }

        val deviceId = event.deviceId
        if (prefConfig.ignoreSynthEvents && deviceId <= 0) {
            return false
        }

        val eventSource = event.source
        if ((eventSource == InputDevice.SOURCE_MOUSE || eventSource == InputDevice.SOURCE_MOUSE_RELATIVE) &&
            event.keyCode == KeyEvent.KEYCODE_BACK
        ) {
            if (!prefConfig.mouseNavButtons) {
                packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT)
            }
            return true
        }

        var handled = false
        if (isGameControllerDevice(event.device)) {
            handled = handleControllerButtonUp(event)
        }

        if (!handled) {
            if (handleSpecialKeys(event.keyCode, down = false)) {
                return true
            }

            if (!isInputGrabbed()) {
                return false
            }

            var translated = keyboardTranslator.translate(event.keyCode, event.scanCode, deviceId)
            if (translated.toInt() == 0) {
                if (prefConfig.backAsMeta && event.keyCode == KeyEvent.KEYCODE_BACK) {
                    translated = META_KEY_FALLBACK_CODE
                } else {
                    val unicodeChar = event.unicodeChar
                    return (unicodeChar and KeyCharacterMap.COMBINING_ACCENT) == 0 &&
                        (unicodeChar and KeyCharacterMap.COMBINING_ACCENT_MASK) != 0
                }
            }

            val flags =
                if (keyboardTranslator.hasNormalizedMapping(event.keyCode, deviceId)) {
                    0.toByte()
                } else {
                    MoonBridge.SS_KBE_FLAG_NON_NORMALIZED.toByte()
                }

            packetSender.sendKeyboardInput(
                translated,
                KeyboardPacket.KEY_UP,
                getModifierState(event),
                flags,
            )
        }

        return true
    }

    @Suppress("DEPRECATION")
    fun handleVirtualKeyEvent(
        keyCode: Int,
        down: Boolean,
    ) {
        val keyMap = keyboardTranslator.translate(keyCode, 0, -1)
        if (keyMap.toInt() != 0) {
            if (handleSpecialKeys(keyCode, down)) {
                return
            }
            val modifier = applyKeySpecificModifiers(keyCode, modifierFlags.toByte())
            packetSender.sendKeyboardInput(
                keyMap,
                if (down) KeyboardPacket.KEY_DOWN else KeyboardPacket.KEY_UP,
                modifier,
                0,
            )
        }
    }

    fun handleKeyMultiple(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_UNKNOWN || event.characters == null) {
            return false
        }

        packetSender.sendUtf8Text(event.characters)
        return true
    }

    fun sendKeys(keys: ShortArray) {
        var modifier: Byte = 0
        for (key in keys) {
            packetSender.sendKeyboardInput(key, KeyboardPacket.KEY_DOWN, modifier, 0)
            modifier = (modifier.toInt() or getModifier(key).toInt()).toByte()
        }

        postDelayedAction(KEY_UP_DELAY_MS) {
            for (i in keys.indices.reversed()) {
                val key = keys[i]
                modifier = (modifier.toInt() and getModifier(key).toInt().inv()).toByte()
                packetSender.sendKeyboardInput(key, KeyboardPacket.KEY_UP, modifier, 0)
            }
        }
    }

    fun handleSpecialKeys(
        androidKeyCode: Int,
        down: Boolean,
    ): Boolean {
        var modifierMask = 0
        var nonModifierKeyCode = KeyEvent.KEYCODE_UNKNOWN

        when (androidKeyCode) {
            KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> {
                modifierMask = KeyboardPacket.MODIFIER_CTRL.toInt()
            }
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                modifierMask = KeyboardPacket.MODIFIER_SHIFT.toInt()
            }
            KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> {
                if (prefConfig.rightAltAsMeta && androidKeyCode == KeyEvent.KEYCODE_ALT_RIGHT) {
                    if (down) {
                        modifierFlags = modifierFlags or KeyboardPacket.MODIFIER_META.toInt()
                    } else {
                        modifierFlags = modifierFlags and KeyboardPacket.MODIFIER_META.toInt().inv()
                    }
                    packetSender.sendKeyboardInput(
                        VK_LWIN.toShort(),
                        if (down) KeyboardPacket.KEY_DOWN else KeyboardPacket.KEY_UP,
                        modifierFlags.toByte(),
                        0,
                    )
                    return true
                }
                modifierMask = KeyboardPacket.MODIFIER_ALT.toInt()
            }
            KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> {
                modifierMask = KeyboardPacket.MODIFIER_META.toInt()
            }
            else -> {
                nonModifierKeyCode = androidKeyCode
            }
        }

        if (down) {
            modifierFlags = modifierFlags or modifierMask
        } else {
            modifierFlags = modifierFlags and modifierMask.inv()
        }

        if (waitingForAllModifiersUp || specialKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
            if (specialKeyCode == androidKeyCode) {
                return true
            } else if (modifierFlags != 0) {
                return down
            } else {
                when (specialKeyCode) {
                    KeyEvent.KEYCODE_Z -> actionListener.onToggleInputGrab()
                    KeyEvent.KEYCODE_Q -> actionListener.onExitRequested()
                    KeyEvent.KEYCODE_C -> actionListener.onToggleCursorVisibility()
                }
                specialKeyCode = KeyEvent.KEYCODE_UNKNOWN
                waitingForAllModifiersUp = false
            }
        } else if ((modifierFlags and REQUIRED_SHORTCUT_MODIFIERS) == REQUIRED_SHORTCUT_MODIFIERS &&
            (down && nonModifierKeyCode != KeyEvent.KEYCODE_UNKNOWN)
        ) {
            when (androidKeyCode) {
                KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_C -> {
                    specialKeyCode = androidKeyCode
                    waitingForAllModifiersUp = true
                    return true
                }
            }
        }

        return false
    }
}
