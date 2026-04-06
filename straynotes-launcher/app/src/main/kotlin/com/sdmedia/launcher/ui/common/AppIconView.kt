package com.sdmedia.launcher.ui.common

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import coil.load
import coil.transform.RoundedCornersTransformation
import com.sdmedia.launcher.data.model.AppItem
import com.sdmedia.launcher.databinding.ViewAppIconBinding

class AppIconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = ViewAppIconBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    fun bind(app: AppItem) {
        binding.tvLabel.text = app.label

        if (app.icon != null) {
            binding.ivIcon.setImageDrawable(app.icon)
        } else {
            // Coil loads from package name via custom fetcher or fallback to PackageManager
            binding.ivIcon.load(app.packageName) {
                placeholder(android.R.drawable.sym_def_app_icon)
                error(android.R.drawable.sym_def_app_icon)
            }
        }
    }

    fun showLabel(show: Boolean) {
        binding.tvLabel.visibility = if (show) VISIBLE else GONE
    }
}
