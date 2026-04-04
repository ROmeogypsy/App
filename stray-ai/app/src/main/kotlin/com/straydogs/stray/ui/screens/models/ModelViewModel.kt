package com.straydogs.stray.ui.screens.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.straydogs.stray.data.local.DownloadState
import com.straydogs.stray.data.model.HFModel
import com.straydogs.stray.data.model.LocalModel
import com.straydogs.stray.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelUiState(
    val localModels: List<LocalModel> = emptyList(),
    val remoteModels: List<HFModel> = emptyList(),
    val activeModelId: String = "",
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val downloadStates: Map<String, DownloadState> = emptyMap(),
    val error: String? = null
)

@HiltViewModel
class ModelViewModel @Inject constructor(
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState.asStateFlow()

    init {
        loadLocalModels()
        viewModelScope.launch {
            modelRepository.activeLocalModelId.collect { id ->
                _uiState.update { it.copy(activeModelId = id) }
            }
        }
        searchModels("")
    }

    fun loadLocalModels() {
        val models = modelRepository.getLocalModels()
        _uiState.update { it.copy(localModels = models) }
    }

    fun setActiveModel(modelId: String) {
        viewModelScope.launch {
            modelRepository.setActiveLocalModel(modelId)
        }
    }

    fun deleteModel(modelId: String) {
        modelRepository.deleteLocalModel(modelId)
        loadLocalModels()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchModels(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }
            modelRepository.searchModels(query)
                .onSuccess { models ->
                    _uiState.update { it.copy(remoteModels = models, isSearching = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isSearching = false) }
                }
        }
    }

    fun downloadModel(hfRepoId: String, filename: String) {
        viewModelScope.launch {
            val apiToken = modelRepository.hfApiToken.first()
            val modelId = "${hfRepoId.replace('/', '_')}_${filename.removeSuffix(".gguf")}"
            val url = "https://huggingface.co/$hfRepoId/resolve/main/$filename"

            modelRepository.downloadModel(url, modelId, apiToken).collect { state ->
                _uiState.update { uiState ->
                    uiState.copy(downloadStates = uiState.downloadStates + (modelId to state))
                }
                if (state is DownloadState.Success) {
                    loadLocalModels()
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
