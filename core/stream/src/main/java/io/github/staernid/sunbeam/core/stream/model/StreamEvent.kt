package io.github.staernid.sunbeam.core.stream.model

sealed interface StreamEvent {
    data class StageStarted(val stage: String) : StreamEvent

    data class StageCompleted(val stage: String) : StreamEvent

    data class StageFailed(
        val stage: String,
        val portFlags: Int,
        val errorCode: Int,
    ) : StreamEvent

    data class ConnectionStatusChanged(val connectionStatus: Int) : StreamEvent

    data class DisplayMessage(val message: String, val isTransient: Boolean) : StreamEvent

    data class Rumble(
        val controllerNumber: Short,
        val lowFreqMotor: Short,
        val highFreqMotor: Short,
    ) : StreamEvent

    data class RumbleTriggers(
        val controllerNumber: Short,
        val leftTrigger: Short,
        val rightTrigger: Short,
    ) : StreamEvent

    data class ControllerLed(
        val controllerNumber: Short,
        val r: Byte,
        val g: Byte,
        val b: Byte,
    ) : StreamEvent

    data class HdrModeChanged(val enabled: Boolean, val hdrMetadata: ByteArray?) : StreamEvent
}
