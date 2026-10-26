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
)

package io.github.staernid.sunbeam.binding.input

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.lights.LightsManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.Toast
import io.github.staernid.sunbeam.GameMenu
import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.binding.input.driver.AbstractController
import io.github.staernid.sunbeam.nvstream.input.ControllerPacket
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration

/**
 * Common state and mouse-emulation logic for a generic game controller.
 */
@Suppress("LongParameterList")
open class GenericControllerContext(
    @JvmField val registry: ControllerDeviceRegistry,
) : GameInputDevice {
    @JvmField var id: Int = 0

    @JvmField var external: Boolean = false

    @JvmField var vendorId: Int = 0

    @JvmField var productId: Int = 0

    @JvmField var leftStickDeadzoneRadius: Float = 0f

    @JvmField var rightStickDeadzoneRadius: Float = 0f

    @JvmField var triggerDeadzone: Float = 0f

    @JvmField var assignedControllerNumber: Boolean = false

    @JvmField var reservedControllerNumber: Boolean = false
    open var controllerNumber: Short = 0

    @JvmField var inputMap: Int = 0

    @JvmField var leftTrigger: Byte = 0

    @JvmField var rightTrigger: Byte = 0

    @JvmField var rightStickX: Short = 0

    @JvmField var rightStickY: Short = 0

    @JvmField var leftStickX: Short = 0

    @JvmField var leftStickY: Short = 0

    @JvmField var mouseEmulationActive: Boolean = false

    @JvmField var mouseEmulationXDown: Boolean = false

    @JvmField var mouseEmulationPixelMultiplier: Int = 1

    @JvmField var mouseEmulationLastInputMap: Int = 0

    @JvmField val mouseEmulationReportPeriod: Int = 50

    @JvmField
    val mouseEmulationRunnable: Runnable =
        object : Runnable {
            override fun run() {
                if (!mouseEmulationActive) return

                when (registry.prefConfig.analogStickForScrolling) {
                    PreferenceConfiguration.AnalogStickForScrolling.RIGHT -> {
                        registry.sendEmulatedMouseMove(
                            leftStickX,
                            leftStickY,
                            mouseEmulationXDown,
                            mouseEmulationPixelMultiplier,
                        )
                        registry.sendEmulatedMouseScroll(rightStickX, rightStickY)
                    }
                    PreferenceConfiguration.AnalogStickForScrolling.LEFT -> {
                        registry.sendEmulatedMouseMove(
                            rightStickX,
                            rightStickY,
                            mouseEmulationXDown,
                            mouseEmulationPixelMultiplier,
                        )
                        registry.sendEmulatedMouseScroll(leftStickX, leftStickY)
                    }
                    else -> {
                        registry.sendEmulatedMouseMove(
                            leftStickX,
                            leftStickY,
                            mouseEmulationXDown,
                            mouseEmulationPixelMultiplier,
                        )
                        registry.sendEmulatedMouseMove(
                            rightStickX,
                            rightStickY,
                            mouseEmulationXDown,
                            mouseEmulationPixelMultiplier,
                        )
                    }
                }

                registry.mainThreadHandler.postDelayed(this, mouseEmulationReportPeriod.toLong())
            }
        }

    override fun getGameMenuOptions(): List<GameMenu.MenuOption> {
        val options = ArrayList<GameMenu.MenuOption>()
        options.add(
            GameMenu.MenuOption(
                registry.activityContext.getString(
                    if (mouseEmulationActive) R.string.game_menu_toggle_mouse_off else R.string.game_menu_toggle_mouse_on,
                ),
                true,
            ) { toggleMouseEmulation() },
        )
        return options
    }

    fun toggleMouseEmulation() {
        registry.mainThreadHandler.removeCallbacks(mouseEmulationRunnable)
        mouseEmulationActive = !mouseEmulationActive
        Toast.makeText(
            registry.activityContext,
            "Mouse emulation is: " + (if (mouseEmulationActive) "ON" else "OFF"),
            Toast.LENGTH_SHORT,
        ).show()

        if (mouseEmulationActive) {
            registry.mainThreadHandler.postDelayed(mouseEmulationRunnable, mouseEmulationReportPeriod.toLong())
        }
    }

    open fun destroy() {
        mouseEmulationActive = false
        registry.mainThreadHandler.removeCallbacks(mouseEmulationRunnable)
    }

    open fun sendControllerArrival() {}
}

