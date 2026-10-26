package io.github.staernid.sunbeam.binding.input

import android.annotation.TargetApi
import android.hardware.lights.Light
import android.hardware.lights.LightState
import android.hardware.lights.LightsRequest
import android.os.Build
import android.view.InputDevice
import io.github.staernid.sunbeam.LimeLog

object ControllerLightsManager {
    const val SONY_VENDOR_ID: Int = 0x054c
    private const val ALPHA_MASK: Int = -0x1000000 // 0xFF000000
    private const val BYTE_MASK: Int = 0xFF
    private const val RED_SHIFT: Int = 16
    private const val GREEN_SHIFT: Int = 8

    fun packRgbToArgb(
        r: Byte,
        g: Byte,
        b: Byte,
    ): Int {
        return ALPHA_MASK or
            ((r.toInt() and BYTE_MASK) shl RED_SHIFT) or
            ((g.toInt() and BYTE_MASK) shl GREEN_SHIFT) or
            (b.toInt() and BYTE_MASK)
    }

    fun hasRgbControl(device: InputDevice): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false
        }
        val hasRgb = device.lightsManager.lights.any { it.hasRgbControl() }
        // Light.hasRgbControl() was broken prior to Android 14 (API 34)
        // It always returned true because LIGHT_CAPABILITY_RGB was defined as 0.
        val supported =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                device.vendorId == SONY_VENDOR_ID
        return hasRgb && supported
    }

    @TargetApi(Build.VERSION_CODES.S)
    fun applyLedState(
        target: LightsTarget,
        argbColor: Int,
    ) {
        val device = target.inputDevice ?: return
        val lightState = LightState.Builder().setColor(argbColor).build()
        val builder = LightsRequest.Builder()
        var hasRgb = false

        for (light: Light in device.lightsManager.lights) {
            if (light.hasRgbControl()) {
                builder.addLight(light, lightState)
                hasRgb = true
            }
        }

        if (hasRgb) {
            var session = target.lightsSession
            if (session == null) {
                session = device.lightsManager.openSession()
                target.lightsSession = session
            }
            session.requestLights(builder.build())
        }
    }

    @TargetApi(Build.VERSION_CODES.S)
    fun closeSession(target: LightsTarget) {
        val session = target.lightsSession ?: return
        try {
            session.close()
        } catch (e: IllegalStateException) {
            LimeLog.warning("Error closing LightsSession: ${e.message}")
        } catch (e: SecurityException) {
            LimeLog.warning("Security error closing LightsSession: ${e.message}")
        } finally {
            target.lightsSession = null
        }
    }
}
