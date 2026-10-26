package io.github.staernid.sunbeam.core.stream.model

sealed interface StreamState {
    data object Idle : StreamState

    data class Connecting(val stage: String) : StreamState

    data class Streaming(val activeFps: Int, val activeBitrateKbps: Int) : StreamState

    data class Terminated(val errorCode: Int, val message: String? = null) : StreamState
}
