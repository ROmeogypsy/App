package com.straydogs.stray.data.remote

import com.straydogs.stray.data.model.HFModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HFModelBrowser @Inject constructor(
    private val api: HuggingFaceApi
) {

    suspend fun searchGGUFModels(query: String = ""): Result<List<HFModel>> {
        return try {
            val response = api.searchModels(
                query = query,
                filter = "gguf",
                sort = "downloads",
                direction = -1,
                limit = 30
            )
            if (response.isSuccessful) {
                val models = response.body()?.map { info ->
                    HFModel(
                        id = info.id,
                        modelId = info.id,
                        pipelineTag = info.pipelineTag,
                        downloads = info.downloads,
                        likes = info.likes,
                        tags = info.tags
                    )
                } ?: emptyList()
                Result.success(models)
            } else {
                Result.failure(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchChatModels(query: String = ""): Result<List<HFModel>> {
        return try {
            val response = api.searchModels(
                query = query,
                filter = "text-generation",
                sort = "downloads",
                direction = -1,
                limit = 30
            )
            if (response.isSuccessful) {
                val models = response.body()?.map { info ->
                    HFModel(
                        id = info.id,
                        modelId = info.id,
                        pipelineTag = info.pipelineTag,
                        downloads = info.downloads,
                        likes = info.likes,
                        tags = info.tags
                    )
                } ?: emptyList()
                Result.success(models)
            } else {
                Result.failure(Exception("Search failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
