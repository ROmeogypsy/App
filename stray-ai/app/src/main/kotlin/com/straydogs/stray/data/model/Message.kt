package com.straydogs.stray.data.model

data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val tokenCount: Int = 0,
    val isStreaming: Boolean = false
)

enum class MessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    companion object {
        fun from(value: String): MessageRole =
            entries.firstOrNull { it.value == value } ?: USER
    }
}
