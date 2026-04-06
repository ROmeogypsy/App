package com.sdmedia.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.sdmedia.launcher.data.db.AppDao
import com.sdmedia.launcher.data.db.AppEntity
import com.sdmedia.launcher.data.model.AppItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao
) {

    /** Flow of all installed apps from Room, sorted alphabetically. */
    val allAppsFlow: Flow<List<AppItem>> = appDao.getAllFlow().map { entities ->
        entities.map { entity ->
            entity.toAppItem().copy(
                icon = loadIcon(entity.packageName)
            )
        }
    }

    /** Syncs installed packages into Room. Called on startup and on package changes. */
    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pm = context.packageManager
        val resolveInfoList: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        val installedPackages = resolveInfoList.map { info ->
            AppEntity(
                packageName = info.activityInfo.packageName,
                activityName = info.activityInfo.name,
                label = info.loadLabel(pm).toString()
            )
        }

        // Remove apps no longer installed
        val installedSet = installedPackages.map { it.packageName }.toSet()
        val existing = appDao.getAll()
        existing.filter { it.packageName !in installedSet }
            .forEach { appDao.deleteByPackageName(it.packageName) }

        // Insert/update all current apps (preserves pinned state via REPLACE strategy
        // combined with retaining existing entity fields below)
        installedPackages.forEach { new ->
            val existing = appDao.getByPackageName(new.packageName)
            if (existing != null) {
                appDao.insert(
                    new.copy(
                        isPinned = existing.isPinned,
                        isPinnedPredictive = existing.isPinnedPredictive,
                        launchCount = existing.launchCount,
                        lastLaunched = existing.lastLaunched,
                        gridPage = existing.gridPage,
                        gridRow = existing.gridRow,
                        gridCol = existing.gridCol
                    )
                )
            } else {
                appDao.insert(new)
            }
        }
    }

    suspend fun recordLaunch(packageName: String) {
        appDao.recordLaunch(packageName, System.currentTimeMillis())
    }

    suspend fun removeApp(packageName: String) {
        appDao.deleteByPackageName(packageName)
    }

    private fun loadIcon(packageName: String) = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
