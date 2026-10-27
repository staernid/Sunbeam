package io.github.staernid.sunbeam.stream

import io.github.staernid.sunbeam.nvstream.NvConnectionListener
import io.github.staernid.sunbeam.nvstream.av.audio.AudioRenderer
import io.github.staernid.sunbeam.nvstream.av.video.VideoDecoderRenderer
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("EmptyFunctionBlock", "LongParameterList", "MaxLineLength")
class StreamLifecycleManagerTest {
    private class FakeConnectionDriver : ConnectionDriver {
        var startCount = 0
        var stopCount = 0
        var capturedListener: NvConnectionListener? = null

        override fun start(
            audioRenderer: AudioRenderer,
            videoDecoderRenderer: VideoDecoderRenderer,
            listener: NvConnectionListener,
        ) {
            startCount++
            capturedListener = listener
        }

        override fun stop() {
            stopCount++
        }
    }

    private class RecordingListener : StreamLifecycleListener {
        var stageStartingCalls = mutableListOf<String>()
        var stageCompleteCalls = mutableListOf<String>()
        var stageFailedRetryCalls = 0
        var stageFailedErrors = mutableListOf<String>()
        var connectionStartedCalls = 0
        var connectionTerminatedResolutions = mutableListOf<TerminationResolution>()
        var statusUpdates = mutableListOf<Int>()
        var displayMessages = mutableListOf<String>()
        var transientMessages = mutableListOf<String>()
        var streamEndingCalls = 0
        var appQuitSuccessCalls = mutableListOf<String>()
        var appQuitFailureCalls = mutableListOf<String>()

        override fun onStageStarting(stage: String) {
            stageStartingCalls.add(stage)
        }

        override fun onStageComplete(stage: String) {
            stageCompleteCalls.add(stage)
        }

        override fun onStageFailedRetryUnlocking() {
            stageFailedRetryCalls++
        }

        override fun onStageFailedError(
            title: String,
            message: String,
            isVideoDecoderFailure: Boolean,
        ) {
            stageFailedErrors.add("$title: $message (video=$isVideoDecoderFailure)")
        }

        override fun onConnectionStarted() {
            connectionStartedCalls++
        }

        override fun onConnectionTerminated(resolution: TerminationResolution) {
            connectionTerminatedResolutions.add(resolution)
        }

        override fun onConnectionStatusUpdate(connectionStatus: Int) {
            statusUpdates.add(connectionStatus)
        }

        override fun onDisplayMessage(message: String) {
            displayMessages.add(message)
        }

        override fun onDisplayTransientMessage(message: String) {
            transientMessages.add(message)
        }

        override fun onStreamEnding() {
            streamEndingCalls++
        }

        override fun onAppQuitSuccess(appName: String) {
            appQuitSuccessCalls.add(appName)
        }

        override fun onAppQuitFailure(message: String) {
            appQuitFailureCalls.add(message)
        }
    }

    private class FakeAudioRenderer : AudioRenderer {
        override fun setup(
            audioConfiguration: MoonBridge.AudioConfiguration?,
            sampleRate: Int,
            samplesPerFrame: Int,
        ): Int = 0

        override fun start() {}

        override fun stop() {}

        override fun playDecodedAudio(audioData: ShortArray?) {}

        override fun cleanup() {}
    }

    private class FakeVideoDecoderRenderer : VideoDecoderRenderer() {
        override fun setup(
            format: Int,
            width: Int,
            height: Int,
            redrawRate: Int,
        ): Int = 0

        override fun start() {}

        override fun stop() {}

        override fun submitDecodeUnit(
            decodeUnitData: ByteArray?,
            decodeUnitLength: Int,
            decodeUnitType: Int,
            frameNumber: Int,
            frameType: Int,
            frameHostProcessingLatency: Char,
            receiveTimeUs: Long,
            enqueueTimeUs: Long,
        ): Int = 0

        override fun cleanup() {}

        override fun getCapabilities(): Int = 0

        override fun setHdrMode(
            enabled: Boolean,
            hdrMetadata: ByteArray?,
        ) {}
    }

    private fun createManager(
        connectionDriver: FakeConnectionDriver = FakeConnectionDriver(),
        listener: RecordingListener = RecordingListener(),
        preferences: StreamLifecyclePreferences = StreamLifecyclePreferences(),
        appQuitter: AppQuitter? = null,
        quitOnStop: Boolean = false,
        appName: String? = "TestApp",
        errorResolver: StreamErrorResolver =
            StreamErrorResolver(
                { "STR" },
                object : StreamConnectivityDriver {
                    override fun testClientConnectivity(
                        server: String,
                        port: Int,
                        portFlags: Int,
                    ): Int = 0

                    override fun getPortFlagsFromTerminationErrorCode(errorCode: Int): Int = 0

                    override fun stringifyPortFlags(
                        portFlags: Int,
                        separator: String,
                    ): String = ""
                },
            ),
        isSurfaceValid: () -> Boolean = { true },
    ): Pair<StreamLifecycleManager, RecordingListener> {
        val manager =
            StreamLifecycleManager(
                connectionDriver = connectionDriver,
                controllerHandler = null,
                decoderRenderer = null,
                preferences = preferences,
                appQuitter = appQuitter,
                quitOnStop = quitOnStop,
                appName = appName,
                errorResolver = errorResolver,
                listener = listener,
                isSurfaceValid = isSurfaceValid,
                uiDispatcher = { it() },
                backgroundExecutor = { it() },
            )
        return manager to listener
    }

