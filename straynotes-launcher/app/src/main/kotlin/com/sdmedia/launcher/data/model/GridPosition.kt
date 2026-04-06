package com.sdmedia.launcher.data.model

data class GridPosition(
    val row: Int,
    val col: Int,
    val page: Int = 0
) {
    fun toIndex(columns: Int): Int = row * columns + col
}