/**
 * Controller context backed by an Android [InputDevice].
 */
@Suppress("LongParameterList")
open class InputDeviceContext(
    registry: ControllerDeviceRegistry,
) : GenericControllerContext(registry), RumbleTarget, SensorTarget, LightsTarget {
    @JvmField var name: String? = null
    override var vibratorManager: VibratorManager? = null
    override var vibrator: Vibrator? = null

    @JvmField var quadVibrators: Boolean = false
    override val hasQuadVibrators: Boolean get() = quadVibrators

    override var lowFreqMotor: Short = 0
    override var highFreqMotor: Short = 0
    override var leftTriggerMotor: Short = 0
    override var rightTriggerMotor: Short = 0

    override var sensorManager: SensorManager? = null
    override var gyroListener: SensorEventListener? = null
    override var gyroReportRateHz: Short = 0
    override var accelListener: SensorEventListener? = null
    override var accelReportRateHz: Short = 0

    override var inputDevice: InputDevice? = null

    override var hasRgbLed: Boolean = false
    override var lightsSession: LightsManager.LightsSession? = null
    override var ledArgbValue: Int = 0

    @JvmField var lastReportedBatteryStatus: Int = 0

    @JvmField var lastReportedBatteryCapacity: Float = 0f

    @JvmField var leftStickXAxis: Int = -1

    @JvmField var leftStickYAxis: Int = -1

    @JvmField var rightStickXAxis: Int = -1

    @JvmField var rightStickYAxis: Int = -1

    @JvmField var leftTriggerAxis: Int = -1

    @JvmField var rightTriggerAxis: Int = -1

    @JvmField var triggersIdleNegative: Boolean = false

    @JvmField var leftTriggerAxisUsed: Boolean = false

    @JvmField var rightTriggerAxisUsed: Boolean = false

    @JvmField var hatXAxis: Int = -1

    @JvmField var hatYAxis: Int = -1

    @JvmField var hatXAxisUsed: Boolean = false

    @JvmField var hatYAxisUsed: Boolean = false

    @JvmField var touchpadXRange: InputDevice.MotionRange? = null

    @JvmField var touchpadYRange: InputDevice.MotionRange? = null

    @JvmField var touchpadPressureRange: InputDevice.MotionRange? = null

    @JvmField var isNonStandardDualShock4: Boolean = false

    @JvmField var usesLinuxGamepadStandardFaceButtons: Boolean = false

    @JvmField var isNonStandardXboxBtController: Boolean = false

    @JvmField var isServal: Boolean = false

    @JvmField var backIsStart: Boolean = false

    @JvmField var modeIsSelect: Boolean = false

    @JvmField var searchIsMode: Boolean = false

    @JvmField var ignoreBack: Boolean = false

    @JvmField var hasJoystickAxes: Boolean = false

    @JvmField var pendingExit: Boolean = false

    @JvmField var isDualShockStandaloneTouchpad: Boolean = false

    @JvmField var emulatingButtonFlags: Int = 0

    @JvmField var hasSelect: Boolean = false

    @JvmField var hasMode: Boolean = false

    @JvmField var hasPaddles: Boolean = false

    @JvmField var hasShare: Boolean = false

    @JvmField var needsClickpadEmulation: Boolean = false

    @JvmField var lastLbUpTime: Long = 0

    @JvmField var lastRbUpTime: Long = 0

    @JvmField var startDownTime: Long = 0

    @JvmField var startUpTime: Long = 0

    @JvmField var backMenuPending: Boolean = false

    @JvmField
    val batteryStateUpdateRunnable: Runnable =
        object : Runnable {
            override fun run() {
                registry.sendControllerBatteryPacket(this@InputDeviceContext)
                registry.backgroundThreadHandler.postDelayed(this, ControllerDeviceRegistry.BATTERY_RECHECK_INTERVAL_MS)
            }
        }

    @JvmField
    val enableSensorRunnable: Runnable =
        Runnable {
            if (accelReportRateHz.toInt() != 0 && accelListener == null) {
                registry.onSetMotionEventState?.invoke(controllerNumber, MoonBridge.LI_MOTION_TYPE_ACCEL, accelReportRateHz)
            }
            if (gyroReportRateHz.toInt() != 0 && gyroListener == null) {
                registry.onSetMotionEventState?.invoke(controllerNumber, MoonBridge.LI_MOTION_TYPE_GYRO, gyroReportRateHz)
            }
        }

    @JvmField
    val setLedStateRunnable: Runnable =
        Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ControllerLightsManager.applyLedState(this@InputDeviceContext, ledArgbValue)
            }
        }

    override fun destroy() {
        super.destroy()
        registry.controllerRumbleManager?.cancel(this)
        registry.backgroundThreadHandler.removeCallbacks(enableSensorRunnable)
        registry.backgroundThreadHandler.removeCallbacks(setLedStateRunnable)
        registry.controllerSensorManager?.disableSensors(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ControllerLightsManager.closeSession(this)
        }
        registry.backgroundThreadHandler.removeCallbacks(batteryStateUpdateRunnable)
    }

    fun disableSensors() {
        registry.backgroundThreadHandler.removeCallbacks(enableSensorRunnable)
        registry.controllerSensorManager?.disableSensors(this)
    }

    fun enableSensors() {
        registry.backgroundThreadHandler.postDelayed(enableSensorRunnable, SENSOR_SETTLE_DELAY_MS)
    }

    fun migrateContext(oldContext: InputDeviceContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.lightsSession = oldContext.lightsSession
            oldContext.lightsSession = null
        }
        this.ledArgbValue = oldContext.ledArgbValue
        this.gyroReportRateHz = oldContext.gyroReportRateHz
        this.accelReportRateHz = oldContext.accelReportRateHz

        oldContext.destroy()

        this.assignedControllerNumber = oldContext.assignedControllerNumber
        this.reservedControllerNumber = oldContext.reservedControllerNumber
        this.controllerNumber = oldContext.controllerNumber

        if (oldContext.sensorManager == registry.deviceSensorManager) {
            this.sensorManager = registry.deviceSensorManager
        }

        this.needsClickpadEmulation = oldContext.needsClickpadEmulation
        enableSensors()

        if (this.lightsSession != null) {
            registry.backgroundThreadHandler.post(setLedStateRunnable)
        }

        registry.backgroundThreadHandler.post(batteryStateUpdateRunnable)
    }

    override fun sendControllerArrival() {
        val dev = inputDevice ?: return
        val type =
            when (dev.vendorId) {
                VENDOR_MICROSOFT -> MoonBridge.LI_CTYPE_XBOX
                VENDOR_SONY -> MoonBridge.LI_CTYPE_PS
                VENDOR_NINTENDO -> MoonBridge.LI_CTYPE_NINTENDO
                else -> MoonBridge.guessControllerType(dev.vendorId, dev.productId)
            }

        var supportedButtonFlags = 0
        for ((keyCode, flag) in ControllerDeviceRegistry.ANDROID_TO_LI_BUTTON_MAP) {
            val hasKeys = dev.hasKeys(keyCode)
            if (hasKeys != null && hasKeys.isNotEmpty() && hasKeys[0]) {
                supportedButtonFlags = supportedButtonFlags or flag
            }
        }

        if (hasPaddles) {
            supportedButtonFlags = supportedButtonFlags or
                (
                    ControllerPacket.PADDLE1_FLAG or
                        ControllerPacket.PADDLE2_FLAG or
                        ControllerPacket.PADDLE3_FLAG or
                        ControllerPacket.PADDLE4_FLAG
                )
        }
        if (hasShare) {
            supportedButtonFlags = supportedButtonFlags or ControllerPacket.MISC_FLAG
        }

        if (ControllerDeviceRegistry.getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_HAT_X) != null) {
            supportedButtonFlags = supportedButtonFlags or (ControllerPacket.LEFT_FLAG or ControllerPacket.RIGHT_FLAG)
        }
        if (ControllerDeviceRegistry.getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_HAT_Y) != null) {
            supportedButtonFlags = supportedButtonFlags or (ControllerPacket.UP_FLAG or ControllerPacket.DOWN_FLAG)
        }

        var capabilities: Short = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (quadVibrators) {
                capabilities =
                    (
                        capabilities.toInt() or
                            MoonBridge.LI_CCAP_RUMBLE.toInt() or
                            MoonBridge.LI_CCAP_TRIGGER_RUMBLE.toInt()
                    ).toShort()
            } else if (vibratorManager != null || vibrator != null) {
                capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_RUMBLE.toInt()).toShort()
            }

            if (external) {
                capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_BATTERY_STATE.toInt()).toShort()
            }

            if (hasRgbLed) {
                capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_RGB_LED.toInt()).toShort()
            }
        }

        if (leftTriggerAxis != -1 || rightTriggerAxis != -1) {
            capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_ANALOG_TRIGGERS.toInt()).toShort()
        }

        val sm = sensorManager
        if (sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_ACCEL.toInt()).toShort()
        }
        if (sm?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_GYRO.toInt()).toShort()
        }

        val reportedType: Byte
        if (type != MoonBridge.LI_CTYPE_PS && sm != null) {
            Toast.makeText(
                registry.activityContext,
                registry.activityContext.resources.getText(R.string.toast_controller_type_changed),
                Toast.LENGTH_LONG,
            ).show()
            reportedType = MoonBridge.LI_CTYPE_UNKNOWN
            needsClickpadEmulation = true
        } else {
            reportedType = type
        }

        if (vibrator != null) {
            capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_RUMBLE.toInt()).toShort()
        }

        val sce = registry.sceManager
        if (sce != null && sce.isRecognizedDevice(dev)) {
            capabilities =
                (
                    capabilities.toInt() or
                        MoonBridge.LI_CCAP_RUMBLE.toInt() or
                        MoonBridge.LI_CCAP_BATTERY_STATE.toInt()
                ).toShort()
        }

        if ((dev.sources and InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD) {
            capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_TOUCHPAD.toInt()).toShort()
            if (ControllerDeviceRegistry.hasButtonUnderTouchpad(dev, type)) {
                supportedButtonFlags = supportedButtonFlags or ControllerPacket.TOUCHPAD_FLAG
            }
        }

        registry.packetSender.sendControllerArrivalEvent(
            controllerNumber.toByte(),
            registry.getActiveControllerMask(),
            reportedType,
            supportedButtonFlags,
            capabilities,
        )

        if (registry.prefConfig.enableBatteryReport) {
            registry.backgroundThreadHandler.post(batteryStateUpdateRunnable)
        }
    }

    companion object {
        private const val SENSOR_SETTLE_DELAY_MS = 1000L
        private const val VENDOR_MICROSOFT = 0x045e
        private const val VENDOR_SONY = 0x054c
        private const val VENDOR_NINTENDO = 0x057e
    }
}

