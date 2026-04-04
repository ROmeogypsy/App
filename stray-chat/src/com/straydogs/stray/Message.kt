package com.straydogs.stray

enum class Role { USER, ASSISTANT, SYSTEM }

data class Message(
    val id: Long = System.nanoTime(),
    val role: Role,
    var content: String,
    var isStreaming: Boolean = false
)
