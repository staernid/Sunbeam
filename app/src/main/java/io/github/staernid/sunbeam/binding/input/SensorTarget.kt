package io.github.staernid.sunbeam.binding.input

import android.hardware.SensorEventListener
import android.hardware.SensorManager

interface SensorTarget {
    val controllerNumber: Short
    val sensorManager: SensorManager?
    var gyroListener: SensorEventListener?
    var gyroReportRateHz: Short
    var accelListener: SensorEventListener?
    var accelReportRateHz: Short
}
