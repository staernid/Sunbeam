package io.github.staernid.sunbeam.binding.input

import android.view.KeyEvent
import io.github.staernid.sunbeam.nvstream.input.KeyboardPacket
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class KeyboardTranslatorTest {
    private lateinit var translator: KeyboardTranslator
    private val keyPrefix = 0x80.toShort()

    private fun expectedGfeKey(vkCode: Int): Short {
        return ((keyPrefix.toInt() shl 8) or (vkCode and 0xFF)).toShort()
    }

    @Before
    fun setUp() {
        val prefConfig =
            PreferenceConfiguration().apply {
                forceQwerty = false
            }
        translator = KeyboardTranslator(prefConfig)
    }

    @Test
    fun translateAlphanumericKeys_mapsToCorrectVirtualKeys() {
        assertEquals(expectedGfeKey(KeyboardTranslator.VK_A), translator.translate(KeyEvent.KEYCODE_A, 0, -1))
        assertEquals(expectedGfeKey(KeyboardTranslator.VK_Z), translator.translate(KeyEvent.KEYCODE_Z, 0, -1))
        assertEquals(expectedGfeKey(KeyboardTranslator.VK_0), translator.translate(KeyEvent.KEYCODE_0, 0, -1))
        assertEquals(expectedGfeKey(KeyboardTranslator.VK_9), translator.translate(KeyEvent.KEYCODE_9, 0, -1))
    }

    @Test
    fun translateFunctionKeys_standardAndExtended() {
        val f1 = expectedGfeKey(KeyboardTranslator.VK_F1)
        val f12 = expectedGfeKey(KeyboardTranslator.VK_F12)
        val f13 = expectedGfeKey(KeyboardTranslator.VK_F13)
        val f24 = expectedGfeKey(KeyboardTranslator.VK_F13 + 11)

        assertEquals(f1, translator.translate(KeyEvent.KEYCODE_F1, 0, -1))
        assertEquals(f12, translator.translate(KeyEvent.KEYCODE_F12, 0, -1))
        assertEquals(f13, translator.translate(KeyEvent.KEYCODE_F13, 0, -1))
        assertEquals(f24, translator.translate(KeyEvent.KEYCODE_F24, 0, -1))
    }

    @Test
    fun translateKeycodePlusAndEquals_mapsToSameVk() {
        val plusVk = translator.translate(KeyEvent.KEYCODE_PLUS, 0, -1)
        val equalsVk = translator.translate(KeyEvent.KEYCODE_EQUALS, 0, -1)
        assertEquals(expectedGfeKey(0xBB), equalsVk)
        assertEquals(equalsVk, plusVk)
    }

    @Test
    fun translateStandardControls_enterEscapeSpaceTab() {
        val enterExpected = expectedGfeKey(0x0D)
        val escExpected = expectedGfeKey(KeyboardTranslator.VK_ESCAPE)
        val spaceExpected = expectedGfeKey(KeyboardTranslator.VK_SPACE)
        val tabExpected = expectedGfeKey(KeyboardTranslator.VK_TAB)
        val backspaceExpected = expectedGfeKey(KeyboardTranslator.VK_BACK_SPACE)

        assertEquals(enterExpected, translator.translate(KeyEvent.KEYCODE_ENTER, 0, -1))
        assertEquals(escExpected, translator.translate(KeyEvent.KEYCODE_ESCAPE, 0, -1))
        assertEquals(spaceExpected, translator.translate(KeyEvent.KEYCODE_SPACE, 0, -1))
        assertEquals(tabExpected, translator.translate(KeyEvent.KEYCODE_TAB, 0, -1))
        assertEquals(backspaceExpected, translator.translate(KeyEvent.KEYCODE_DEL, 0, -1))
    }

    @Test
    fun getModifier_translatesFlagsAccurately() {
        assertEquals(
            KeyboardPacket.MODIFIER_SHIFT.toByte(),
            KeyboardTranslator.getModifier(KeyboardTranslator.VK_LSHIFT.toShort()),
        )
        assertEquals(
            KeyboardPacket.MODIFIER_CTRL.toByte(),
            KeyboardTranslator.getModifier(KeyboardTranslator.VK_LCONTROL.toShort()),
        )
        assertEquals(
            KeyboardPacket.MODIFIER_ALT.toByte(),
            KeyboardTranslator.getModifier(KeyboardTranslator.VK_LMENU.toShort()),
        )
        assertEquals(
            KeyboardPacket.MODIFIER_META.toByte(),
            KeyboardTranslator.getModifier(KeyboardTranslator.VK_LWIN.toShort()),
        )
        assertEquals(0.toByte(), KeyboardTranslator.getModifier(KeyboardTranslator.VK_A.toShort()))
    }

    @Test
    fun translateUnknownKeycode_withoutScancode_returnsZero() {
        assertEquals(0.toShort(), translator.translate(KeyEvent.KEYCODE_UNKNOWN, 0, -1))
    }
}
