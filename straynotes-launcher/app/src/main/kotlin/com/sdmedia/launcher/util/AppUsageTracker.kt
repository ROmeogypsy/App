package com.sdmedia.launcher.util

import android.app.usage.UsageStatsManager
import android.content.Context
import com.sdmedia.launcher.data.db.AppDao
import com.sdmedia.launcher.data.model.AppItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUsageTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageManager: UsageStatsManager,
    private val appDao: AppDao
) {

    /**
     * Returns predicted apps based on 7-day usage stats.
     * Requires PACKAGE_USAGE_STATS permission — user must grant via Settings.
     */
    suspend fun getPredictedApps(count: Int = 4): List<AppItem> {
        return try {
            val end = System.currentTimeMillis()
            val start = end - (7 * 24 * 60 * 60 * 1000L) // 7 days

            val stats = usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_WEEKLY, start, end
            )

            if (stats.isNullOrEmpty()) {
                getFallbackApps()
            } else {
                stats
                    .filter { it.totalTimeInForeground > 0 }
                    .sortedByDescending { it.totalTimeInForeground }
                    .take(count)
                    .mapNotNull { stat ->
                        appDao.getByPackageName(stat.packageName)?.toAppItem()?.copy(
                            icon = loadIcon(stat.packageName)
                        )
                    }
                    .ifEmpty { getFallbackApps() }
            }
        } catch (e: Exception) {
            getFallbackApps()
        }
    }

    /** Fallback if permission denied: return manually pinned apps from Room. */
    suspend fun getFallbackApps(): List<AppItem> =
        appDao.getPinnedPredictive().map { entity ->
            entity.toAppItem().copy(icon = loadIcon(entity.packageName))
        }

    private fun loadIcon(packageName: String) = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }
}
