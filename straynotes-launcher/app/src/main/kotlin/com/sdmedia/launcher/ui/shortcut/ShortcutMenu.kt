package com.sdmedia.launcher.ui.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import com.sdmedia.launcher.R
import com.sdmedia.launcher.data.model.AppItem
import com.sdmedia.launcher.databinding.ShortcutMenuBinding
import com.sdmedia.launcher.databinding.ShortcutMenuItemBinding

class ShortcutMenu(context: Context) : PopupWindow(context) {

    private val inflater = LayoutInflater.from(context)

    fun show(anchor: View, app: AppItem) {
        val binding = ShortcutMenuBinding.inflate(inflater)

        // Fetch shortcuts via LauncherApps (correct API for a launcher to query other apps)
        val shortcuts: List<ShortcutInfo> = try {
            val launcherApps = anchor.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val query = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                )
                setPackage(app.packageName)
            }
            launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        shortcuts.take(4).forEach { shortcut ->
            val itemBinding = ShortcutMenuItemBinding.inflate(inflater, binding.shortcutList, true)
            itemBinding.tvShortcutLabel.text = shortcut.shortLabel
            try {
                val launcherApps = anchor.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val drawable = launcherApps.getShortcutIconDrawable(shortcut, 0)
                if (drawable != null) itemBinding.ivShortcutIcon.setImageDrawable(drawable)
                else itemBinding.ivShortcutIcon.visibility = View.GONE
            } catch (e: Exception) {
                itemBinding.ivShortcutIcon.visibility = View.GONE
            }
            itemBinding.root.setOnClickListener {
                try {
                    val launcherApps = anchor.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    launcherApps.startShortcut(shortcut, null, null)
                } catch (e: Exception) {
                    // Shortcut unavailable
                }
                dismiss()
            }
        }

        // App Info action
        addAction(binding, "App Info") {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            anchor.context.startActivity(intent)
            dismiss()
        }

        // Uninstall action
        addAction(binding, "Uninstall") {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            anchor.context.startActivity(intent)
            dismiss()
        }

        contentView = binding.root
        width = (220 * anchor.context.resources.displayMetrics.density).toInt()
        height = WRAP_CONTENT
        isOutsideTouchable = true
        isFocusable = true
        setBackgroundDrawable(ColorDrawable(0x00000000))
        animationStyle = R.style.ShortcutMenuAnimation

        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val popupY = (location[1] - contentView.measuredHeight).coerceAtLeast(0)

        showAtLocation(anchor, Gravity.NO_GRAVITY, location[0], popupY)
        anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    private fun addAction(binding: ShortcutMenuBinding, label: String, onClick: () -> Unit) {
        val itemBinding = ShortcutMenuItemBinding.inflate(inflater, binding.shortcutList, true)
        itemBinding.tvShortcutLabel.text = label
        itemBinding.ivShortcutIcon.visibility = View.GONE
        itemBinding.root.setOnClickListener { onClick() }
    }
}
