package com.sdmedia.launcher.ui.folder

import android.view.View
import android.view.animation.OvershootInterpolator

object FolderOpenAnimator {

    private val OPEN_INTERPOLATOR = OvershootInterpolator(1.2f)
    private val CLOSE_INTERPOLATOR = android.view.animation.AccelerateInterpolator()

    /**
     * Scale open from the anchor tap point — matches the Moto Launcher folder animation.
     */
    fun open(folderView: FolderView, anchor: View, onClose: () -> Unit) {
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)

        // Pivot at the tap origin
        folderView.pivotX = location[0].toFloat()
        folderView.pivotY = location[1].toFloat()
        folderView.scaleX = 0f
        folderView.scaleY = 0f
        folderView.alpha = 0f
        folderView.visibility = View.VISIBLE

        folderView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(OPEN_INTERPOLATOR)
            .start()

        // Dismiss tap outside
        folderView.setOnClickListener { close(folderView, onClose) }
    }

    fun close(folderView: FolderView, onClose: () -> Unit) {
        folderView.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(CLOSE_INTERPOLATOR)
            .withEndAction {
                folderView.visibility = View.GONE
                onClose()
            }
            .start()
    }
}
