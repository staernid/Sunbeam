package io.github.staernid.sunbeam.core.stream

import io.github.staernid.sunbeam.nvstream.jni.MoonBridge

@Suppress("TooManyFunctions", "LongParameterList")
interface MoonBridgeDriver {
    fun stopConnection()

    fun interruptConnection()

    fun sendMouseMove(
        deltaX: Short,
        deltaY: Short,
    )

    fun sendMousePosition(
        x: Short,
        y: Short,
        referenceWidth: Short,
        referenceHeight: Short,
    )

    fun sendMouseButton(
        buttonEvent: Byte,
        mouseButton: Byte,
    )

    fun sendMultiControllerInput(
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

    fun sendKeyboardInput(
        keyCode: Short,
        keyAction: Byte,
        modifiers: Byte,
        flags: Byte,
    )

    fun sendMouseHighResScroll(scrollAmount: Short)

    fun sendMouseHighResHScroll(scrollAmount: Short)

    fun sendControllerArrivalEvent(
        controllerNumber: Byte,
        activeGamepadMask: Short,
        type: Byte,
        supportedButtonFlags: Int,
        capabilities: Short,
    ): Int

    fun sendControllerTouchEvent(
        controllerNumber: Byte,
        eventType: Byte,
        pointerId: Int,
        x: Float,
        y: Float,
        pressure: Float,
    ): Int

    fun sendControllerMotionEvent(
        controllerNumber: Byte,
        motionType: Byte,
        x: Float,
        y: Float,
        z: Float,
    ): Int

    fun sendControllerBatteryEvent(
        controllerNumber: Byte,
        batteryState: Byte,
        batteryPercentage: Byte,
    )

    fun sendUtf8Text(text: String)
}

@Suppress("TooManyFunctions", "LongParameterList")
object DefaultMoonBridgeDriver : MoonBridgeDriver {
    override fun stopConnection() {
        synchronized(MoonBridge::class.java) {
            MoonBridge.stopConnection()
            MoonBridge.cleanupBridge()
        }
    }

    override fun interruptConnection() {
        MoonBridge.interruptConnection()
    }

    override fun sendMouseMove(
        deltaX: Short,
        deltaY: Short,
    ) {
        MoonBridge.sendMouseMove(deltaX, deltaY)
    }

    override fun sendMousePosition(
        x: Short,
        y: Short,
        referenceWidth: Short,
        referenceHeight: Short,
    ) {
        MoonBridge.sendMousePosition(x, y, referenceWidth, referenceHeight)
    }

    override fun sendMouseButton(
        buttonEvent: Byte,
        mouseButton: Byte,
    ) {
        MoonBridge.sendMouseButton(buttonEvent, mouseButton)
    }

    override fun sendMultiControllerInput(
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
        MoonBridge.sendMultiControllerInput(
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

    override fun sendKeyboardInput(
        keyCode: Short,
        keyAction: Byte,
        modifiers: Byte,
        flags: Byte,
    ) {
        MoonBridge.sendKeyboardInput(keyCode, keyAction, modifiers, flags)
    }

    override fun sendMouseHighResScroll(scrollAmount: Short) {
        MoonBridge.sendMouseHighResScroll(scrollAmount)
    }

    override fun sendMouseHighResHScroll(scrollAmount: Short) {
        MoonBridge.sendMouseHighResHScroll(scrollAmount)
    }

    override fun sendControllerArrivalEvent(
        controllerNumber: Byte,
        activeGamepadMask: Short,
        type: Byte,
        supportedButtonFlags: Int,
        capabilities: Short,
    ): Int =
        MoonBridge.sendControllerArrivalEvent(
            controllerNumber,
            activeGamepadMask,
            type,
            supportedButtonFlags,
            capabilities,
        )

    override fun sendControllerTouchEvent(
        controllerNumber: Byte,
        eventType: Byte,
        pointerId: Int,
        x: Float,
        y: Float,
        pressure: Float,
    ): Int =
        MoonBridge.sendControllerTouchEvent(
            controllerNumber,
            eventType,
            pointerId,
            x,
            y,
            pressure,
        )

    override fun sendControllerMotionEvent(
        controllerNumber: Byte,
        motionType: Byte,
        x: Float,
        y: Float,
        z: Float,
    ): Int =
        MoonBridge.sendControllerMotionEvent(
            controllerNumber,
            motionType,
            x,
            y,
            z,
        )

    override fun sendControllerBatteryEvent(
        controllerNumber: Byte,
        batteryState: Byte,
        batteryPercentage: Byte,
    ) {
        MoonBridge.sendControllerBatteryEvent(controllerNumber, batteryState, batteryPercentage)
    }

    override fun sendUtf8Text(text: String) {
        MoonBridge.sendUtf8Text(text)
    }
}
