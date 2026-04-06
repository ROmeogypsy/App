package com.sdmedia.launcher.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads icons from third-party icon packs that expose the `com.novalauncher.THEME` category.
 */
@Singleton
class IconPackLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Returns the package names of all installed icon packs. */
    fun getInstalledIconPacks(): List<String> {
        val pm = context.packageManager
        return pm.queryIntentActivities(
            android.content.Intent("com.novalauncher.THEME"), 0
        ).map { it.activityInfo.packageName }
    }

    /**
     * Loads an icon for [targetPackage] from [iconPackPackage].
     * Returns null if the icon pack doesn't provide an icon for that package.
     */
    fun loadIcon(iconPackPackage: String, targetPackage: String): Drawable? {
        return try {
            val iconPackRes = context.packageManager.getResourcesForApplication(iconPackPackage)

            // Icon packs store drawables named after activity component flattened
            val drawableName = targetPackage.replace(".", "_")
            val id = iconPackRes.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (id != 0) iconPackRes.getDrawable(id, null) else null
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
