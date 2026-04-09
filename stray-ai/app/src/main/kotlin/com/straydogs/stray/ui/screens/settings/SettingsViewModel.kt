package com.straydogs.stray.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.straydogs.stray.data.model.InferenceMode
import com.straydogs.stray.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hfApiToken: String = "",
    val inferenceMode: InferenceMode = InferenceMode.LOCAL,
    val remoteModelId: String = "",
    val temperature: Float = 0.8f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    val contextSize: Int = 2048
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                modelRepository.hfApiToken,
                modelRepository.inferenceMode,
                modelRepository.remoteModelId,
                modelRepository.temperature,
                modelRepository.topP,
                modelRepository.maxTokens,
                modelRepository.contextSize
            ) { values ->
                SettingsUiState(
                    hfApiToken = values[0] as String,
                    inferenceMode = values[1] as InferenceMode,
                    remoteModelId = values[2] as String,
                    temperature = values[3] as Float,
                    topP = values[4] as Float,
                    maxTokens = values[5] as Int,
                    contextSize = values[6] as Int
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setHfApiToken(token: String) {
        viewModelScope.launch { modelRepository.setHfApiToken(token) }
    }

    fun setInferenceMode(mode: InferenceMode) {
        viewModelScope.launch { modelRepository.setInferenceMode(mode) }
    }

    fun setRemoteModelId(modelId: String) {
        viewModelScope.launch { modelRepository.setRemoteModelId(modelId) }
    }

    fun setTemperature(value: Float) {
        viewModelScope.launch { modelRepository.setTemperature(value) }
    }

    fun setTopP(value: Float) {
        viewModelScope.launch { modelRepository.setTopP(value) }
    }

    fun setMaxTokens(value: Int) {
        viewModelScope.launch { modelRepository.setMaxTokens(value) }
    }

    fun setContextSize(value: Int) {
        viewModelScope.launch { modelRepository.setContextSize(value) }
    }
}
