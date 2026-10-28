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
 * Parses performance log text from decoder/network into [PerformanceStats].
 */
object PerformanceStatsParser {
    private val fpsRegex =
        Regex("""(?:(\d+(?:\.\d+)?)\s*FPS|(?:FPS[:\s]+|FPS：|Incoming frame rate:\s*)(\d+(?:\.\d+)?))""")
    private val rttRegex =
        Regex("""(?:avg\s*(\d+)ms|RTT[:\s]+(\d+)ms|延迟[：:]\s*(\d+)ms)""")
    private val decodeRegex =
        Regex("""(?:Dec[:\s]+|解码[：:]\s*|Decode time:\s*)(\d+(?:\.\d+)?)\s*ms""")
    private val lossRegex =
        Regex("""(?:Loss[:\s]+|丢包率[：:]\s*|Frames dropped by your network:\s*)(\d+(?:\.\d+)?)%""")
    private val codecRegex =
        Regex("""Video stream:\s*\d+x\d+\s+[\d.]+\s*FPS\s*\((.*?)\)""")

    fun parse(
        text: String,
        existing: PerformanceStats = PerformanceStats(),
    ): PerformanceStats {
        var fps = existing.fps
        var rtt = existing.networkLatencyMs
        var decode = existing.decodeLatencyMs
        var loss = existing.packetLossPercentage
        var codec = existing.videoFormat

        fpsRegex.find(text)?.let { match ->
            for (i in 1..2) {
                match.groups[i]?.value?.toFloatOrNull()?.let {
                    fps = it
                    return@let
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
        codecRegex.find(text)?.let { match ->
            codec = match.groupValues[1]
        }

        return existing.copy(
            fps = fps,
            networkLatencyMs = rtt,
            decodeLatencyMs = decode,
            packetLossPercentage = loss,
            videoFormat = codec,
            customDetails = text,
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
                if (mode == StreamMouseMode.LOCAL_CURSOR_TOGGLE) {
                    onToggleLocalCursor?.run()
                } else {
                    val intMode = when (mode) {
                        StreamMouseMode.DISABLED_DIRECT -> 0
                        StreamMouseMode.TRACKPAD_NATURAL -> 2
                        StreamMouseMode.TRACKPAD_GAMING -> 3
                        StreamMouseMode.LOCAL_CURSOR_TOGGLE -> -1
                    }
                    onApplyMouseMode?.onMouseModeSelected(intMode)
                }
            }
            viewModel.onZoomModeToggledAction = { onZoomToggleRequested.run() }
            viewModel.onKeyboardToggledAction = { onKeyboardRequested?.run() }
            viewModel.onVirtualControllerToggledAction = { _ -> onToggleVirtualController?.run() }

            val initialPerfMode = if (prefConfig.enablePerfOverlay) {
                if (prefConfig.enablePerfOverlayLite) PerfOverlayMode.LITE else PerfOverlayMode.EXPANDED
            } else {
                PerfOverlayMode.NONE
            }
            viewModel.setPerfOverlayMode(initialPerfMode)
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
            if (viewModel.uiState.value.isPanZoomMode != isPanZoomMode) {
                viewModel.toggleZoomMode()
            }

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
            prefConfig.enablePerfOverlay = !prefConfig.enablePerfOverlay
            viewModel.togglePerfHud()

            if (composeView == null) {
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
