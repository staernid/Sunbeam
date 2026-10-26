package io.github.staernid.sunbeam.stream

import android.os.Handler
import android.os.Looper
import io.github.staernid.sunbeam.LimeLog
import io.github.staernid.sunbeam.binding.input.ControllerHandler
import io.github.staernid.sunbeam.nvstream.NvConnection
import io.github.staernid.sunbeam.nvstream.NvConnectionListener
import io.github.staernid.sunbeam.nvstream.av.audio.AudioRenderer
import io.github.staernid.sunbeam.nvstream.av.video.VideoDecoderRenderer
import io.github.staernid.sunbeam.nvstream.http.NvHTTP
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration

interface ConnectionDriver {
    fun start(
        audioRenderer: AudioRenderer,
        videoDecoderRenderer: VideoDecoderRenderer,
        listener: NvConnectionListener,
    )

    fun stop()
}

class NvConnectionDriver(private val connection: NvConnection) : ConnectionDriver {
    override fun start(
        audioRenderer: AudioRenderer,
        videoDecoderRenderer: VideoDecoderRenderer,
        listener: NvConnectionListener,
    ) {
        connection.start(audioRenderer, videoDecoderRenderer, listener)
    }

    override fun stop() {
        connection.stop()
    }
}

fun interface AppQuitter {
    fun quitApp()
}

data class StreamLifecyclePreferences(
    val enableRumble: Boolean = true,
    val disableWarnings: Boolean = false,
)

