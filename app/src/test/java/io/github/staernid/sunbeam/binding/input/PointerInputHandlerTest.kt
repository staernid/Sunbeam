package io.github.staernid.sunbeam.binding.input

import android.content.Context
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import io.github.staernid.sunbeam.binding.input.capture.InputCaptureProvider
import io.github.staernid.sunbeam.binding.input.evdev.EvdevListener
import io.github.staernid.sunbeam.nvstream.input.MouseButtonPacket
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@Suppress("LongParameterList", "MaxLineLength", "MagicNumber")
class PointerInputHandlerTest {
    private class RecordingPointerPacketSender : PointerPacketSender {
        val mouseMoves = mutableListOf<Pair<Short, Short>>()
        val mouseMovesAsPositions = mutableListOf<List<Short>>()
        val mousePositions = mutableListOf<List<Short>>()
        val mouseButtonsDown = mutableListOf<Byte>()
        val mouseButtonsUp = mutableListOf<Byte>()
        val mouseScrolls = mutableListOf<Byte>()
        val mouseHScrolls = mutableListOf<Byte>()
        val mouseHighResScrolls = mutableListOf<Short>()
        val mouseHighResHScrolls = mutableListOf<Short>()
        val touchEvents = mutableListOf<Byte>()
        val penEvents = mutableListOf<Byte>()

        override fun sendMouseMove(
            deltaX: Short,
            deltaY: Short,
        ) {
            mouseMoves.add(deltaX to deltaY)
        }

        override fun sendMouseMoveAsMousePosition(
            deltaX: Short,
            deltaY: Short,
            width: Short,
            height: Short,
        ) {
            mouseMovesAsPositions.add(listOf(deltaX, deltaY, width, height))
        }

        override fun sendMousePosition(
            x: Short,
            y: Short,
            width: Short,
            height: Short,
        ) {
            mousePositions.add(listOf(x, y, width, height))
        }

        override fun sendMouseButtonDown(button: Byte) {
            mouseButtonsDown.add(button)
        }

        override fun sendMouseButtonUp(button: Byte) {
            mouseButtonsUp.add(button)
        }

        override fun sendMouseScroll(amount: Byte) {
            mouseScrolls.add(amount)
        }

        override fun sendMouseHScroll(amount: Byte) {
            mouseHScrolls.add(amount)
        }

        override fun sendMouseHighResScroll(amount: Short) {
            mouseHighResScrolls.add(amount)
        }

        override fun sendMouseHighResHScroll(amount: Short) {
            mouseHighResHScrolls.add(amount)
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
            touchEvents.add(eventType)
            return 0
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
            penEvents.add(eventType)
            return 0
        }
    }

    private class RecordingGestureListener : PointerGestureListener {
        var keyboardToggled = 0
        var fullKeyboardToggled = 0
        var gameMenuShown = 0
        var vibratedCount = 0

        override fun onToggleKeyboard() {
            keyboardToggled++
        }

        override fun onToggleFullKeyboard() {
            fullKeyboardToggled++
        }

        override fun onShowGameMenu() {
            gameMenuShown++
        }

        override fun onVibrate(
            durationMs: Long,
            amplitude: Int,
        ) {
            vibratedCount++
        }
    }

    private class TestInputCaptureProvider : InputCaptureProvider() {
        var hasRelativeAxes = false
        var relX = 0f
        var relY = 0f

        init {
            isCapturing = true
        }

        override fun eventHasRelativeMouseAxes(event: MotionEvent): Boolean = hasRelativeAxes

        override fun getRelativeAxisX(
            event: MotionEvent,
            pointerIndex: Int,
        ): Float = relX

        override fun getRelativeAxisY(
            event: MotionEvent,
            pointerIndex: Int,
        ): Float = relY
    }

    private lateinit var packetSender: RecordingPointerPacketSender
    private lateinit var gestureListener: RecordingGestureListener
    private lateinit var captureProvider: TestInputCaptureProvider
    private lateinit var prefConfig: PreferenceConfiguration
    private lateinit var streamView: View
    private lateinit var handler: PointerInputHandler

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        packetSender = RecordingPointerPacketSender()
        gestureListener = RecordingGestureListener()
        captureProvider = TestInputCaptureProvider()
        prefConfig = PreferenceConfiguration()
        streamView = View(context)
        streamView.layout(0, 0, 1920, 1080)

