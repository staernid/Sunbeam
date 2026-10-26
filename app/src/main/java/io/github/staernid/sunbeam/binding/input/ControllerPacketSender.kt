@file:Suppress(
    "LongParameterList",
)

package io.github.staernid.sunbeam.binding.input

import io.github.staernid.sunbeam.nvstream.NvConnection

/**
 * Decoupled packet sending contract for controller lifecycle, button/axis state,
 * arrival notification, battery status, and emulated mouse events.
 */
interface ControllerPacketSender {
    fun sendControllerInput(
        controllerNumber: Short,
        activeGamepadMask: Short,
        buttonFlags: Int,
        leftTrigger: Byte,
        rightTrigger: Byte,
        leftStickX: Short,
        leftStickY: Short,
        rightStickX: Short,
        rightStickY: Short,
    )

    fun sendControllerArrivalEvent(
        controllerNumber: Byte,
        activeGamepadMask: Short,
        type: Byte,
        supportedButtonFlags: Int,
        capabilities: Short,
    ): Int

    fun sendControllerBatteryEvent(
        controllerNumber: Byte,
        batteryState: Byte,
        batteryPercentage: Byte,
    )

    fun sendMouseMove(
        deltaX: Short,
        deltaY: Short,
    )

    fun sendMouseHighResScroll(scrollAmount: Short)

    fun sendMouseHighResHScroll(scrollAmount: Short)
}

/**
 * NvConnection-backed implementation of [ControllerPacketSender].
 */
class NvControllerPacketSender(
    private val conn: NvConnection,
) : ControllerPacketSender {
    override fun sendControllerInput(
        controllerNumber: Short,
        activeGamepadMask: Short,
        buttonFlags: Int,
        leftTrigger: Byte,
        rightTrigger: Byte,
        leftStickX: Short,
        leftStickY: Short,
        rightStickX: Short,
        rightStickY: Short,
    ) {
        conn.sendControllerInput(
            controllerNumber,
            activeGamepadMask,
            buttonFlags,
            leftTrigger,
            rightTrigger,
            leftStickX,
            leftStickY,
            rightStickX,
            rightStickY,
        )
    }

    override fun sendControllerArrivalEvent(
        controllerNumber: Byte,
        activeGamepadMask: Short,
        type: Byte,
        supportedButtonFlags: Int,
        capabilities: Short,
    ): Int =
        conn.sendControllerArrivalEvent(
            controllerNumber,
            activeGamepadMask,
            type,
            supportedButtonFlags,
            capabilities,
        )

    override fun sendControllerBatteryEvent(
        controllerNumber: Byte,
        batteryState: Byte,
        batteryPercentage: Byte,
    ) {
        conn.sendControllerBatteryEvent(controllerNumber, batteryState, batteryPercentage)
    }

    override fun sendMouseMove(
        deltaX: Short,
        deltaY: Short,
    ) {
        conn.sendMouseMove(deltaX, deltaY)
    }

    override fun sendMouseHighResScroll(scrollAmount: Short) {
        conn.sendMouseHighResScroll(scrollAmount)
    }

    override fun sendMouseHighResHScroll(scrollAmount: Short) {
        conn.sendMouseHighResHScroll(scrollAmount)
    }
}
