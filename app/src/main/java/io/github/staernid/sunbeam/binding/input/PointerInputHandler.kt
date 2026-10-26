@file:Suppress(
    "LongMethod",
    "CyclomaticComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "LongParameterList",
    "MagicNumber",
    "TooManyFunctions",
    "LargeClass",
    "ComplexCondition",
    "MaxLineLength",
)

package io.github.staernid.sunbeam.binding.input

import android.os.Build
import android.util.SparseArray
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import io.github.staernid.sunbeam.LimeLog
import io.github.staernid.sunbeam.SensitivityBean
import io.github.staernid.sunbeam.binding.input.capture.InputCaptureProvider
import io.github.staernid.sunbeam.binding.input.evdev.EvdevListener
import io.github.staernid.sunbeam.binding.input.touch.AbsoluteTouchContext
import io.github.staernid.sunbeam.binding.input.touch.RelativeTouchContext
import io.github.staernid.sunbeam.binding.input.touch.TouchContext
import io.github.staernid.sunbeam.binding.input.touch.TrackpadContext
import io.github.staernid.sunbeam.nvstream.NvConnection
import io.github.staernid.sunbeam.nvstream.input.MouseButtonPacket
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration

interface PointerPacketSender {
    fun sendMouseMove(
        deltaX: Short,
        deltaY: Short,
    )

    fun sendMouseMoveAsMousePosition(
        deltaX: Short,
        deltaY: Short,
        width: Short,
        height: Short,
    )

    fun sendMousePosition(
        x: Short,
        y: Short,
        width: Short,
        height: Short,
    )

    fun sendMouseButtonDown(button: Byte)

    fun sendMouseButtonUp(button: Byte)

    fun sendMouseScroll(amount: Byte)

    fun sendMouseHScroll(amount: Byte)

    fun sendMouseHighResScroll(amount: Short)

    fun sendMouseHighResHScroll(amount: Short)

    fun sendTouchEvent(
        eventType: Byte,
        pointerId: Int,
        x: Float,
        y: Float,
        pressure: Float,
        contactAreaMajor: Float,
        contactAreaMinor: Float,
        rotation: Short,
    ): Int

    fun sendPenEvent(
        eventType: Byte,
        toolType: Byte,
        penButtons: Byte,
        x: Float,
        y: Float,
        pressureOrDistance: Float,
        contactAreaMajor: Float,
        contactAreaMinor: Float,
        rotation: Short,
        tilt: Byte,
    ): Int
}

class NvPointerPacketSender(private val connection: NvConnection) : PointerPacketSender {
    override fun sendMouseMove(
        deltaX: Short,
        deltaY: Short,
    ) {
        connection.sendMouseMove(deltaX, deltaY)
    }

    override fun sendMouseMoveAsMousePosition(
        deltaX: Short,
        deltaY: Short,
        width: Short,
        height: Short,
    ) {
        connection.sendMouseMoveAsMousePosition(deltaX, deltaY, width, height)
    }

    override fun sendMousePosition(
        x: Short,
        y: Short,
        width: Short,
        height: Short,
    ) {
        connection.sendMousePosition(x, y, width, height)
    }

    override fun sendMouseButtonDown(button: Byte) {
        connection.sendMouseButtonDown(button)
    }

    override fun sendMouseButtonUp(button: Byte) {
        connection.sendMouseButtonUp(button)
    }

    override fun sendMouseScroll(amount: Byte) {
        connection.sendMouseScroll(amount)
    }

    override fun sendMouseHScroll(amount: Byte) {
        connection.sendMouseHScroll(amount)
    }

    override fun sendMouseHighResScroll(amount: Short) {
        connection.sendMouseHighResScroll(amount)
    }

    override fun sendMouseHighResHScroll(amount: Short) {
        connection.sendMouseHighResHScroll(amount)
    }

    override fun sendTouchEvent(
        eventType: Byte,
        pointerId: Int,
        x: Float,
        y: Float,
        pressure: Float,
        contactAreaMajor: Float,
        contactAreaMinor: Float,
        rotation: Short,
    ): Int {
        return connection.sendTouchEvent(eventType, pointerId, x, y, pressure, contactAreaMajor, contactAreaMinor, rotation)
    }

    override fun sendPenEvent(
        eventType: Byte,
        toolType: Byte,
        penButtons: Byte,
        x: Float,
        y: Float,
        pressureOrDistance: Float,
        contactAreaMajor: Float,
        contactAreaMinor: Float,
        rotation: Short,
        tilt: Byte,
    ): Int {
        return connection.sendPenEvent(
            eventType,
            toolType,
            penButtons,
            x,
            y,
            pressureOrDistance,
            contactAreaMajor,
            contactAreaMinor,
            rotation,
            tilt,
        )
    }
}

interface PointerGestureListener {
    fun onToggleKeyboard()

    fun onToggleFullKeyboard()

    fun onShowGameMenu()

    fun onVibrate(
        durationMs: Long,
        amplitude: Int,
    )
}

fun interface PanZoomHandlerDelegate {
    fun handlePanZoomTouchEvent(event: MotionEvent)
}

