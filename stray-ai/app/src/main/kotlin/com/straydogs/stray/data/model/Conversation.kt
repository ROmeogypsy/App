package com.straydogs.stray.data.model

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "You are a helpful AI assistant."
    }
}
