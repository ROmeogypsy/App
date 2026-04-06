package com.sdmedia.launcher.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sdmedia.launcher.data.model.AppItem

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @ColumnInfo(name = "is_pinned_predictive") val isPinnedPredictive: Boolean = false,
    @ColumnInfo(name = "launch_count") val launchCount: Int = 0,
    @ColumnInfo(name = "last_launched") val lastLaunched: Long = 0L,
    @ColumnInfo(name = "grid_page") val gridPage: Int = -1,
    @ColumnInfo(name = "grid_row") val gridRow: Int = -1,
    @ColumnInfo(name = "grid_col") val gridCol: Int = -1
) {
    fun toAppItem(): AppItem = AppItem(
        packageName = packageName,
        activityName = activityName,
        label = label,
        icon = null, // icon loaded separately via PackageManager
        isPinned = isPinned,
        isPinnedPredictive = isPinnedPredictive
    )
}