        handler =
            PointerInputHandler(
                packetSender = packetSender,
                prefConfig = prefConfig,
                inputCaptureProvider = captureProvider,
                streamView = streamView,
                gestureListener = gestureListener,
                isInputGrabbed = { true },
                screenWidthPixelsProvider = { 1920 },
            )
    }

    @Test
    fun handleMouseMoveAndScroll_forwardsToPacketSender() {
        handler.handleMouseMove(15, -20)
        assertEquals(1, packetSender.mouseMoves.size)
        assertEquals(15.toShort() to (-20).toShort(), packetSender.mouseMoves[0])

        handler.handleMouseVScroll(3.toByte())
        assertEquals(listOf(3.toByte()), packetSender.mouseScrolls)

        handler.handleMouseHScroll((-2).toByte())
        assertEquals(listOf((-2).toByte()), packetSender.mouseHScrolls)
    }

    @Test
    fun handleMouseButtonEvent_mapsEvdevButtonsProperly() {
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_LEFT, true)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_LEFT, false)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_RIGHT, true)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_RIGHT, false)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_MIDDLE, true)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_MIDDLE, false)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_X1, true)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_X1, false)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_X2, true)
        handler.handleMouseButtonEvent(EvdevListener.BUTTON_X2, false)

        assertEquals(
            listOf(
                MouseButtonPacket.BUTTON_LEFT,
                MouseButtonPacket.BUTTON_RIGHT,
                MouseButtonPacket.BUTTON_MIDDLE,
                MouseButtonPacket.BUTTON_X1,
                MouseButtonPacket.BUTTON_X2,
            ),
            packetSender.mouseButtonsDown,
        )
        assertEquals(
            listOf(
                MouseButtonPacket.BUTTON_LEFT,
                MouseButtonPacket.BUTTON_RIGHT,
                MouseButtonPacket.BUTTON_MIDDLE,
                MouseButtonPacket.BUTTON_X1,
                MouseButtonPacket.BUTTON_X2,
            ),
            packetSender.mouseButtonsUp,
        )
    }

    @Test
    fun handleMotionEvent_whenInputNotGrabbed_returnsFalse() {
        val ungrabbedHandler =
            PointerInputHandler(
                packetSender = packetSender,
                prefConfig = prefConfig,
                inputCaptureProvider = captureProvider,
                streamView = streamView,
                gestureListener = gestureListener,
                isInputGrabbed = { false },
            )

        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 100f, 100f, 0)
        event.source = InputDevice.SOURCE_MOUSE

        assertFalse(ungrabbedHandler.handleMotionEvent(streamView, event))
        assertEquals(0, packetSender.mouseMoves.size)
        event.recycle()
    }

    @Test
    fun handleMotionEvent_withRelativeMouseAxes_sendsMouseMoveOrPosition() {
        captureProvider.hasRelativeAxes = true
        captureProvider.relX = 10f
        captureProvider.relY = 25f

        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0f, 0f, 0)
        event.source = InputDevice.SOURCE_MOUSE

        // Normal relative mouse mode
        prefConfig.absoluteMouseMode = false
        assertTrue(handler.handleMotionEvent(streamView, event))
        assertEquals(1, packetSender.mouseMoves.size)
        assertEquals(10.toShort() to 25.toShort(), packetSender.mouseMoves[0])

        // Absolute mouse mode
        prefConfig.absoluteMouseMode = true
        assertTrue(handler.handleMotionEvent(streamView, event))
        assertEquals(1, packetSender.mouseMovesAsPositions.size)
        assertEquals(listOf(10.toShort(), 25.toShort(), 1920.toShort(), 1080.toShort()), packetSender.mouseMovesAsPositions[0])
        event.recycle()
    }

    @Test
    fun applyMouseMode_configuresMultiTouchAndTrackpadFlags() {
        handler.applyMouseMode(0) // Multi-touch
        assertTrue(prefConfig.enableMultiTouchScreen)
        assertFalse(prefConfig.touchscreenTrackpad)

        handler.applyMouseMode(1) // Normal mouse
        assertFalse(prefConfig.enableMultiTouchScreen)
        assertFalse(prefConfig.touchscreenTrackpad)

        handler.applyMouseMode(2) // Trackpad (natural)
        assertFalse(prefConfig.enableMultiTouchScreen)
        assertTrue(prefConfig.touchscreenTrackpad)

        handler.applyMouseMode(3) // Trackpad (gaming)
        assertFalse(prefConfig.enableMultiTouchScreen)
        assertTrue(prefConfig.touchscreenTrackpad)
    }

    @Test
    fun mathHelpers_polarAndCartesianCalculations() {
        val outCart = FloatArray(2)
        PointerInputHandler.polarToCartesian(10f, 0f, outCart)
        assertEquals(10f, outCart[0], 0.001f)
        assertEquals(0f, outCart[1], 0.001f)

        val r = PointerInputHandler.cartesianToR(3f, 4f)
        assertEquals(5f, r, 0.001f)

        val outNorm = FloatArray(2)
        streamView.x = 100f
        streamView.y = 50f
        streamView.scaleX = 2.0f
        streamView.scaleY = 2.0f
        PointerInputHandler.getNormalizedCoordinates(streamView, 300f, 250f, outNorm)
        assertEquals(100f, outNorm[0], 0.001f) // (300 - 100) / 2
        assertEquals(100f, outNorm[1], 0.001f) // (250 - 50) / 2
    }

    @Test
    fun touchAndStylusTypeHelpers() {
        val now = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        assertEquals(MoonBridge.LI_TOUCH_EVENT_DOWN, PointerInputHandler.getLiTouchTypeFromEvent(downEvent))

        val moveEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, 0f, 0f, 0)
        assertEquals(MoonBridge.LI_TOUCH_EVENT_MOVE, PointerInputHandler.getLiTouchTypeFromEvent(moveEvent))

        val cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        assertEquals(MoonBridge.LI_TOUCH_EVENT_CANCEL_ALL, PointerInputHandler.getLiTouchTypeFromEvent(cancelEvent))

        downEvent.recycle()
        moveEvent.recycle()
        cancelEvent.recycle()
    }
}
