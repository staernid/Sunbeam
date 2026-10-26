@file:Suppress(
    "LongMethod",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "LongParameterList",
    "MagicNumber",
    "TooManyFunctions",
    "LargeClass",
    "ComplexCondition",
    "MaxLineLength",
    "LoopWithTooManyJumpStatements",
)

package io.github.staernid.sunbeam.binding.input

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.BatteryState
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.input.InputManager
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.os.VibratorManager
import android.util.SparseArray
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import io.github.staernid.sunbeam.LimeLog
import io.github.staernid.sunbeam.binding.input.driver.AbstractController
import io.github.staernid.sunbeam.binding.input.driver.UsbDriverService
import io.github.staernid.sunbeam.nvstream.input.ControllerPacket
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import org.cgutman.shieldcontrollerextensions.SceChargingState
import org.cgutman.shieldcontrollerextensions.SceConnectionType
import org.cgutman.shieldcontrollerextensions.SceManager
import kotlin.math.abs
import kotlin.math.max

/**
 * Registry and hotplug manager for physical [InputDevice] and USB gamepads.
 * Manages device contexts, controller slot assignments (0..15), capabilities probing,
 * and zero-allocation target lists for rumble, sensors, and LEDs.
 */
@Suppress("LongParameterList", "LargeClass", "TooManyFunctions", "MagicNumber")
class ControllerDeviceRegistry(
    @JvmField val activityContext: Context,
    @JvmField val packetSender: ControllerPacketSender,
    @JvmField val prefConfig: PreferenceConfiguration,
    @JvmField val deviceVibrator: Vibrator?,
    @JvmField val deviceVibratorManager: VibratorManager?,
    @JvmField val deviceSensorManager: SensorManager?,
    @JvmField val sceManager: SceManager?,
    @JvmField val inputManager: InputManager?,
    @JvmField val mainThreadHandler: Handler = Handler(Looper.getMainLooper()),
    @JvmField val backgroundThreadHandler: Handler,
    @JvmField var controllerRumbleManager: ControllerRumbleManager? = null,
    @JvmField var controllerSensorManager: ControllerSensorManager? = null,
    @JvmField var onSetMotionEventState: ((controllerNumber: Short, motionType: Byte, reportRateHz: Short) -> Unit)? = null,
) : InputManager.InputDeviceListener {
    @JvmField
    val inputDeviceContexts = SparseArray<InputDeviceContext>()

    @JvmField
    val usbDeviceContexts = SparseArray<UsbDeviceContext>()

    @JvmField
    val defaultContext: InputDeviceContext

    @JvmField
    var currentControllers: Short = 0

    @JvmField
    var initialControllers: Short = 0

    @JvmField
    var hasGameController: Boolean = false

    @JvmField
    var isStopped: Boolean = false

    @JvmField
    val stickDeadzone: Double

    @JvmField
    val activeInputRumbleTargets = ArrayList<RumbleTarget>()

    @JvmField
    val activeUsbRumbleTargets = ArrayList<RumbleTarget>()

    @JvmField
    val activeSensorTargets = ArrayList<SensorTarget>()

    private val tempStickCoords = FloatArray(2)

    init {
        val deadzonePercentage = prefConfig.deadzonePercentage
        stickDeadzone = deadzonePercentage.toDouble() / 100.0

        val ids = InputDevice.getDeviceIds()
        for (id in ids) {
            val dev = InputDevice.getDevice(id) ?: continue
            if ((dev.sources and InputDevice.SOURCE_JOYSTICK) != 0 ||
                (dev.sources and InputDevice.SOURCE_GAMEPAD) != 0
            ) {
                if (getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_X) != null &&
                    getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_Y) != null
                ) {
                    hasGameController = true
                }
            }
        }

        defaultContext =
            InputDeviceContext(this).apply {
                leftStickXAxis = MotionEvent.AXIS_X
                leftStickYAxis = MotionEvent.AXIS_Y
                leftStickDeadzoneRadius = stickDeadzone.toFloat()
                rightStickXAxis = MotionEvent.AXIS_Z
                rightStickYAxis = MotionEvent.AXIS_RZ
                rightStickDeadzoneRadius = stickDeadzone.toFloat()
                leftTriggerAxis = MotionEvent.AXIS_BRAKE
                rightTriggerAxis = MotionEvent.AXIS_GAS
                hatXAxis = MotionEvent.AXIS_HAT_X
                hatYAxis = MotionEvent.AXIS_HAT_Y
                controllerNumber = 0
                assignedControllerNumber = true
                external = false
                ignoreBack = true
            }

        initialControllers = getAttachedControllerMask(activityContext)
        inputManager?.registerInputDeviceListener(this, null)
    }

    override fun onInputDeviceAdded(deviceId: Int) {
        // Nothing happening here yet
    }

    override fun onInputDeviceRemoved(deviceId: Int) {
        val context = inputDeviceContexts.get(deviceId)
        if (context != null) {
            LimeLog.info("Removed controller: ${context.name} ($deviceId)")
            releaseControllerNumber(context)
            context.destroy()
            inputDeviceContexts.remove(deviceId)
        }
    }

    override fun onInputDeviceChanged(deviceId: Int) {
        val device = InputDevice.getDevice(deviceId) ?: return
        val existingContext = inputDeviceContexts.get(deviceId) ?: return

        LimeLog.info("Device changed: ${existingContext.name} ($deviceId)")
        val newContext = createInputDeviceContextForDevice(device)
        newContext.migrateContext(existingContext)
        inputDeviceContexts.put(deviceId, newContext)
    }

    fun onUsbDeviceAdded(controller: AbstractController) {
        if (isStopped) return
        val context = createUsbDeviceContextForDevice(controller)
        usbDeviceContexts.put(controller.controllerId, context)
    }

    fun onUsbDeviceRemoved(controller: AbstractController) {
        val context = usbDeviceContexts.get(controller.controllerId)
        if (context != null) {
            LimeLog.info("Removed controller: ${controller.controllerId}")
            releaseControllerNumber(context)
            context.destroy()
            usbDeviceContexts.remove(controller.controllerId)
        }
    }

    fun getContextForEvent(event: InputEvent): InputDeviceContext? {
        if (isStopped) {
            return null
        }
        if (event.deviceId == 0) {
            return defaultContext
        }
        if (event.device == null) {
            return null
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && event.deviceId == -1) {
            return defaultContext
        }

        var context = inputDeviceContexts.get(event.deviceId)
        if (context == null) {
            context = createInputDeviceContextForDevice(event.device)
            inputDeviceContexts.put(event.deviceId, context)
        }
        return context
    }

    fun assignControllerNumberIfNeeded(context: GenericControllerContext) {
        if (context.assignedControllerNumber) return

        if (context is InputDeviceContext) {
            val devContext = context
            if (context is UsbDeviceContext) {
                if (prefConfig.multiController) {
                    LimeLog.info("Reserving the next available controller number for USB device")
                    for (i in 0 until MAX_GAMEPADS) {
                        val bit = 1 shl i
                        if ((currentControllers.toInt() and bit) == 0) {
                            currentControllers = (currentControllers.toInt() or bit).toShort()
                            initialControllers = (initialControllers.toInt() and bit.inv()).toShort()
                            context.controllerNumber = i.toShort()
                            context.reservedControllerNumber = true
                            break
                        }
                    }
                } else {
                    LimeLog.info("Not reserving a controller number")
                    context.controllerNumber = 0
                }

                if (prefConfig.gamepadMotionSensorsFallbackToDevice &&
                    context.controllerNumber.toInt() == 0 &&
                    (prefConfig.forceMotionSensorsFallbackToDevice || devContext.sensorManager == null)
                ) {
                    devContext.sensorManager = deviceSensorManager
                }
            } else {
                LimeLog.info("${devContext.name} (${context.id}) needs a controller number assigned")
                if (!devContext.external) {
                    LimeLog.info("Built-in buttons hardcoded as controller 0")
                    context.controllerNumber = 0
                } else if (prefConfig.multiController && devContext.hasJoystickAxes) {
                    LimeLog.info("Reserving the next available controller number")
                    for (i in 0 until MAX_GAMEPADS) {
                        val bit = 1 shl i
                        if ((currentControllers.toInt() and bit) == 0) {
                            currentControllers = (currentControllers.toInt() or bit).toShort()
                            initialControllers = (initialControllers.toInt() and bit.inv()).toShort()
                            context.controllerNumber = i.toShort()
                            context.reservedControllerNumber = true
                            break
                        }
                    }
                } else if (!devContext.hasJoystickAxes) {
                    context.controllerNumber = 0
                    val inputDev = devContext.inputDevice
                    var associatedDevice: InputDevice? = if (inputDev != null) InputDevice.getDevice(devContext.id + 1) else null
                    if (inputDev != null && !isAssociatedJoystick(inputDev, associatedDevice)) {
                        associatedDevice = InputDevice.getDevice(devContext.id - 1)
                        if (!isAssociatedJoystick(inputDev, associatedDevice)) {
                            LimeLog.info("No associated joystick device found")
                            associatedDevice = null
                        }
                    }

                    if (associatedDevice != null) {
                        var associatedDeviceContext = inputDeviceContexts.get(associatedDevice.id)
                        if (associatedDeviceContext == null) {
                            associatedDeviceContext = createInputDeviceContextForDevice(associatedDevice)
                            inputDeviceContexts.put(associatedDevice.id, associatedDeviceContext)
                        }
                        if (!associatedDeviceContext.assignedControllerNumber) {
                            assignControllerNumberIfNeeded(associatedDeviceContext)
                        }
                        context.controllerNumber = associatedDeviceContext.controllerNumber
                        LimeLog.info("Propagated controller number from ${associatedDeviceContext.name}")
                    }
                } else {
                    LimeLog.info("Not reserving a controller number")
                    context.controllerNumber = 0
                }

                if (prefConfig.gamepadMotionSensorsFallbackToDevice &&
                    context.controllerNumber.toInt() == 0 &&
                    (prefConfig.forceMotionSensorsFallbackToDevice || devContext.sensorManager == null)
                ) {
                    devContext.sensorManager = deviceSensorManager
                }
            }
        }

        LimeLog.info("Assigned as controller ${context.controllerNumber}")
        context.assignedControllerNumber = true
        context.sendControllerArrival()
    }

    fun releaseControllerNumber(context: GenericControllerContext) {
        if (context.reservedControllerNumber) {
            LimeLog.info("Controller number ${context.controllerNumber} is now available")
            currentControllers = (currentControllers.toInt() and (1 shl context.controllerNumber.toInt()).inv()).toShort()
        }

        if (context.assignedControllerNumber) {
            packetSender.sendControllerInput(
                context.controllerNumber,
                getActiveControllerMask(),
                0,
                0.toByte(),
                0.toByte(),
                0.toShort(),
                0.toShort(),
                0.toShort(),
                0.toShort(),
            )
        }
    }

    fun getActiveControllerMask(): Short {
        return if (prefConfig.multiController) {
            (currentControllers.toInt() or initialControllers.toInt() or (if (prefConfig.onscreenController) 1 else 0)).toShort()
        } else {
            1
        }
    }

    fun shouldIgnoreBack(dev: InputDevice): Boolean {
        val devName = dev.name ?: ""
        if (devName.contains("Razer Serval")) {
            return true
        }

        if (!hasJoystickAxes(dev) && devName.lowercase().contains("remote")) {
            return true
        }

        if (!isExternal(dev)) {
            val im = activityContext.getSystemService(Context.INPUT_SERVICE) as? InputManager
            var foundInternalGamepad = false
            var foundInternalSelect = false
            val deviceIds = im?.inputDeviceIds ?: IntArray(0)
            for (id in deviceIds) {
                val currentDev = im?.getInputDevice(id) ?: continue
                if (isExternal(currentDev)) continue

                val hasKeys = currentDev.hasKeys(KeyEvent.KEYCODE_BUTTON_SELECT)
                if (hasKeys != null && hasKeys.isNotEmpty() && hasKeys[0]) {
                    foundInternalSelect = true
                }
                if (hasGamepadButtons(currentDev)) {
                    foundInternalGamepad = true
                }
            }
            return !foundInternalGamepad || foundInternalSelect
        } else {
            return !hasJoystickAxes(dev) && !hasGamepadButtons(dev)
        }
    }

    fun createUsbDeviceContextForDevice(device: AbstractController): UsbDeviceContext {
        val context = UsbDeviceContext(this)
        context.id = device.controllerId
        context.device = device
        context.external = true
        context.vendorId = device.vendorId
        context.productId = device.productId
        context.leftStickDeadzoneRadius = stickDeadzone.toFloat()
        context.rightStickDeadzoneRadius = stickDeadzone.toFloat()
        context.triggerDeadzone = 0.13f
        return context
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    fun createInputDeviceContextForDevice(dev: InputDevice): InputDeviceContext {
        val context = InputDeviceContext(this)
        val devName = dev.name ?: ""

        LimeLog.info("Creating controller context for device: $devName")
        LimeLog.info("Vendor ID: ${dev.vendorId}")
        LimeLog.info("Product ID: ${dev.productId}")
        LimeLog.info(dev.toString())

        context.inputDevice = dev
        context.name = devName
        context.id = dev.id
        context.external = isExternal(dev)
        context.vendorId = dev.vendorId
        context.productId = dev.productId

        context.hasPaddles = MoonBridge.guessControllerHasPaddles(context.vendorId, context.productId)
        context.hasShare = MoonBridge.guessControllerHasShareButton(context.vendorId, context.productId)

        configureVibration(context, dev)
        configureSensors(context, dev)

        context.hasRgbLed = ControllerLightsManager.hasRgbControl(dev)

        val buttons = dev.hasKeys(KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BACK, 0)
        context.hasMode = buttons != null && buttons.isNotEmpty() && buttons[0]
        context.hasSelect = buttons != null && buttons.size > 2 && (buttons[1] || buttons[2])

        context.touchpadXRange = dev.getMotionRange(MotionEvent.AXIS_X, InputDevice.SOURCE_TOUCHPAD)
        context.touchpadYRange = dev.getMotionRange(MotionEvent.AXIS_Y, InputDevice.SOURCE_TOUCHPAD)
        context.touchpadPressureRange = dev.getMotionRange(MotionEvent.AXIS_PRESSURE, InputDevice.SOURCE_TOUCHPAD)

        context.leftStickXAxis = MotionEvent.AXIS_X
        context.leftStickYAxis = MotionEvent.AXIS_Y
        if (getMotionRangeForJoystickAxis(dev, context.leftStickXAxis) != null &&
            getMotionRangeForJoystickAxis(dev, context.leftStickYAxis) != null
        ) {
            hasGameController = true
            context.hasJoystickAxes = true
        }

        context.isDualShockStandaloneTouchpad =
            context.vendorId == 0x054c &&
            devName.endsWith(" Touchpad") &&
            dev.sources == (InputDevice.SOURCE_KEYBOARD or InputDevice.SOURCE_MOUSE)

        configureAxesAndTriggers(context, dev, devName)

        LimeLog.info("Analog stick deadzone: ${context.leftStickDeadzoneRadius} ${context.rightStickDeadzoneRadius}")
        LimeLog.info("Trigger deadzone: ${context.triggerDeadzone}")

        return context
    }

    private fun configureVibration(
        context: InputDeviceContext,
        dev: InputDevice,
    ) {
        if (prefConfig.enableDeviceRumble) {
            context.vibrator = deviceVibrator
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ControllerRumbleManager.hasQuadAmplitudeControlledRumbleVibrators(dev.vibratorManager)
            ) {
                context.vibratorManager = dev.vibratorManager
                context.quadVibrators = true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ControllerRumbleManager.hasDualAmplitudeControlledRumbleVibrators(dev.vibratorManager)
            ) {
                context.vibratorManager = dev.vibratorManager
                context.quadVibrators = false
            } else if (dev.vibrator.hasVibrator()) {
                context.vibrator = dev.vibrator
            } else if (!context.external) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ControllerRumbleManager.hasQuadAmplitudeControlledRumbleVibrators(deviceVibratorManager)
                ) {
                    context.vibratorManager = deviceVibratorManager
                    context.quadVibrators = true
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ControllerRumbleManager.hasDualAmplitudeControlledRumbleVibrators(deviceVibratorManager)
                ) {
                    context.vibratorManager = deviceVibratorManager
                    context.quadVibrators = false
                } else if (deviceVibrator?.hasVibrator() == true) {
                    context.vibrator = deviceVibrator
                }
            }
        }
    }

    private fun configureSensors(
        context: InputDeviceContext,
        dev: InputDevice,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
                        context.vendorId == 0x054c || context.vendorId == 0x057e
                ) &&
                prefConfig.gamepadMotionSensors
            ) {
                val sm = dev.sensorManager
                if (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null ||
                    sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
                ) {
                    context.sensorManager = sm
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    private fun configureAxesAndTriggers(
        context: InputDeviceContext,
        dev: InputDevice,
        devName: String,
    ) {
        val leftTriggerRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_LTRIGGER)
        val rightTriggerRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_RTRIGGER)
        val brakeRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_BRAKE)
        val gasRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_GAS)
        val throttleRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_THROTTLE)

        if (leftTriggerRange != null && rightTriggerRange != null) {
            context.leftTriggerAxis = MotionEvent.AXIS_LTRIGGER
            context.rightTriggerAxis = MotionEvent.AXIS_RTRIGGER
        } else if (brakeRange != null && gasRange != null) {
            context.leftTriggerAxis = MotionEvent.AXIS_BRAKE
            context.rightTriggerAxis = MotionEvent.AXIS_GAS
        } else if (brakeRange != null && throttleRange != null) {
            context.leftTriggerAxis = MotionEvent.AXIS_BRAKE
            context.rightTriggerAxis = MotionEvent.AXIS_THROTTLE
        } else {
            val rxRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_RX)
            val ryRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_RY)
            if (rxRange != null && ryRange != null) {
                if (dev.vendorId == 0x054c) {
                    val hasKeyC = dev.hasKeys(KeyEvent.KEYCODE_BUTTON_C)
                    if (hasKeyC != null && hasKeyC.isNotEmpty() && hasKeyC[0]) {
                        LimeLog.info("Detected non-standard DualShock 4 mapping")
                        context.isNonStandardDualShock4 = true
                    } else {
                        LimeLog.info("Detected DualShock 4 (Linux standard mapping)")
                        context.usesLinuxGamepadStandardFaceButtons = true
                    }
                }

                if (context.isNonStandardDualShock4) {
                    context.leftTriggerAxis = MotionEvent.AXIS_RX
                    context.rightTriggerAxis = MotionEvent.AXIS_RY
                    context.hasSelect = true
                    context.hasMode = true
                } else {
                    context.rightStickXAxis = MotionEvent.AXIS_RX
                    context.rightStickYAxis = MotionEvent.AXIS_RY
                    if (getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_Z) != null &&
                        getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_RZ) != null
                    ) {
                        context.leftTriggerAxis = MotionEvent.AXIS_Z
                        context.rightTriggerAxis = MotionEvent.AXIS_RZ
                    }
                }
                context.triggersIdleNegative = true
            }
        }

        if (context.rightStickXAxis == -1 && context.rightStickYAxis == -1) {
            val zRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_Z)
            val rzRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_RZ)
            if (zRange != null && rzRange != null) {
                context.rightStickXAxis = MotionEvent.AXIS_Z
                context.rightStickYAxis = MotionEvent.AXIS_RZ
            } else {
                val rxRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_RX)
                val ryRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_RY)
                if (rxRange != null && ryRange != null) {
                    context.rightStickXAxis = MotionEvent.AXIS_RX
                    context.rightStickYAxis = MotionEvent.AXIS_RY
                }
            }
        }

        val hatXRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_HAT_X)
        val hatYRange = getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_HAT_Y)
        if (hatXRange != null && hatYRange != null) {
            context.hatXAxis = MotionEvent.AXIS_HAT_X
            context.hatYAxis = MotionEvent.AXIS_HAT_Y
        }

        if (context.leftStickXAxis != -1 && context.leftStickYAxis != -1) {
            context.leftStickDeadzoneRadius = stickDeadzone.toFloat()
        }
        if (context.rightStickXAxis != -1 && context.rightStickYAxis != -1) {
            context.rightStickDeadzoneRadius = stickDeadzone.toFloat()
        }

        if (context.leftTriggerAxis != -1 && context.rightTriggerAxis != -1) {
            val ltRange = getMotionRangeForJoystickAxis(dev, context.leftTriggerAxis)
            val rtRange = getMotionRangeForJoystickAxis(dev, context.rightTriggerAxis)
            if (ltRange != null && rtRange != null) {
                context.triggerDeadzone = max(abs(ltRange.flat), abs(rtRange.flat))
            }
            if (context.triggerDeadzone < 0.13f || context.triggerDeadzone > 0.30f) {
                context.triggerDeadzone = 0.13f
            }
        }

        applySpecialDeviceFixups(context, dev, devName, gasRange)
    }

    private fun applySpecialDeviceFixups(
        context: InputDeviceContext,
        dev: InputDevice,
        devName: String,
        gasRange: InputDevice.MotionRange?,
    ) {
        if (dev.vendorId == 0x18d1 && dev.productId == 0x2c40) {
            context.backIsStart = true
            context.modeIsSelect = true
            context.triggerDeadzone = 0.30f
            context.hasSelect = true
            context.hasMode = false
        }

        context.ignoreBack = shouldIgnoreBack(dev)

        if (devName.contains("ASUS Gamepad")) {
            val hasStartKey = dev.hasKeys(KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_MENU, 0)
            if (hasStartKey != null && hasStartKey.size > 1 && !hasStartKey[0] && !hasStartKey[1]) {
                context.backIsStart = true
                context.modeIsSelect = true
                context.hasSelect = true
                context.hasMode = false
            }
            context.triggerDeadzone = 0.30f
        } else if (devName.contains("SHIELD") || devName.contains("NVIDIA Controller")) {
            if (devName.contains("NVIDIA Controller v01.03") || devName.contains("NVIDIA Controller v01.04")) {
                context.searchIsMode = true
                context.hasMode = true
            }
        } else if (devName.contains("Razer Serval")) {
            context.isServal = true
            context.hasMode = true
            context.hasSelect = true
        } else if (devName == "Xbox Wireless Controller" && gasRange == null) {
            context.isNonStandardXboxBtController = true
            context.hasMode = true
            context.hasSelect = true
        }

        if (dev.vendorId == 0x044f && dev.productId == 0xb328) {
            context.hasMode = false
        }
    }

    @SuppressLint("InlinedApi")
    fun sendControllerBatteryPacket(context: InputDeviceContext) {
        val inputDev = context.inputDevice ?: return
        var currentBatteryStatus = BatteryState.STATUS_FULL
        var currentBatteryCapacity = 0f
        var batteryPresent = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val batteryState = inputDev.batteryState
            batteryPresent = batteryState.isPresent
            if (batteryPresent) {
                currentBatteryStatus = batteryState.status
                currentBatteryCapacity = batteryState.capacity
            }
        }

        val sce = sceManager
        if (!batteryPresent) {
            if (sce != null && sce.isRecognizedDevice(inputDev)) {
                val batteryPercentage = sce.getBatteryPercentage(inputDev)
                currentBatteryCapacity = if (batteryPercentage < 0) Float.NaN else batteryPercentage / 100.0f

                val connectionType = sce.getConnectionType(inputDev)
                val chargingState = sce.getChargingState(inputDev)

                if (connectionType == SceConnectionType.WIRED || connectionType == SceConnectionType.BOTH) {
                    currentBatteryStatus =
                        when {
                            batteryPercentage == 100 -> BatteryState.STATUS_FULL
                            chargingState == SceChargingState.NOT_CHARGING -> BatteryState.STATUS_NOT_CHARGING
                            else -> BatteryState.STATUS_CHARGING
                        }
                } else if (connectionType == SceConnectionType.WIRELESS) {
                    currentBatteryStatus =
                        if (chargingState == SceChargingState.CHARGING) {
                            BatteryState.STATUS_CHARGING
                        } else {
                            BatteryState.STATUS_DISCHARGING
                        }
                } else {
                    currentBatteryStatus =
                        when {
                            batteryPercentage == 100 -> BatteryState.STATUS_FULL
                            chargingState == SceChargingState.NOT_CHARGING -> BatteryState.STATUS_DISCHARGING
                            chargingState == SceChargingState.CHARGING -> BatteryState.STATUS_CHARGING
                            else -> BatteryState.STATUS_UNKNOWN
                        }
                }
            } else {
                return
            }
        }

        if (currentBatteryStatus != context.lastReportedBatteryStatus ||
            !areBatteryCapacitiesEqual(currentBatteryCapacity, context.lastReportedBatteryCapacity)
        ) {
            val state: Byte =
                when (currentBatteryStatus) {
                    BatteryState.STATUS_UNKNOWN -> MoonBridge.LI_BATTERY_STATE_UNKNOWN
                    BatteryState.STATUS_CHARGING -> MoonBridge.LI_BATTERY_STATE_CHARGING
                    BatteryState.STATUS_DISCHARGING -> MoonBridge.LI_BATTERY_STATE_DISCHARGING
                    BatteryState.STATUS_NOT_CHARGING -> MoonBridge.LI_BATTERY_STATE_NOT_CHARGING
                    BatteryState.STATUS_FULL -> MoonBridge.LI_BATTERY_STATE_FULL
                    else -> return
                }

            val percentage: Byte =
                if (currentBatteryCapacity.isNaN()) {
                    MoonBridge.LI_BATTERY_PERCENTAGE_UNKNOWN
                } else {
                    (currentBatteryCapacity * 100).toInt().toByte()
                }

            packetSender.sendControllerBatteryEvent(context.controllerNumber.toByte(), state, percentage)
            context.lastReportedBatteryStatus = currentBatteryStatus
            context.lastReportedBatteryCapacity = currentBatteryCapacity
        }
    }

    fun handleSetControllerLED(
        controllerNumber: Short,
        r: Byte,
        g: Byte,
        b: Byte,
    ) {
        if (isStopped) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (i in 0 until inputDeviceContexts.size()) {
                val deviceContext = inputDeviceContexts.valueAt(i)
                if (deviceContext.controllerNumber == controllerNumber && deviceContext.hasRgbLed) {
                    deviceContext.ledArgbValue = ControllerLightsManager.packRgbToArgb(r, g, b)
                    backgroundThreadHandler.post(deviceContext.setLedStateRunnable)
                }
            }
        }
    }

    fun disableSensors() {
        for (i in 0 until inputDeviceContexts.size()) {
            inputDeviceContexts.valueAt(i).disableSensors()
        }
    }

    fun enableSensors() {
        if (isStopped) return
        for (i in 0 until inputDeviceContexts.size()) {
            inputDeviceContexts.valueAt(i).enableSensors()
        }
    }

    fun getActiveInputRumbleTargets(): List<RumbleTarget> {
        activeInputRumbleTargets.clear()
        for (i in 0 until inputDeviceContexts.size()) {
            activeInputRumbleTargets.add(inputDeviceContexts.valueAt(i))
        }
        return activeInputRumbleTargets
    }

    fun getActiveUsbRumbleTargets(): List<RumbleTarget> {
        activeUsbRumbleTargets.clear()
        for (i in 0 until usbDeviceContexts.size()) {
            activeUsbRumbleTargets.add(usbDeviceContexts.valueAt(i))
        }
        return activeUsbRumbleTargets
    }

    fun getActiveSensorTargets(): List<SensorTarget> {
        activeSensorTargets.clear()
        for (i in 0 until inputDeviceContexts.size()) {
            activeSensorTargets.add(inputDeviceContexts.valueAt(i))
        }
        for (i in 0 until usbDeviceContexts.size()) {
            activeSensorTargets.add(usbDeviceContexts.valueAt(i))
        }
        return activeSensorTargets
    }

    fun sendEmulatedMouseMove(
        x: Short,
        y: Short,
        mouseEmulationXDown: Boolean,
        mouseEmulationPixelMultiplier: Int,
    ) {
        convertRawStickAxisToPixelMovement(x, y, tempStickCoords)
        val vx = tempStickCoords[0]
        val vy = tempStickCoords[1]
        val mag = kotlin.math.hypot(vx.toDouble(), vy.toDouble())
        if (mag >= 1.0) {
            if (mouseEmulationXDown) {
                val dx = (java.lang.Integer.signum(vx.toInt()) * mouseEmulationPixelMultiplier).toShort()
                val dy = (java.lang.Integer.signum((-vy).toInt()) * mouseEmulationPixelMultiplier).toShort()
                packetSender.sendMouseMove(dx, dy)
            } else {
                packetSender.sendMouseMove(vx.toInt().toShort(), (-vy).toInt().toShort())
            }
        }
    }

    fun sendEmulatedMouseScroll(
        x: Short,
        y: Short,
    ) {
        convertRawStickAxisToPixelMovement(x, y, tempStickCoords)
        val vx = tempStickCoords[0]
        val vy = tempStickCoords[1]
        val mag = kotlin.math.hypot(vx.toDouble(), vy.toDouble())
        if (mag >= 1.0) {
            packetSender.sendMouseHighResScroll(vy.toInt().toShort())
            packetSender.sendMouseHighResHScroll(vx.toInt().toShort())
        }
    }

    private fun convertRawStickAxisToPixelMovement(
        stickX: Short,
        stickY: Short,
        outCoords: FloatArray,
    ) {
        var x = stickX * (1f / 32766.0f) * 4f
        var y = stickY * (1f / 32766.0f) * 4f
        val mag = kotlin.math.hypot(x.toDouble(), y.toDouble())
        if (mag > 0) {
            val mult = (mag * mag).toFloat()
            x *= mult
            y *= mult
        }
        outCoords[0] = x
        outCoords[1] = y
    }

    fun stop() {
        if (isStopped) return
        isStopped = true
        controllerRumbleManager?.isStopped = true
        inputManager?.unregisterInputDeviceListener(this)

        for (i in 0 until inputDeviceContexts.size()) {
            inputDeviceContexts.valueAt(i).destroy()
        }
        for (i in 0 until usbDeviceContexts.size()) {
            usbDeviceContexts.valueAt(i).destroy()
        }
        deviceVibrator?.cancel()
    }

    fun destroy() {
        if (!isStopped) {
            stop()
        }
        sceManager?.stop()
    }

    companion object {
        const val MAX_GAMEPADS: Short = 16
        const val BATTERY_RECHECK_INTERVAL_MS: Long = 120_000L

        @JvmField
        val ANDROID_TO_LI_BUTTON_MAP: Map<Int, Int> =
            mapOf(
                KeyEvent.KEYCODE_BUTTON_A to ControllerPacket.A_FLAG,
                KeyEvent.KEYCODE_BUTTON_B to ControllerPacket.B_FLAG,
                KeyEvent.KEYCODE_BUTTON_X to ControllerPacket.X_FLAG,
                KeyEvent.KEYCODE_BUTTON_Y to ControllerPacket.Y_FLAG,
                KeyEvent.KEYCODE_DPAD_UP to ControllerPacket.UP_FLAG,
                KeyEvent.KEYCODE_DPAD_DOWN to ControllerPacket.DOWN_FLAG,
                KeyEvent.KEYCODE_DPAD_LEFT to ControllerPacket.LEFT_FLAG,
                KeyEvent.KEYCODE_DPAD_RIGHT to ControllerPacket.RIGHT_FLAG,
                KeyEvent.KEYCODE_DPAD_UP_LEFT to (ControllerPacket.UP_FLAG or ControllerPacket.LEFT_FLAG),
                KeyEvent.KEYCODE_DPAD_UP_RIGHT to (ControllerPacket.UP_FLAG or ControllerPacket.RIGHT_FLAG),
                KeyEvent.KEYCODE_DPAD_DOWN_LEFT to (ControllerPacket.DOWN_FLAG or ControllerPacket.LEFT_FLAG),
                KeyEvent.KEYCODE_DPAD_DOWN_RIGHT to (ControllerPacket.DOWN_FLAG or ControllerPacket.RIGHT_FLAG),
                KeyEvent.KEYCODE_BUTTON_L1 to ControllerPacket.LB_FLAG,
                KeyEvent.KEYCODE_BUTTON_R1 to ControllerPacket.RB_FLAG,
                KeyEvent.KEYCODE_BUTTON_THUMBL to ControllerPacket.LS_CLK_FLAG,
                KeyEvent.KEYCODE_BUTTON_THUMBR to ControllerPacket.RS_CLK_FLAG,
                KeyEvent.KEYCODE_BUTTON_START to ControllerPacket.PLAY_FLAG,
                KeyEvent.KEYCODE_MENU to ControllerPacket.PLAY_FLAG,
                KeyEvent.KEYCODE_BUTTON_SELECT to ControllerPacket.BACK_FLAG,
                KeyEvent.KEYCODE_BACK to ControllerPacket.BACK_FLAG,
                KeyEvent.KEYCODE_BUTTON_MODE to ControllerPacket.SPECIAL_BUTTON_FLAG,
                KeyEvent.KEYCODE_MEDIA_RECORD to ControllerPacket.MISC_FLAG,
                KeyEvent.KEYCODE_BUTTON_1 to ControllerPacket.TOUCHPAD_FLAG,
            )

        @JvmStatic
        fun getMotionRangeForJoystickAxis(
            dev: InputDevice,
            axis: Int,
        ): InputDevice.MotionRange? {
            var range = dev.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK)
            if (range == null) {
                range = dev.getMotionRange(axis, InputDevice.SOURCE_GAMEPAD)
            }
            return range
        }

        @JvmStatic
        fun hasJoystickAxes(device: InputDevice): Boolean {
            return (device.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                getMotionRangeForJoystickAxis(device, MotionEvent.AXIS_X) != null &&
                getMotionRangeForJoystickAxis(device, MotionEvent.AXIS_Y) != null
        }

        @JvmStatic
        fun hasGamepadButtons(device: InputDevice): Boolean {
            return (device.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        }

        @JvmStatic
        fun isGameControllerDevice(device: InputDevice?): Boolean {
            if (device == null) return true
            if (hasJoystickAxes(device) || hasGamepadButtons(device)) return true

            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R && device.id == -1) {
                val ids = InputDevice.getDeviceIds()
                for (id in ids) {
                    val dev = InputDevice.getDevice(id) ?: continue
                    if (hasJoystickAxes(dev) || hasGamepadButtons(dev)) return true
                }
            }
            return device.keyboardType != InputDevice.KEYBOARD_TYPE_ALPHABETIC
        }

        @JvmStatic
        fun isExternal(dev: InputDevice): Boolean {
            if (Build.MODEL == "Tinker Board") return true

            val deviceName = dev.name ?: ""
            if (deviceName.contains("gpio") ||
                deviceName.contains("joy_key") ||
                deviceName.contains("keypad") ||
                deviceName.equals("NVIDIA Corporation NVIDIA Controller v01.01", ignoreCase = true) ||
                deviceName.equals("NVIDIA Corporation NVIDIA Controller v01.02", ignoreCase = true) ||
                deviceName.equals("GR0006", ignoreCase = true)
            ) {
                LimeLog.info("${dev.name} is internal by hardcoded mapping")
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return dev.isExternal
            } else {
                try {
                    val method = dev.javaClass.getMethod("isExternal")
                    val result = method.invoke(dev) as? Boolean
                    if (result != null) return result
                } catch (e: ReflectiveOperationException) {
                    LimeLog.warning("Failed to invoke isExternal via reflection: ${e.message}")
                }
            }
            return true
        }

        @JvmStatic
        fun isAssociatedJoystick(
            originalDevice: InputDevice,
            possibleAssociatedJoystick: InputDevice?,
        ): Boolean {
            if (possibleAssociatedJoystick == null) return false
            if ((possibleAssociatedJoystick.sources and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) {
                return false
            }
            if (possibleAssociatedJoystick.name == originalDevice.name) return false
            if (possibleAssociatedJoystick.descriptor != originalDevice.descriptor) return false
            return true
        }

        @JvmStatic
        fun hasButtonUnderTouchpad(
            dev: InputDevice,
            type: Byte,
        ): Boolean {
            if ((dev.sources and InputDevice.SOURCE_TOUCHPAD) != InputDevice.SOURCE_TOUCHPAD) {
                return false
            }
            return type == MoonBridge.LI_CTYPE_PS
        }

        @JvmStatic
        fun getAttachedControllerMask(context: Context): Short {
            var count = 0
            var mask: Short = 0
            val im = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
            if (im != null) {
                for (id in im.inputDeviceIds) {
                    val dev = im.getInputDevice(id) ?: continue
                    if (hasJoystickAxes(dev)) {
                        LimeLog.info("Counting InputDevice: ${dev.name}")
                        mask = (mask.toInt() or (1 shl count++)).toShort()
                    }
                }
            }

            if (PreferenceConfiguration.readPreferences(context).usbDriver) {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                if (usbManager != null) {
                    for (dev in usbManager.deviceList.values) {
                        if (UsbDriverService.shouldClaimDevice(dev, false) &&
                            !UsbDriverService.isRecognizedInputDevice(dev)
                        ) {
                            LimeLog.info("Counting UsbDevice: ${dev.deviceName}")
                            mask = (mask.toInt() or (1 shl count++)).toShort()
                        }
                    }
                }
            }

            if (PreferenceConfiguration.readPreferences(context).onscreenController) {
                LimeLog.info("Counting OSC gamepad")
                mask = (mask.toInt() or 1).toShort()
            }

            LimeLog.info("Enumerated $count gamepads")
            return mask
        }

        @JvmStatic
        fun areBatteryCapacitiesEqual(
            first: Float,
            second: Float,
        ): Boolean {
            return if (!first.isNaN() && !second.isNaN()) {
                first == second
            } else {
                first.isNaN() == second.isNaN()
            }
        }
    }
}
