package io.github.staernid.sunbeam.stream

@Suppress("TooManyFunctions")
interface StreamLifecycleListener {
    fun onStageStarting(stage: String)

    fun onStageComplete(stage: String)

    fun onStageFailedRetryUnlocking()

    fun onStageFailedError(
        title: String,
        message: String,
        isVideoDecoderFailure: Boolean,
    )

    fun onConnectionStarted()

    fun onConnectionTerminated(resolution: TerminationResolution)

    fun onConnectionStatusUpdate(connectionStatus: Int)

    fun onDisplayMessage(message: String)

    fun onDisplayTransientMessage(message: String)

    fun onStreamEnding()

    fun onAppQuitSuccess(appName: String)

    fun onAppQuitFailure(message: String)
}
