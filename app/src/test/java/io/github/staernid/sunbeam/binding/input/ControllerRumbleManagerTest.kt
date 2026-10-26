package io.github.staernid.sunbeam.binding.input

import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [33])
@RunWith(RobolectricTestRunner::class)
class ControllerRumbleManagerTest {
    private class FakeRumbleTarget(
        override val controllerNumber: Short,
    ) : RumbleTarget {
        override var lowFreqMotor: Short = 0
        override var highFreqMotor: Short = 0
        override var leftTriggerMotor: Short = 0
        override var rightTriggerMotor: Short = 0
        override val vibratorManager: VibratorManager? = null
        override val vibrator: Vibrator? = null
        override val hasQuadVibrators: Boolean = false
        override val inputDevice: InputDevice? = null

        var customRumbleCalled = false
        var customTriggersCalled = false

        override fun rumbleCustom(
            lowFreqMotor: Short,
            highFreqMotor: Short,
        ): Boolean {
            customRumbleCalled = true
            this.lowFreqMotor = lowFreqMotor
            this.highFreqMotor = highFreqMotor
            return true
        }

        override fun rumbleTriggersCustom(
            leftTrigger: Short,
            rightTrigger: Short,
        ): Boolean {
            customTriggersCalled = true
            this.leftTriggerMotor = leftTrigger
            this.rightTriggerMotor = rightTrigger
            return true
        }
    }

    @Test
    fun calculateSimulatedAmplitude_computesExpectedAmplitudes() {
        // Both motors zero
        assertEquals(0, ControllerRumbleManager.calculateSimulatedAmplitude(0, 0))

        // Max low frequency motor only (MSB = 255: 255 * 0.8 = 204)
        val maxLow = ((255 shl 8).toShort())
        assertEquals(204, ControllerRumbleManager.calculateSimulatedAmplitude(maxLow, 0))

        // Max high frequency motor only (MSB = 255: 255 * 0.33 = 84)
        val maxHigh = ((255 shl 8).toShort())
        assertEquals(84, ControllerRumbleManager.calculateSimulatedAmplitude(0, maxHigh))

        // Both motors max: 204 + 84 = 288 -> capped to 255
        assertEquals(255, ControllerRumbleManager.calculateSimulatedAmplitude(maxLow, maxHigh))
    }

    @Test
    fun calculateFallbackStrength_computesPercentageCorrectly() {
        val input: Short = 1000
        assertEquals(0.toShort(), ControllerRumbleManager.calculateFallbackStrength(input, 0))
        assertEquals(500.toShort(), ControllerRumbleManager.calculateFallbackStrength(input, 50))
        assertEquals(1000.toShort(), ControllerRumbleManager.calculateFallbackStrength(input, 100))
    }

    @Test
    fun handleRumble_routesToMatchingUsbTarget() {
        val prefConfig = PreferenceConfiguration()
        val manager = ControllerRumbleManager(null, prefConfig, null)

        val target0 = FakeRumbleTarget(controllerNumber = 0)
        val target1 = FakeRumbleTarget(controllerNumber = 1)

        val lowVal: Short = 0x1200.toShort()
        val highVal: Short = 0x3400.toShort()

        manager.handleRumble(
            controllerNumber = 1,
            lowFreqMotor = lowVal,
            highFreqMotor = highVal,
            inputTargets = emptyList(),
            usbTargets = listOf(target0, target1),
        )

        assertEquals(0.toShort(), target0.lowFreqMotor)
        assertEquals(lowVal, target1.lowFreqMotor)
        assertEquals(highVal, target1.highFreqMotor)
        assertTrue(target1.customRumbleCalled)
    }

    @Test
    fun handleRumbleTriggers_routesToMatchingUsbTarget() {
        val prefConfig = PreferenceConfiguration()
        val manager = ControllerRumbleManager(null, prefConfig, null)

        val target0 = FakeRumbleTarget(controllerNumber = 0)
        val target1 = FakeRumbleTarget(controllerNumber = 1)

        val ltVal: Short = 0x5600.toShort()
        val rtVal: Short = 0x7800.toShort()

        manager.handleRumbleTriggers(
            controllerNumber = 1,
            leftTrigger = ltVal,
            rightTrigger = rtVal,
            inputTargets = emptyList(),
            usbTargets = listOf(target0, target1),
        )

        assertEquals(0.toShort(), target0.leftTriggerMotor)
        assertEquals(ltVal, target1.leftTriggerMotor)
        assertEquals(rtVal, target1.rightTriggerMotor)
        assertTrue(target1.customTriggersCalled)
    }

    @Test
    fun stoppedManager_ignoresRumbleEvents() {
        val prefConfig = PreferenceConfiguration()
        val manager =
            ControllerRumbleManager(null, prefConfig, null).apply {
                isStopped = true
            }

        val target = FakeRumbleTarget(controllerNumber = 0)

        manager.handleRumble(
            controllerNumber = 0,
            lowFreqMotor = 0x1200.toShort(),
            highFreqMotor = 0x3400.toShort(),
            inputTargets = listOf(target),
            usbTargets = emptyList(),
        )

        assertEquals(0.toShort(), target.lowFreqMotor)
    }
}
