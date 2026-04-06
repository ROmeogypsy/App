package com.sdmedia.launcher.ui.common

import android.content.ClipData
import android.graphics.Canvas
import android.graphics.Point
import android.view.HapticFeedbackConstants
import android.view.View
import com.sdmedia.launcher.data.model.HomeItem
import com.sdmedia.launcher.ui.home.HomeActivity
import com.sdmedia.launcher.ui.home.HomeGrid

class DragController(private val activity: HomeActivity) {

    private var draggedItem: HomeItem? = null

    fun startDrag(source: View, item: HomeItem, grid: HomeGrid) {
        draggedItem = item
        grid.enableDragLayer()

        val shadowBuilder = object : View.DragShadowBuilder(source) {
            override fun onProvideShadowMetrics(size: Point, touch: Point) {
                size.set(source.width, source.height)
                touch.set(source.width / 2, source.height / 2)
            }

            override fun onDrawShadow(canvas: Canvas) {
                canvas.scale(1.1f, 1.1f, source.width / 2f, source.height / 2f)
                super.onDrawShadow(canvas)
            }
        }

        source.startDragAndDrop(
            ClipData.newPlainText("", ""),
            shadowBuilder,
            item,
            View.DRAG_FLAG_OPAQUE
        )

        HapticHelper.longPress(source)
        source.animate().alpha(0.3f).setDuration(100).start()
    }

    fun onDragEnded(source: View, grid: HomeGrid) {
        source.animate().alpha(1f).setDuration(100).start()
        grid.disableDragLayer()
        draggedItem = null
    }

    fun getDraggedItem(): HomeItem? = draggedItem
}
