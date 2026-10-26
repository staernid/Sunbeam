@file:Suppress(
    "LongParameterList",
    "MagicNumber",
    "TooManyFunctions",
    "ReturnCount",
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
import io.github.staernid.sunbeam.R
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
    ) {
        private var isHidingOverlays = false
        private var floatingButtonShown = false
        private var overlayZoomButtonShown = false
        var requestedNotificationOverlayVisibility: Int = View.GONE
            private set

        init {
            setupPerformanceOverlay()
            setupFloatingButtons()
        }

        private fun setupPerformanceOverlay() {
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
        }

        fun toggleFloatingButtonVisibility() {
            if (floatingMenuButton != null) {
                updateFloatingButtonVisibility(floatingMenuButton.visibility == View.GONE)
            }
        }

        fun toggleHUD(): Boolean {
            prefConfig.enablePerfOverlay = !prefConfig.enablePerfOverlay
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
            uiHandler.post {
                if (prefConfig.enablePerfOverlayLite) {
                    perfOverlayLite?.text = text
                } else {
                    perfOverlayBig?.text = text
                }
            }
        }

        fun onConnectionStatusUpdate(connectionStatus: Int) {
            val context = notificationOverlayView?.context
            if (connectionStatus == MoonBridge.CONN_STATUS_POOR && context != null) {
                val resId =
                    if (prefConfig.bitrate > 5000) {
                        R.string.slow_connection_msg
                    } else {
                        R.string.poor_connection_msg
                    }
                notificationOverlayView.setText(resId)
                requestedNotificationOverlayVisibility = View.VISIBLE
            } else if (connectionStatus == MoonBridge.CONN_STATUS_OKAY) {
                requestedNotificationOverlayVisibility = View.GONE
            }

            if (!isHidingOverlays) {
                notificationOverlayView?.visibility = requestedNotificationOverlayVisibility
            }
        }

        fun onEnterPiP() {
            isHidingOverlays = true
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
