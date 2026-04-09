package com.straydogs.stray.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.*

interface HuggingFaceApi {

    @POST("models/{modelId}")
    suspend fun textGeneration(
        @Path("modelId") modelId: String,
        @Header("Authorization") authHeader: String,
        @Body request: HFTextGenRequest
    ): Response<HFTextGenResponse>

    @POST("models/{modelId}/v1/chat/completions")
    suspend fun chatCompletion(
        @Path("modelId") modelId: String,
        @Header("Authorization") authHeader: String,
        @Body request: HFChatRequest
    ): Response<HFChatResponse>

    @GET("api/models")
    suspend fun searchModels(
        @Query("search") query: String,
        @Query("filter") filter: String = "gguf",
        @Query("sort") sort: String = "downloads",
        @Query("direction") direction: Int = -1,
        @Query("limit") limit: Int = 20
    ): Response<List<HFModelInfo>>
}

@Serializable
data class HFTextGenRequest(
    @SerialName("inputs") val inputs: String,
    @SerialName("parameters") val parameters: HFParameters = HFParameters()
)

@Serializable
data class HFParameters(
    @SerialName("max_new_tokens") val maxNewTokens: Int = 512,
    @SerialName("temperature") val temperature: Float = 0.8f,
    @SerialName("top_p") val topP: Float = 0.9f,
    @SerialName("do_sample") val doSample: Boolean = true,
    @SerialName("return_full_text") val returnFullText: Boolean = false
)

@Serializable
data class HFTextGenResponse(
    @SerialName("generated_text") val generatedText: String = ""
)

@Serializable
data class HFChatRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<HFChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 512,
    @SerialName("temperature") val temperature: Float = 0.8f,
    @SerialName("top_p") val topP: Float = 0.9f,
    @SerialName("stream") val stream: Boolean = false
)

@Serializable
data class HFChatMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class HFChatResponse(
    @SerialName("choices") val choices: List<HFChoice> = emptyList()
)

@Serializable
data class HFChoice(
    @SerialName("message") val message: HFChatMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class HFModelInfo(
    @SerialName("id") val id: String,
    @SerialName("pipeline_tag") val pipelineTag: String? = null,
    @SerialName("downloads") val downloads: Int = 0,
    @SerialName("likes") val likes: Int = 0,
    @SerialName("tags") val tags: List<String> = emptyList()
)
