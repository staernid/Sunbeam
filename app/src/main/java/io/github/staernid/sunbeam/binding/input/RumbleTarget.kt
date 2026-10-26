package io.github.staernid.sunbeam.binding.input

import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice

interface RumbleTarget {
    val controllerNumber: Short
    var lowFreqMotor: Short
    var highFreqMotor: Short
    var leftTriggerMotor: Short
    var rightTriggerMotor: Short
    val vibratorManager: VibratorManager?
    val vibrator: Vibrator?
    val hasQuadVibrators: Boolean
    val inputDevice: InputDevice?

    fun rumbleCustom(
        lowFreqMotor: Short,
        highFreqMotor: Short,
    ): Boolean = false

    fun rumbleTriggersCustom(
        leftTrigger: Short,
        rightTrigger: Short,
    ): Boolean = false
}
