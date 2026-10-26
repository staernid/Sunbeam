package io.github.staernid.sunbeam.binding.input

import android.hardware.lights.LightsManager
import android.view.InputDevice

interface LightsTarget {
    val controllerNumber: Short
    val inputDevice: InputDevice?
    val hasRgbLed: Boolean
    var lightsSession: LightsManager.LightsSession?
    var ledArgbValue: Int
}
