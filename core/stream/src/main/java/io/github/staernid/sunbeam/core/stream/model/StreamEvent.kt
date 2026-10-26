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

    data class MotionEventStateChanged(
        val controllerNumber: Short,
        val motionType: Byte,
        val reportRateHz: Short,
    ) : StreamEvent

    data class HdrModeChanged(val enabled: Boolean, val hdrMetadata: ByteArray?) : StreamEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HdrModeChanged

            if (enabled != other.enabled) return false
            if (!hdrMetadata.contentEquals(other.hdrMetadata)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = enabled.hashCode()
            result = 31 * result + (hdrMetadata?.contentHashCode() ?: 0)
            return result
        }
    }
}
