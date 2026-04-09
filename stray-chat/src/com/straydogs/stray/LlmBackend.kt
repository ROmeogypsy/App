package com.straydogs.stray

/**
 * Core abstraction for any LLM inference backend.
 *
 * Implement this interface to add new backends:
 *   - LocalLlmBackend  → llama.cpp via JNI (see stray-ai/ for full impl)
 *   - HuggingFaceBackend → Hugging Face Inference API
 *   - OpenAiBackend     → OpenAI-compatible endpoints
 *   - OllamaBackend     → local Ollama server
 */
interface LlmBackend {
    /** Stable identifier used for persistence */
    val id: String

    /** Human-readable name shown in the UI */
    val displayName: String

    /** Whether the backend can currently accept requests */
    val isReady: Boolean

    /**
     * Generate a completion for the given conversation.
     *
     * All callbacks fire on a background thread — post to main thread in callers.
     *
     * @param messages  Full conversation history including the new user turn
     * @param onToken   Called for each generated token (streaming)
     * @param onDone    Called when generation is complete (after last onToken)
     * @param onError   Called on failure; onDone is NOT called in this case
     */
    fun generate(
        messages: List<Message>,
        onToken: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    )

    /** Cancel an in-flight generation. Safe to call at any time. */
    fun cancel()

    /** Optional: human-readable status/config description */
    fun statusLine(): String = if (isReady) "ready" else "unavailable"
}
