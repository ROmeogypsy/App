package com.straydogs.stray.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HFModel(
    @SerialName("id") val id: String,
    @SerialName("modelId") val modelId: String = id,
    @SerialName("pipeline_tag") val pipelineTag: String? = null,
    @SerialName("downloads") val downloads: Int = 0,
    @SerialName("likes") val likes: Int = 0,
    @SerialName("private") val isPrivate: Boolean = false,
    val description: String = "",
    val tags: List<String> = emptyList()
)

data class LocalModel(
    val id: String,
    val name: String,
    val filePath: String,
    val sizeBytes: Long,
    val quantization: String = "",
    val contextSize: Int = 2048,
    val promptTemplate: String = "chatml"
)
