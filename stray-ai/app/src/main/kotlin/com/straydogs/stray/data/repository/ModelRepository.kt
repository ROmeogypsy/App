package com.straydogs.stray.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.straydogs.stray.data.local.DownloadState
import com.straydogs.stray.data.local.ModelDownloader
import com.straydogs.stray.data.local.ModelManager
import com.straydogs.stray.data.model.HFModel
import com.straydogs.stray.data.model.InferenceMode
import com.straydogs.stray.data.model.LocalModel
import com.straydogs.stray.data.remote.HFModelBrowser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stray_prefs")

@Singleton
class ModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelManager: ModelManager,
    private val modelDownloader: ModelDownloader,
    private val hfModelBrowser: HFModelBrowser
) {

    companion object {
        val KEY_HF_API_TOKEN = stringPreferencesKey("hf_api_token")
        val KEY_ACTIVE_MODEL_ID = stringPreferencesKey("active_model_id")
        val KEY_INFERENCE_MODE = stringPreferencesKey("inference_mode")
        val KEY_REMOTE_MODEL_ID = stringPreferencesKey("remote_model_id")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_TOP_P = floatPreferencesKey("top_p")
        val KEY_CONTEXT_SIZE = intPreferencesKey("context_size")
    }

    val preferences: Flow<Preferences> = context.dataStore.data

    val hfApiToken: Flow<String> = context.dataStore.data.map {
        it[KEY_HF_API_TOKEN] ?: ""
    }

    val activeLocalModelId: Flow<String> = context.dataStore.data.map {
        it[KEY_ACTIVE_MODEL_ID] ?: ""
    }

    val inferenceMode: Flow<InferenceMode> = context.dataStore.data.map {
        when (it[KEY_INFERENCE_MODE]) {
            "REMOTE" -> InferenceMode.REMOTE
            else -> InferenceMode.LOCAL
        }
    }

    val remoteModelId: Flow<String> = context.dataStore.data.map {
        it[KEY_REMOTE_MODEL_ID] ?: "mistralai/Mistral-7B-Instruct-v0.3"
    }

    val temperature: Flow<Float> = context.dataStore.data.map {
        it[KEY_TEMPERATURE] ?: 0.8f
    }

    val topP: Flow<Float> = context.dataStore.data.map {
        it[KEY_TOP_P] ?: 0.9f
    }

    val maxTokens: Flow<Int> = context.dataStore.data.map {
        it[KEY_MAX_TOKENS] ?: 512
    }

    val contextSize: Flow<Int> = context.dataStore.data.map {
        it[KEY_CONTEXT_SIZE] ?: 2048
    }

    suspend fun setHfApiToken(token: String) {
        context.dataStore.edit { it[KEY_HF_API_TOKEN] = token }
    }

    suspend fun setActiveLocalModel(modelId: String) {
        context.dataStore.edit { it[KEY_ACTIVE_MODEL_ID] = modelId }
    }

    suspend fun setInferenceMode(mode: InferenceMode) {
        context.dataStore.edit { it[KEY_INFERENCE_MODE] = mode.name }
    }

    suspend fun setRemoteModelId(modelId: String) {
        context.dataStore.edit { it[KEY_REMOTE_MODEL_ID] = modelId }
    }

    suspend fun setTemperature(value: Float) {
        context.dataStore.edit { it[KEY_TEMPERATURE] = value }
    }

    suspend fun setTopP(value: Float) {
        context.dataStore.edit { it[KEY_TOP_P] = value }
    }

    suspend fun setMaxTokens(value: Int) {
        context.dataStore.edit { it[KEY_MAX_TOKENS] = value }
    }

    suspend fun setContextSize(value: Int) {
        context.dataStore.edit { it[KEY_CONTEXT_SIZE] = value }
    }

    fun getLocalModels(): List<LocalModel> = modelManager.getLocalModels()

    fun deleteLocalModel(modelId: String): Boolean = modelManager.deleteModel(modelId)

    fun downloadModel(
        url: String,
        modelId: String,
        apiToken: String = ""
    ): Flow<DownloadState> = modelDownloader.downloadModel(url, modelId, apiToken)

    suspend fun searchModels(query: String): Result<List<HFModel>> =
        hfModelBrowser.searchGGUFModels(query)
}
