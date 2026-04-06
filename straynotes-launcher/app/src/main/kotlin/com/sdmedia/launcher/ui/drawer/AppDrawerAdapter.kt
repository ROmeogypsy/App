package com.sdmedia.launcher.ui.drawer

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sdmedia.launcher.data.model.AppItem
import com.sdmedia.launcher.ui.common.AppIconView

class AppDrawerAdapter(
    private val onAppClick: (AppItem) -> Unit,
    private val onAppLongPress: (AppItem, View) -> Unit
) : ListAdapter<AppItem, AppDrawerAdapter.AppIconViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppIconViewHolder {
        val view = AppIconView(parent.context)
        return AppIconViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppIconViewHolder, position: Int) {
        val app = getItem(position)
        holder.bind(app, onAppClick, onAppLongPress)
    }

    inner class AppIconViewHolder(
        private val iconView: AppIconView
    ) : RecyclerView.ViewHolder(iconView) {

        fun bind(
            app: AppItem,
            onClick: (AppItem) -> Unit,
            onLongPress: (AppItem, View) -> Unit
        ) {
            iconView.bind(app)
            iconView.setOnClickListener { onClick(app) }
            iconView.setOnLongClickListener {
                onLongPress(app, it)
                true
            }
        }
    }

    /** DiffCallback ensures only changed icons re-render — no full list refresh. */
    class AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(a: AppItem, b: AppItem) =
            a.packageName == b.packageName

        override fun areContentsTheSame(a: AppItem, b: AppItem) =
            a.label == b.label && a.packageName == b.packageName
    }
}
