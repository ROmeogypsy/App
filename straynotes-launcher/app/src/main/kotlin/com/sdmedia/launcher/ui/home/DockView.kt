package com.sdmedia.launcher.ui.home

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.sdmedia.launcher.data.model.AppItem
import com.sdmedia.launcher.ui.common.AppIconView

class DockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        orientation = HORIZONTAL
        weightSum = 4f
    }

    private var dockApps: List<AppItem> = emptyList()
    private var predictiveApps: List<AppItem> = emptyList()

    fun setApps(apps: List<AppItem>) {
        dockApps = apps
        render()
    }

    fun setPredictiveApps(apps: List<AppItem>) {
        predictiveApps = apps
        if (dockApps.isEmpty()) render()
    }

    private fun render() {
        removeAllViews()
        val displayApps = if (dockApps.isNotEmpty()) dockApps else predictiveApps
        displayApps.take(4).forEach { app ->
            val iconView = AppIconView(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                bind(app)
                showLabel(false)
            }
            addView(iconView)
        }
    }
}
