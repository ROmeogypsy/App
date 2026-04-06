package com.sdmedia.launcher.ui.home

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.util.SparseArrayCompat
import com.sdmedia.launcher.data.model.AppItem
import com.sdmedia.launcher.data.model.GridPosition
import com.sdmedia.launcher.data.model.HomeItem
import com.sdmedia.launcher.ui.common.AppIconView

class HomeGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    var columns: Int = 4
        set(value) { field = value; requestLayout() }

    var rows: Int = 5
        set(value) { field = value; requestLayout() }

    var page: Int = 0

    private val dockHeight: Int get() = 0 // Dock is outside this view

    private val cellWidth get() = if (width > 0) width / columns else 0
    private val cellHeight get() = if (height > 0) height / rows else 0

    private val items = SparseArrayCompat<HomeItem>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val pos = child.tag as? GridPosition ?: continue
            val left = pos.col * cellWidth
            val top = pos.row * cellHeight
            child.layout(left, top, left + cellWidth, top + cellHeight)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val cw = if (w > 0) w / columns else 0
        val ch = if (h > 0) h / rows else 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(
                MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY)
            )
        }
    }

    fun setApps(apps: List<AppItem>, targetPage: Int) {
        removeAllViews()
        items.clear()

        var col = 0
        var row = 0

        for (app in apps) {
            if (row >= rows) break
            val position = GridPosition(row, col, targetPage)
            val iconView = AppIconView(context).apply {
                tag = position
                bind(app)
            }
            addView(iconView)
            items.put(position.toIndex(columns), HomeItem.App(app, position))

            col++
            if (col >= columns) {
                col = 0
                row++
            }
        }
        requestLayout()
    }

    fun placeItem(item: HomeItem, position: GridPosition) {
        val idx = position.toIndex(columns)
        val existing = items[idx]
        if (existing != null) return // cell occupied

        when (item) {
            is HomeItem.App -> {
                val view = AppIconView(context).apply {
                    tag = position
                    bind(item.appItem)
                }
                addView(view)
                items.put(idx, item)
            }
            else -> { /* Widget and folder placement handled separately */ }
        }
        requestLayout()
    }

    fun findEmptyCell(): GridPosition? {
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val idx = row * columns + col
                if (items[idx] == null) return GridPosition(row, col, page)
            }
        }
        return null
    }

    /** Enable hardware layer only during active drag for performance. */
    fun enableDragLayer() {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun disableDragLayer() {
        setLayerType(LAYER_TYPE_NONE, null)
    }
}
