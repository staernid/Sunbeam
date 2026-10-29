@file:Suppress("MagicNumber", "LongParameterList", "TooManyFunctions")

package io.github.staernid.sunbeam.binding.input.virtual_controller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import io.github.staernid.sunbeam.R
import kotlin.math.min

/**
 * Zero-allocation Canvas renderer for in-stream [VirtualController] elements.
 * Matches the modern translucent dark-glass aesthetic of the Compose On-Screen Controls preview.
 */
class VirtualControllerRenderer {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val boundsRect = RectF()
    private val innerRect = RectF()
    private val dpadPath = Path()
    private val arrowPath = Path()

    fun drawDigitalButton(
        canvas: Canvas,
        width: Int,
        height: Int,
        elementId: Int,
        text: String,
        isPressed: Boolean,
        opacityPercent: Int,
        controllerMode: VirtualController.ControllerMode,
        isEnabled: Boolean,
    ) {
        if (width <= 0 || height <= 0) return

        val effectiveOpacity = getEffectiveOpacity(opacityPercent, controllerMode, isEnabled)
        val alphaScale = (effectiveOpacity.coerceIn(10, 100) / 100f)
        val strokeWidth = (min(width, height) * 0.045f).coerceIn(2f, 6f)
        val inset = strokeWidth * 1.2f

        boundsRect.set(inset, inset, width - inset, height - inset)

        val isRound = isRoundElement(elementId, width, height)
        val cornerRadius = if (isRound) {
            min(boundsRect.width(), boundsRect.height()) / 2f
        } else {
            boundsRect.height() * 0.38f
        }

        // Fill
        fillPaint.color = if (isPressed) {
             applyAlpha(COLOR_PRESSED_FILL, (alphaScale * 235).toInt())
        } else {
            applyAlpha(COLOR_SURFACE_FILL, (alphaScale * 185).toInt())
        }
        if (isRound) {
            canvas.drawOval(boundsRect, fillPaint)
        } else {
            canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, fillPaint)
        }