    @Test
    fun startConnection_startsDriverOnlyOnce() {
        val driver = FakeConnectionDriver()
        val (manager, _) = createManager(connectionDriver = driver)
        val audio = FakeAudioRenderer()
        val video = FakeVideoDecoderRenderer()

        manager.startConnection(audio, video)
        assertTrue(manager.isConnecting)
        assertTrue(manager.hasAttemptedConnection)
        assertEquals(1, driver.startCount)

        manager.startConnection(audio, video)
        assertEquals(1, driver.startCount)
    }

    @Test
    fun connectionStarted_transitionsStateAndNotifiesListener() {
        val (manager, listener) = createManager()
        val audio = FakeAudioRenderer()
        val video = FakeVideoDecoderRenderer()

        manager.startConnection(audio, video)
        assertTrue(manager.isConnecting)
        assertFalse(manager.isConnected)

        manager.connectionStarted()
        assertFalse(manager.isConnecting)
        assertTrue(manager.isConnected)
        assertEquals(1, listener.connectionStartedCalls)
    }

    @Test
    fun stopConnection_stopsDriverAndNotifiesStreamEnding() {
        val driver = FakeConnectionDriver()
        var quitCalled = false
        val (manager, listener) =
            createManager(
                connectionDriver = driver,
                appQuitter = { quitCalled = true },
                quitOnStop = true,
            )

        manager.connectionStarted()
        assertTrue(manager.isConnected)

        manager.stopConnection()
        assertFalse(manager.isConnected)
        assertFalse(manager.isConnecting)
        assertEquals(1, driver.stopCount)
        assertEquals(1, listener.streamEndingCalls)
        assertTrue(quitCalled)
        assertEquals(listOf("TestApp"), listener.appQuitSuccessCalls)
    }

    @Test
    fun stopConnection_quitsAppWhenQuitOnStopRequestedDynamically() {
        val driver = FakeConnectionDriver()
        var quitCalled = false
        val (manager, listener) =
            createManager(
                connectionDriver = driver,
                appQuitter = { quitCalled = true },
                quitOnStop = false,
            )

        manager.connectionStarted()
        assertTrue(manager.isConnected)

        manager.requestQuitOnStop()
        manager.stopConnection()

        assertFalse(manager.isConnected)
        assertFalse(manager.isConnecting)
        assertEquals(1, driver.stopCount)
        assertEquals(1, listener.streamEndingCalls)
        assertTrue(quitCalled)
        assertEquals(listOf("TestApp"), listener.appQuitSuccessCalls)
    }

    @Test
    fun stageFailed_retryResolutionReturnsTrueAndNotifiesListener() {
        val resolver =
            StreamErrorResolver(
                { "STR" },
                object : StreamConnectivityDriver {
                    override fun testClientConnectivity(
                        server: String,
                        port: Int,
                        portFlags: Int,
                    ): Int = 0

                    override fun getPortFlagsFromTerminationErrorCode(errorCode: Int): Int = 0

                    override fun stringifyPortFlags(
                        portFlags: Int,
                        separator: String,
                    ): String = ""
                },
            )
        val (manager, listener) = createManager(errorResolver = resolver)

        val retryResult = manager.stageFailed("Handshake", portFlags = 1, errorCode = 0)
        assertTrue(retryResult)
        assertEquals(1, listener.stageFailedRetryCalls)
        assertEquals(0, listener.stageFailedErrors.size)
    }

    @Test
    fun stageFailed_errorResolutionReturnsFalseAndSuppressesDuplicateDialogs() {
        val resolver =
            StreamErrorResolver(
                { "STR" },
                object : StreamConnectivityDriver {
                    override fun testClientConnectivity(
                        server: String,
                        port: Int,
                        portFlags: Int,
                    ): Int = 0

                    override fun getPortFlagsFromTerminationErrorCode(errorCode: Int): Int = 0

                    override fun stringifyPortFlags(
                        portFlags: Int,
                        separator: String,
                    ): String = ""
                },
            )
        val (manager, listener) = createManager(errorResolver = resolver)

        val failedResult1 = manager.stageFailed("video init", portFlags = 0, errorCode = -1)
        assertFalse(failedResult1)
        assertEquals(1, listener.stageFailedErrors.size)
        assertTrue(manager.hasDisplayedFailureDialog)

        val failedResult2 = manager.stageFailed("video init", portFlags = 0, errorCode = -1)
        assertFalse(failedResult2)
        assertEquals(1, listener.stageFailedErrors.size)
    }

    @Test
    fun connectionTerminated_stopsConnectionAndNotifiesResolution() {
        val driver = FakeConnectionDriver()
        val (manager, listener) = createManager(connectionDriver = driver)
        manager.connectionStarted()

        manager.connectionTerminated(MoonBridge.ML_ERROR_GRACEFUL_TERMINATION)
        assertEquals(1, driver.stopCount)
        assertEquals(1, listener.connectionTerminatedResolutions.size)
        assertTrue(listener.connectionTerminatedResolutions.first() is TerminationResolution.GracefulExit)
    }

    @Test
    fun warningsDisabled_suppressesStatusAndTransientUpdates() {
        val (manager, listener) =
            createManager(
                preferences = StreamLifecyclePreferences(disableWarnings = true),
            )

        manager.connectionStatusUpdate(MoonBridge.CONN_STATUS_POOR)
        manager.displayTransientMessage("Low bandwidth")
        manager.displayMessage("Important notice")

        assertEquals(0, listener.statusUpdates.size)
        assertEquals(0, listener.transientMessages.size)
        assertEquals(listOf("Important notice"), listener.displayMessages)
    }
}
