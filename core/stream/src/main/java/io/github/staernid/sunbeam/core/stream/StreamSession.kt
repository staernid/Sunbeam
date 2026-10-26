package io.github.staernid.sunbeam.core.stream

import io.github.staernid.sunbeam.core.stream.model.ControllerInput
import io.github.staernid.sunbeam.core.stream.model.StreamConfig
import io.github.staernid.sunbeam.core.stream.model.StreamEvent
import io.github.staernid.sunbeam.core.stream.model.StreamState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface StreamSession {
    val state: StateFlow<StreamState>
    val events: SharedFlow<StreamEvent>

    suspend fun start(config: StreamConfig): Result<Unit>

    suspend fun stop()

    fun sendControllerInput(input: ControllerInput)

    fun sendKeyboardInput(
        keyCode: Short,
        keyAction: Byte,
        modifiers: Byte,
        flags: Byte,
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

    fun sendMouseScroll(scrollAmount: Short)

    fun sendMouseMove(
        deltaX: Short,
        deltaY: Short,
    ) {}

    fun sendControllerMotionEvent(
        controllerNumber: Byte,
        motionType: Byte,
        x: Float,
        y: Float,
        z: Float,
    ) {}
}
