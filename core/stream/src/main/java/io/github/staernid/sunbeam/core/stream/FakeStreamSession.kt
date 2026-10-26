package io.github.staernid.sunbeam.core.stream

import io.github.staernid.sunbeam.core.stream.model.ControllerInput
import io.github.staernid.sunbeam.core.stream.model.StreamConfig
import io.github.staernid.sunbeam.core.stream.model.StreamEvent
import io.github.staernid.sunbeam.core.stream.model.StreamState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeStreamSession : StreamSession {
    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<StreamEvent>(replay = 1, extraBufferCapacity = 64)
    override val events = _events.asSharedFlow()

    var lastConfig: StreamConfig? = null
        private set

    val sentControllerInputs = mutableListOf<ControllerInput>()
    val sentKeyboardInputs = mutableListOf<KeyboardInputSnapshot>()
    val sentMousePositions = mutableListOf<MousePositionSnapshot>()
    val sentMouseButtons = mutableListOf<MouseButtonSnapshot>()
    val sentMouseScrolls = mutableListOf<Short>()

    data class KeyboardInputSnapshot(
        val keyCode: Short,
        val keyAction: Byte,
        val modifiers: Byte,
        val flags: Byte,
    )

    data class MousePositionSnapshot(
        val x: Short,
        val y: Short,
        val referenceWidth: Short,
        val referenceHeight: Short,
    )

    data class MouseButtonSnapshot(
        val buttonEvent: Byte,
        val mouseButton: Byte,
    )

    fun emitState(newState: StreamState) {
        _state.value = newState
    }

    fun emitEvent(event: StreamEvent) {
        _events.tryEmit(event)
    }

    override suspend fun start(config: StreamConfig): Result<Unit> {
        lastConfig = config
        _state.value = StreamState.Connecting("RTSP Handshake")
        _state.value = StreamState.Streaming(config.fps, config.bitrateKbps)
        return Result.success(Unit)
    }

    override suspend fun stop() {
        _state.value = StreamState.Terminated(0, "Normal termination")
    }

    override fun sendControllerInput(input: ControllerInput) {
        sentControllerInputs.add(input)
    }

    override fun sendKeyboardInput(
        keyCode: Short,
        keyAction: Byte,
        modifiers: Byte,
        flags: Byte,
    ) {
        sentKeyboardInputs.add(KeyboardInputSnapshot(keyCode, keyAction, modifiers, flags))
    }

    override fun sendMousePosition(
        x: Short,
        y: Short,
        referenceWidth: Short,
        referenceHeight: Short,
    ) {
        sentMousePositions.add(MousePositionSnapshot(x, y, referenceWidth, referenceHeight))
    }

    override fun sendMouseButton(
        buttonEvent: Byte,
        mouseButton: Byte,
    ) {
        sentMouseButtons.add(MouseButtonSnapshot(buttonEvent, mouseButton))
    }

    override fun sendMouseScroll(scrollAmount: Short) {
        sentMouseScrolls.add(scrollAmount)
    }
}
