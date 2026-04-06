package com.sdmedia.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM apps ORDER BY label ASC")
    fun getAllFlow(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps ORDER BY label ASC")
    suspend fun getAll(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): AppEntity?

    @Query("SELECT * FROM apps WHERE is_pinned_predictive = 1 ORDER BY launch_count DESC LIMIT 4")
    suspend fun getPinnedPredictive(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE grid_page >= 0 ORDER BY grid_page, grid_row, grid_col")
    suspend fun getPlacedApps(): List<AppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: AppEntity)

    @Update
    suspend fun update(app: AppEntity)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("UPDATE apps SET launch_count = launch_count + 1, last_launched = :timestamp WHERE packageName = :packageName")
    suspend fun recordLaunch(packageName: String, timestamp: Long)
}
