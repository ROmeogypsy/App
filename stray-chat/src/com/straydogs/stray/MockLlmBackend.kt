package com.straydogs.stray

import android.os.Handler
import android.os.Looper

/**
 * Working demo backend — streams realistic fake responses token-by-token.
 * Useful for testing UI without a real model.
 */
class MockLlmBackend : LlmBackend {
    override val id = "mock"
    override val displayName = "Demo (Mock)"
    override val isReady = true

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var cancelled = false
    private var responseIdx = 0

    private val responses = listOf(
        "I'm STRAY — your offline-first AI assistant built by SD Media.\n\nRight now I'm running in demo mode. To unlock real inference, either:\n• Drop a .gguf model into /sdcard/stray/ for local on-device inference\n• Add a Hugging Face API key in settings for remote inference\n\nSwitch backends anytime via the chip in the header.",
        "Local inference via llama.cpp keeps every conversation 100% private — no data leaves your device. STRAY supports any GGUF-format model:\n\n• Mistral 7B / Mixtral\n• Llama 3 8B / 70B\n• Phi-3 Mini / Medium\n• Gemma 2B / 7B\n• Qwen 2\n\nQuantized Q4_K_M or Q5_K_M variants give the best quality/speed tradeoff on mobile hardware.",
        "The backend architecture is designed to be extensible. The LlmBackend interface defines:\n\n  generate(messages, onToken, onDone, onError)\n\nAny backend that implements this interface plugs straight into the chat engine. Future backends could include Ollama, llama-server, OpenAI-compatible endpoints, or custom inference servers.",
        "Streaming is implemented token-by-token via callbacks. The UI shows a blinking cursor (▋) while generating and updates the bubble in real time as each token arrives.\n\nYou can stop generation at any time by tapping the ■ stop button — the partial response is preserved.",
        "To load a local model:\n\n1. Download a GGUF file (e.g. from Hugging Face)\n2. Copy it to /sdcard/stray/model.gguf\n3. Restart STRAY\n4. Tap the backend chip → select \"Local Model\"\n\nThe JNI bridge to llama.cpp is defined in stray-ai/app/src/main/cpp/llama_bridge.cpp — compile with NDK to enable real inference."
    )

    override fun generate(
        messages: List<Message>,
        onToken: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        cancelled = false
        val text = responses[responseIdx % responses.size]
        responseIdx++
        streamText(text, 0, onToken, onDone)
    }

    private fun streamText(text: String, pos: Int, onToken: (String) -> Unit, onDone: () -> Unit) {
        if (cancelled || pos >= text.length) {
            if (!cancelled) handler.post { onDone() }
            return
        }
        // Stream word-by-word with occasional longer chunks for realism
        val end = findNextBoundary(text, pos)
        val token = text.substring(pos, end)
        val delay = when {
            token.contains('\n') -> 80L
            token.endsWith('.') || token.endsWith('!') || token.endsWith('?') -> 120L
            token.endsWith(',') || token.endsWith(':') -> 60L
            else -> 25L + (Math.random() * 20).toLong()
        }
        handler.postDelayed({
            if (!cancelled) {
                onToken(token)
                streamText(text, end, onToken, onDone)
            }
        }, delay)
    }

    private fun findNextBoundary(text: String, start: Int): Int {
        var end = start + 1
        // Extend to word boundary for natural streaming
        while (end < text.length && end < start + 8) {
            if (text[end] == ' ' || text[end] == '\n') { end++; break }
            end++
        }
        return minOf(end, text.length)
    }

    override fun cancel() { cancelled = true }

    override fun statusLine() = "demo mode — no real inference"
}
