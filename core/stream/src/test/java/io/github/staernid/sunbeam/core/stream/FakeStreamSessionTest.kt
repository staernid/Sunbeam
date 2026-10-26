package io.github.staernid.sunbeam.core.stream

import io.github.staernid.sunbeam.core.stream.model.ControllerInput
import io.github.staernid.sunbeam.core.stream.model.StreamConfig
import io.github.staernid.sunbeam.core.stream.model.StreamEvent
import io.github.staernid.sunbeam.core.stream.model.StreamState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeStreamSessionTest {
    @Test
    fun sessionLifecycle_transitionsStateCleanly() =
        runTest {
            val session = FakeStreamSession()
            assertEquals(StreamState.Idle, session.state.value)

            val config =
                StreamConfig(
                    hostAddress = "192.168.1.100",
                    httpsPort = 47984,
                    width = 1920,
                    height = 1080,
                    fps = 60,
                    bitrateKbps = 20000,
                )

            val result = session.start(config)
            assertTrue(result.isSuccess)
            assertEquals(StreamState.Streaming(60, 20000), session.state.value)
            assertEquals(config, session.lastConfig)

            session.stop()
            assertEquals(StreamState.Terminated(0, "Normal termination"), session.state.value)
        }

    @Test
    fun emitEvent_deliversToSubscriber() =
        runTest {
            val session = FakeStreamSession()
            session.emitEvent(StreamEvent.StageStarted("Control Handshake"))

            val event = session.events.first()
            assertTrue(event is StreamEvent.StageStarted)
            assertEquals("Control Handshake", (event as StreamEvent.StageStarted).stage)
        }

    @Test
    fun sendInput_recordsSnapshots() {
        val session = FakeStreamSession()

        val controllerInput =
            ControllerInput(
                controllerNumber = 0,
                activeGamepadMask = 0x1,
                leftTrigger = 127.toByte(),
                rightTrigger = 0.toByte(),
                leftStickX = 1000,
                leftStickY = -1000,
                rightStickX = 0,
                rightStickY = 0,
            )
        session.sendControllerInput(controllerInput)
        assertEquals(1, session.sentControllerInputs.size)
        assertEquals(127.toByte(), session.sentControllerInputs[0].leftTrigger)

        session.sendKeyboardInput(
            keyCode = 0x41,
            keyAction = 0x03,
            modifiers = 0,
            flags = 0,
        )
        assertEquals(1, session.sentKeyboardInputs.size)
        assertEquals(0x41.toShort(), session.sentKeyboardInputs[0].keyCode)

        session.sendMouseButton(buttonEvent = 1, mouseButton = 2)
        assertEquals(1, session.sentMouseButtons.size)

        session.sendMouseScroll(scrollAmount = 120)
        assertEquals(1, session.sentMouseScrolls.size)
    }
}
