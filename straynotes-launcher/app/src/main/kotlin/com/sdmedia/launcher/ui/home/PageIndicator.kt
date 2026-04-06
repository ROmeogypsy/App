package com.sdmedia.launcher.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.viewpager2.widget.ViewPager2

class PageIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66FFFFFF")
    }

    private val dotRadius = 4f.dp
    private val dotSpacing = 12f.dp

    private var pageCount: Int = 1
    private var currentPage: Int = 0

    fun attachToViewPager(viewPager: ViewPager2) {
        pageCount = (viewPager.adapter?.itemCount ?: 1)
        currentPage = viewPager.currentItem

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                pageCount = viewPager.adapter?.itemCount ?: 1
                invalidate()
            }
        })
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pageCount <= 1) return

        val totalWidth = pageCount * (dotRadius * 2) + (pageCount - 1) * dotSpacing
        var x = (width - totalWidth) / 2f + dotRadius
        val y = height / 2f

        for (i in 0 until pageCount) {
            canvas.drawCircle(x, y, dotRadius, if (i == currentPage) activePaint else inactivePaint)
            x += dotRadius * 2 + dotSpacing
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (dotRadius * 2 + paddingTop + paddingBottom).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h)
    }

    private val Float.dp: Float get() = this * context.resources.displayMetrics.density
}
