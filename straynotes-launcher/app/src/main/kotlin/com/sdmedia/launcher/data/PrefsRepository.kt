package com.sdmedia.launcher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        val KEY_GRID_ROWS = intPreferencesKey("grid_rows")
        val KEY_ICON_SIZE_DP = intPreferencesKey("icon_size_dp")
        val KEY_ICON_THEME = stringPreferencesKey("icon_theme")
        val KEY_DOUBLE_TAP_LOCK = booleanPreferencesKey("double_tap_lock")
        val KEY_SHOW_LABELS = booleanPreferencesKey("show_labels")
        val KEY_DOCK_COUNT = intPreferencesKey("dock_count")
    }

    val gridColumns: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_GRID_COLUMNS] ?: 4
    }

    val gridRows: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_GRID_ROWS] ?: 5
    }

    val iconSizeDp: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_ICON_SIZE_DP] ?: 56
    }

    val doubleTapLock: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DOUBLE_TAP_LOCK] ?: true
    }

    val showLabels: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SHOW_LABELS] ?: true
    }

    val dockCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_DOCK_COUNT] ?: 4
    }

    suspend fun setGridColumns(value: Int) = dataStore.edit { it[KEY_GRID_COLUMNS] = value }
    suspend fun setGridRows(value: Int) = dataStore.edit { it[KEY_GRID_ROWS] = value }
    suspend fun setIconSizeDp(value: Int) = dataStore.edit { it[KEY_ICON_SIZE_DP] = value }
    suspend fun setDoubleTapLock(value: Boolean) = dataStore.edit { it[KEY_DOUBLE_TAP_LOCK] = value }
    suspend fun setShowLabels(value: Boolean) = dataStore.edit { it[KEY_SHOW_LABELS] = value }
    suspend fun setDockCount(value: Int) = dataStore.edit { it[KEY_DOCK_COUNT] = value }
}
