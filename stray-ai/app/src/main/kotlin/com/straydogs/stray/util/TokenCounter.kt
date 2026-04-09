package com.straydogs.stray.util

object TokenCounter {

    // Rough approximation: ~4 chars per token for English text
    fun estimate(text: String): Int = (text.length / 4).coerceAtLeast(1)

    fun estimateMessages(messages: List<Pair<String, String>>): Int =
        messages.sumOf { (role, content) -> estimate(role) + estimate(content) + 4 }
}
