package com.sdmedia.launcher.ui.folder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sdmedia.launcher.data.model.AppItem
import com.sdmedia.launcher.databinding.ViewFolderBinding
import com.sdmedia.launcher.ui.common.AppIconView

class FolderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val binding = ViewFolderBinding.inflate(LayoutInflater.from(context), this, true)
    private val apps = mutableListOf<AppItem>()

    fun setApps(items: List<AppItem>, folderName: String) {
        apps.clear()
        apps.addAll(items)
        binding.tvFolderName.text = folderName
        renderPreview()
    }

    private fun renderPreview() {
        binding.folderGrid.removeAllViews()
        apps.take(4).forEach { app ->
            val iconView = AppIconView(context).apply {
                bind(app)
                showLabel(false)
            }
            binding.folderGrid.addView(iconView)
        }
    }

    fun openFolder(anchor: android.view.View, onClose: () -> Unit) {
        FolderOpenAnimator.open(this, anchor, onClose)
    }
}
