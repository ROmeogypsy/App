package com.sdmedia.launcher.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

class WidgetResizeFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    companion object {
        private const val HANDLE_SIZE_DP = 24f
        private const val HANDLE_TOUCH_TARGET_DP = 44f
    }

    private val handleSize = (HANDLE_SIZE_DP * context.resources.displayMetrics.density).toInt()
    private val handleTouchTarget = (HANDLE_TOUCH_TARGET_DP * context.resources.displayMetrics.density).toInt()

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 200
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 80
        style = Paint.Style.STROKE
        strokeWidth = 2f * context.resources.displayMetrics.density
    }

    var onResizeListener: ((deltaW: Int, deltaH: Int) -> Unit)? = null

    private var dragHandle: Handle? = null
    private var lastX = 0f
    private var lastY = 0f

    enum class Handle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw border
        canvas.drawRect(
            borderPaint.strokeWidth / 2,
            borderPaint.strokeWidth / 2,
            width - borderPaint.strokeWidth / 2,
            height - borderPaint.strokeWidth / 2,
            borderPaint
        )
        // Draw corner handles
        val r = handleSize / 2f
        canvas.drawCircle(0f, 0f, r, handlePaint)
        canvas.drawCircle(width.toFloat(), 0f, r, handlePaint)
        canvas.drawCircle(0f, height.toFloat(), r, handlePaint)
        canvas.drawCircle(width.toFloat(), height.toFloat(), r, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragHandle = hitTestHandle(event.x, event.y)
                if (dragHandle != null) {
                    lastX = event.rawX
                    lastY = event.rawY
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragHandle != null) {
                    val dx = (event.rawX - lastX).toInt()
                    val dy = (event.rawY - lastY).toInt()
                    onResizeListener?.invoke(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragHandle = null
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitTestHandle(x: Float, y: Float): Handle? {
        val t = handleTouchTarget / 2f
        return when {
            x < t && y < t -> Handle.TOP_LEFT
            x > width - t && y < t -> Handle.TOP_RIGHT
            x < t && y > height - t -> Handle.BOTTOM_LEFT
            x > width - t && y > height - t -> Handle.BOTTOM_RIGHT
            else -> null
        }
    }
}
