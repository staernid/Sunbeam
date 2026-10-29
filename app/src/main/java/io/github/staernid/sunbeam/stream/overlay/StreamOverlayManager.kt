@file:Suppress(
    "LongParameterList",
    "MagicNumber",
    "TooManyFunctions",
    "ReturnCount",
    "NestedBlockDepth",
    "MaxLineLength",
)

package io.github.staernid.sunbeam.stream.overlay

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.github.staernid.sunbeam.R
import io.github.staernid.sunbeam.core.designsystem.theme.SunbeamTheme
import io.github.staernid.sunbeam.features.stream.StreamViewModel
import io.github.staernid.sunbeam.features.stream.component.StreamOverlayLayer
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayMode
import io.github.staernid.sunbeam.features.stream.model.PerfOverlayPosition
import io.github.staernid.sunbeam.features.stream.model.PerformanceStats
import io.github.staernid.sunbeam.features.stream.model.StreamDialog
import io.github.staernid.sunbeam.features.stream.model.StreamMouseMode
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Interface providing root/decor view bounds without heap allocations.
 */
fun interface DecorBoundsProvider {
    fun getDecorDimension(isWidth: Boolean): Int
}

/**
 * Interface consuming shortcut keycodes.
 */
fun interface ShortcutKeyConsumer {
    fun onSendKeys(keys: ShortArray)
}

/**
 * Parses performance log text from [io.github.staernid.sunbeam.binding.video.MediaCodecDecoderRenderer]
 * into structured [PerformanceStats]. Supports both full and lite decoder log formats.
 */
object PerformanceStatsParser {
    private val renderingFpsRegex =
        Regex("""Rendering frame rate:\s*(\d+(?:\.\d+)?)\s*FPS""", RegexOption.IGNORE_CASE)
    private val fpsRegex =
        Regex(
            """(?:(\d+(?:\.\d+)?)\s*FPS|(?:FPS[ \t]*(?::|：)[ \t]*|FPS：[ \t]*|Incoming frame rate[^:]*:\s*)(\d+(?:\.\d+)?))""",
            RegexOption.IGNORE_CASE,
        )
    private val rttRegex =
        Regex(
            """(?:Average network latency:\s*(\d+)\s*ms|Network/Decoding delay:\s*(\d+)\s*ms|(?:avg|net|rtt|latency|延迟)[ \t]*[:：][ \t]*(\d+)\s*ms|(?:avg)[ \t]+(\d+)\s*ms)""",
            RegexOption.IGNORE_CASE,
        )
    private val decodeRegex =
        Regex(
            """(?:Average decoding time:\s*|Network/Decoding delay:\s*\d+\s*ms\s*/\s*|Dec[ \t]*:[ \t]*|解码[ \t]*[:：][ \t]*|Decode time:\s*)(\d+(?:\.\d+)?)\s*ms""",
            RegexOption.IGNORE_CASE,
        )
    private val lossRegex =
        Regex(
            """(?:Frames dropped by your network(?:\s+connection)?:\s*|Packet loss:\s*|Loss[ \t]*:[ \t]*|丢包率[ \t]*[:：][ \t]*)(\d+(?:\.\d+)?)%""",
            RegexOption.IGNORE_CASE,
        )
    private val bandwidthRegex =
        Regex(
            """(?:Bandwidth|带宽)[ \t]*[:：][ \t]*(\d+(?:\.\d+)?)\s*([MK])/s|(?:Bandwidth|带宽)[ \t]*[:：][ \t]*(\d+(?:\.\d+)?)\s*(Kbps|Mbps)""",
            RegexOption.IGNORE_CASE,
        )
    private val streamDetailsRegex =
        Regex("""Video stream:\s*(\d+x\d+)\s+[\d.]+\s*FPS(?:\s*\(([^)]+)\))?""", RegexOption.IGNORE_CASE)
    private val decoderRegex =
        Regex("""Decoder:\s*([^\r\n\t]+)""", RegexOption.IGNORE_CASE)
    private val hostLatencyRegex =
        Regex("""Host processing latency min/max/average:\s*([^\r\n\t]+)""", RegexOption.IGNORE_CASE)

