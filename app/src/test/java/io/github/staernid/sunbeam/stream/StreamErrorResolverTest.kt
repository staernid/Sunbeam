package io.github.staernid.sunbeam.stream

import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamErrorResolverTest {
    private val stringMap =
        mapOf(
            R.string.conn_error_title to "Connection Error",
            R.string.conn_error_msg to "Failed to start:",
            R.string.error_msg_permission_denied to "Permission denied",
            R.string.permission_launch_app to "Launch App",
            R.string.error_msg_timeout to "Connection timed out",
            R.string.check_ports_msg to "Check following ports:",
            R.string.nettest_text_blocked to "Ports are blocked by firewall",
            R.string.conn_terminated_title to "Stream Terminated",
            R.string.conn_terminated_msg to "The connection was terminated",
            R.string.error_code_prefix to "Error code:",
            R.string.no_video_received_error to "No video received from host",
            R.string.no_frame_received_error to "No frame received",
            R.string.early_termination_error to "Early termination error",
            R.string.frame_conversion_error to "Frame conversion error",
        )

    private val stringProvider =
        StringProvider { resId ->
            stringMap[resId] ?: "STRING_$resId"
        }

    private class FakeConnectivityDriver(
        var testResult: Int = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE,
        var portFlagsForTermination: Int = 0,
        var stringifiedPorts: String = "UDP 47998, 47999",
    ) : StreamConnectivityDriver {
        override fun testClientConnectivity(
            server: String,
            port: Int,
            portFlags: Int,
        ): Int = testResult

        override fun getPortFlagsFromTerminationErrorCode(errorCode: Int): Int = portFlagsForTermination

        override fun stringifyPortFlags(
            portFlags: Int,
            separator: String,
        ): String = stringifiedPorts
    }

    @Test
    fun stageFailure_retriesUnlockingWhenPortFlagsPresentAndInconclusive() {
        val driver = FakeConnectivityDriver(testResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE)
        val resolver = StreamErrorResolver(stringProvider, driver)

        val result =
            resolver.resolveStageFailure(
                stage = "RTSP Handshake",
                portFlags = 0x01,
                errorCode = 0,
                isSurfaceValid = true,
            )

        assertTrue(result is StageFailureResolution.RetryUnlocking)
    }

    @Test
    fun stageFailure_retriesUnlockingWhenPortFlagsPresentAndTestZero() {
        val driver = FakeConnectivityDriver(testResult = 0)
        val resolver = StreamErrorResolver(stringProvider, driver)

        val result =
            resolver.resolveStageFailure(
                stage = "RTSP Handshake",
                portFlags = 0x02,
                errorCode = 0,
                isSurfaceValid = true,
            )

        assertTrue(result is StageFailureResolution.RetryUnlocking)
    }

    @Test
    fun stageFailure_detectsVideoDecoderFailureWhenSurfaceValid() {
        val driver = FakeConnectivityDriver(testResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE)
        val resolver = StreamErrorResolver(stringProvider, driver)

        val result =
            resolver.resolveStageFailure(
                stage = "video decoder init",
                portFlags = 0,
                errorCode = -1,
                isSurfaceValid = true,
            )

        assertTrue(result is StageFailureResolution.ShowErrorDialog)
        val dialog = result as StageFailureResolution.ShowErrorDialog
        assertTrue(dialog.isVideoDecoderFailure)
        assertEquals("Connection Error", dialog.title)
        assertTrue(dialog.message.contains("Failed to start: video decoder init (error -1)"))
    }

    @Test
    fun stageFailure_doesNotFlagVideoFailureWhenSurfaceInvalid() {
        val driver = FakeConnectivityDriver(testResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE)
        val resolver = StreamErrorResolver(stringProvider, driver)

        val result =
            resolver.resolveStageFailure(
                stage = "video decoder init",
                portFlags = 0,
                errorCode = -1,
                isSurfaceValid = false,
            )

        assertTrue(result is StageFailureResolution.ShowErrorDialog)
        val dialog = result as StageFailureResolution.ShowErrorDialog
        assertFalse(dialog.isVideoDecoderFailure)
    }

    @Test
    fun stageFailure_formatsPermissionDeniedError() {
        val driver = FakeConnectivityDriver(testResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE)
        val resolver = StreamErrorResolver(stringProvider, driver)

        val result =
            resolver.resolveStageFailure(
                stage = "Launch",
                portFlags = 0,
                errorCode = StreamErrorResolver.ERROR_CODE_PERMISSION_DENIED,
            )

        assertTrue(result is StageFailureResolution.ShowErrorDialog)
        val dialog = result as StageFailureResolution.ShowErrorDialog
        assertTrue(dialog.message.contains("Permission denied (Launch App)"))
    }

    @Test
    fun stageFailure_formatsTimeoutErrorAndBlockedPorts() {
        val driver =
            FakeConnectivityDriver(
                testResult = 1,
                stringifiedPorts = "UDP 47998",
            )
        val resolver = StreamErrorResolver(stringProvider, driver)

        val result =
            resolver.resolveStageFailure(
                stage = "Control Stream",
                portFlags = 0x04,
                errorCode = StreamErrorResolver.ERROR_CODE_TIMEOUT,
            )

        assertTrue(result is StageFailureResolution.ShowErrorDialog)
        val dialog = result as StageFailureResolution.ShowErrorDialog
        assertTrue(dialog.message.contains("Connection timed out"))
        assertTrue(dialog.message.contains("Check following ports:\nUDP 47998"))
        assertTrue(dialog.message.contains("Ports are blocked by firewall"))
    }

    @Test
    fun termination_gracefulExitReturnsGraceful() {
        val resolver = StreamErrorResolver(stringProvider, FakeConnectivityDriver())

        val result = resolver.resolveTermination(MoonBridge.ML_ERROR_GRACEFUL_TERMINATION)

        assertTrue(result is TerminationResolution.GracefulExit)
    }

    @Test
    fun termination_blockedPortsSupersedesSpecificError() {
        val driver =
            FakeConnectivityDriver(
                testResult = 1,
                portFlagsForTermination = 0x01,
                stringifiedPorts = "TCP 47984",
            )
        val resolver = StreamErrorResolver(stringProvider, driver)

        val result = resolver.resolveTermination(MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC)

        assertTrue(result is TerminationResolution.ShowErrorDialog)
        val dialog = result as TerminationResolution.ShowErrorDialog
        assertTrue(dialog.message.contains("Ports are blocked by firewall"))
        assertTrue(dialog.message.contains("Check following ports:\nTCP 47984"))
        assertFalse(dialog.message.contains("No video received from host"))
    }

    @Test
    fun termination_resolvesPredefinedErrorCodes() {
        val driver = FakeConnectivityDriver(testResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE)
        val resolver = StreamErrorResolver(stringProvider, driver)

        val trafficResult = resolver.resolveTermination(MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC)
        assertTrue(trafficResult is TerminationResolution.ShowErrorDialog)
        val trafficDialog = trafficResult as TerminationResolution.ShowErrorDialog
        assertTrue(trafficDialog.message.contains("No video received from host"))

        val frameResult = resolver.resolveTermination(MoonBridge.ML_ERROR_NO_VIDEO_FRAME)
        assertTrue(frameResult is TerminationResolution.ShowErrorDialog)
        val frameDialog = frameResult as TerminationResolution.ShowErrorDialog
        assertTrue(frameDialog.message.contains("No frame received"))

        val earlyResult = resolver.resolveTermination(MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION)
        assertTrue(earlyResult is TerminationResolution.ShowErrorDialog)
        val earlyDialog = earlyResult as TerminationResolution.ShowErrorDialog
        assertTrue(earlyDialog.message.contains("Early termination error"))

        val convResult = resolver.resolveTermination(MoonBridge.ML_ERROR_FRAME_CONVERSION)
        assertTrue(convResult is TerminationResolution.ShowErrorDialog)
        val convDialog = convResult as TerminationResolution.ShowErrorDialog
        assertTrue(convDialog.message.contains("Frame conversion error"))
    }

    @Test
    fun termination_formatsLargeErrorAsHexAndSmallAsDecimal() {
        val driver = FakeConnectivityDriver(testResult = MoonBridge.ML_TEST_RESULT_INCONCLUSIVE)
        val resolver = StreamErrorResolver(stringProvider, driver)

        val largeResult = resolver.resolveTermination(0x2000)
        assertTrue(largeResult is TerminationResolution.ShowErrorDialog)
        val largeDialog = largeResult as TerminationResolution.ShowErrorDialog
        assertTrue(largeDialog.message.contains("Error code: 2000"))

        val smallResult = resolver.resolveTermination(500)
        assertTrue(smallResult is TerminationResolution.ShowErrorDialog)
        val smallDialog = smallResult as TerminationResolution.ShowErrorDialog
        assertTrue(smallDialog.message.contains("Error code: 500"))
    }
}
