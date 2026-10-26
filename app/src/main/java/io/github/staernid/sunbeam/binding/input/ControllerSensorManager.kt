package io.github.staernid.sunbeam.binding.input

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import kotlin.math.min

class ControllerSensorManager(
    private val rotationProvider: DisplayRotationProvider,
    private val motionEventSink: MotionEventSink,
) {
    fun interface DisplayRotationProvider {
        fun getRotation(): Int
    }

    fun interface MotionEventSink {
        fun sendMotionEvent(
            controllerNumber: Byte,
            motionType: Byte,
            x: Float,
            y: Float,
            z: Float,
        )
    }

    private data class PermutedAxes(
        val xIndex: Int,
        val yIndex: Int,
        val zIndex: Int,
        val xFactor: Float,
        val yFactor: Float,
        val zFactor: Float,
    )

    private fun computeOrientationAxes(): PermutedAxes {
        val rotation = rotationProvider.getRotation()
        val isPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
        val xIndex = if (isPortrait) 0 else 1
        val yIndex = 2
        val zIndex = if (isPortrait) 1 else 0

        val (xFactor, zFactor) =
            when (rotation) {
                Surface.ROTATION_0 -> Pair(1f, -1f)
                Surface.ROTATION_90 -> Pair(-1f, -1f)
                Surface.ROTATION_180 -> Pair(-1f, 1f)
                else -> Pair(1f, 1f)
            }

        return PermutedAxes(xIndex, yIndex, zIndex, xFactor, 1f, zFactor)
    }

    fun createSensorListener(
        controllerNumber: Short,
        motionType: Byte,
        needsDeviceOrientationCorrection: Boolean,
    ): SensorEventListener {
        return object : SensorEventListener {
            private val lastValues = FloatArray(AXIS_COUNT)
            private var hasReceivedEvent = false

            private fun isDuplicate(
                v0: Float,
                v1: Float,
                v2: Float,
            ): Boolean {
                if (!hasReceivedEvent) return false
                return v0 == lastValues[0] && v1 == lastValues[1] && v2 == lastValues[2]
            }

            override fun onSensorChanged(event: SensorEvent) {
                val v0 = event.values[0]
                val v1 = event.values[1]
                val v2 = event.values[2]

                if (isDuplicate(v0, v1, v2)) return
                hasReceivedEvent = true
                lastValues[0] = v0
                lastValues[1] = v1
                lastValues[2] = v2

                val axes =
                    if (needsDeviceOrientationCorrection) {
                        computeOrientationAxes()
                    } else {
                        PermutedAxes(0, 1, 2, 1f, 1f, 1f)
                    }

                val scale = if (motionType == MoonBridge.LI_MOTION_TYPE_GYRO) RAD_TO_DEG else 1f
                motionEventSink.sendMotionEvent(
                    controllerNumber.toByte(),
                    motionType,
                    event.values[axes.xIndex] * axes.xFactor * scale,
                    event.values[axes.yIndex] * axes.yFactor * scale,
                    event.values[axes.zIndex] * axes.zFactor * scale,
                )
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {
                // no-op: sensor accuracy changes do not affect stream motion
            }
        }
    }

    fun handleSetMotionEventState(
        controllerNumber: Short,
        motionType: Byte,
        reportRateHz: Short,
        targets: Iterable<SensorTarget>,
        isBuiltInManager: (SensorManager?) -> Boolean,
    ) {
        val clampedRateHz = clampReportRate(reportRateHz)
        val target = targets.firstOrNull { it.controllerNumber == controllerNumber } ?: return

        when (motionType) {
            MoonBridge.LI_MOTION_TYPE_ACCEL -> target.accelReportRateHz = clampedRateHz
            MoonBridge.LI_MOTION_TYPE_GYRO -> target.gyroReportRateHz = clampedRateHz
        }

        val sm = target.sensorManager ?: return

        when (motionType) {
            MoonBridge.LI_MOTION_TYPE_ACCEL -> {
                target.accelListener?.let { sm.unregisterListener(it) }
                target.accelListener = null

                val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                if (clampedRateHz > 0 && accelSensor != null) {
                    val listener =
                        createSensorListener(
                            controllerNumber,
                            motionType,
                            isBuiltInManager(sm),
                        )
                    target.accelListener = listener
                    sm.registerListener(listener, accelSensor, US_PER_SECOND / clampedRateHz)
                }
            }
            MoonBridge.LI_MOTION_TYPE_GYRO -> {
                target.gyroListener?.let { sm.unregisterListener(it) }
                target.gyroListener = null

                val gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                if (clampedRateHz > 0 && gyroSensor != null) {
                    val listener =
                        createSensorListener(
                            controllerNumber,
                            motionType,
                            isBuiltInManager(sm),
                        )
                    target.gyroListener = listener
                    sm.registerListener(listener, gyroSensor, US_PER_SECOND / clampedRateHz)
                }
            }
        }
    }

    fun disableSensors(target: SensorTarget) {
        val sm = target.sensorManager
        if (target.gyroListener != null) {
            sm?.unregisterListener(target.gyroListener)
            target.gyroListener = null
            // Send zero gyro event to ensure virtual controller stops spinning
            motionEventSink.sendMotionEvent(
                target.controllerNumber.toByte(),
                MoonBridge.LI_MOTION_TYPE_GYRO,
                0f,
                0f,
                0f,
            )
        }
        if (target.accelListener != null) {
            sm?.unregisterListener(target.accelListener)
            target.accelListener = null
        }
    }

    companion object {
        const val RAD_TO_DEG: Float = 57.2957795f
        const val MAX_REPORT_RATE_HZ: Short = 200
        private const val AXIS_COUNT = 3
        private const val US_PER_SECOND = 1_000_000

        fun clampReportRate(rateHz: Short): Short {
            return min(MAX_REPORT_RATE_HZ.toInt(), rateHz.toInt()).toShort()
        }
    }
}
