package io.github.staernid.sunbeam.binding.input

import android.annotation.TargetApi
import android.media.AudioAttributes
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import org.cgutman.shieldcontrollerextensions.SceManager
import kotlin.math.min

class ControllerRumbleManager(
    private val deviceVibrator: Vibrator?,
    private val prefConfig: PreferenceConfiguration,
    private val sceManager: SceManager?,
) {
    var isStopped: Boolean = false

    fun handleRumble(
        controllerNumber: Short,
        lowFreqMotor: Short,
        highFreqMotor: Short,
        inputTargets: Iterable<RumbleTarget>,
        usbTargets: Iterable<RumbleTarget>,
    ) {
        if (isStopped) return

        var foundMatchingDevice = false
        var vibrated = false

        for (target in inputTargets) {
            if (target.controllerNumber == controllerNumber) {
                foundMatchingDevice = true
                if (rumbleInputTarget(target, lowFreqMotor, highFreqMotor)) {
                    vibrated = true
                }
            }
        }

        for (target in usbTargets) {
            if (target.controllerNumber == controllerNumber) {
                foundMatchingDevice = true
                vibrated = true
                target.rumbleCustom(lowFreqMotor, highFreqMotor)
            }
        }

        if (controllerNumber == 0.toShort()) {
            handlePlayerOneFallback(foundMatchingDevice, vibrated, lowFreqMotor, highFreqMotor)
        }
    }

    private fun rumbleInputTarget(
        target: RumbleTarget,
        low: Short,
        high: Short,
    ): Boolean {
        target.lowFreqMotor = low
        target.highFreqMotor = high

        val vm = target.vibratorManager
        val vibrated =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && vm != null) {
                if (target.hasQuadVibrators) {
                    rumbleQuadVibrators(
                        vm,
                        target.lowFreqMotor,
                        target.highFreqMotor,
                        target.leftTriggerMotor,
                        target.rightTriggerMotor,
                    )
                } else {
                    rumbleDualVibrators(vm, target.lowFreqMotor, target.highFreqMotor)
                }
                true
            } else if (sceManager?.rumble(target.inputDevice, low.toInt(), high.toInt()) == true) {
                true
            } else if (target.vibrator != null) {
                rumbleSingleVibrator(target.vibrator!!, low, high)
                true
            } else {
                false
            }

        return vibrated
    }

    private fun handlePlayerOneFallback(
        foundMatchingDevice: Boolean,
        vibrated: Boolean,
        low: Short,
        high: Short,
    ) {
        val vibrator = deviceVibrator ?: return
        if (shouldVibrateOnScreenControls(foundMatchingDevice)) {
            rumbleSingleVibrator(vibrator, low, high)
        } else if (foundMatchingDevice && !vibrated && prefConfig.vibrateFallbackToDevice) {
            val lowAdjusted =
                calculateFallbackStrength(
                    low,
                    prefConfig.vibrateFallbackToDeviceStrength,
                )
            val highAdjusted =
                calculateFallbackStrength(
                    high,
                    prefConfig.vibrateFallbackToDeviceStrength,
                )
            rumbleSingleVibrator(vibrator, lowAdjusted, highAdjusted)
        }
    }

    private fun shouldVibrateOnScreenControls(foundMatchingDevice: Boolean): Boolean {
        val oscActive = prefConfig.onscreenController && !prefConfig.onlyL3R3 && prefConfig.vibrateOsc
        return !foundMatchingDevice && oscActive
    }

    fun handleRumbleTriggers(
        controllerNumber: Short,
        leftTrigger: Short,
        rightTrigger: Short,
        inputTargets: Iterable<RumbleTarget>,
        usbTargets: Iterable<RumbleTarget>,
    ) {
        if (isStopped) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (target in inputTargets) {
                if (target.controllerNumber == controllerNumber) {
                    rumbleInputTriggers(target, leftTrigger, rightTrigger)
                }
            }
        }

        for (target in usbTargets) {
            if (target.controllerNumber == controllerNumber) {
                target.rumbleTriggersCustom(leftTrigger, rightTrigger)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.S)
    private fun rumbleInputTriggers(
        target: RumbleTarget,
        leftTrigger: Short,
        rightTrigger: Short,
    ) {
        target.leftTriggerMotor = leftTrigger
        target.rightTriggerMotor = rightTrigger

        val vm = target.vibratorManager
        if (target.hasQuadVibrators && vm != null) {
            rumbleQuadVibrators(
                vm,
                target.lowFreqMotor,
                target.highFreqMotor,
                target.leftTriggerMotor,
                target.rightTriggerMotor,
            )
        }
    }

    fun cancel(target: RumbleTarget) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && target.vibratorManager != null) {
            target.vibratorManager?.cancel()
        } else {
            target.vibrator?.cancel()
        }
    }

    companion object {
        private const val MOTOR_AMPLITUDE_MASK = 0xFF
        private const val BYTE_SHIFT = 8
        private const val MAX_FALLBACK_STRENGTH = 100
        private const val RUMBLE_DURATION_MS = 60000L
        private const val PWM_PERIOD_MS = 20L
        private const val MAX_AMPLITUDE = 255
        private const val MAX_AMPLITUDE_DOUBLE = 255.0
        private const val LOW_FREQ_WEIGHT = 0.80
        private const val HIGH_FREQ_WEIGHT = 0.33
        private const val DUAL_VIBRATOR_COUNT = 2
        private const val QUAD_VIBRATOR_COUNT = 4
        private const val UNSIGNED_SHORT_MASK = 0xFFFF
        private const val MAX_UNSIGNED_SHORT = 65534

        fun calculateSimulatedAmplitude(
            lowFreqMotor: Short,
            highFreqMotor: Short,
        ): Int {
            val lowFreqMotorMSB = ((lowFreqMotor.toInt() shr BYTE_SHIFT) and MOTOR_AMPLITUDE_MASK)
            val highFreqMotorMSB = ((highFreqMotor.toInt() shr BYTE_SHIFT) and MOTOR_AMPLITUDE_MASK)
            val combined = (lowFreqMotorMSB * LOW_FREQ_WEIGHT) + (highFreqMotorMSB * HIGH_FREQ_WEIGHT)
            return min(MAX_AMPLITUDE, combined.toInt())
        }

        fun calculateFallbackStrength(
            motor: Short,
            fallbackStrengthPercent: Int,
        ): Short {
            val unsignedMotor = motor.toInt() and UNSIGNED_SHORT_MASK
            val adjusted = (unsignedMotor * fallbackStrengthPercent) / MAX_FALLBACK_STRENGTH
            return min(adjusted, MAX_UNSIGNED_SHORT).toShort()
        }

        @TargetApi(Build.VERSION_CODES.S)
        fun hasDualAmplitudeControlledRumbleVibrators(vm: VibratorManager?): Boolean {
            val ids = vm?.vibratorIds
            val isDual = ids != null && ids.size == DUAL_VIBRATOR_COUNT
            return isDual && ids.all { vm.getVibrator(it).hasAmplitudeControl() }
        }

        @TargetApi(Build.VERSION_CODES.S)
        fun hasQuadAmplitudeControlledRumbleVibrators(vm: VibratorManager?): Boolean {
            val ids = vm?.vibratorIds
            val isQuad = ids != null && ids.size == QUAD_VIBRATOR_COUNT
            return isQuad && ids.all { vm.getVibrator(it).hasAmplitudeControl() }
        }

        @TargetApi(Build.VERSION_CODES.S)
        fun rumbleDualVibrators(
            vm: VibratorManager,
            lowFreqMotor: Short,
            highFreqMotor: Short,
        ) {
            val high = ((highFreqMotor.toInt() shr BYTE_SHIFT) and MOTOR_AMPLITUDE_MASK)
            val low = ((lowFreqMotor.toInt() shr BYTE_SHIFT) and MOTOR_AMPLITUDE_MASK)

            if (low == 0 && high == 0) {
                vm.cancel()
                return
            }

            val vibratorIds = vm.vibratorIds
            val vibratorAmplitudes = intArrayOf(high, low)
            val combo = CombinedVibration.startParallel()

            for (i in vibratorIds.indices) {
                if (vibratorAmplitudes[i] != 0) {
                    combo.addVibrator(
                        vibratorIds[i],
                        VibrationEffect.createOneShot(RUMBLE_DURATION_MS, vibratorAmplitudes[i]),
                    )
                }
            }

            val vibrationAttributes = VibrationAttributes.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrationAttributes.setUsage(VibrationAttributes.USAGE_MEDIA)
            }
            vm.vibrate(combo.combine(), vibrationAttributes.build())
        }

        @TargetApi(Build.VERSION_CODES.S)
        fun rumbleQuadVibrators(
            vm: VibratorManager,
            lowFreqMotor: Short,
            highFreqMotor: Short,
            leftTrigger: Short,
            rightTrigger: Short,
        ) {
            val high = ((highFreqMotor.toInt() shr BYTE_SHIFT) and MOTOR_AMPLITUDE_MASK)
            val low = ((lowFreqMotor.toInt() shr BYTE_SHIFT) and MOTOR_AMPLITUDE_MASK)
            val lt = ((leftTrigger.toInt() shr BYTE_SHIFT) and MOTOR_AMPLITUDE_MASK)
            val rt = ((rightTrigger.toInt() shr BYTE_SHIFT) and MOTOR_AMPLITUDE_MASK)

            val allMotorsZero = low == 0 && high == 0 && lt == 0 && rt == 0
            if (allMotorsZero) {
                vm.cancel()
                return
            }

            val vibratorIds = vm.vibratorIds
            val vibratorAmplitudes = intArrayOf(high, low, lt, rt)
            val combo = CombinedVibration.startParallel()

            for (i in vibratorIds.indices) {
                if (vibratorAmplitudes[i] != 0) {
                    combo.addVibrator(
                        vibratorIds[i],
                        VibrationEffect.createOneShot(RUMBLE_DURATION_MS, vibratorAmplitudes[i]),
                    )
                }
            }

            val vibrationAttributes = VibrationAttributes.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrationAttributes.setUsage(VibrationAttributes.USAGE_MEDIA)
            }
            vm.vibrate(combo.combine(), vibrationAttributes.build())
        }

        fun rumbleSingleVibrator(
            vibrator: Vibrator,
            lowFreqMotor: Short,
            highFreqMotor: Short,
        ) {
            val simulatedAmplitude = calculateSimulatedAmplitude(lowFreqMotor, highFreqMotor)
            if (simulatedAmplitude == 0) {
                vibrator.cancel()
                return
            }

            if (vibrator.hasAmplitudeControl()) {
                val effect = VibrationEffect.createOneShot(RUMBLE_DURATION_MS, simulatedAmplitude)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val vibrationAttributes =
                        VibrationAttributes.Builder()
                            .setUsage(VibrationAttributes.USAGE_MEDIA)
                            .build()
                    vibrator.vibrate(effect, vibrationAttributes)
                } else {
                    val audioAttributes =
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    vibrator.vibrate(effect, audioAttributes)
                }
                return
            }

            // PWM fallback for vibrators lacking amplitude control
            val onTime = ((simulatedAmplitude / MAX_AMPLITUDE_DOUBLE) * PWM_PERIOD_MS).toLong()
            val offTime = PWM_PERIOD_MS - onTime
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val vibrationAttributes =
                    VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_MEDIA)
                        .build()
                vibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, onTime, offTime), 0),
                    vibrationAttributes,
                )
            } else {
                val audioAttributes =
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, onTime, offTime), 0, audioAttributes)
            }
        }
    }
}
