package io.github.staernid.sunbeam.binding.input

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.binding.input.driver.AbstractController
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import io.github.staernid.sunbeam.shadows.ShadowMoonBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowMoonBridge::class])
@Suppress("MagicNumber", "LongMethod")
class ControllerDeviceRegistryTest {
    private class RecordingControllerPacketSender : ControllerPacketSender {
        val controllerInputs = mutableListOf<List<Any>>()
        val arrivalEvents = mutableListOf<List<Any>>()
        val batteryEvents = mutableListOf<List<Any>>()
        val mouseMoves = mutableListOf<Pair<Short, Short>>()
        val mouseHighResScrolls = mutableListOf<Short>()
        val mouseHighResHScrolls = mutableListOf<Short>()

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
            controllerInputs.add(
                listOf(
                    controllerNumber,
                    activeGamepadMask,
                    buttonFlags,
                    leftTrigger,
                    rightTrigger,
                    leftStickX,
                    leftStickY,
                    rightStickX,
                    rightStickY,
                ),
            )
        }

        override fun sendControllerArrivalEvent(
            controllerNumber: Byte,
            activeGamepadMask: Short,
            type: Byte,
            supportedButtonFlags: Int,
            capabilities: Short,
        ): Int {
            arrivalEvents.add(
                listOf(
                    controllerNumber,
                    activeGamepadMask,
                    type,
                    supportedButtonFlags,
                    capabilities,
                ),
            )
            return 0
        }

        override fun sendControllerBatteryEvent(
            controllerNumber: Byte,
            batteryState: Byte,
            batteryPercentage: Byte,
        ) {
            batteryEvents.add(
                listOf(
                    controllerNumber,
                    batteryState,
                    batteryPercentage,
                ),
            )
        }

        override fun sendMouseMove(
            deltaX: Short,
            deltaY: Short,
        ) {
            mouseMoves.add(deltaX to deltaY)
        }

        override fun sendMouseHighResScroll(scrollAmount: Short) {
            mouseHighResScrolls.add(scrollAmount)
        }

