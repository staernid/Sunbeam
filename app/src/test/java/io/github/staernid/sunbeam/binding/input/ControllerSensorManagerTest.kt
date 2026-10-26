package io.github.staernid.sunbeam.binding.input

import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import org.junit.Assert.assertEquals
import org.junit.Test

class ControllerSensorManagerTest {
    private class FakeSensorTarget(
        override val controllerNumber: Short,
        override val sensorManager: SensorManager? = null,
        override var gyroListener: SensorEventListener? = null,
        override var gyroReportRateHz: Short = 0,
        override var accelListener: SensorEventListener? = null,
        override var accelReportRateHz: Short = 0,
    ) : SensorTarget

    private data class SentMotionEvent(
        val controllerNumber: Byte,
        val motionType: Byte,
        val x: Float,
        val y: Float,
        val z: Float,
    )

    @Test
    fun clampReportRate_clampsTo200HzMax() {
        assertEquals(0.toShort(), ControllerSensorManager.clampReportRate(0))
        assertEquals(100.toShort(), ControllerSensorManager.clampReportRate(100))
        assertEquals(200.toShort(), ControllerSensorManager.clampReportRate(200))
        assertEquals(200.toShort(), ControllerSensorManager.clampReportRate(250))
        assertEquals(200.toShort(), ControllerSensorManager.clampReportRate(1000))
    }

    @Test
    fun radToDeg_accurateToWithinFloatPrecision() {
        val calculated = (180.0 / Math.PI).toFloat()
        assertEquals(calculated, ControllerSensorManager.RAD_TO_DEG, 0.0001f)
    }

    @Test
    fun disableSensors_sendsZeroGyroEvent() {
        val sentEvents = mutableListOf<SentMotionEvent>()
        val manager =
            ControllerSensorManager(
                rotationProvider = { 0 },
                motionEventSink = { c, m, x, y, z ->
                    sentEvents.add(SentMotionEvent(c, m, x, y, z))
                },
            )

        // Mock a listener on the target
        val fakeListener =
            object : SensorEventListener {
                override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                    // no-op for test
                }

                override fun onAccuracyChanged(
                    sensor: android.hardware.Sensor?,
                    accuracy: Int,
                ) {
                    // no-op for test
                }
            }

        val target =
            FakeSensorTarget(
                controllerNumber = 1,
                gyroListener = fakeListener,
                gyroReportRateHz = 100,
            )

        manager.disableSensors(target)

        assertEquals(null, target.gyroListener)
        assertEquals(1, sentEvents.size)
        val event = sentEvents.first()
        assertEquals(1.toByte(), event.controllerNumber)
        assertEquals(MoonBridge.LI_MOTION_TYPE_GYRO, event.motionType)
        assertEquals(0f, event.x, 0.0f)
        assertEquals(0f, event.y, 0.0f)
        assertEquals(0f, event.z, 0.0f)
    }
}