/**
 * Controller context backed by a custom [AbstractController] USB driver.
 */
open class UsbDeviceContext(
    registry: ControllerDeviceRegistry,
) : InputDeviceContext(registry) {
    @JvmField var device: AbstractController? = null

    override fun rumbleCustom(
        lowFreqMotor: Short,
        highFreqMotor: Short,
    ): Boolean {
        val dev = device ?: return false
        dev.rumble(lowFreqMotor, highFreqMotor)
        return true
    }

    override fun rumbleTriggersCustom(
        leftTrigger: Short,
        rightTrigger: Short,
    ): Boolean {
        val dev = device ?: return false
        dev.rumbleTriggers(leftTrigger, rightTrigger)
        return true
    }

    override fun sendControllerArrival() {
        val dev = device ?: return
        var type = dev.type
        var capabilities = dev.capabilities

        val sm = sensorManager
        if (type != MoonBridge.LI_CTYPE_PS && type != MoonBridge.LI_CTYPE_NINTENDO && sm != null) {
            if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
                capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_GYRO.toInt()).toShort()
            }
            if (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                capabilities = (capabilities.toInt() or MoonBridge.LI_CCAP_ACCEL.toInt()).toShort()
            }
            type = MoonBridge.LI_CTYPE_UNKNOWN
        }

        if (type != MoonBridge.LI_CTYPE_PS &&
            (capabilities.toInt() and (MoonBridge.LI_CCAP_GYRO.toInt() or MoonBridge.LI_CCAP_ACCEL.toInt())) != 0
        ) {
            registry.mainThreadHandler.post {
                Toast.makeText(
                    registry.activityContext,
                    registry.activityContext.resources.getText(R.string.toast_controller_type_changed),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

        registry.packetSender.sendControllerArrivalEvent(
            controllerNumber.toByte(),
            registry.getActiveControllerMask(),
            type,
            dev.supportedButtonFlags,
            capabilities,
        )
    }
}
