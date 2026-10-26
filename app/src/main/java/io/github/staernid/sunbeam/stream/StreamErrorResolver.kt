package io.github.staernid.sunbeam.stream

import androidx.annotation.StringRes
import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.utils.ServerHelper
import kotlin.math.abs

fun interface StringProvider {
    fun getString(
        @StringRes resId: Int,
    ): String
}

interface StreamConnectivityDriver {
    fun testClientConnectivity(
        server: String,
        port: Int,
        portFlags: Int,
    ): Int

    fun getPortFlagsFromTerminationErrorCode(errorCode: Int): Int

    fun stringifyPortFlags(
        portFlags: Int,
        separator: String,
    ): String
}

object DefaultStreamConnectivityDriver : StreamConnectivityDriver {
    override fun testClientConnectivity(
        server: String,
        port: Int,
        portFlags: Int,
    ): Int {
        return MoonBridge.testClientConnectivity(server, port, portFlags)
    }

    override fun getPortFlagsFromTerminationErrorCode(errorCode: Int): Int {
        return MoonBridge.getPortFlagsFromTerminationErrorCode(errorCode)
    }

    override fun stringifyPortFlags(
        portFlags: Int,
        separator: String,
    ): String {
        return MoonBridge.stringifyPortFlags(portFlags, separator)
    }
}

sealed interface StageFailureResolution {
    data object RetryUnlocking : StageFailureResolution

    data class ShowErrorDialog(
        val title: String,
        val message: String,
        val isVideoDecoderFailure: Boolean,
    ) : StageFailureResolution
}

sealed interface TerminationResolution {
    data object GracefulExit : TerminationResolution

    data class ShowErrorDialog(
        val title: String,
        val message: String,
    ) : TerminationResolution
}

class StreamErrorResolver(
    private val stringProvider: StringProvider,
    private val connectivityDriver: StreamConnectivityDriver = DefaultStreamConnectivityDriver,
) {
    companion object {
        const val TEST_PORT = 443
        const val ERROR_CODE_PERMISSION_DENIED = 403
        const val ERROR_CODE_TIMEOUT = -408
        private const val HEX_ERROR_CODE_THRESHOLD = 1000
    }

    private fun shouldRetryUnlocking(
        errorCode: Int,
        portFlags: Int,
        portTestResult: Int,
    ): Boolean {
        if (errorCode != 0 || portFlags == 0) return false
        return portTestResult == MoonBridge.ML_TEST_RESULT_INCONCLUSIVE || portTestResult == 0
    }

    fun resolveStageFailure(
        stage: String,
        portFlags: Int,
        errorCode: Int,
        isSurfaceValid: Boolean = false,
    ): StageFailureResolution {
        val portTestResult =
            connectivityDriver.testClientConnectivity(
                ServerHelper.CONNECTION_TEST_SERVER,
                TEST_PORT,
                portFlags,
            )

        if (shouldRetryUnlocking(errorCode, portFlags, portTestResult)) {
            return StageFailureResolution.RetryUnlocking
        }

        val isVideoDecoderFailure = stage.contains("video", ignoreCase = true) && isSurfaceValid
        val title = stringProvider.getString(R.string.conn_error_title)

        val dialogText = StringBuilder()
        dialogText.append(stringProvider.getString(R.string.conn_error_msg))
            .append(" ")
            .append(stage)
            .append(" (error ")
            .append(errorCode)
            .append(")")

        when (errorCode) {
            ERROR_CODE_PERMISSION_DENIED -> {
                dialogText.append("\n\n")
                    .append(stringProvider.getString(R.string.error_msg_permission_denied))
                    .append(" (")
                    .append(stringProvider.getString(R.string.permission_launch_app))
                    .append(")")
            }
            ERROR_CODE_TIMEOUT -> {
                dialogText.append("\n\n")
                    .append(stringProvider.getString(R.string.error_msg_timeout))
            }
        }

        if (portFlags != 0) {
            dialogText.append("\n\n")
                .append(stringProvider.getString(R.string.check_ports_msg))
                .append("\n")
                .append(connectivityDriver.stringifyPortFlags(portFlags, "\n"))
        }

        if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0) {
            dialogText.append("\n\n")
                .append(stringProvider.getString(R.string.nettest_text_blocked))
        }

        return StageFailureResolution.ShowErrorDialog(
            title = title,
            message = dialogText.toString(),
            isVideoDecoderFailure = isVideoDecoderFailure,
        )
    }

    fun resolveTermination(errorCode: Int): TerminationResolution {
        if (errorCode == MoonBridge.ML_ERROR_GRACEFUL_TERMINATION) {
            return TerminationResolution.GracefulExit
        }

        val portFlags = connectivityDriver.getPortFlagsFromTerminationErrorCode(errorCode)
        val portTestResult =
            connectivityDriver.testClientConnectivity(
                ServerHelper.CONNECTION_TEST_SERVER,
                TEST_PORT,
                portFlags,
            )

        val title = stringProvider.getString(R.string.conn_terminated_title)

        var message =
            if (portTestResult != MoonBridge.ML_TEST_RESULT_INCONCLUSIVE && portTestResult != 0) {
                stringProvider.getString(R.string.nettest_text_blocked)
            } else {
                when (errorCode) {
                    MoonBridge.ML_ERROR_NO_VIDEO_TRAFFIC ->
                        stringProvider.getString(R.string.no_video_received_error)
                    MoonBridge.ML_ERROR_NO_VIDEO_FRAME ->
                        stringProvider.getString(R.string.no_frame_received_error)
                    MoonBridge.ML_ERROR_UNEXPECTED_EARLY_TERMINATION,
                    MoonBridge.ML_ERROR_PROTECTED_CONTENT,
                    ->
                        stringProvider.getString(R.string.early_termination_error)
                    MoonBridge.ML_ERROR_FRAME_CONVERSION ->
                        stringProvider.getString(R.string.frame_conversion_error)
                    else -> {
                        val errorCodeString =
                            if (abs(errorCode) > HEX_ERROR_CODE_THRESHOLD) {
                                Integer.toHexString(errorCode)
                            } else {
                                errorCode.toString()
                            }
                        stringProvider.getString(R.string.conn_terminated_msg) + "\n\n" +
                            stringProvider.getString(R.string.error_code_prefix) + " " + errorCodeString
                    }
                }
            }

        if (portFlags != 0) {
            message += "\n\n" + stringProvider.getString(R.string.check_ports_msg) + "\n" +
                connectivityDriver.stringifyPortFlags(portFlags, "\n")
        }

        return TerminationResolution.ShowErrorDialog(
            title = title,
            message = message,
        )
    }
}
