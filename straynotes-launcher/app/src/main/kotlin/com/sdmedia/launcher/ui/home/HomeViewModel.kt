package com.sdmedia.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdmedia.launcher.data.AppRepository
import com.sdmedia.launcher.data.model.AppItem
import com.sdmedia.launcher.util.AppUsageTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val usageTracker: AppUsageTracker
) : ViewModel() {

    /** All installed apps — used by drawer and home grid. */
    val allApps: StateFlow<List<AppItem>> = appRepository.allAppsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _dockApps = MutableStateFlow<List<AppItem>>(emptyList())
    val dockApps: StateFlow<List<AppItem>> = _dockApps

    private val _predictiveApps = MutableStateFlow<List<AppItem>>(emptyList())
    val predictiveApps: StateFlow<List<AppItem>> = _predictiveApps

    fun syncApps() {
        viewModelScope.launch {
            appRepository.syncInstalledApps()
            loadPredictiveApps()
        }
    }

    fun refreshPredictive() {
        viewModelScope.launch { loadPredictiveApps() }
    }

    private suspend fun loadPredictiveApps() {
        _predictiveApps.value = usageTracker.getPredictedApps(4)
    }

    fun recordLaunch(packageName: String) {
        viewModelScope.launch {
            appRepository.recordLaunch(packageName)
        }
    }
}