        override fun sendMouseHighResHScroll(scrollAmount: Short) {
            mouseHighResHScrolls.add(scrollAmount)
        }
    }

    private class FakeUsbController(
        deviceId: Int = 1,
        vendorId: Int = 0x045e,
        productId: Int = 0x028e,
    ) : AbstractController(deviceId, null, vendorId, productId) {
        var lastLowFreq: Short = 0
        var lastHighFreq: Short = 0
        var lastLeftTrigger: Short = 0
        var lastRightTrigger: Short = 0

        init {
            type = MoonBridge.LI_CTYPE_XBOX
            supportedButtonFlags = 0xFFFF
            capabilities = MoonBridge.LI_CCAP_RUMBLE
        }

        override fun start(): Boolean = true

        override fun stop() {
            // No-op for test stub
        }

        override fun rumble(
            lowFreqMotor: Short,
            highFreqMotor: Short,
        ) {
            lastLowFreq = lowFreqMotor
            lastHighFreq = highFreqMotor
        }

        override fun rumbleTriggers(
            leftTrigger: Short,
            rightTrigger: Short,
        ) {
            lastLeftTrigger = leftTrigger
            lastRightTrigger = rightTrigger
        }
    }

    private lateinit var context: Context
    private lateinit var packetSender: RecordingControllerPacketSender
    private lateinit var prefConfig: PreferenceConfiguration
    private lateinit var registry: ControllerDeviceRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        packetSender = RecordingControllerPacketSender()
        prefConfig = PreferenceConfiguration()
        prefConfig.multiController = true
        prefConfig.deadzonePercentage = 10

        registry =
            ControllerDeviceRegistry(
                activityContext = context,
                packetSender = packetSender,
                prefConfig = prefConfig,
                deviceVibrator = null,
                deviceVibratorManager = null,
                deviceSensorManager = null,
                sceManager = null,
                inputManager = null,
                mainThreadHandler = Handler(Looper.getMainLooper()),
                backgroundThreadHandler = Handler(Looper.getMainLooper()),
            )
    }

    @Test
    fun assignControllerNumber_multiController_assignsIncrementalSlots() {
        val dev1 = FakeUsbController(deviceId = 1)
        val dev2 = FakeUsbController(deviceId = 2)

        registry.onUsbDeviceAdded(dev1)
        registry.onUsbDeviceAdded(dev2)

        val context1 = registry.usbDeviceContexts.get(1)
        val context2 = registry.usbDeviceContexts.get(2)

        assertNotNull(context1)
        assertNotNull(context2)

        registry.assignControllerNumberIfNeeded(context1)
        assertEquals(0.toShort(), context1.controllerNumber)
        assertTrue(context1.assignedControllerNumber)
        assertTrue(context1.reservedControllerNumber)

        registry.assignControllerNumberIfNeeded(context2)
        assertEquals(1.toShort(), context2.controllerNumber)
        assertTrue(context2.assignedControllerNumber)
        assertTrue(context2.reservedControllerNumber)

        // Bitmask contains slots 0 and 1: 0b11 = 3
        assertEquals(3.toShort(), registry.getActiveControllerMask())
        assertEquals(2, packetSender.arrivalEvents.size)
    }

    @Test
    fun assignControllerNumber_singleController_alwaysAssignsZero() {
        prefConfig.multiController = false

        val dev1 = FakeUsbController(deviceId = 1)
        val dev2 = FakeUsbController(deviceId = 2)

        registry.onUsbDeviceAdded(dev1)
        registry.onUsbDeviceAdded(dev2)

        val context1 = registry.usbDeviceContexts.get(1)
        val context2 = registry.usbDeviceContexts.get(2)

        registry.assignControllerNumberIfNeeded(context1)
        assertEquals(0.toShort(), context1.controllerNumber)

        registry.assignControllerNumberIfNeeded(context2)
        assertEquals(0.toShort(), context2.controllerNumber)

        // Single controller active mask is always 1
        assertEquals(1.toShort(), registry.getActiveControllerMask())
    }

    @Test
    fun releaseControllerNumber_freesSlotAndSendsZeroedInputPacket() {
        val dev = FakeUsbController(deviceId = 5)
        registry.onUsbDeviceAdded(dev)
        val ctx = registry.usbDeviceContexts.get(5)
        assertNotNull(ctx)

        registry.assignControllerNumberIfNeeded(ctx)
        assertEquals(0.toShort(), ctx.controllerNumber)
        assertEquals(1.toShort(), (registry.currentControllers.toInt() and 1).toShort())

        registry.releaseControllerNumber(ctx)

        // Slot 0 should be freed
        assertEquals(0.toShort(), (registry.currentControllers.toInt() and 1).toShort())

        // Should have sent a zeroed input packet to inform server of controller removal
        assertEquals(1, packetSender.controllerInputs.size)
        val packet = packetSender.controllerInputs[0]
        assertEquals(0.toShort(), packet[0]) // controllerNumber
        assertEquals(0, packet[2]) // buttonFlags
        assertEquals(0.toByte(), packet[3]) // leftTrigger
        assertEquals(0.toByte(), packet[4]) // rightTrigger
        assertEquals(0.toShort(), packet[5]) // leftStickX
        assertEquals(0.toShort(), packet[6]) // leftStickY
    }

    @Test
    fun hotplugUsbDevice_addAndRemove_managesContextLifecycle() {
        val dev = FakeUsbController(deviceId = 10)
        registry.onUsbDeviceAdded(dev)

        assertNotNull(registry.usbDeviceContexts.get(10))
        val ctx = registry.usbDeviceContexts.get(10)
        registry.assignControllerNumberIfNeeded(ctx)

        // Verify rumble delegate works on UsbDeviceContext
        assertTrue(ctx.rumbleCustom(100.toShort(), 200.toShort()))
        assertEquals(100.toShort(), dev.lastLowFreq)
        assertEquals(200.toShort(), dev.lastHighFreq)

        assertTrue(ctx.rumbleTriggersCustom(50.toShort(), 75.toShort()))
        assertEquals(50.toShort(), dev.lastLeftTrigger)
        assertEquals(75.toShort(), dev.lastRightTrigger)

        registry.onUsbDeviceRemoved(dev)
        assertNull(registry.usbDeviceContexts.get(10))
    }

    @Test
    fun areBatteryCapacitiesEqual_handlesNaNsAndValues() {
        assertTrue(ControllerDeviceRegistry.areBatteryCapacitiesEqual(0.75f, 0.75f))
        assertFalse(ControllerDeviceRegistry.areBatteryCapacitiesEqual(0.75f, 0.50f))
        assertTrue(ControllerDeviceRegistry.areBatteryCapacitiesEqual(Float.NaN, Float.NaN))
        assertFalse(ControllerDeviceRegistry.areBatteryCapacitiesEqual(0.75f, Float.NaN))
        assertFalse(ControllerDeviceRegistry.areBatteryCapacitiesEqual(Float.NaN, 0.75f))
    }

    @Test
    fun mouseEmulation_stickToPixelCalculations_forwardsExpectedMoveAndScroll() {
        // Full tilt right: +32766 on X, 0 on Y
        registry.sendEmulatedMouseMove(
            x = 32766.toShort(),
            y = 0.toShort(),
            mouseEmulationXDown = false,
            mouseEmulationPixelMultiplier = 1,
        )

        assertEquals(1, packetSender.mouseMoves.size)
        val move = packetSender.mouseMoves[0]
        assertTrue(move.first > 0)
        assertEquals(0.toShort(), move.second)

        // Scroll upward: Y positive
        registry.sendEmulatedMouseScroll(x = 0.toShort(), y = 32766.toShort())
        assertEquals(1, packetSender.mouseHighResScrolls.size)
        assertTrue(packetSender.mouseHighResScrolls[0] > 0)
    }

    @Test
    fun targetQueries_reuseAllocatedCollectionsWithoutHeapThrashing() {
        val dev1 = FakeUsbController(deviceId = 1)
        val dev2 = FakeUsbController(deviceId = 2)

        registry.onUsbDeviceAdded(dev1)
        registry.onUsbDeviceAdded(dev2)

        val rumbleTargets1 = registry.getActiveUsbRumbleTargets()
        assertEquals(2, rumbleTargets1.size)

        val rumbleTargets2 = registry.getActiveUsbRumbleTargets()
        // Must be the exact same reusable buffer instance
        assertTrue(rumbleTargets1 === rumbleTargets2)

        val sensorTargets1 = registry.getActiveSensorTargets()
        assertEquals(2, sensorTargets1.size)
        val sensorTargets2 = registry.getActiveSensorTargets()
        assertTrue(sensorTargets1 === sensorTargets2)
    }
}
