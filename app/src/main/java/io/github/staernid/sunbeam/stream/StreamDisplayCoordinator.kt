@file:Suppress(
    "TooManyFunctions",
    "LongParameterList",
    "LongMethod",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "MagicNumber",
    "TooGenericExceptionCaught",
    "SwallowedException",
    "LargeClass",
    "ComplexCondition",
    "ReturnCount",
    "MaxLineLength",
    "LoopWithTooManyJumpStatements",
)

package io.github.staernid.sunbeam.stream

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.Display
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import io.github.staernid.sunbeam.BuildConfig
import io.github.staernid.sunbeam.LimeLog
import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.binding.video.MediaCodecDecoderRenderer
import io.github.staernid.sunbeam.binding.video.SurfaceUtils
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import io.github.staernid.sunbeam.ui.StreamContainer
import io.github.staernid.sunbeam.utils.PerformanceDataTracker
import io.github.staernid.sunbeam.utils.ServerHelper.getActiveDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Coordinates display mode negotiation, refresh rate matching, window insets/cutouts,
 * orientation, immersive system UI, Picture-in-Picture, and post-stream telemetry for [io.github.staernid.sunbeam.Game].
 */
class StreamDisplayCoordinator(
    private val activity: Activity,
    private val prefConfig: PreferenceConfiguration,
    private val streamContainer: StreamContainer,
) {
    var desiredRefreshRate: Float = 60f
        private set

    var autoEnterPip: Boolean = false
        private set

    @SuppressLint("InlinedApi")
    private val hideSystemUiRunnable =
        Runnable {
            if (!prefConfig.fullScreen || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode)) {
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            } else {
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
        }

    fun hideSystemUi(delayMs: Int) {
        val handler = activity.window.decorView.handler ?: return
        handler.removeCallbacks(hideSystemUiRunnable)
        handler.postDelayed(hideSystemUiRunnable, delayMs.toLong())
    }

    fun onMultiWindowModeChanged(
        isInMultiWindowMode: Boolean,
        decoderRenderer: MediaCodecDecoderRenderer,
    ) {
        if (isInMultiWindowMode) {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decoderRenderer.notifyVideoBackground()
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decoderRenderer.notifyVideoForeground()
        }
        hideSystemUi(50)
    }

    fun applyWindowCutoutMode(
        displayWidth: Int,
        displayHeight: Int,
    ) {
        if (prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.STRETCH ||
            shouldIgnoreInsetsForResolution(displayWidth, displayHeight)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                activity.window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    fun shouldIgnoreInsetsForResolution(
        width: Int,
        height: Int,
    ): Boolean {
        if (!PreferenceConfiguration.isNativeResolution(width, height)) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = getActiveDisplay(activity, prefConfig)
            for (candidate in display.supportedModes) {
                if ((width == candidate.physicalWidth && height == candidate.physicalHeight) ||
                    (height == candidate.physicalWidth && width == candidate.physicalHeight)
                ) {
                    return true
                }
            }
        }

        return false
    }

    fun setPreferredOrientationForActivity(
        currentOrientation: Int,
        displayWidth: Int,
        displayHeight: Int,
    ) {
        val display = getActiveDisplay(activity, prefConfig)
        if (PreferenceConfiguration.isSquarishScreen(display)) {
            var desiredOrientation = Configuration.ORIENTATION_UNDEFINED
            if (prefConfig.onscreenController) {
                desiredOrientation = Configuration.ORIENTATION_LANDSCAPE
            }
            if (PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height)) {
                desiredOrientation =
                    if (displayWidth > displayHeight) {
                        Configuration.ORIENTATION_LANDSCAPE
                    } else {
                        Configuration.ORIENTATION_PORTRAIT
                    }
            }

            when (desiredOrientation) {
                Configuration.ORIENTATION_LANDSCAPE ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                Configuration.ORIENTATION_PORTRAIT ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                else ->
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            }
        } else {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        }
    }

    fun rotateScreen(currentOrientation: Int): Int =
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            Configuration.ORIENTATION_PORTRAIT
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            Configuration.ORIENTATION_LANDSCAPE
        }

    fun isRefreshRateEqualMatch(refreshRate: Float): Boolean =
        refreshRate >= prefConfig.fps && refreshRate <= prefConfig.fps + 3

    fun isRefreshRateGoodMatch(refreshRate: Float): Boolean =
        refreshRate >= prefConfig.fps && refreshRate.roundToInt() % prefConfig.fps <= 3

    fun mayReduceRefreshRate(): Boolean =
        prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS ||
            prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS ||
            (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_BALANCED && prefConfig.reduceRefreshRate)

    fun prepareDisplayForRendering(
        currentDisplay: Display,
        displayWidth: Int,
        displayHeight: Int,
        isOnExternalDisplay: Boolean,
    ): Float {
        val windowLayoutParams = activity.window.attributes
        val displayRefreshRate: Float

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var bestMode = currentDisplay.mode
            val isNativeResolutionStream =
                PreferenceConfiguration.isNativeResolution(prefConfig.width, prefConfig.height)
            var refreshRateIsGood = isRefreshRateGoodMatch(bestMode.refreshRate)
            var refreshRateIsEqual = isRefreshRateEqualMatch(bestMode.refreshRate)

            LimeLog.info(
                "Current display mode: ${bestMode.physicalWidth}x${bestMode.physicalHeight}x${bestMode.refreshRate}",
            )

            for (candidate in currentDisplay.supportedModes) {
                val refreshRateReduced = candidate.refreshRate < bestMode.refreshRate
                val resolutionReduced =
                    candidate.physicalWidth < bestMode.physicalWidth ||
                        candidate.physicalHeight < bestMode.physicalHeight
                val resolutionFitsStream =
                    candidate.physicalWidth >= prefConfig.width &&
                        candidate.physicalHeight >= prefConfig.height

                LimeLog.info(
                    "Examining display mode: ${candidate.physicalWidth}x${candidate.physicalHeight}x${candidate.refreshRate}",
                )

                if (candidate.physicalWidth > 4096 && prefConfig.width <= 4096) {
                    continue
                }

                if (prefConfig.width < 3840 && prefConfig.fps <= 60 && !isNativeResolutionStream) {
                    if (currentDisplay.mode.physicalWidth != candidate.physicalWidth ||
                        currentDisplay.mode.physicalHeight != candidate.physicalHeight
                    ) {
                        continue
                    }
                }

                if (resolutionReduced && !(prefConfig.fps > 60 && resolutionFitsStream)) {
                    continue
                }

                if (mayReduceRefreshRate() && refreshRateIsEqual && !isRefreshRateEqualMatch(candidate.refreshRate)) {
                    continue
                } else if (refreshRateIsGood) {
                    if (!isRefreshRateGoodMatch(candidate.refreshRate)) {
                        continue
                    }
                    if (mayReduceRefreshRate()) {
                        if (candidate.refreshRate > bestMode.refreshRate) {
                            continue
                        }
                    } else if (refreshRateReduced) {
                        continue
                    }
                } else if (!isRefreshRateGoodMatch(candidate.refreshRate)) {
                    if (refreshRateReduced) {
                        continue
                    }
                }

                bestMode = candidate
                refreshRateIsGood = isRefreshRateGoodMatch(candidate.refreshRate)
                refreshRateIsEqual = isRefreshRateEqualMatch(candidate.refreshRate)
            }

            LimeLog.info(
                "Best display mode: ${bestMode.physicalWidth}x${bestMode.physicalHeight}x${bestMode.refreshRate}",
            )

            if (currentDisplay.mode.modeId != bestMode.modeId) {
                if (prefConfig.enforceDisplayMode ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    currentDisplay.mode.physicalWidth != bestMode.physicalWidth ||
                    currentDisplay.mode.physicalHeight != bestMode.physicalHeight
                ) {
                    windowLayoutParams.preferredDisplayModeId = bestMode.modeId
                    activity.window.attributes = windowLayoutParams
                } else {
                    LimeLog.info("Using setFrameRate() instead of preferredDisplayModeId due to matching resolution")
                }
            } else {
                LimeLog.info("Current display mode is already the best display mode")
            }

            displayRefreshRate = bestMode.refreshRate
        } else {
            var bestRefreshRate = currentDisplay.refreshRate
            @Suppress("DEPRECATION")
            for (candidate in currentDisplay.supportedRefreshRates) {
                LimeLog.info("Examining refresh rate: $candidate")
                if (candidate > bestRefreshRate) {
                    if (prefConfig.fps <= 60 && candidate >= 63) {
                        continue
                    }
                    bestRefreshRate = candidate
                }
            }

            LimeLog.info("Selected refresh rate: $bestRefreshRate")
            windowLayoutParams.preferredRefreshRate = bestRefreshRate
            displayRefreshRate = bestRefreshRate
            activity.window.attributes = windowLayoutParams
        }

        var aspectRatioMatch = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val screenSize = Point(0, 0)
            @Suppress("DEPRECATION")
            currentDisplay.getSize(screenSize)
            val screenAspectRatio = screenSize.y.toDouble() / screenSize.x
            val streamAspectRatio = displayHeight.toDouble() / displayWidth
            if (abs(screenAspectRatio - streamAspectRatio) < 0.001 || isOnExternalDisplay) {
                LimeLog.info("Stream has compatible aspect ratio with output display")
                aspectRatioMatch = true
            }
        }

        if (!(prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.STRETCH || aspectRatioMatch)) {
            streamContainer.setDesiredAspectRatio(displayWidth.toDouble() / displayHeight.toDouble())
            streamContainer.setFillDisplay(prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.FILL)
            LimeLog.info("surfaceChanged-->${displayWidth.toDouble() / displayHeight.toDouble()}")
            LimeLog.info("scaleMode-->${prefConfig.videoScaleMode}")
        }

        desiredRefreshRate = displayRefreshRate

        return if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            isOnExternalDisplay
        ) {
            displayRefreshRate
        } else {
            min(currentDisplay.refreshRate, displayRefreshRate)
        }
    }

    fun computeStreamFrameRate(displayRefreshRate: Float): Float {
        val roundedRefreshRate = displayRefreshRate.roundToInt()
        var chosenFrameRate = prefConfig.fps.toFloat()
        if (prefConfig.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS) {
            if (prefConfig.fps >= roundedRefreshRate) {
                if (prefConfig.fps > roundedRefreshRate + 3) {
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED
                    LimeLog.info("Using drop mode for FPS > Hz")
                } else if (roundedRefreshRate <= 49) {
                    prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED
                    LimeLog.info("Bogus refresh rate: $roundedRefreshRate")
                } else {
                    chosenFrameRate = (roundedRefreshRate - 1).toFloat()
                    LimeLog.info("Adjusting FPS target for screen to $chosenFrameRate")
                }
            }
        }

        if (prefConfig.framePacingWarpFactor > 0) {
            chosenFrameRate *= prefConfig.framePacingWarpFactor
        }
        return chosenFrameRate
    }

    fun applySurfaceFrameRate(holder: SurfaceHolder) {
        val desiredFrameRate =
            if (mayReduceRefreshRate() || desiredRefreshRate < prefConfig.fps) {
                prefConfig.fps.toFloat()
            } else {
                desiredRefreshRate
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            holder.surface.setFrameRate(
                desiredFrameRate,
                Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                Surface.CHANGE_FRAME_RATE_ALWAYS,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            holder.surface.setFrameRate(
                desiredFrameRate,
                Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
            )
        }

        SurfaceUtils.setProducerThrottlingEnabled(holder.surface, false)
    }

    fun configureSurfaceViewForDisplay(
        rootView: View?,
        currentDisplay: Display,
        displayWidth: Int,
        displayHeight: Int,
    ) {
        try {
            val streamSurfaceView = findFirstSurfaceViewFrom(rootView) ?: return
            val vw = if (prefConfig.width > 0) prefConfig.width else displayWidth
            val vh = if (prefConfig.height > 0) prefConfig.height else displayHeight
            try {
                streamSurfaceView.holder.setFixedSize(vw, vh)
            } catch (_: Throwable) {
            }
            try {
                streamSurfaceView.setZOrderOnTop(false)
            } catch (_: Throwable) {
            }
            try {
                streamSurfaceView.setZOrderMediaOverlay(false)
            } catch (_: Throwable) {
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                var displayHz = 60f
                try {
                    displayHz =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            currentDisplay.mode.refreshRate
                        } else {
                            currentDisplay.refreshRate
                        }
                } catch (_: Throwable) {
                }

                val targetFps = if (prefConfig.fps > 0) prefConfig.fps.toFloat() else displayHz
                val sum =
                    "${Build.MANUFACTURER} ${Build.HARDWARE} ${Build.BOARD}".lowercase(Locale.US)
                val isMtkDevice = sum.contains("mtk") || sum.contains("mediatek")
                val compat =
                    if (isMtkDevice) {
                        Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                    } else {
                        Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE
                    }

                try {
                    val m =
                        SurfaceView::class.java.getMethod(
                            "setFrameRate",
                            Float::class.javaPrimitiveType,
                            Int::class.javaPrimitiveType,
                        )
                    m.invoke(streamSurfaceView, min(targetFps, displayHz), compat)
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun findFirstSurfaceViewFrom(view: View?): SurfaceView? {
        if (view is SurfaceView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findFirstSurfaceViewFrom(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    fun resolveHdrSupport(
        currentDisplay: Display,
        isOnExternalDisplay: Boolean,
    ): Boolean {
        if (!prefConfig.enableHdr) {
            return false
        }
        if (isOnExternalDisplay) {
            return true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val hdrCaps = currentDisplay.hdrCapabilities
            var supportsHdr10 = false
            if (hdrCaps != null) {
                @Suppress("DEPRECATION")
                for (hdrType in hdrCaps.supportedHdrTypes) {
                    if (hdrType == Display.HdrCapabilities.HDR_TYPE_HDR10) {
                        supportsHdr10 = true
                        break
                    }
                }
            }
            if (!supportsHdr10) {
                Toast.makeText(activity, "Display does not support HDR10", Toast.LENGTH_LONG).show()
            }
            return supportsHdr10
        } else {
            Toast.makeText(activity, "HDR requires Android 7.0 or later", Toast.LENGTH_LONG).show()
            return false
        }
    }

    fun configureDecoderAndResolveVideoFormats(
        decoderRenderer: MediaCodecDecoderRenderer,
        initialWillStreamHdr: Boolean,
    ): Int {
        try {
            var forceTight = false
            try {
                val field = prefConfig.javaClass.getDeclaredField("forceTightThresholds")
                field.isAccessible = true
                val value = field.get(prefConfig)
                if (value is Boolean) forceTight = value
            } catch (_: Throwable) {
            }
            try {
                decoderRenderer.setForceTightThresholds(forceTight)
            } catch (_: Throwable) {
            }
            if (forceTight) {
                LimeLog.info("ForceTightThresholds enabled: using vsync-based thresholds on all devices")
            }
        } catch (_: Throwable) {
        }

        try {
            if (prefConfig.preferLowerDelays) {
                decoderRenderer.setPreferLowerDelays(true)
                decoderRenderer.setPreferLowerDelaysTimeoutUs(500)
                prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED
                LimeLog.info("PreferLowerDelays: preferLowerDelays=true, timeout=500us, pacing=BALANCED")
            } else {
                decoderRenderer.setPreferLowerDelays(false)
                decoderRenderer.setPreferLowerDelaysTimeoutUs(2000)
                prefConfig.framePacing = PreferenceConfiguration.FRAME_PACING_BALANCED
                LimeLog.info("Balanced: preferLowerDelays=false, timeout=2000us, pacing=BALANCED")
            }
        } catch (_: Throwable) {
        }

        var willStreamHdr = initialWillStreamHdr
        if (willStreamHdr && !decoderRenderer.isHevcMain10Hdr10Supported && !decoderRenderer.isAv1Main10Supported) {
            willStreamHdr = false
            Toast.makeText(activity, "Decoder does not support HDR10 profile", Toast.LENGTH_LONG).show()
        }
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_HEVC &&
            !decoderRenderer.isHevcSupported
        ) {
            Toast.makeText(activity, "No HEVC decoder found", Toast.LENGTH_LONG).show()
        }
        if (prefConfig.videoFormat == PreferenceConfiguration.FormatOption.FORCE_AV1 &&
            !decoderRenderer.isAv1Supported
        ) {
            Toast.makeText(activity, "No AV1 decoder found", Toast.LENGTH_LONG).show()
        }

        var supportedVideoFormats = MoonBridge.VIDEO_FORMAT_H264
        if (decoderRenderer.isHevcSupported) {
            supportedVideoFormats = supportedVideoFormats or MoonBridge.VIDEO_FORMAT_H265
            if (willStreamHdr && decoderRenderer.isHevcMain10Hdr10Supported) {
                supportedVideoFormats = supportedVideoFormats or MoonBridge.VIDEO_FORMAT_H265_MAIN10
            }
        }
        if (decoderRenderer.isAv1Supported) {
            supportedVideoFormats = supportedVideoFormats or MoonBridge.VIDEO_FORMAT_AV1_MAIN8
            if (willStreamHdr && decoderRenderer.isAv1Main10Supported) {
                supportedVideoFormats = supportedVideoFormats or MoonBridge.VIDEO_FORMAT_AV1_MAIN10
            }
        }
        return supportedVideoFormats
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun getPictureInPictureParams(
        autoEnter: Boolean,
        rootView: View,
        appName: String?,
        pcName: String?,
    ): PictureInPictureParams {
        val view =
            if (prefConfig.videoScaleMode == PreferenceConfiguration.ScaleMode.FIT && streamContainer.scaleX == 1f) {
                streamContainer
            } else {
                rootView
            }

        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)

        val left = viewLocation[0]
        val top = viewLocation[1]
        val width = view.width
        val height = view.height
        val aspectRatio = Rational(width, height)
        val hint = Rect(left, top, left + width, top + height)

        val builder =
            PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .setSourceRectHint(hint)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appName != null) {
                builder.setTitle(appName)
                if (pcName != null) {
                    builder.setSubtitle(pcName)
                }
            } else if (pcName != null) {
                builder.setTitle(pcName)
            }
        }

        return builder.build()
    }

    fun updatePipAutoEnter(
        connected: Boolean,
        suppressPipRefCount: Int,
        isOnExternalDisplay: Boolean,
        rootView: View,
        appName: String?,
        pcName: String?,
    ) {
        if (!prefConfig.enablePip || isOnExternalDisplay) {
            return
        }

        val autoEnter = connected && suppressPipRefCount == 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.setPictureInPictureParams(getPictureInPictureParams(autoEnter, rootView, appName, pcName))
        } else {
            autoEnterPip = autoEnter
        }
    }

    fun onUserLeaveHint(
        isOnExternalDisplay: Boolean,
        rootView: View,
        appName: String?,
        pcName: String?,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (autoEnterPip && !isOnExternalDisplay) {
                try {
                    activity.enterPictureInPictureMode(getPictureInPictureParams(false, rootView, appName, pcName))
                } catch (e: Exception) {
                    LimeLog.warning("Failed to enter PiP: ${e.message}")
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    fun onPictureInPictureRequested(
        rootView: View,
        appName: String?,
        pcName: String?,
    ): Boolean {
        if (autoEnterPip && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            activity.enterPictureInPictureMode(getPictureInPictureParams(false, rootView, appName, pcName))
        }
        return true
    }

    fun setMetaKeyCaptureState(enabled: Boolean) {
        var keyboardCaptureSupported = false
        try {
            val windowLayoutParams = activity.window.attributes
            val setCaptureMethod =
                windowLayoutParams.javaClass.getMethod(
                    "setKeyboardCaptureEnabled",
                    Boolean::class.javaPrimitiveType,
                )
            setCaptureMethod.invoke(windowLayoutParams, enabled)
            activity.window.attributes = windowLayoutParams
            keyboardCaptureSupported = true
        } catch (_: Throwable) {
        }

        if (!keyboardCaptureSupported) {
            try {
                val semWindowManager = Class.forName("com.samsung.android.view.SemWindowManager")
                val getInstanceMethod = semWindowManager.getMethod("getInstance")
                val manager = getInstanceMethod.invoke(null)
                if (manager != null) {
                    val requestMetaKeyEventMethod =
                        semWindowManager.getDeclaredMethod(
                            "requestMetaKeyEvent",
                            ComponentName::class.java,
                            Boolean::class.javaPrimitiveType,
                        )
                    requestMetaKeyEventMethod.invoke(manager, activity.componentName, enabled)
                } else {
                    LimeLog.warning("SemWindowManager.getInstance() returned null")
                }
            } catch (_: ClassNotFoundException) {
            } catch (e: Exception) {
                LimeLog.warning("Failed to request Samsung meta key capture: ${e.message}")
            }
        }
    }

    fun reportPostStreamTelemetry(
        decoderRenderer: MediaCodecDecoderRenderer,
        tombstonePrefs: SharedPreferences,
        reportedCrash: Boolean,
        displayWidth: Int,
        displayHeight: Int,
    ) {
        val videoFormat = decoderRenderer.activeVideoFormat
        val averageEndToEndLat = decoderRenderer.averageEndToEndLatency
        val averageDecoderLat = decoderRenderer.averageDecoderLatency

        var message: String? = null
        if (averageEndToEndLat > 0) {
            message = "${activity.resources.getString(R.string.conn_client_latency)} $averageEndToEndLat ms"
            if (averageDecoderLat > 0) {
                message += " (${activity.resources.getString(R.string.conn_client_latency_hw)} $averageDecoderLat ms)"
            }
        } else if (averageDecoderLat > 0) {
            message = "${activity.resources.getString(R.string.conn_hardware_latency)} $averageDecoderLat ms"
        }

        val codecName =
            when {
                (videoFormat and MoonBridge.VIDEO_FORMAT_MASK_H264) != 0 -> "H.264"
                (videoFormat and MoonBridge.VIDEO_FORMAT_MASK_H265) != 0 -> "HEVC"
                (videoFormat and MoonBridge.VIDEO_FORMAT_MASK_AV1) != 0 -> "AV1"
                else -> "UNKNOWN"
            }
        val hdrSuffix = if ((videoFormat and MoonBridge.VIDEO_FORMAT_MASK_10BIT) != 0) " HDR" else ""
        val selectedVideoFormat = " [$codecName$hdrSuffix]"

        if (message != null && prefConfig.enableLatencyToast) {
            Toast.makeText(activity, message + selectedVideoFormat, Toast.LENGTH_LONG).show()
        }

        if (!reportedCrash && tombstonePrefs.getInt("CrashCount", 0) != 0) {
            tombstonePrefs.edit()
                .putInt("CrashCount", 0)
                .putInt("LastNotifiedCrashCount", 0)
                .apply()
        }

        if (prefConfig.enablePerfLogging && decoderRenderer.performanceWasTracked()) {
            PerformanceDataTracker().savePerformanceStatistics(
                activity.baseContext,
                Build.MODEL,
                Build.VERSION.SDK_INT.toString(),
                BuildConfig.VERSION_NAME,
                selectedVideoFormat,
                decoderRenderer.minDecoderLatency,
                decoderRenderer.minDecoderLatencyFullLog,
                (prefConfig.bitrate / 1000).toString(),
                "${displayWidth}x$displayHeight",
                "${prefConfig.fps} hz",
                "${decoderRenderer.averageDecoderLatency} ms",
                PreferenceConfiguration.getSelectedFramePacingName(activity.baseContext),
                formatCurrentTime(System.currentTimeMillis()),
            )
        }
    }

    companion object {
        @JvmStatic
        fun formatCurrentTime(currentTimeMillis: Long): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            return dateFormat.format(Date(currentTimeMillis))
        }
    }
}
