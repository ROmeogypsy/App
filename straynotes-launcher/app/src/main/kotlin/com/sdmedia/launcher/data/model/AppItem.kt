package com.sdmedia.launcher.data.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable?,
    val isPinned: Boolean = false,
    val isPinnedPredictive: Boolean = false
)