class PointerInputHandler(
    private val packetSender: PointerPacketSender,
    private val prefConfig: PreferenceConfiguration,
    private val inputCaptureProvider: InputCaptureProvider,
    private val streamView: View,
    private val gestureListener: PointerGestureListener,
    private val controllerHandler: ControllerHandler? = null,
    private val isInputGrabbed: () -> Boolean = { true },
    private val isVirtualControllerConfiguring: () -> Boolean = { false },
    private val isPanZoomMode: () -> Boolean = { false },
    private val panZoomDelegate: PanZoomHandlerDelegate? = null,
    private val screenWidthPixelsProvider: () -> Int = { streamView.resources.displayMetrics.widthPixels },
    private val connection: NvConnection? = null,
) {
    constructor(
        connection: NvConnection,
        prefConfig: PreferenceConfiguration,
        inputCaptureProvider: InputCaptureProvider,
        streamView: View,
        gestureListener: PointerGestureListener,
        controllerHandler: ControllerHandler? = null,
        isInputGrabbed: () -> Boolean = { true },
        isVirtualControllerConfiguring: () -> Boolean = { false },
        isPanZoomMode: () -> Boolean = { false },
        panZoomDelegate: PanZoomHandlerDelegate? = null,
        screenWidthPixelsProvider: () -> Int = { streamView.resources.displayMetrics.widthPixels },
    ) : this(
        packetSender = NvPointerPacketSender(connection),
        prefConfig = prefConfig,
        inputCaptureProvider = inputCaptureProvider,
        streamView = streamView,
        gestureListener = gestureListener,
        controllerHandler = controllerHandler,
        isInputGrabbed = isInputGrabbed,
        isVirtualControllerConfiguring = isVirtualControllerConfiguring,
        isPanZoomMode = isPanZoomMode,
        panZoomDelegate = panZoomDelegate,
        screenWidthPixelsProvider = screenWidthPixelsProvider,
        connection = connection,
    )

    val touchContextMap = arrayOfNulls<TouchContext>(2)
    val trackpadContextMap = arrayOfNulls<TouchContext>(2)

    private var lastButtonState = 0
    private var threeFingerDownTime = 0L
    private var fourFingerDownTime = 0L
    private var fiveFingerDownTime = 0L

    private var synthClickPending = false
    private var pointerSwiping = false
    private var synthTouchDownTime = 0L
    private var pendingDrag = false
    private var isDragging = false
    private var lastTouchDownX = 0f
    private var lastTouchDownY = 0f

    private var lastAbsTouchUpTime = 0L
    private var lastAbsTouchDownTime = 0L
    private var lastAbsTouchUpX = 0f
    private var lastAbsTouchUpY = 0f
    private var lastAbsTouchDownX = 0f
    private var lastAbsTouchDownY = 0f

    // Reusable buffers on latency-critical paths to achieve zero allocation per event
    private val tempCoords = FloatArray(2)
    private val tempSensitivityCoords = FloatArray(2)
    private val tempContactArea = FloatArray(2)
    private val tempCartesianMajor = FloatArray(2)
    private val tempCartesianMinor = FloatArray(2)
    private val sensitivityBeans = SparseArray<SensitivityBean>()

    init {
        initTrackpads()
    }

    fun initTrackpads() {
        val conn = connection ?: return
        for (i in trackpadContextMap.indices) {
            trackpadContextMap[i] =
                TrackpadContext(
                    conn,
                    i,
                    prefConfig.trackpadSwapAxis,
                    prefConfig.trackpadSensitivityX,
                    prefConfig.trackpadSensitivityY,
                )
        }
    }

    fun applyMouseMode(mode: Int) {
        when (mode) {
            0 -> {
                prefConfig.enableMultiTouchScreen = true
                prefConfig.touchscreenTrackpad = false
            }
            1, 5 -> {
                prefConfig.enableMultiTouchScreen = false
                prefConfig.touchscreenTrackpad = false
            }
            2, 3 -> {
                prefConfig.enableMultiTouchScreen = false
                prefConfig.touchscreenTrackpad = true
            }
            4 -> Unit
        }

        val conn = connection
        for (i in touchContextMap.indices) {
            touchContextMap[i]?.cancelTouch()
            touchContextMap[i] =
                if (conn == null || mode == 4) {
                    null
                } else if (!prefConfig.touchscreenTrackpad) {
                    AbsoluteTouchContext(conn, i, streamView, mode == 5)
                } else if (mode == 3) {
                    RelativeTouchContext(conn, i, REFERENCE_HORIZ_RES, REFERENCE_VERT_RES, streamView, prefConfig)
                } else {
                    TrackpadContext(conn, i)
                }
        }
    }

    fun cancelAllTouches() {
        for (ctx in touchContextMap) {
            ctx?.cancelTouch()
            ctx?.setPointerCount(0)
        }
        for (ctx in trackpadContextMap) {
            ctx?.cancelTouch()
            ctx?.setPointerCount(0)
        }
    }

    fun handleMouseMove(
        deltaX: Int,
        deltaY: Int,
    ) {
        packetSender.sendMouseMove(deltaX.toShort(), deltaY.toShort())
    }

    fun handleMouseButtonEvent(
        buttonId: Int,
        down: Boolean,
    ) {
        val buttonIndex: Byte =
            when (buttonId) {
                EvdevListener.BUTTON_LEFT -> MouseButtonPacket.BUTTON_LEFT
                EvdevListener.BUTTON_MIDDLE -> MouseButtonPacket.BUTTON_MIDDLE
                EvdevListener.BUTTON_RIGHT -> MouseButtonPacket.BUTTON_RIGHT
                EvdevListener.BUTTON_X1 -> MouseButtonPacket.BUTTON_X1
                EvdevListener.BUTTON_X2 -> MouseButtonPacket.BUTTON_X2
                else -> {
                    LimeLog.warning("Unhandled button: $buttonId")
                    return
                }
            }

        if (down) {
            packetSender.sendMouseButtonDown(buttonIndex)
        } else {
            packetSender.sendMouseButtonUp(buttonIndex)
        }
    }

    fun handleMouseVScroll(amount: Byte) {
        packetSender.sendMouseScroll(amount)
    }

    fun handleMouseHScroll(amount: Byte) {
        packetSender.sendMouseHScroll(amount)
    }

    fun handleMotionEvent(
        view: View?,
        event: MotionEvent,
    ): Boolean {
        if (!isInputGrabbed()) {
            return false
        }

        val deviceId = event.deviceId
        if (prefConfig.ignoreSynthEvents && deviceId <= 0) {
            return false
        }

        val eventSource = event.source
        val deviceSources = event.device?.sources ?: 0

        if ((eventSource and InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
            if (controllerHandler?.handleMotionEvent(event) == true) {
                return true
            }
        } else if ((deviceSources and InputDevice.SOURCE_CLASS_JOYSTICK) != 0 && controllerHandler?.tryHandleTouchpadEvent(event) == true) {
            return true
        } else if ((eventSource and InputDevice.SOURCE_CLASS_POINTER) != 0 ||
            (eventSource and InputDevice.SOURCE_CLASS_POSITION) != 0 ||
            eventSource == InputDevice.SOURCE_MOUSE_RELATIVE
        ) {
            val hasActionButton = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (event.actionButton != 0)
            if (eventSource == InputDevice.SOURCE_MOUSE ||
                ((eventSource and InputDevice.SOURCE_CLASS_POSITION) != 0 && hasActionButton) ||
                (
                    eventSource == InputDevice.SOURCE_MOUSE_RELATIVE ||
                        (
                            event.pointerCount >= 1 &&
                                (
                                    event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE ||
                                        event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS ||
                                        event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
                                )
                        )
                ) ||
                eventSource == SOURCE_DEX_MOUSE
            ) {
                return handleMouseAndStylusMotion(view, event, eventSource)
            } else {
                return handleFingerTouchMotion(view, event, eventSource)
            }
        }

        return false
    }

    private fun handleMouseAndStylusMotion(
        view: View?,
        event: MotionEvent,
        eventSource: Int,
    ): Boolean {
        var buttonState = event.buttonState
        var changedButtons = buttonState xor lastButtonState

        // Two finger click emulation
        if ((eventSource and InputDevice.SOURCE_CLASS_POSITION) != 0 &&
            event.pointerCount == 2 &&
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && event.actionButton == MotionEvent.BUTTON_PRIMARY)
        ) {
            if (event.actionMasked == MotionEvent.ACTION_BUTTON_PRESS) {
                buttonState = buttonState or MotionEvent.BUTTON_SECONDARY
            } else if (event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE) {
                buttonState = buttonState and MotionEvent.BUTTON_SECONDARY.inv()
            }
            buttonState = buttonState and MotionEvent.BUTTON_PRIMARY.inv()
            buttonState = buttonState or (lastButtonState and MotionEvent.BUTTON_PRIMARY)
            changedButtons = buttonState xor lastButtonState
        }

        if (!inputCaptureProvider.isCapturingActive) {
            return true
        }

        if (inputCaptureProvider.eventHasRelativeMouseAxes(event)) {
            val deltaX = inputCaptureProvider.getRelativeAxisX(event).toInt().toShort()
            val deltaY = inputCaptureProvider.getRelativeAxisY(event).toInt().toShort()

            if (deltaX.toInt() != 0 || deltaY.toInt() != 0) {
                if (prefConfig.absoluteMouseMode) {
                    packetSender.sendMouseMoveAsMousePosition(deltaX, deltaY, streamView.width.toShort(), streamView.height.toShort())
                } else {
                    packetSender.sendMouseMove(deltaX, deltaY)
                }
            }
        } else if ((eventSource and InputDevice.SOURCE_CLASS_POSITION) != 0) {
            val device = event.device
            if (device != null) {
                val xRange = device.getMotionRange(MotionEvent.AXIS_X, eventSource)
                val yRange = device.getMotionRange(MotionEvent.AXIS_Y, eventSource)

                if (xRange != null && yRange != null && xRange.min == 0f && yRange.min == 0f) {
                    val xMax = xRange.max.toInt()
                    val yMax = yRange.max.toInt()

                    if (xMax <= Short.MAX_VALUE && yMax <= Short.MAX_VALUE) {
                        packetSender.sendMousePosition(event.x.toInt().toShort(), event.y.toInt().toShort(), xMax.toShort(), yMax.toShort())
                    }
                }
            }
        } else if (view != null && trySendPenEvent(view, event)) {
            return true
        } else if (view != null) {
            if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER) {
                val handledFinger = handleFingerAsMouse(event, eventSource, view)
                if (handledFinger) return true
            } else {
                updateMousePosition(view, event)
            }
        }

        if (event.actionMasked == MotionEvent.ACTION_SCROLL) {
            packetSender.sendMouseHighResScroll((event.getAxisValue(MotionEvent.AXIS_VSCROLL) * 120).toInt().toShort())
            packetSender.sendMouseHighResHScroll((event.getAxisValue(MotionEvent.AXIS_HSCROLL) * 120).toInt().toShort())
        }

        handleButtonStateChanges(buttonState, changedButtons)
        handleStylusDownUp(event)

        lastButtonState = buttonState
        return true
    }

    private fun handleFingerAsMouse(
        event: MotionEvent,
        eventSource: Int,
        view: View,
    ): Boolean {
        val eventAction = event.actionMasked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && event.classification == MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE) {
            if (!pointerSwiping) {
                pointerSwiping = true
                handleTouchInput(event, trackpadContextMap, false, prefConfig.trackpadSwapAxis, MotionEvent.ACTION_POINTER_DOWN, 1, 2)
            }
            return handleTouchInput(event, trackpadContextMap, false, prefConfig.trackpadSwapAxis, MotionEvent.ACTION_MOVE, 1, 2)
        } else if (pointerSwiping && eventAction == MotionEvent.ACTION_UP) {
            pointerSwiping = false
            synthClickPending = false
            handleTouchInput(event, trackpadContextMap, false, prefConfig.trackpadSwapAxis, MotionEvent.ACTION_POINTER_UP, 1, 2)
            return true
        }

        val dx = (event.x - lastTouchDownX).toDouble()
        val dy = (event.y - lastTouchDownY).toDouble()
        val positionDelta = kotlin.math.sqrt(dx * dx + dy * dy)

        if (synthClickPending && event.eventTime - synthTouchDownTime >= prefConfig.trackpadDragDropThreshold) {
            if (positionDelta > 50) {
                pendingDrag = false
            } else if (pendingDrag) {
                pendingDrag = false
                isDragging = true
                if (prefConfig.trackpadDragDropVibration) {
                    gestureListener.onVibrate(20, 127)
                }
                packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT)
                return true
            }
        }

        return when (eventAction) {
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_MOVE,
            -> {
                updateMousePosition(view, event)
                true
            }
            MotionEvent.ACTION_HOVER_EXIT,
            MotionEvent.ACTION_DOWN,
            -> {
                pendingDrag = true
                synthClickPending = true
                lastTouchDownX = event.x
                lastTouchDownY = event.y
                synthTouchDownTime = event.eventTime
                true
            }
            MotionEvent.ACTION_HOVER_ENTER,
            MotionEvent.ACTION_UP,
            -> {
                if (synthClickPending) {
                    val timeDiff = event.eventTime - synthTouchDownTime
                    if (eventSource == SOURCE_DEX_MOUSE) {
                        if (timeDiff < 120) {
                            packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT)
                            packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT)
                        }
                    } else {
                        if (timeDiff < 20) {
                            packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT)
                            packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT)
                        } else if (timeDiff < 120) {
                            packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT)
                            packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT)
                        }
                    }
                    if (isDragging) {
                        isDragging = false
                        packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT)
                    }
                    pendingDrag = false
                    synthClickPending = false
                }
                true
            }
            MotionEvent.ACTION_BUTTON_PRESS,
            MotionEvent.ACTION_BUTTON_RELEASE,
            -> {
                synthClickPending = false
                false
            }
            else -> false
        }
    }

    private fun handleButtonStateChanges(
        buttonState: Int,
        changedButtons: Int,
    ) {
        if ((changedButtons and MotionEvent.BUTTON_PRIMARY) != 0) {
            if ((buttonState and MotionEvent.BUTTON_PRIMARY) != 0) {
                packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT)
            } else {
                packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT)
            }
        }

        val secondaryOrStylusPrimary = MotionEvent.BUTTON_SECONDARY or MotionEvent.BUTTON_STYLUS_PRIMARY
        if ((changedButtons and secondaryOrStylusPrimary) != 0) {
            if ((buttonState and secondaryOrStylusPrimary) != 0) {
                packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT)
            } else {
                packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT)
            }
        }

        val tertiaryOrStylusSecondary = MotionEvent.BUTTON_TERTIARY or MotionEvent.BUTTON_STYLUS_SECONDARY
        if ((changedButtons and tertiaryOrStylusSecondary) != 0) {
            if ((buttonState and tertiaryOrStylusSecondary) != 0) {
                packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_MIDDLE)
            } else {
                packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_MIDDLE)
            }
        }

        if (prefConfig.mouseNavButtons) {
            if ((changedButtons and MotionEvent.BUTTON_BACK) != 0) {
                if ((buttonState and MotionEvent.BUTTON_BACK) != 0) {
                    packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_X1)
                } else {
                    packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_X1)
                }
            }

            if ((changedButtons and MotionEvent.BUTTON_FORWARD) != 0) {
                if ((buttonState and MotionEvent.BUTTON_FORWARD) != 0) {
                    packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_X2)
                } else {
                    packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_X2)
                }
            }
        }
    }

    private fun handleStylusDownUp(event: MotionEvent) {
        if (event.pointerCount == 1 && event.actionIndex == 0) {
            val toolType = event.getToolType(0)
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                    lastAbsTouchDownTime = event.eventTime
                    lastAbsTouchDownX = event.getX(0)
                    lastAbsTouchDownY = event.getY(0)
                    packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT)
                } else if (toolType == MotionEvent.TOOL_TYPE_ERASER) {
                    lastAbsTouchDownTime = event.eventTime
                    lastAbsTouchDownX = event.getX(0)
                    lastAbsTouchDownY = event.getY(0)
                    packetSender.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT)
                }
            } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                    lastAbsTouchUpTime = event.eventTime
                    lastAbsTouchUpX = event.getX(0)
                    lastAbsTouchUpY = event.getY(0)
                    packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT)
                } else if (toolType == MotionEvent.TOOL_TYPE_ERASER) {
                    lastAbsTouchUpTime = event.eventTime
                    lastAbsTouchUpX = event.getX(0)
                    lastAbsTouchUpY = event.getY(0)
                    packetSender.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT)
                }
            }
        }
    }

    private fun handleFingerTouchMotion(
        view: View?,
        event: MotionEvent,
        eventSource: Int,
    ): Boolean {
        if (eventSource == InputDevice.SOURCE_TOUCHPAD) {
            return handleTouchInput(event, trackpadContextMap, false)
        }

        if (isVirtualControllerConfiguring()) {
            return true
        }

        if (isPanZoomMode()) {
            panZoomDelegate?.handlePanZoomTouchEvent(event)
            return true
        }

        if (touchContextMap[0] == null) {
            return true
        }

        val pointerCount = event.pointerCount
        if (prefConfig.enableMultiTouchGestures || !prefConfig.enableMultiTouchScreen) {
            if (pointerCount > 2) {
                val eventAction = event.actionMasked
                if ((
                        eventAction == MotionEvent.ACTION_POINTER_DOWN ||
                            eventAction == MotionEvent.ACTION_POINTER_UP ||
                            eventAction == MotionEvent.ACTION_UP
                    ) &&
                    handleMultiTouchGesture(event, eventAction, pointerCount, view)
                ) {
                    return true
                }
            }
        }

        if (prefConfig.enableMultiTouchScreen && !prefConfig.touchscreenTrackpad && view != null && trySendTouchEvent(view, event)) {
            return true
        }

        return handleTouchInput(event, touchContextMap, true)
    }

    private fun handleTouchInput(
        event: MotionEvent,
        inputContextMap: Array<TouchContext?>,
        isTouchScreen: Boolean,
        invertAxis: Boolean = false,
        eventAction: Int = event.actionMasked,
        actionIndex: Int = event.actionIndex,
        pointerCount: Int = event.pointerCount,
    ): Boolean {
        val context = inputContextMap.getOrNull(actionIndex) ?: return false

        val actualActionIndex = event.actionIndex
        val actualPointerCount = event.pointerCount
        val shouldDuplicateMovement = actualPointerCount < pointerCount

        if (eventAction == MotionEvent.ACTION_MOVE) {
            for (i in 0 until event.historySize) {
                for (aTouchContextMap in inputContextMap) {
                    if (aTouchContextMap != null && aTouchContextMap.actionIndex < pointerCount) {
                        val aActionIndex = if (shouldDuplicateMovement) 0 else aTouchContextMap.actionIndex
                        var historicalX = event.getHistoricalX(aActionIndex, i).toInt()
                        var historicalY = event.getHistoricalY(aActionIndex, i).toInt()
                        if (isTouchScreen) {
                            getNormalizedCoordinates(streamView, historicalX.toFloat(), historicalY.toFloat(), tempCoords)
                            historicalX = tempCoords[0].toInt()
                            historicalY = tempCoords[1].toInt()
                        }

                        if (invertAxis) {
                            aTouchContextMap.touchMoveEvent(historicalY, historicalX, event.getHistoricalEventTime(i))
                        } else {
                            aTouchContextMap.touchMoveEvent(historicalX, historicalY, event.getHistoricalEventTime(i))
                        }
                    }
                }
            }

            for (aTouchContextMap in inputContextMap) {
                if (aTouchContextMap != null && aTouchContextMap.actionIndex < pointerCount) {
                    val aActionIndex = if (shouldDuplicateMovement) 0 else aTouchContextMap.actionIndex
                    var currentX = event.getX(aActionIndex).toInt()
                    var currentY = event.getY(aActionIndex).toInt()
                    if (isTouchScreen) {
                        getNormalizedCoordinates(streamView, currentX.toFloat(), currentY.toFloat(), tempCoords)
                        currentX = tempCoords[0].toInt()
                        currentY = tempCoords[1].toInt()
                    }

                    if (invertAxis) {
                        aTouchContextMap.touchMoveEvent(currentY, currentX, event.eventTime)
                    } else {
                        aTouchContextMap.touchMoveEvent(currentX, currentY, event.eventTime)
                    }
                }
            }

            return true
        }

        var eventX = event.getX(actualActionIndex).toInt()
        var eventY = event.getY(actualActionIndex).toInt()

        if (isTouchScreen) {
            getNormalizedCoordinates(streamView, eventX.toFloat(), eventY.toFloat(), tempCoords)
            eventX = tempCoords[0].toInt()
            eventY = tempCoords[1].toInt()
        }

        when (eventAction) {
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_DOWN,
            -> {
                for (touchContext in inputContextMap) {
                    touchContext?.setPointerCount(pointerCount)
                }
                context.touchDownEvent(eventX, eventY, event.eventTime, true)
            }
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            -> {
                if (prefConfig.touchscreenTrackpad) {
                    if (pointerCount == 1 &&
                        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || (event.flags and MotionEvent.FLAG_CANCELED) == 0)
                    ) {
                        val currentEventTime = event.eventTime
                        if (currentEventTime - threeFingerDownTime < THREE_FINGER_TAP_THRESHOLD) {
                            gestureListener.onToggleKeyboard()
                            return true
                        } else if (currentEventTime - fourFingerDownTime < FOUR_FINGER_TAP_THRESHOLD) {
                            gestureListener.onToggleFullKeyboard()
                            return true
                        } else if (currentEventTime - fiveFingerDownTime < FIVE_FINGER_TAP_THRESHOLD) {
                            if (prefConfig.enableBackMenu) {
                                gestureListener.onShowGameMenu()
                            }
                            return true
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (event.flags and MotionEvent.FLAG_CANCELED) != 0) {
                    context.cancelTouch()
                } else {
                    context.touchUpEvent(eventX, eventY, event.eventTime)
                }

                for (touchContext in inputContextMap) {
                    touchContext?.setPointerCount(pointerCount - 1)
                }

                if (actionIndex == 0 && pointerCount > 1 && !context.isCancelled) {
                    var pointer1X = event.getX(1).toInt()
                    var pointer1Y = event.getY(1).toInt()
                    if (isTouchScreen) {
                        getNormalizedCoordinates(streamView, pointer1X.toFloat(), pointer1Y.toFloat(), tempCoords)
                        pointer1X = tempCoords[0].toInt()
                        pointer1Y = tempCoords[1].toInt()
                    }
                    context.touchDownEvent(pointer1X, pointer1Y, event.eventTime, false)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                for (aTouchContext in inputContextMap) {
                    aTouchContext?.cancelTouch()
                    aTouchContext?.setPointerCount(0)
                }
            }
            else -> return false
        }

        return true
    }

    private fun handleMultiTouchGesture(
        event: MotionEvent,
        eventAction: Int,
        pointerCount: Int,
        view: View?,
    ): Boolean {
        if (eventAction == MotionEvent.ACTION_POINTER_DOWN) {
            when (pointerCount) {
                3 -> threeFingerDownTime = event.eventTime
                4 -> {
                    threeFingerDownTime = 0
                    fourFingerDownTime = event.eventTime
                }
                5 -> {
                    threeFingerDownTime = 0
                    fourFingerDownTime = 0
                    fiveFingerDownTime = event.eventTime
                }
            }
        }

        when (eventAction) {
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            -> {
                val currentEventTime = event.eventTime
                val isFiveFingerTap =
                    pointerCount >= 5 && fiveFingerDownTime > 0 &&
                        currentEventTime - fiveFingerDownTime < FIVE_FINGER_TAP_THRESHOLD
                val isFourFingerTap =
                    pointerCount == 4 && fourFingerDownTime > 0 &&
                        currentEventTime - fourFingerDownTime < FOUR_FINGER_TAP_THRESHOLD
                val isThreeFingerTap =
                    pointerCount == 3 && threeFingerDownTime > 0 &&
                        currentEventTime - threeFingerDownTime < THREE_FINGER_TAP_THRESHOLD

                if (isFiveFingerTap) {
                    if (prefConfig.enableBackMenu) {
                        gestureListener.onShowGameMenu()
                    }
                    fiveFingerDownTime = 0
                } else if (isFourFingerTap) {
                    gestureListener.onToggleFullKeyboard()
                    fourFingerDownTime = 0
                } else if (isThreeFingerTap) {
                    gestureListener.onToggleKeyboard()
                    threeFingerDownTime = 0
                } else {
                    threeFingerDownTime = 0
                    fourFingerDownTime = 0
                    fiveFingerDownTime = 0
                    cancelStaleTouchState(event, view)
                    return false
                }
                threeFingerDownTime = 0
                fourFingerDownTime = 0
                fiveFingerDownTime = 0
            }
            else -> return false
        }

        cancelStaleTouchState(event, view)
        return true
    }

    private fun cancelStaleTouchState(
        event: MotionEvent,
        view: View?,
    ) {
        if (view != null) {
            val cancelEvent = MotionEvent.obtain(event)
            cancelEvent.action = MotionEvent.ACTION_CANCEL
            view.dispatchTouchEvent(cancelEvent)
            cancelEvent.recycle()
        }
        for (aTouchContext in touchContextMap) {
            aTouchContext?.cancelTouch()
            aTouchContext?.setPointerCount(0)
        }
    }

    private fun updateMousePosition(
        touchedView: View,
        event: MotionEvent,
    ) {
        var eventX: Float
        var eventY: Float

        if (touchedView == streamView) {
            eventX = event.getX(0)
            eventY = event.getY(0)
        } else {
            eventX = event.getX(0) - streamView.x
            eventY = event.getY(0) - streamView.y
        }

        if (event.pointerCount == 1 && event.actionIndex == 0 &&
            (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER || event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS)
        ) {
            val dxUp = (eventX - lastAbsTouchUpX).toDouble()
            val dyUp = (eventY - lastAbsTouchUpY).toDouble()
            val distUp = kotlin.math.sqrt(dxUp * dxUp + dyUp * dyUp)

            val dxDown = (eventX - lastAbsTouchDownX).toDouble()
            val dyDown = (eventY - lastAbsTouchDownY).toDouble()
            val distDown = kotlin.math.sqrt(dxDown * dxDown + dyDown * dyDown)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_HOVER_ENTER,
                MotionEvent.ACTION_HOVER_EXIT,
                MotionEvent.ACTION_HOVER_MOVE,
                -> {
                    if (event.eventTime - lastAbsTouchUpTime <= STYLUS_UP_DEAD_ZONE_DELAY && distUp <= STYLUS_UP_DEAD_ZONE_RADIUS) {
                        return
                    }
                }
                MotionEvent.ACTION_MOVE,
                MotionEvent.ACTION_UP,
                -> {
                    if (event.eventTime - lastAbsTouchDownTime <= STYLUS_DOWN_DEAD_ZONE_DELAY && distDown <= STYLUS_DOWN_DEAD_ZONE_RADIUS) {
                        return
                    }
                }
            }
        }

        val containerWidth = streamView.width
        val containerHeight = streamView.height
        eventX = eventX.coerceIn(0f, containerWidth.toFloat())
        eventY = eventY.coerceIn(0f, containerHeight.toFloat())

        packetSender.sendMousePosition(
            eventX.toInt().toShort(),
            eventY.toInt().toShort(),
            containerWidth.toShort(),
            containerHeight.toShort(),
        )
    }

    private fun trySendTouchEvent(
        view: View,
        event: MotionEvent,
    ): Boolean {
        val eventType = getLiTouchTypeFromEvent(event)
        if (eventType < 0) {
            return false
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    if (!sendTouchEventForPointer(view, event, eventType, i)) {
                        return false
                    }
                }
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                packetSender.sendTouchEvent(
                    MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                    0,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    MoonBridge.LI_ROT_UNKNOWN,
                ) != MoonBridge.LI_ERR_UNSUPPORTED
            }
            else -> sendTouchEventForPointer(view, event, eventType, event.actionIndex)
        }
    }

    private fun sendTouchEventForPointer(
        view: View,
        event: MotionEvent,
        eventType: Byte,
        pointerIndex: Int,
    ): Boolean {
        getStreamViewRelativeNormalizedXY(view, event, pointerIndex, tempCoords)
        getStreamViewNormalizedContactArea(event, pointerIndex, tempContactArea)
        return packetSender.sendTouchEvent(
            eventType,
            event.getPointerId(pointerIndex),
            tempCoords[0],
            tempCoords[1],
            getPressureOrDistance(event, pointerIndex),
            tempContactArea[0],
            tempContactArea[1],
            getRotationDegrees(event, pointerIndex),
        ) != MoonBridge.LI_ERR_UNSUPPORTED
    }

    private fun trySendPenEvent(
        view: View,
        event: MotionEvent,
    ): Boolean {
        val eventType = getLiTouchTypeFromEvent(event)
        if (eventType < 0) {
            return false
        }

        return when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                var handledStylusEvent = false
                for (i in 0 until event.pointerCount) {
                    val toolType = convertToolTypeToStylusToolType(event, i)
                    if (toolType != MoonBridge.LI_TOOL_TYPE_UNKNOWN) {
                        handledStylusEvent = true
                        if (!sendPenEventForPointer(view, event, eventType, toolType, i)) {
                            return false
                        }
                    }
                }
                handledStylusEvent
            }
            MotionEvent.ACTION_CANCEL -> {
                packetSender.sendPenEvent(
                    MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL,
                    MoonBridge.LI_TOOL_TYPE_UNKNOWN,
                    0.toByte(),
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    MoonBridge.LI_ROT_UNKNOWN,
                    MoonBridge.LI_TILT_UNKNOWN,
                ) != MoonBridge.LI_ERR_UNSUPPORTED
            }
            else -> {
                val toolType = convertToolTypeToStylusToolType(event, event.actionIndex)
                if (toolType == MoonBridge.LI_TOOL_TYPE_UNKNOWN) {
                    false
                } else {
                    sendPenEventForPointer(view, event, eventType, toolType, event.actionIndex)
                }
            }
        }
    }

    private fun sendPenEventForPointer(
        view: View,
        event: MotionEvent,
        eventType: Byte,
        toolType: Byte,
        pointerIndex: Int,
    ): Boolean {
        var penButtons: Byte = 0
        if ((event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0) {
            penButtons = (penButtons.toInt() or MoonBridge.LI_PEN_BUTTON_PRIMARY.toInt()).toByte()
        }
        if ((event.buttonState and MotionEvent.BUTTON_STYLUS_SECONDARY) != 0) {
            penButtons = (penButtons.toInt() or MoonBridge.LI_PEN_BUTTON_SECONDARY.toInt()).toByte()
        }

        var tiltDegrees = MoonBridge.LI_TILT_UNKNOWN
        val dev = event.device
        if (dev != null && dev.getMotionRange(MotionEvent.AXIS_TILT, event.source) != null) {
            tiltDegrees = Math.toDegrees(event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex).toDouble()).toInt().toByte()
        }

        getStreamViewRelativeNormalizedXY(view, event, pointerIndex, tempCoords)
        getStreamViewNormalizedContactArea(event, pointerIndex, tempContactArea)

        return packetSender.sendPenEvent(
            eventType,
            toolType,
            penButtons,
            tempCoords[0],
            tempCoords[1],
            getPressureOrDistance(event, pointerIndex),
            tempContactArea[0],
            tempContactArea[1],
            getRotationDegrees(event, pointerIndex),
            tiltDegrees,
        ) != MoonBridge.LI_ERR_UNSUPPORTED
    }

    private fun getStreamViewRelativeNormalizedXY(
        view: View?,
        event: MotionEvent,
        pointerIndex: Int,
        out: FloatArray,
    ) {
        var normalizedX = event.getX(pointerIndex)
        var normalizedY = event.getY(pointerIndex)

        if (prefConfig.enableTouchSensitivity && (prefConfig.touchSensitivityX != 100 || prefConfig.touchSensitivityY != 100)) {
            getStreamViewRelativeSensitivityXY(event, normalizedX, normalizedY, pointerIndex, tempSensitivityCoords)
            normalizedX = tempSensitivityCoords[0]
            normalizedY = tempSensitivityCoords[1]
        }

        if (view != null && view != streamView) {
            getNormalizedCoordinates(streamView, normalizedX, normalizedY, tempCoords)
            normalizedX = tempCoords[0]
            normalizedY = tempCoords[1]
        }

        val containerWidth = streamView.width.toFloat()
        val containerHeight = streamView.height.toFloat()

        normalizedX = normalizedX.coerceIn(0f, containerWidth)
        normalizedY = normalizedY.coerceIn(0f, containerHeight)

        if (containerWidth > 0f) {
            normalizedX /= containerWidth
        }
        if (containerHeight > 0f) {
            normalizedY /= containerHeight
        }

        out[0] = normalizedX
        out[1] = normalizedY
    }

    private fun getStreamViewRelativeSensitivityXY(
        event: MotionEvent,
        rawX: Float,
        rawY: Float,
        pointerIndex: Int,
        out: FloatArray,
    ) {
        var normalizedX = rawX
        var normalizedY = rawY
        val pointerId = event.getPointerId(pointerIndex)

        if (!prefConfig.touchSensitivityGlobal && normalizedX < screenWidthPixelsProvider() / 2f) {
            out[0] = normalizedX
            out[1] = normalizedY
            return
        }

        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            var bean = sensitivityBeans.get(pointerId)
            if (bean == null) {
                bean = SensitivityBean()
                sensitivityBeans.put(pointerId, bean)
            }
            if (bean.lastAbsoluteX != -1f) {
                var dx = normalizedX - bean.lastAbsoluteX
                var dy = normalizedY - bean.lastAbsoluteY
                dx *= 0.01f * prefConfig.touchSensitivityX
                dy *= 0.01f * prefConfig.touchSensitivityY
                normalizedX = bean.lastRelativelyX + dx
                normalizedY = bean.lastRelativelyY + dy
            }
            if (prefConfig.touchSensitivityRotationAuto) {
                if (normalizedX >= streamView.width) {
                    normalizedX = streamView.width / 2.0f
                }
                if (normalizedY >= streamView.height) {
                    normalizedY = streamView.height / 2.0f
                }
            }
            bean.lastAbsoluteX = event.getX(pointerIndex)
            bean.lastAbsoluteY = event.getY(pointerIndex)
            bean.lastRelativelyX = normalizedX
            bean.lastRelativelyY = normalizedY
        }

        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP) {
            sensitivityBeans.remove(pointerId)
        }

        out[0] = normalizedX
        out[1] = normalizedY
    }

    private fun getStreamViewNormalizedContactArea(
        event: MotionEvent,
        pointerIndex: Int,
        out: FloatArray,
    ) {
        val orientation =
            if (event.device == null || event.device.getMotionRange(MotionEvent.AXIS_ORIENTATION, event.source) == null) {
                (Math.PI / 4).toFloat()
            } else {
                event.getOrientation(pointerIndex)
            }

        val contactAreaMajor: Float
        val contactAreaMinor: Float
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER,
            MotionEvent.ACTION_HOVER_MOVE,
            MotionEvent.ACTION_HOVER_EXIT,
            -> {
                contactAreaMajor = event.getToolMajor(pointerIndex)
                contactAreaMinor = event.getToolMinor(pointerIndex)
            }
            else -> {
                contactAreaMajor = event.getTouchMajor(pointerIndex)
                contactAreaMinor = event.getTouchMinor(pointerIndex)
            }
        }

        polarToCartesian(contactAreaMajor, orientation, tempCartesianMajor)
        polarToCartesian(contactAreaMinor, (orientation + (Math.PI / 2)).toFloat(), tempCartesianMinor)

        val width = streamView.width.toFloat()
        val height = streamView.height.toFloat()

        if (width > 0f && height > 0f) {
            tempCartesianMajor[0] = Math.min(Math.abs(tempCartesianMajor[0]), width) / width
            tempCartesianMinor[0] = Math.min(Math.abs(tempCartesianMinor[0]), width) / width
            tempCartesianMajor[1] = Math.min(Math.abs(tempCartesianMajor[1]), height) / height
            tempCartesianMinor[1] = Math.min(Math.abs(tempCartesianMinor[1]), height) / height
        }

        out[0] = cartesianToR(tempCartesianMajor[0], tempCartesianMajor[1])
        out[1] = cartesianToR(tempCartesianMinor[0], tempCartesianMinor[1])
    }

    companion object {
        const val REFERENCE_HORIZ_RES = 1280
        const val REFERENCE_VERT_RES = 720

        const val STYLUS_DOWN_DEAD_ZONE_DELAY = 100
        const val STYLUS_DOWN_DEAD_ZONE_RADIUS = 20

        const val STYLUS_UP_DEAD_ZONE_DELAY = 150
        const val STYLUS_UP_DEAD_ZONE_RADIUS = 50

        const val THREE_FINGER_TAP_THRESHOLD = 300
        const val FOUR_FINGER_TAP_THRESHOLD = 300
        const val FIVE_FINGER_TAP_THRESHOLD = 300

        const val SOURCE_DEX_MOUSE = 12290

        fun getNormalizedCoordinates(
            streamView: View,
            rawX: Float,
            rawY: Float,
            out: FloatArray,
        ) {
            val scaleX = streamView.scaleX
            val scaleY = streamView.scaleY
            out[0] = if (scaleX != 0f) (rawX - streamView.x) / scaleX else rawX
            out[1] = if (scaleY != 0f) (rawY - streamView.y) / scaleY else rawY
        }

        fun normalizeValueInRange(
            value: Float,
            range: InputDevice.MotionRange,
        ): Float {
            return (value - range.min) / range.range
        }

        fun getPressureOrDistance(
            event: MotionEvent,
            pointerIndex: Int,
        ): Float {
            val dev = event.device
            return when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER,
                MotionEvent.ACTION_HOVER_MOVE,
                MotionEvent.ACTION_HOVER_EXIT,
                -> {
                    if (dev != null) {
                        val distanceRange = dev.getMotionRange(MotionEvent.AXIS_DISTANCE, event.source)
                        if (distanceRange != null) {
                            return normalizeValueInRange(event.getAxisValue(MotionEvent.AXIS_DISTANCE, pointerIndex), distanceRange)
                        }
                    }
                    0.0f
                }
                else -> event.getPressure(pointerIndex)
            }
        }

        fun getRotationDegrees(
            event: MotionEvent,
            pointerIndex: Int,
        ): Short {
            val dev = event.device
            if (dev != null && dev.getMotionRange(MotionEvent.AXIS_ORIENTATION, event.source) != null) {
                var rotationDegrees = Math.toDegrees(event.getOrientation(pointerIndex).toDouble()).toInt().toShort()
                if (rotationDegrees < 0) {
                    rotationDegrees = (rotationDegrees + 360).toShort()
                }
                return rotationDegrees
            }
            return MoonBridge.LI_ROT_UNKNOWN
        }

        fun polarToCartesian(
            r: Float,
            theta: Float,
            out: FloatArray,
        ) {
            out[0] = (r * kotlin.math.cos(theta.toDouble())).toFloat()
            out[1] = (r * kotlin.math.sin(theta.toDouble())).toFloat()
        }

        fun cartesianToR(
            x: Float,
            y: Float,
        ): Float {
            return kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
        }

        fun convertToolTypeToStylusToolType(
            event: MotionEvent,
            pointerIndex: Int,
        ): Byte {
            return when (event.getToolType(pointerIndex)) {
                MotionEvent.TOOL_TYPE_ERASER -> MoonBridge.LI_TOOL_TYPE_ERASER
                MotionEvent.TOOL_TYPE_STYLUS -> MoonBridge.LI_TOOL_TYPE_PEN
                else -> MoonBridge.LI_TOOL_TYPE_UNKNOWN
            }
        }

        fun getLiTouchTypeFromEvent(event: MotionEvent): Byte {
            return when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN,
                -> MoonBridge.LI_TOUCH_EVENT_DOWN
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP,
                -> {
                    if ((event.flags and MotionEvent.FLAG_CANCELED) != 0) {
                        MoonBridge.LI_TOUCH_EVENT_CANCEL
                    } else {
                        MoonBridge.LI_TOUCH_EVENT_UP
                    }
                }
                MotionEvent.ACTION_MOVE -> MoonBridge.LI_TOUCH_EVENT_MOVE
                MotionEvent.ACTION_CANCEL -> MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL
                MotionEvent.ACTION_HOVER_ENTER,
                MotionEvent.ACTION_HOVER_MOVE,
                -> MoonBridge.LI_TOUCH_EVENT_HOVER
                MotionEvent.ACTION_HOVER_EXIT -> MoonBridge.LI_TOUCH_EVENT_HOVER_LEAVE
                MotionEvent.ACTION_BUTTON_PRESS,
                MotionEvent.ACTION_BUTTON_RELEASE,
                -> MoonBridge.LI_TOUCH_EVENT_BUTTON_ONLY
                else -> -1
            }
        }
    }
}