    fun parse(
        text: String,
        existing: PerformanceStats = PerformanceStats(),
    ): PerformanceStats {
        var fps = existing.fps
        var rtt = existing.networkLatencyMs
        var decode = existing.decodeLatencyMs
        var loss = existing.packetLossPercentage
        var bandwidthKbps = existing.bandwidthKbps
        var videoFormat = existing.videoFormat
        var decoderName = existing.decoderName
        var hostLatency = existing.customDetails

        val renderedMatch = renderingFpsRegex.find(text)
        if (renderedMatch != null) {
            renderedMatch.groupValues[1].toFloatOrNull()?.let { fps = it }
        } else {
            fpsRegex.find(text)?.let { match ->
                for (i in 1..2) {
                    match.groups[i]?.value?.toFloatOrNull()?.let {
                        fps = it
                        return@let
                    }
                }
            }
        }

        rttRegex.find(text)?.let { match ->
            for (i in 1 until match.groupValues.size) {
                val value = match.groupValues[i]
                if (value.isNotEmpty()) {
                    value.toIntOrNull()?.let {
                        rtt = it
                        return@let
                    }
                }
            }
        }

        decodeRegex.find(text)?.let { match ->
            match.groupValues[1].toFloatOrNull()?.let { decode = it }
        }

        lossRegex.find(text)?.let { match ->
            match.groupValues[1].toFloatOrNull()?.let { loss = it }
        }

        bandwidthRegex.find(text)?.let { match ->
            val rawValueMs = match.groupValues[1].toFloatOrNull()
            val unitMs = match.groupValues[2].uppercase()
            val rawValueSi = match.groupValues.getOrElse(3) { "" }.toFloatOrNull()
            val unitSi = match.groupValues.getOrElse(4) { "" }.uppercase()
            when {
                rawValueMs != null -> {
                    // MediaCodecDecoderRenderer reports bytes/sec in M/s or K/s; convert to Kbps
                    bandwidthKbps = if (unitMs == "M") (rawValueMs * 8000f).roundToInt() else (rawValueMs * 8f).roundToInt()
                }
                rawValueSi != null -> {
                    bandwidthKbps = if (unitSi == "MBPS") (rawValueSi * 1000f).roundToInt() else rawValueSi.roundToInt()
                }
            }
        }

        streamDetailsRegex.find(text)?.let { match ->
            val resolution = match.groupValues[1]
            val explicitCodec = match.groupValues.getOrNull(2).orEmpty()
            videoFormat = if (explicitCodec.isNotEmpty()) {
                explicitCodec
            } else {
                resolution
            }
        }

        decoderRegex.find(text)?.let { match ->
            decoderName = match.groupValues[1].trim()
        }

        hostLatencyRegex.find(text)?.let { match ->
            hostLatency = match.groupValues[1].trim()
        }

        return existing.copy(
            fps = fps,
            networkLatencyMs = rtt,
            decodeLatencyMs = decode,
            packetLossPercentage = loss,
            bandwidthKbps = bandwidthKbps,
            videoFormat = videoFormat,
            decoderName = decoderName,
            customDetails = hostLatency,
        )
    }
}

/**
 * Draggable touch listener that bounds a floating view to the window
 * and distinguishes tap clicks from drag gestures with zero runtime object allocations.
 */
class DraggableTouchListener(
    private val decorBoundsProvider: DecorBoundsProvider,
    private val clickThresholdPx: Float = 5f,
    private val onSingleTap: Runnable,
) : View.OnTouchListener {
    private var startX = 0f
    private var startY = 0f
    private var deltaX = 0f
    private var deltaY = 0f
    private var isMoving = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(
        view: View,
        event: MotionEvent,
    ): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                deltaX = view.x - event.rawX
                deltaY = view.y - event.rawY
                isMoving = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                var newX = event.rawX + deltaX
                var newY = event.rawY + deltaY

                if (abs(event.rawX - startX) > clickThresholdPx ||
                    abs(event.rawY - startY) > clickThresholdPx
                ) {
                    isMoving = true
                }

                if (newX < 0f) newX = 0f
                if (newY < 0f) newY = 0f

                val maxOffsetX = (decorBoundsProvider.getDecorDimension(true) - view.width).toFloat()
                if (newX > maxOffsetX) {
                    newX = maxOffsetX
                }

                val maxOffsetY = (decorBoundsProvider.getDecorDimension(false) - view.height).toFloat()
                if (newY > maxOffsetY) {
                    newY = maxOffsetY
                }

                view.x = newX
                view.y = newY
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isMoving) {
                    onSingleTap.run()
                }
                isMoving = false
                return true
            }
            else -> return false
        }
    }
}

