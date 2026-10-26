package io.github.staernid.sunbeam.core.stream

import io.github.staernid.sunbeam.core.stream.model.ControllerInput
import io.github.staernid.sunbeam.core.stream.model.StreamConfig
import io.github.staernid.sunbeam.core.stream.model.StreamEvent
import io.github.staernid.sunbeam.core.stream.model.StreamState
import io.github.staernid.sunbeam.nvstream.NvConnectionListener
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("TooManyFunctions", "LongParameterList")
class NvStreamSession(
    private val bridgeDriver: MoonBridgeDriver = DefaultMoonBridgeDriver,
    private val connectionDelegate: StreamConnectionDelegate? = null,
    private val stageFailureHandler: ((stage: String, portFlags: Int, errorCode: Int) -> Boolean)? = null,
) : StreamSession, NvConnectionListener {
    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    override val state = _state.asStateFlow()

    private val _events =
        MutableSharedFlow<StreamEvent>(
            replay = 0,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val events = _events.asSharedFlow()

    var currentConfig: StreamConfig? = null
        private set

    override suspend fun start(config: StreamConfig): Result<Unit> {
        currentConfig = config
        _state.value = StreamState.Connecting("Initializing")

        if (connectionDelegate != null) {
            val result = connectionDelegate.start(config, this)
            if (result.isFailure) {
                _state.value =
                    StreamState.Terminated(
                        errorCode = -1,
                        message = result.exceptionOrNull()?.message ?: "Stream failed to start",
                    )
                return result
            }
        }

        return Result.success(Unit)
    }

    override suspend fun stop() {
        bridgeDriver.interruptConnection()
        bridgeDriver.stopConnection()
        _state.value = StreamState.Terminated(0, "Stopped by user")
    }

    // Input sending path - strict zero allocations per frame/event
    override fun sendControllerInput(input: ControllerInput) {
        bridgeDriver.sendMultiControllerInput(
            input.controllerNumber,
            input.activeGamepadMask,
            input.buttonFlags,
            input.leftTrigger,
            input.rightTrigger,
            input.leftStickX,
            input.leftStickY,
            input.rightStickX,
            input.rightStickY,
        )
    }

    override fun sendKeyboardInput(
        keyCode: Short,
        keyAction: Byte,
        modifiers: Byte,
        flags: Byte,
    ) {
        bridgeDriver.sendKeyboardInput(keyCode, keyAction, modifiers, flags)
    }

    override fun sendMousePosition(
        x: Short,
        y: Short,
        referenceWidth: Short,
        referenceHeight: Short,
    ) {
        bridgeDriver.sendMousePosition(x, y, referenceWidth, referenceHeight)
    }

    override fun sendMouseButton(
        buttonEvent: Byte,
        mouseButton: Byte,
    ) {
        bridgeDriver.sendMouseButton(buttonEvent, mouseButton)
    }

    override fun sendMouseScroll(scrollAmount: Short) {
        bridgeDriver.sendMouseHighResScroll(scrollAmount)
    }

    override fun sendMouseMove(
        deltaX: Short,
        deltaY: Short,
    ) {
        bridgeDriver.sendMouseMove(deltaX, deltaY)
    }

    override fun sendControllerMotionEvent(
        controllerNumber: Byte,
        motionType: Byte,
        x: Float,
        y: Float,
        z: Float,
    ) {
        bridgeDriver.sendControllerMotionEvent(controllerNumber, motionType, x, y, z)
    }

    fun sendControllerArrivalEvent(
        controllerNumber: Byte,
        activeGamepadMask: Short,
        type: Byte,
        supportedButtonFlags: Int,
        capabilities: Short,
    ) {
        bridgeDriver.sendControllerArrivalEvent(
            controllerNumber,
            activeGamepadMask,
            type,
            supportedButtonFlags,
            capabilities,
        )
    }

    fun sendControllerTouchEvent(
        controllerNumber: Byte,
        eventType: Byte,
        pointerId: Int,
        x: Float,
        y: Float,
        pressure: Float,
    ) {
        bridgeDriver.sendControllerTouchEvent(
            controllerNumber,
            eventType,
            pointerId,
            x,
            y,
            pressure,
        )
    }

    fun sendControllerBatteryEvent(
        controllerNumber: Byte,
        batteryState: Byte,
        batteryPercentage: Byte,
    ) {
        bridgeDriver.sendControllerBatteryEvent(
            controllerNumber,
            batteryState,
            batteryPercentage,
        )
    }

    fun sendUtf8Text(text: String) {
        bridgeDriver.sendUtf8Text(text)
    }

    // NvConnectionListener callbacks
    override fun stageStarting(stage: String) {
        _state.value = StreamState.Connecting(stage)
        _events.tryEmit(StreamEvent.StageStarted(stage))
    }

    override fun stageComplete(stage: String) {
        _events.tryEmit(StreamEvent.StageCompleted(stage))
    }

    override fun stageFailed(
        stage: String,
        portFlags: Int,
        errorCode: Int,
    ): Boolean {
        _events.tryEmit(StreamEvent.StageFailed(stage, portFlags, errorCode))
        val shouldRetry = stageFailureHandler?.invoke(stage, portFlags, errorCode) ?: false
        if (!shouldRetry) {
            _state.value = StreamState.Terminated(errorCode, "Stage failed: $stage (error $errorCode)")
        }
        return shouldRetry
    }

    override fun connectionStarted() {
        val config = currentConfig
        val fps = config?.fps ?: 0
        val bitrate = config?.bitrateKbps ?: 0
        _state.value = StreamState.Streaming(fps, bitrate)
    }

    override fun connectionTerminated(errorCode: Int) {
        _state.value = StreamState.Terminated(errorCode, "Connection terminated (code: $errorCode)")
    }

    override fun connectionStatusUpdate(connectionStatus: Int) {
        _events.tryEmit(StreamEvent.ConnectionStatusChanged(connectionStatus))
    }

    override fun displayMessage(message: String) {
        _events.tryEmit(StreamEvent.DisplayMessage(message, isTransient = false))
    }

    override fun displayTransientMessage(message: String) {
        _events.tryEmit(StreamEvent.DisplayMessage(message, isTransient = true))
    }

    override fun rumble(
        controllerNumber: Short,
        lowFreqMotor: Short,
        highFreqMotor: Short,
    ) {
        _events.tryEmit(StreamEvent.Rumble(controllerNumber, lowFreqMotor, highFreqMotor))
    }

    override fun rumbleTriggers(
        controllerNumber: Short,
        leftTrigger: Short,
        rightTrigger: Short,
    ) {
        _events.tryEmit(StreamEvent.RumbleTriggers(controllerNumber, leftTrigger, rightTrigger))
    }

    override fun setHdrMode(
        enabled: Boolean,
        hdrMetadata: ByteArray?,
    ) {
        _events.tryEmit(StreamEvent.HdrModeChanged(enabled, hdrMetadata))
    }

    override fun setMotionEventState(
        controllerNumber: Short,
        motionType: Byte,
        reportRateHz: Short,
    ) {
        _events.tryEmit(StreamEvent.MotionEventStateChanged(controllerNumber, motionType, reportRateHz))
    }

    override fun setControllerLED(
        controllerNumber: Short,
        r: Byte,
        g: Byte,
        b: Byte,
    ) {
        _events.tryEmit(StreamEvent.ControllerLed(controllerNumber, r, g, b))
    }
}