@Suppress("TooManyFunctions", "LongParameterList")
class StreamLifecycleManager(
    private val connectionDriver: ConnectionDriver,
    private val controllerHandler: ControllerHandler?,
    private val decoderRenderer: VideoDecoderRenderer?,
    private val preferences: StreamLifecyclePreferences,
    private val appQuitter: AppQuitter?,
    private val quitOnStop: Boolean,
    private val appName: String?,
    private val errorResolver: StreamErrorResolver,
    private val listener: StreamLifecycleListener,
    private val isSurfaceValid: () -> Boolean,
    private val uiDispatcher: (action: () -> Unit) -> Unit = { action ->
        Handler(Looper.getMainLooper()).post(action)
    },
    private val backgroundExecutor: (action: () -> Unit) -> Unit = { action ->
        Thread(action).start()
    },
) : NvConnectionListener {
    constructor(
        connection: NvConnection,
        controllerHandler: ControllerHandler?,
        decoderRenderer: VideoDecoderRenderer?,
        prefConfig: PreferenceConfiguration,
        httpConn: NvHTTP?,
        quitOnStop: Boolean,
        appName: String?,
        errorResolver: StreamErrorResolver,
        listener: StreamLifecycleListener,
        isSurfaceValid: () -> Boolean,
    ) : this(
        connectionDriver = NvConnectionDriver(connection),
        controllerHandler = controllerHandler,
        decoderRenderer = decoderRenderer,
        preferences =
            StreamLifecyclePreferences(
                enableRumble = prefConfig.enableRumble,
                disableWarnings = prefConfig.disableWarnings,
            ),
        appQuitter = if (httpConn != null) AppQuitter { httpConn.quitApp() } else null,
        quitOnStop = quitOnStop,
        appName = appName,
        errorResolver = errorResolver,
        listener = listener,
        isSurfaceValid = isSurfaceValid,
    )

    companion object {
        const val QUIT_DELAY_MS = 1000L
    }

    var isConnecting: Boolean = false
        private set

    var isConnected: Boolean = false
        private set

    var hasAttemptedConnection: Boolean = false
        private set

    var hasDisplayedFailureDialog: Boolean = false

    fun startConnection(
        audioRenderer: AudioRenderer,
        videoDecoderRenderer: VideoDecoderRenderer,
    ) {
        if (!hasAttemptedConnection) {
            hasAttemptedConnection = true
            isConnecting = true
            connectionDriver.start(audioRenderer, videoDecoderRenderer, this)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun stopConnection() {
        if (isConnecting || isConnected) {
            isConnecting = false
            isConnected = false
            uiDispatcher { listener.onStreamEnding() }

            backgroundExecutor {
                try {
                    connectionDriver.stop()
                } catch (e: Exception) {
                    LimeLog.warning("Error stopping connection: ${e.message}")
                }
                if (appQuitter != null && quitOnStop) {
                    try {
                        Thread.sleep(QUIT_DELAY_MS)
                        appQuitter.quitApp()
                        uiDispatcher { listener.onAppQuitSuccess(appName.orEmpty()) }
                    } catch (e: Exception) {
                        val msg = e.message ?: "Failed to quit app"
                        uiDispatcher { listener.onAppQuitFailure(msg) }
                    }
                }
            }
        }
    }

    override fun stageStarting(stage: String) {
        uiDispatcher { listener.onStageStarting(stage) }
    }

    override fun stageComplete(stage: String) {
        uiDispatcher { listener.onStageComplete(stage) }
    }

    override fun stageFailed(
        stage: String,
        portFlags: Int,
        errorCode: Int,
    ): Boolean {
        val resolution =
            errorResolver.resolveStageFailure(
                stage = stage,
                portFlags = portFlags,
                errorCode = errorCode,
                isSurfaceValid = isSurfaceValid(),
            )

        return when (resolution) {
            is StageFailureResolution.RetryUnlocking -> {
                uiDispatcher { listener.onStageFailedRetryUnlocking() }
                true
            }
            is StageFailureResolution.ShowErrorDialog -> {
                if (!hasDisplayedFailureDialog) {
                    hasDisplayedFailureDialog = true
                    LimeLog.severe("$stage failed: $errorCode")
                    uiDispatcher {
                        listener.onStageFailedError(
                            resolution.title,
                            resolution.message,
                            resolution.isVideoDecoderFailure,
                        )
                    }
                }
                false
            }
        }
    }

    override fun connectionStarted() {
        isConnecting = false
        isConnected = true
        uiDispatcher { listener.onConnectionStarted() }
    }

    override fun connectionTerminated(errorCode: Int) {
        val resolution = errorResolver.resolveTermination(errorCode)
        if (!hasDisplayedFailureDialog) {
            hasDisplayedFailureDialog = true
            LimeLog.severe("Connection terminated: $errorCode")
            stopConnection()
            uiDispatcher { listener.onConnectionTerminated(resolution) }
        }
    }

    override fun connectionStatusUpdate(connectionStatus: Int) {
        if (!preferences.disableWarnings) {
            uiDispatcher { listener.onConnectionStatusUpdate(connectionStatus) }
        }
    }

    override fun displayMessage(message: String) {
        uiDispatcher { listener.onDisplayMessage(message) }
    }

    override fun displayTransientMessage(message: String) {
        if (!preferences.disableWarnings) {
            uiDispatcher { listener.onDisplayTransientMessage(message) }
        }
    }

    override fun rumble(
        controllerNumber: Short,
        lowFreqMotor: Short,
        highFreqMotor: Short,
    ) {
        if (preferences.enableRumble) {
            controllerHandler?.handleRumble(controllerNumber, lowFreqMotor, highFreqMotor)
        }
    }

    override fun rumbleTriggers(
        controllerNumber: Short,
        leftTrigger: Short,
        rightTrigger: Short,
    ) {
        controllerHandler?.handleRumbleTriggers(controllerNumber, leftTrigger, rightTrigger)
    }

    override fun setHdrMode(
        enabled: Boolean,
        hdrMetadata: ByteArray?,
    ) {
        LimeLog.info("Display HDR mode: " + if (enabled) "enabled" else "disabled")
        decoderRenderer?.setHdrMode(enabled, hdrMetadata)
    }

    override fun setMotionEventState(
        controllerNumber: Short,
        motionType: Byte,
        reportRateHz: Short,
    ) {
        controllerHandler?.handleSetMotionEventState(controllerNumber, motionType, reportRateHz)
    }

    override fun setControllerLED(
        controllerNumber: Short,
        r: Byte,
        g: Byte,
        b: Byte,
    ) {
        controllerHandler?.handleSetControllerLED(controllerNumber, r, g, b)
    }
}
