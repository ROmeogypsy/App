package com.sdmedia.launcher.data.model

/**
 * Union type for items that can appear on the home grid.
 */
sealed class HomeItem {

    data class App(
        val appItem: AppItem,
        val position: GridPosition
    ) : HomeItem()

    data class Folder(
        val id: String,
        val name: String,
        val apps: List<AppItem>,
        val position: GridPosition
    ) : HomeItem()

    data class Widget(
        val widgetId: Int,
        val position: GridPosition,
        val spanCols: Int = 1,
        val spanRows: Int = 1
    ) : HomeItem()

    /** Empty cell placeholder used during drag-and-drop rearrangement. */
    data class Gap(val position: GridPosition) : HomeItem()
}
