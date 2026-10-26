package io.github.staernid.sunbeam.core.stream

import io.github.staernid.sunbeam.core.stream.model.ControllerInput
import io.github.staernid.sunbeam.core.stream.model.StreamConfig
import io.github.staernid.sunbeam.core.stream.model.StreamEvent
import io.github.staernid.sunbeam.core.stream.model.StreamState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class NvStreamSessionTest {
    @Suppress("EmptyFunctionBlock")
    private class RecordingMoonBridgeDriver : MoonBridgeDriver {
        var stopCount = 0
        var interruptCount = 0
        var lastControllerNumber: Short = -1
        var lastButtonFlags: Int = -1
        var lastLeftTrigger: Byte = 0
        var lastRightTrigger: Byte = 0
        var lastLeftStickX: Short = 0
        var lastLeftStickY: Short = 0
        var lastKeyCode: Short = -1
        var lastKeyAction: Byte = -1
        var lastMouseX: Short = -1
        var lastMouseY: Short = -1
        var lastMouseButtonEvent: Byte = -1
        var lastMouseButton: Byte = -1
        var lastScrollAmount: Short = 0
        var lastMouseMoveDeltaX: Short = 0
        var lastMouseMoveDeltaY: Short = 0
        var lastMotionType: Byte = -1
        var lastMotionX: Float = 0f

        override fun stopConnection() {
            stopCount++
        }

        override fun interruptConnection() {
            interruptCount++
        }

        override fun sendMouseMove(
            deltaX: Short,
            deltaY: Short,
        ) {
            lastMouseMoveDeltaX = deltaX
            lastMouseMoveDeltaY = deltaY
        }

        override fun sendMousePosition(
            x: Short,
            y: Short,
            referenceWidth: Short,
            referenceHeight: Short,
        ) {
            lastMouseX = x
            lastMouseY = y
        }

        override fun sendMouseButton(
            buttonEvent: Byte,
            mouseButton: Byte,
        ) {
            lastMouseButtonEvent = buttonEvent
            lastMouseButton = mouseButton
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
            lastControllerNumber = controllerNumber
            lastButtonFlags = buttonFlags
            lastLeftTrigger = leftTrigger
            lastRightTrigger = rightTrigger
            lastLeftStickX = leftStickX
            lastLeftStickY = leftStickY
        }

        override fun sendKeyboardInput(
            keyCode: Short,
            keyAction: Byte,
            modifiers: Byte,
            flags: Byte,
        ) {
            lastKeyCode = keyCode
            lastKeyAction = keyAction
        }

        override fun sendMouseHighResScroll(scrollAmount: Short) {
            lastScrollAmount = scrollAmount
        }

        override fun sendMouseHighResHScroll(scrollAmount: Short) {}

        override fun sendControllerArrivalEvent(
            controllerNumber: Byte,
            activeGamepadMask: Short,
            type: Byte,
            supportedButtonFlags: Int,
            capabilities: Short,
        ): Int = 0

        override fun sendControllerTouchEvent(
            controllerNumber: Byte,
            eventType: Byte,
            pointerId: Int,
            x: Float,
            y: Float,
            pressure: Float,
        ): Int = 0

        override fun sendControllerMotionEvent(
            controllerNumber: Byte,
            motionType: Byte,
            x: Float,
            y: Float,
            z: Float,
        ): Int {
            lastMotionType = motionType
            lastMotionX = x
            return 0
        }

        override fun sendControllerBatteryEvent(
            controllerNumber: Byte,
            batteryState: Byte,
            batteryPercentage: Byte,
        ) {}

        override fun sendUtf8Text(text: String) {}
    }

    private val sampleConfig =
        StreamConfig(
            hostAddress = "192.168.1.50",
            httpsPort = 47984,
            width = 1920,
            height = 1080,
            fps = 60,
            bitrateKbps = 25000,
        )

    @Test
    fun sessionLifecycle_idleToConnectingToStreamingToTerminated() =
        runTest {
            val driver = RecordingMoonBridgeDriver()
            val session = NvStreamSession(bridgeDriver = driver)

            assertEquals(StreamState.Idle, session.state.value)

            session.start(sampleConfig)
            assertEquals(StreamState.Connecting("Initializing"), session.state.value)

            session.stageStarting("RTSP Handshake")
            assertEquals(StreamState.Connecting("RTSP Handshake"), session.state.value)

            session.connectionStarted()
            assertEquals(StreamState.Streaming(60, 25000), session.state.value)

            session.connectionTerminated(-100)
            assertTrue(session.state.value is StreamState.Terminated)
            assertEquals(-100, (session.state.value as StreamState.Terminated).errorCode)
        }

    @Test
    fun stop_invokesDriverAndTerminates() =
        runTest {
            val driver = RecordingMoonBridgeDriver()
            val session = NvStreamSession(bridgeDriver = driver)

            session.start(sampleConfig)
            session.stop()

            assertEquals(1, driver.interruptCount)
            assertEquals(1, driver.stopCount)
            assertEquals(StreamState.Terminated(0, "Stopped by user"), session.state.value)
        }

    @Test
    fun stageFailure_withoutRetry_terminatesSession() =
        runTest {
            val session = NvStreamSession()

            val shouldRetry = session.stageFailed("Audio Setup", 0, -500)
            assertFalse(shouldRetry)
            assertTrue(session.state.value is StreamState.Terminated)
            assertEquals(-500, (session.state.value as StreamState.Terminated).errorCode)
        }

    @Test
    fun stageFailure_withRetryHandler_allowsRetry() =
        runTest {
            val session =
                NvStreamSession(
                    stageFailureHandler = { _, _, _ -> true },
                )

            val shouldRetry = session.stageFailed("RTSP Handshake", 0x1, 0)
            assertTrue(shouldRetry)
            assertEquals(StreamState.Idle, session.state.value)
        }

    @Test
    fun connectionDelegateFailure_setsTerminatedState() =
        runTest {
            val session =
                NvStreamSession(
                    connectionDelegate = { _, _ -> Result.failure(IOException("Host offline")) },
                )

            val result = session.start(sampleConfig)
            assertTrue(result.isFailure)
            assertTrue(session.state.value is StreamState.Terminated)
            assertEquals("Host offline", (session.state.value as StreamState.Terminated).message)
        }

    @Test
    fun feedbackEvents_routedToSharedFlow() =
        runTest(UnconfinedTestDispatcher()) {
            val session = NvStreamSession()
            val collectedEvents = mutableListOf<StreamEvent>()

            val job =
                launch {
                    session.events.toList(collectedEvents)
                }

            session.stageStarting("Handshake")
            session.stageComplete("Handshake")
            session.rumble(0, 100, 200)
            session.rumbleTriggers(0, 30, 40)
            session.setControllerLED(0, 255.toByte(), 128.toByte(), 0.toByte())
            session.setHdrMode(true, byteArrayOf(1, 2, 3))
            session.setMotionEventState(0, 2.toByte(), 100.toShort())
            session.connectionStatusUpdate(1)
            session.displayMessage("Low Bitrate")
            session.displayTransientMessage("Transient Alert")

            job.cancel()

            assertEquals(10, collectedEvents.size)
            assertTrue(collectedEvents[0] is StreamEvent.StageStarted)
            assertTrue(collectedEvents[1] is StreamEvent.StageCompleted)
            assertTrue(collectedEvents[2] is StreamEvent.Rumble)
            assertEquals(100.toShort(), (collectedEvents[2] as StreamEvent.Rumble).lowFreqMotor)
            assertTrue(collectedEvents[3] is StreamEvent.RumbleTriggers)
            assertTrue(collectedEvents[4] is StreamEvent.ControllerLed)
            assertTrue(collectedEvents[5] is StreamEvent.HdrModeChanged)
            assertTrue(collectedEvents[6] is StreamEvent.MotionEventStateChanged)
            assertTrue(collectedEvents[7] is StreamEvent.ConnectionStatusChanged)
            assertTrue(collectedEvents[8] is StreamEvent.DisplayMessage)
            assertFalse((collectedEvents[8] as StreamEvent.DisplayMessage).isTransient)
            assertTrue(collectedEvents[9] is StreamEvent.DisplayMessage)
            assertTrue((collectedEvents[9] as StreamEvent.DisplayMessage).isTransient)
        }

    @Test
    fun inputForwarding_forwardsToDriver() {
        val driver = RecordingMoonBridgeDriver()
        val session = NvStreamSession(bridgeDriver = driver)

        val input =
            ControllerInput(
                controllerNumber = 1,
                activeGamepadMask = 0x2,
                buttonFlags = 0x1000,
                leftTrigger = 200.toByte(),
                rightTrigger = 50.toByte(),
                leftStickX = 1234,
                leftStickY = -5678,
                rightStickX = 0,
                rightStickY = 0,
            )
        session.sendControllerInput(input)
        assertEquals(1.toShort(), driver.lastControllerNumber)
        assertEquals(0x1000, driver.lastButtonFlags)
        assertEquals(200.toByte(), driver.lastLeftTrigger)
        assertEquals(1234.toShort(), driver.lastLeftStickX)

        session.sendKeyboardInput(0x20, 0x01, 0, 0)
        assertEquals(0x20.toShort(), driver.lastKeyCode)
        assertEquals(0x01.toByte(), driver.lastKeyAction)

        session.sendMousePosition(100, 200, 1920, 1080)
        assertEquals(100.toShort(), driver.lastMouseX)
        assertEquals(200.toShort(), driver.lastMouseY)

        session.sendMouseButton(1, 2)
        assertEquals(1.toByte(), driver.lastMouseButtonEvent)
        assertEquals(2.toByte(), driver.lastMouseButton)

        session.sendMouseScroll(120)
        assertEquals(120.toShort(), driver.lastScrollAmount)

        session.sendMouseMove(15, -25)
        assertEquals(15.toShort(), driver.lastMouseMoveDeltaX)
        assertEquals((-25).toShort(), driver.lastMouseMoveDeltaY)

        session.sendControllerMotionEvent(1, 2, 0.5f, 1.0f, -0.5f)
        assertEquals(2.toByte(), driver.lastMotionType)
        assertEquals(0.5f, driver.lastMotionX, 0.001f)
    }
}