        // Border
        strokePaint.strokeWidth = strokeWidth
        strokePaint.color = if (isPressed) {
            applyAlpha(COLOR_PRESSED_BORDER, (alphaScale * 255).toInt())
        } else {
            val accentColor = getButtonBorderColor(elementId)
            applyAlpha(accentColor, (alphaScale * 215).toInt())
        }
        if (isRound) {
            canvas.drawOval(boundsRect, strokePaint)
        } else {
            canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, strokePaint)
        }

        // Label
        if (text.isNotEmpty()) {
            val labelText = formatButtonLabel(elementId, text)
            val maxDim = min(width, height).toFloat()
            textPaint.textSize = when {
                labelText.length == 1 -> maxDim * 0.40f
                labelText.length <= 3 -> maxDim * 0.32f
                else -> maxDim * 0.25f
            }
            textPaint.color = if (isPressed) {
                applyAlpha(Color.WHITE, (alphaScale * 255).toInt())
            } else {
                applyAlpha(COLOR_TEXT_PRIMARY, (alphaScale * 240).toInt())
            }
            val textY = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(labelText, width / 2f, textY, textPaint)
        }

        drawEditOverlay(canvas, width, height, strokeWidth, controllerMode, isEnabled)
    }

    fun drawDigitalPad(
        canvas: Canvas,
        width: Int,
        height: Int,
        direction: Int,
        opacityPercent: Int,
        controllerMode: VirtualController.ControllerMode,
        isEnabled: Boolean,
    ) {
        if (width <= 0 || height <= 0) return

        val effectiveOpacity = getEffectiveOpacity(opacityPercent, controllerMode, isEnabled)
        val alphaScale = (effectiveOpacity.coerceIn(10, 100) / 100f)
        val size = min(width, height).toFloat()
        val strokeWidth = (size * 0.022f).coerceIn(2f, 5f)
        val padInset = size * 0.04f

        val armStart = size * 0.33f
        val armEnd = size * 0.67f
        val left = padInset
        val top = padInset
        val right = width - padInset
        val bottom = height - padInset
        val armCorner = size * 0.08f

        // Outer subtle circular backing
        fillPaint.color = applyAlpha(COLOR_STICK_WELL_FILL, (alphaScale * 110).toInt())
        canvas.drawCircle(width / 2f, height / 2f, (size / 2f) - padInset, fillPaint)

        // Vertical & horizontal cross bars
        fillPaint.color = applyAlpha(COLOR_SURFACE_FILL, (alphaScale * 195).toInt())
        strokePaint.strokeWidth = strokeWidth
        strokePaint.color = applyAlpha(COLOR_DEFAULT_BORDER, (alphaScale * 185).toInt())

        // Vertical bar
        innerRect.set(armStart, top, armEnd, bottom)
        canvas.drawRoundRect(innerRect, armCorner, armCorner, fillPaint)
        canvas.drawRoundRect(innerRect, armCorner, armCorner, strokePaint)

        // Horizontal bar
        innerRect.set(left, armStart, right, armEnd)
        canvas.drawRoundRect(innerRect, armCorner, armCorner, fillPaint)
        canvas.drawRoundRect(innerRect, armCorner, armCorner, strokePaint)

        // Re-fill center intersection so cross looks seamless
        innerRect.set(
            armStart + strokeWidth / 2f,
            armStart + strokeWidth / 2f,
            armEnd - strokeWidth / 2f,
            armEnd - strokeWidth / 2f,
        )
        canvas.drawRect(innerRect, fillPaint)

        // Active directional highlights
        val upPressed = (direction and DigitalPad.DIGITAL_PAD_DIRECTION_UP) != 0
        val downPressed = (direction and DigitalPad.DIGITAL_PAD_DIRECTION_DOWN) != 0
        val leftPressed = (direction and DigitalPad.DIGITAL_PAD_DIRECTION_LEFT) != 0
        val rightPressed = (direction and DigitalPad.DIGITAL_PAD_DIRECTION_RIGHT) != 0

        if (upPressed) {
            fillPaint.color = applyAlpha(COLOR_PRESSED_FILL, (alphaScale * 230).toInt())
            innerRect.set(armStart + strokeWidth, top + strokeWidth, armEnd - strokeWidth, armStart)
            canvas.drawRoundRect(innerRect, armCorner * 0.7f, armCorner * 0.7f, fillPaint)
        }
        if (downPressed) {
            fillPaint.color = applyAlpha(COLOR_PRESSED_FILL, (alphaScale * 230).toInt())
            innerRect.set(armStart + strokeWidth, armEnd, armEnd - strokeWidth, bottom - strokeWidth)
            canvas.drawRoundRect(innerRect, armCorner * 0.7f, armCorner * 0.7f, fillPaint)
        }
        if (leftPressed) {
            fillPaint.color = applyAlpha(COLOR_PRESSED_FILL, (alphaScale * 230).toInt())
            innerRect.set(left + strokeWidth, armStart + strokeWidth, armStart, armEnd - strokeWidth)
            canvas.drawRoundRect(innerRect, armCorner * 0.7f, armCorner * 0.7f, fillPaint)
        }
        if (rightPressed) {
            fillPaint.color = applyAlpha(COLOR_PRESSED_FILL, (alphaScale * 230).toInt())
            innerRect.set(armEnd, armStart + strokeWidth, right - strokeWidth, armEnd - strokeWidth)
            canvas.drawRoundRect(innerRect, armCorner * 0.7f, armCorner * 0.7f, fillPaint)
        }

        // Directional chevrons
        val arrowSize = size * 0.075f
        val cx = width / 2f
        val cy = height / 2f
        drawChevron(canvas, cx, top + size * 0.14f, arrowSize, Direction.UP, upPressed, alphaScale)
        drawChevron(canvas, cx, bottom - size * 0.14f, arrowSize, Direction.DOWN, downPressed, alphaScale)
        drawChevron(canvas, left + size * 0.14f, cy, arrowSize, Direction.LEFT, leftPressed, alphaScale)
        drawChevron(canvas, right - size * 0.14f, cy, arrowSize, Direction.RIGHT, rightPressed, alphaScale)

        // Center pivot dot
        fillPaint.color = applyAlpha(COLOR_DEFAULT_BORDER, (alphaScale * 90).toInt())
        canvas.drawCircle(cx, cy, size * 0.04f, fillPaint)

        drawEditOverlay(canvas, width, height, strokeWidth, controllerMode, isEnabled)
    }

    fun drawAnalogStick(
        canvas: Canvas,
        width: Int,
        height: Int,
        baseCenterX: Float,
        baseCenterY: Float,
        stickX: Float,
        stickY: Float,
        outerRadius: Float,
        deadzoneRadius: Float,
        capRadius: Float,
        isPressed: Boolean,
        opacityPercent: Int,
        controllerMode: VirtualController.ControllerMode,
        isEnabled: Boolean,
    ) {
        if (width <= 0 || height <= 0 || outerRadius <= 0f) return

        val effectiveOpacity = getEffectiveOpacity(opacityPercent, controllerMode, isEnabled)
        val alphaScale = (effectiveOpacity.coerceIn(10, 100) / 100f)
        val strokeWidth = (min(width, height) * 0.022f).coerceIn(2f, 5f)

        // Outer stick well
        fillPaint.color = applyAlpha(COLOR_STICK_WELL_FILL, (alphaScale * 145).toInt())
        canvas.drawCircle(baseCenterX, baseCenterY, outerRadius, fillPaint)

        strokePaint.strokeWidth = strokeWidth
        strokePaint.color = if (isPressed) {
            applyAlpha(COLOR_PRESSED_BORDER, (alphaScale * 210).toInt())
        } else {
            applyAlpha(COLOR_DEFAULT_BORDER, (alphaScale * 150).toInt())
        }
        canvas.drawCircle(baseCenterX, baseCenterY, outerRadius, strokePaint)

        // Subtle inner deadzone guide ring
        strokePaint.strokeWidth = (strokeWidth * 0.65f).coerceAtLeast(1.5f)
        strokePaint.color = applyAlpha(COLOR_DEFAULT_BORDER, (alphaScale * 65).toInt())
        canvas.drawCircle(baseCenterX, baseCenterY, deadzoneRadius, strokePaint)

        // Thumb cap
        val thumbRadius = capRadius.coerceAtLeast(outerRadius * 0.36f)
        val thumbX = if (isPressed) stickX else baseCenterX
        val thumbY = if (isPressed) stickY else baseCenterY

        fillPaint.color = if (isPressed) {
            applyAlpha(COLOR_PRESSED_FILL, (alphaScale * 235).toInt())
        } else {
            applyAlpha(COLOR_SURFACE_FILL, (alphaScale * 215).toInt())
        }
        canvas.drawCircle(thumbX, thumbY, thumbRadius, fillPaint)

        strokePaint.strokeWidth = strokeWidth
        strokePaint.color = if (isPressed) {
            applyAlpha(COLOR_PRESSED_BORDER, (alphaScale * 255).toInt())
        } else {
            applyAlpha(COLOR_DEFAULT_BORDER, (alphaScale * 210).toInt())
        }
        canvas.drawCircle(thumbX, thumbY, thumbRadius, strokePaint)

        // Inner thumb grip ring
        strokePaint.strokeWidth = (strokeWidth * 0.6f).coerceAtLeast(1.2f)
        strokePaint.color = applyAlpha(Color.WHITE, (alphaScale * 85).toInt())
        canvas.drawCircle(thumbX, thumbY, thumbRadius * 0.58f, strokePaint)

        drawEditOverlay(canvas, width, height, strokeWidth, controllerMode, isEnabled)
    }

    private fun drawEditOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        strokeWidth: Float,
        controllerMode: VirtualController.ControllerMode,
        isEnabled: Boolean,
    ) {
        if (controllerMode == VirtualController.ControllerMode.Active) return

        val editColor = when (controllerMode) {
            VirtualController.ControllerMode.MoveButtons -> COLOR_EDIT_MOVE
            VirtualController.ControllerMode.ResizeButtons -> COLOR_EDIT_Resize
            VirtualController.ControllerMode.DisableEnableButtons -> {
                if (isEnabled) COLOR_EDIT_ENABLED else COLOR_EDIT_DISABLED
            }
            else -> return
        }

        val inset = strokeWidth * 1.5f
        boundsRect.set(inset, inset, width - inset, height - inset)
        val corner = min(width, height) * 0.16f

        fillPaint.color = applyAlpha(editColor, 36)
        canvas.drawRoundRect(boundsRect, corner, corner, fillPaint)

        strokePaint.strokeWidth = (strokeWidth * 1.25f).coerceAtLeast(3f)
        strokePaint.color = applyAlpha(editColor, 225)
        canvas.drawRoundRect(boundsRect, corner, corner, strokePaint)

        if (controllerMode == VirtualController.ControllerMode.ResizeButtons) {
            val handleSize = (min(width, height) * 0.16f).coerceIn(8f, 20f)
            fillPaint.color = applyAlpha(editColor, 240)
            innerRect.set(
                width - inset - handleSize,
                height - inset - handleSize,
                width - inset,
                height - inset,
            )
            canvas.drawRoundRect(innerRect, 4f, 4f, fillPaint)
        } else if (controllerMode == VirtualController.ControllerMode.DisableEnableButtons && !isEnabled) {
            strokePaint.color = applyAlpha(COLOR_EDIT_DISABLED, 210)
            canvas.drawLine(
                boundsRect.left + corner * 0.5f,
                boundsRect.top + corner * 0.5f,
                boundsRect.right - corner * 0.5f,
                boundsRect.bottom - corner * 0.5f,
                strokePaint,
            )
        }
    }

    private enum class Direction { UP, DOWN, LEFT, RIGHT }

    private fun drawChevron(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        dir: Direction,
        isPressed: Boolean,
        alphaScale: Float,
    ) {
        arrowPath.reset()
        when (dir) {
            Direction.UP -> {
                arrowPath.moveTo(cx, cy - size)
                arrowPath.lineTo(cx - size, cy + size * 0.65f)
                arrowPath.lineTo(cx + size, cy + size * 0.65f)
            }
            Direction.DOWN -> {
                arrowPath.moveTo(cx, cy + size)
                arrowPath.lineTo(cx - size, cy - size * 0.65f)
                arrowPath.lineTo(cx + size, cy - size * 0.65f)
            }
            Direction.LEFT -> {
                arrowPath.moveTo(cx - size, cy)
                arrowPath.lineTo(cx + size * 0.65f, cy - size)
                arrowPath.lineTo(cx + size * 0.65f, cy + size)
            }
            Direction.RIGHT -> {
                arrowPath.moveTo(cx + size, cy)
                arrowPath.lineTo(cx - size * 0.65f, cy - size)
                arrowPath.lineTo(cx - size * 0.65f, cy + size)
            }
        }
        arrowPath.close()
        fillPaint.color = if (isPressed) {
            applyAlpha(Color.WHITE, (alphaScale * 255).toInt())
        } else {
            applyAlpha(COLOR_TEXT_PRIMARY, (alphaScale * 210).toInt())
        }
        canvas.drawPath(arrowPath, fillPaint)
    }

    private fun isRoundElement(elementId: Int, width: Int, height: Int): Boolean {
        return when (elementId) {
            VirtualControllerElement.EID_A,
            VirtualControllerElement.EID_B,
            VirtualControllerElement.EID_X,
            VirtualControllerElement.EID_Y,
            VirtualControllerElement.EID_LSB,
            VirtualControllerElement.EID_RSB,
            VirtualControllerElement.EID_GDB,
            -> true
            else -> width == height
        }
    }

    private fun getButtonBorderColor(elementId: Int): Int {
        return when (elementId) {
            VirtualControllerElement.EID_A -> 0xFF6EE7B7.toInt()
            VirtualControllerElement.EID_B -> 0xFFFCA5A5.toInt()
            VirtualControllerElement.EID_X -> 0xFF93C5FD.toInt()
            VirtualControllerElement.EID_Y -> 0xFFFDE68A.toInt()
            else -> COLOR_DEFAULT_BORDER
        }
    }

    private fun formatButtonLabel(elementId: Int, rawText: String): String {
        return when (elementId) {
            VirtualControllerElement.EID_GDB -> "HOME"
            VirtualControllerElement.EID_BACK -> "SELECT"
            else -> rawText
        }
    }

    private fun getEffectiveOpacity(
        opacityPercent: Int,
        controllerMode: VirtualController.ControllerMode,
        isEnabled: Boolean,
    ): Int {
        if (controllerMode == VirtualController.ControllerMode.DisableEnableButtons && !isEnabled) {
            return 35
        }
        if (controllerMode != VirtualController.ControllerMode.Active) {
            return opacityPercent.coerceAtLeast(75)
        }
        return opacityPercent
    }

    private fun applyAlpha(color: Int, alpha: Int): Int {
        val clamped = alpha.coerceIn(0, 255)
        return (clamped shl 24) or (color and 0x00FFFFFF)
    }

    companion object {
        private const val COLOR_SURFACE_FILL = 0xFF1A2230.toInt()
        private const val COLOR_STICK_WELL_FILL = 0xFF111722.toInt()
        private const val COLOR_DEFAULT_BORDER = 0xFFD1D9E6.toInt()
        private const val COLOR_PRESSED_FILL = 0xFF2563EB.toInt()
        private const val COLOR_PRESSED_BORDER = 0xFF93C5FD.toInt()
        private const val COLOR_TEXT_PRIMARY = 0xFFF1F5F9.toInt()

        private const val COLOR_EDIT_MOVE = 0xFFF59E0B.toInt()
        private const val COLOR_EDIT_Resize = 0xFF06B6D4.toInt()
        private const val COLOR_EDIT_ENABLED = 0xFF10B981.toInt()
        private const val COLOR_EDIT_DISABLED = 0xFF64748B.toInt()

        @JvmStatic
        fun styleConfigureButton(
            button: Button,
            context: Context,
            mode: VirtualController.ControllerMode,
        ) {
            val density = context.resources.displayMetrics.density
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 50f * density
                val strokeColor = when (mode) {
                    VirtualController.ControllerMode.Active -> 0x44FFFFFF
                    VirtualController.ControllerMode.DisableEnableButtons -> COLOR_EDIT_ENABLED
                    VirtualController.ControllerMode.MoveButtons -> COLOR_EDIT_MOVE
                    VirtualController.ControllerMode.ResizeButtons -> COLOR_EDIT_Resize
                }
                setColor(if (mode == VirtualController.ControllerMode.Active) 0x770B0F17 else 0xD90B0F17.toInt())
                setStroke((1.5f * density).toInt().coerceAtLeast(1), strokeColor)
            }
            button.background = bg
            button.isAllCaps = false
            button.setTextColor(0xFFF3F4F6.toInt())
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            button.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            val hPad = (12 * density).toInt()
            val vPad = (4 * density).toInt()
            button.setPadding(hPad, vPad, hPad, vPad)

            when (mode) {
                VirtualController.ControllerMode.Active -> {
                    button.text = "Layout"
                    button.alpha = 0.55f
                }
                VirtualController.ControllerMode.DisableEnableButtons -> {
                    button.text = context.getString(R.string.configuration_mode_disable_enable_buttons) + " • Next"
                    button.alpha = 0.95f
                }
                VirtualController.ControllerMode.MoveButtons -> {
                    button.text = context.getString(R.string.configuration_mode_move_buttons) + " • Next"
                    button.alpha = 0.95f
                }
                VirtualController.ControllerMode.ResizeButtons -> {
                    button.text = context.getString(R.string.configuration_mode_resize_buttons) + " • Save"
                    button.alpha = 0.95f
                }
            }
        }

        @JvmStatic
        fun createConfigureButtonLayoutParams(context: Context): FrameLayout.LayoutParams {
            val density = context.resources.displayMetrics.density
            val heightPx = (32 * density).toInt()
            return FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                heightPx,
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = (12 * density).toInt()
                rightMargin = (16 * density).toInt()
            }
        }
    }
}