/**
 * Manages in-stream performance stats HUD, transient network condition alerts,
 * and draggable floating action buttons (game menu, pan/zoom mode toggle).
 * Also coordinates overlay visibility transitions during Picture-in-Picture (PiP).
 */
class StreamOverlayManager
    @JvmOverloads
    constructor(
        private val prefConfig: PreferenceConfiguration,
        private val decorBoundsProvider: DecorBoundsProvider,
        private val perfOverlayView: View?,
        private val perfOverlayLite: TextView?,
        private val perfOverlayBig: TextView?,
        private val notificationOverlayView: TextView?,
        private val floatingMenuButton: ImageButton?,
        private val overlayZoomButton: ImageButton?,
        private val onMenuRequested: Runnable,
        private val onZoomToggleRequested: Runnable,
        private val uiHandler: Handler = Handler(Looper.getMainLooper()),
        private val composeView: ComposeView? = null,
        val viewModel: StreamViewModel = StreamViewModel(),
        private val onDisconnectRequested: Runnable? = null,
        private val onQuitRequested: Runnable? = null,
        private val onSendKeys: ShortcutKeyConsumer? = null,
        private val onApplyMouseMode: MouseModeConsumer? = null,
        private val onToggleLocalCursor: Runnable? = null,
        private val onKeyboardRequested: Runnable? = null,
        private val onToggleVirtualController: Runnable? = null,
        private val onEditVirtualController: Runnable? = null,
        private val initialMouseModeId: Int = 0,
    ) {
        private var isHidingOverlays = false
        private var floatingButtonShown = false
        private var overlayZoomButtonShown = false
        var requestedNotificationOverlayVisibility: Int = View.GONE
            private set

        init {
            setupComposeOverlay()
            setupPerformanceOverlay()
            setupFloatingButtons()
        }

        fun hasComposeOverlay(): Boolean = composeView != null

        fun showQuitConfirmation() {
            viewModel.openQuitConfirm()
        }

        @Suppress("DEPRECATION")
        private fun setupComposeOverlay() {
            if (composeView == null) return

            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
            )
            composeView.setContent {
                SunbeamTheme {
                    StreamOverlayLayer(viewModel = viewModel)
                }
            }

            viewModel.onDisconnectAction = { onDisconnectRequested?.run() }
            viewModel.onQuitAction = { onQuitRequested?.run() }
            viewModel.onSendShortcutAction = { shortcut -> onSendKeys?.onSendKeys(shortcut.keyCodes) }
            viewModel.onMouseModeChangedAction = { mode ->
                onApplyMouseMode?.onMouseModeSelected(mode.modeId)
                if (prefConfig.rememberMouseMode) {
                    PreferenceManager.getDefaultSharedPreferences(composeView.context)
                        .edit()
                        .putString("mouse_mode_list", mode.modeId.toString())
                        .apply()
                }
            }
            viewModel.onLocalCursorToggledAction = {
                onToggleLocalCursor?.run()
            }
            viewModel.onZoomModeToggledAction = { onZoomToggleRequested.run() }
            viewModel.onKeyboardToggledAction = { onKeyboardRequested?.run() }
            viewModel.onVirtualControllerToggledAction = { enabled ->
                // VirtualController.show/hide touches Views and must run on the main thread.
                Handler(Looper.getMainLooper()).post {
                    onToggleVirtualController?.run()
                }
                prefConfig.onscreenController = enabled
            }
            viewModel.onEditVirtualControllerAction = {
                onEditVirtualController?.run()
            }
            viewModel.onPerfOverlayModeChangedAction = { mode ->
                prefConfig.enablePerfOverlay = mode != PerfOverlayMode.NONE
                // Request full decoder telemetry so both Lite and Expanded HUDs have complete stats.
                prefConfig.enablePerfOverlayLite = false
            }

            val initialPerfMode = if (prefConfig.enablePerfOverlay) {
                PerfOverlayMode.LITE
            } else {
                PerfOverlayMode.NONE
            }
            viewModel.setPerfOverlayMode(initialPerfMode)
            viewModel.setInitialMouseMode(StreamMouseMode.fromModeId(initialMouseModeId))
            viewModel.setVirtualControllerEnabled(prefConfig.onscreenController)
            viewModel.setLocalCursorEnabled(prefConfig.enableMouseLocalCursor)

            viewModel.setPerfOverlayPosition(
                if (prefConfig.enablePerfOverlayBottom) {
                    PerfOverlayPosition.BOTTOM_CENTER
                } else {
                    PerfOverlayPosition.TOP_CENTER
                },
            )
            viewModel.setFloatingButtonsVisibility(
                menuVisible = prefConfig.enableBackMenu && prefConfig.enableFloatingButton,
                zoomVisible = prefConfig.showOverlayZoomToggleButton,
            )
        }

        private fun setupPerformanceOverlay() {
            if (composeView != null) {
                perfOverlayView?.visibility = View.GONE
                return
            }

            if (prefConfig.enablePerfOverlay) {
                perfOverlayView?.visibility = View.VISIBLE
                if (prefConfig.enablePerfOverlayLite) {
                    perfOverlayLite?.visibility = View.VISIBLE
                    perfOverlayBig?.visibility = View.GONE
                    if (prefConfig.enablePerfOverlayLiteDialog) {
                        perfOverlayLite?.setOnClickListener { onMenuRequested.run() }
                    }
                } else {
                    perfOverlayBig?.visibility = View.VISIBLE
                    perfOverlayLite?.visibility = View.GONE
                }
                if (prefConfig.enablePerfOverlayBottom && perfOverlayView != null) {
                    val params = perfOverlayView.layoutParams as? FrameLayout.LayoutParams
                    if (params != null) {
                        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        perfOverlayView.layoutParams = params
                    }
                }
            } else {
                perfOverlayView?.visibility = View.GONE
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setupFloatingButtons() {
            if (composeView != null) {
                floatingMenuButton?.visibility = View.GONE
                overlayZoomButton?.visibility = View.GONE
                return
            }

            if (floatingMenuButton != null) {
                val shouldShow = prefConfig.enableBackMenu && prefConfig.enableFloatingButton
                floatingMenuButton.visibility = if (shouldShow) View.VISIBLE else View.GONE
                floatingMenuButton.setOnTouchListener(
                    DraggableTouchListener(decorBoundsProvider) {
                        onMenuRequested.run()
                    },
                )
            }

            refreshOverlayZoomButtonVisibility()
        }

        @SuppressLint("ClickableViewAccessibility")
        fun refreshOverlayZoomButtonVisibility() {
            if (composeView != null) {
                overlayZoomButton?.visibility = View.GONE
                viewModel.setFloatingButtonsVisibility(
                    menuVisible = prefConfig.enableBackMenu && prefConfig.enableFloatingButton,
                    zoomVisible = prefConfig.showOverlayZoomToggleButton,
                )
                return
            }

            if (overlayZoomButton != null) {
                if (prefConfig.showOverlayZoomToggleButton) {
                    overlayZoomButton.visibility = View.VISIBLE
                    updateZoomButtonAppearance(false)
                    overlayZoomButton.setOnTouchListener(
                        DraggableTouchListener(decorBoundsProvider) {
                            onZoomToggleRequested.run()
                        },
                    )
                } else {
                    overlayZoomButton.visibility = View.GONE
                }
            }
        }

        fun updateZoomButtonAppearance(isPanZoomMode: Boolean) {
            viewModel.setZoomMode(isPanZoomMode)

            if (overlayZoomButton != null) {
                overlayZoomButton.setBackgroundResource(
                    if (isPanZoomMode) {
                        R.drawable.floating_menu_button_active
                    } else {
                        R.drawable.floating_menu_button
                    },
                )
                overlayZoomButton.alpha = 1.0f
            }
        }

        fun updateVirtualControllerState(enabled: Boolean) {
            viewModel.setVirtualControllerEnabled(enabled)
        }

        fun updateLocalCursorState(enabled: Boolean) {
            viewModel.setLocalCursorEnabled(enabled)
        }

        fun updateFloatingButtonVisibility(show: Boolean) {
            floatingMenuButton?.visibility = if (show) View.VISIBLE else View.GONE
            viewModel.setFloatingButtonsVisibility(
                menuVisible = show,
                zoomVisible = prefConfig.showOverlayZoomToggleButton,
            )
        }

        fun toggleFloatingButtonVisibility() {
            val nextVisible = if (composeView != null) {
                !viewModel.uiState.value.isFloatingMenuButtonVisible
            } else {
                floatingMenuButton?.visibility == View.GONE
            }
            updateFloatingButtonVisibility(nextVisible)
        }

        fun toggleHUD(): Boolean {
            if (composeView != null) {
                viewModel.togglePerfHud()
                return prefConfig.enablePerfOverlay
            }

            prefConfig.enablePerfOverlay = !prefConfig.enablePerfOverlay
            viewModel.togglePerfHud()

            if (prefConfig.enablePerfOverlay) {
                perfOverlayView?.visibility = View.VISIBLE
                if (prefConfig.enablePerfOverlayLite) {
                    perfOverlayLite?.visibility = View.VISIBLE
                } else {
                    perfOverlayBig?.visibility = View.VISIBLE
                }
            } else {
                perfOverlayView?.visibility = View.GONE
            }
            return prefConfig.enablePerfOverlay
        }

        fun onPerfUpdate(text: String) {
            val stats = PerformanceStatsParser.parse(text, viewModel.uiState.value.perfStats)
            viewModel.updatePerformanceStats(stats)

            if (composeView == null) {
                uiHandler.post {
                    if (prefConfig.enablePerfOverlayLite) {
                        perfOverlayLite?.text = text
                    } else {
                        perfOverlayBig?.text = text
                    }
                }
            }
        }

        fun onConnectionStatusUpdate(connectionStatus: Int) {
            val context = notificationOverlayView?.context
            if (connectionStatus == MoonBridge.CONN_STATUS_POOR) {
                val resId =
                    if (prefConfig.bitrate > 5000) {
                        R.string.slow_connection_msg
                    } else {
                        R.string.poor_connection_msg
                    }
                val warningText = context?.getString(resId) ?: "Poor connection"
                if (context != null) {
                    notificationOverlayView?.setText(resId)
                }
                viewModel.setConnectionWarning(warningText)
                requestedNotificationOverlayVisibility = View.VISIBLE
            } else if (connectionStatus == MoonBridge.CONN_STATUS_OKAY) {
                viewModel.dismissConnectionWarning()
                requestedNotificationOverlayVisibility = View.GONE
            }

            if (!isHidingOverlays && composeView == null) {
                notificationOverlayView?.visibility = requestedNotificationOverlayVisibility
            }
        }

        fun onEnterPiP() {
            isHidingOverlays = true
            viewModel.onEnterPiP()

            floatingButtonShown = floatingMenuButton?.visibility == View.VISIBLE
            if (floatingButtonShown) {
                floatingMenuButton?.visibility = View.GONE
            }

            overlayZoomButtonShown = overlayZoomButton?.visibility == View.VISIBLE
            if (overlayZoomButtonShown) {
                overlayZoomButton?.visibility = View.GONE
            }

            perfOverlayView?.visibility = View.GONE
            notificationOverlayView?.visibility = View.GONE
        }

        fun onExitPiP() {
            isHidingOverlays = false
            viewModel.onExitPiP()
            prefConfig.enablePerfOverlay = viewModel.uiState.value.perfOverlayMode != PerfOverlayMode.NONE

            if (composeView == null) {
                if (floatingButtonShown) {
                    floatingMenuButton?.visibility = View.VISIBLE
                }

                if (overlayZoomButtonShown) {
                    overlayZoomButton?.visibility = View.VISIBLE
                }

                if (prefConfig.enablePerfOverlay) {
                    perfOverlayView?.visibility = View.VISIBLE
                }

                notificationOverlayView?.visibility = requestedNotificationOverlayVisibility
            }
        }

        fun showMenu() {
            viewModel.toggleMenu()
        }

        fun hideMenu() {
            viewModel.dismissDialog()
        }

        fun isMenuOpen(): Boolean =
            viewModel.uiState.value.activeDialog != StreamDialog.NONE
    }
