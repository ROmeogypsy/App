package com.straydogs.stray

import android.os.Handler
import android.os.Looper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Remote inference via Hugging Face Inference API (text-generation endpoint).
 * Uses Server-Sent Events (SSE) for streaming.
 *
 * Model default: mistralai/Mistral-7B-Instruct-v0.2
 * Can be swapped to any supported TGI-compatible model.
 */
class HuggingFaceBackend(
    private var apiToken: String = "",
    private var modelId: String = "mistralai/Mistral-7B-Instruct-v0.2"
) : LlmBackend {
    override val id = "huggingface"
    override val displayName = "HF Inference"
    override val isReady = apiToken.isNotEmpty()

    @Volatile private var cancelled = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var connection: HttpURLConnection? = null

    fun configure(token: String, model: String = modelId) {
        apiToken = token
        modelId = model
    }

    override fun generate(
        messages: List<Message>,
        onToken: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (apiToken.isEmpty()) {
            onError("No Hugging Face API token configured. Add your token in settings.")
            onDone()
            return
        }

        cancelled = false

        Thread {
            try {
                val prompt = buildMistralPrompt(messages)
                val url = URL("https://api-inference.huggingface.co/models/$modelId")
                val conn = url.openConnection() as HttpURLConnection
                connection = conn
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $apiToken")
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "text/event-stream")
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000

                val body = """{"inputs":"$prompt","parameters":{"max_new_tokens":512,"temperature":0.7,"top_p":0.9,"return_full_text":false},"stream":true}"""
                OutputStreamWriter(conn.outputStream).use { it.write(body) }

                val code = conn.responseCode
                if (code != 200) {
                    val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                    mainHandler.post { onError("API error $code: ${err.take(200)}") }
                    return@Thread
                }

                BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null && !cancelled) {
                        val l = line ?: continue
                        if (!l.startsWith("data:")) continue
                        val data = l.removePrefix("data:").trim()
                        if (data == "[DONE]") break

                        // Parse SSE JSON: {"token":{"text":"..."}}
                        val tokenText = extractTokenText(data) ?: continue
                        mainHandler.post { onToken(tokenText) }
                    }
                }

                if (!cancelled) mainHandler.post { onDone() }

            } catch (e: Exception) {
                if (!cancelled) mainHandler.post { onError(e.message ?: "Network error") }
            } finally {
                connection?.disconnect()
                connection = null
            }
        }.start()
    }

    private fun extractTokenText(json: String): String? {
        // Minimal JSON extraction without a JSON library
        val marker = "\"text\":"
        val start = json.indexOf(marker) + marker.length
        if (start < marker.length) return null
        val s = json.indexOf('"', start)
        val e = json.indexOf('"', s + 1)
        return if (s >= 0 && e > s) {
            json.substring(s + 1, e)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        } else null
    }

    private fun buildMistralPrompt(messages: List<Message>): String {
        val sb = StringBuilder()
        var first = true
        for (msg in messages) {
            when (msg.role) {
                Role.USER -> {
                    if (first) { sb.append("[INST] "); first = false }
                    else sb.append(" [INST] ")
                    sb.append(msg.content.replace("\"", "\\\""))
                    sb.append(" [/INST]")
                }
                Role.ASSISTANT -> {
                    sb.append(" ")
                    sb.append(msg.content.replace("\"", "\\\""))
                }
                Role.SYSTEM -> { /* prepend to first user turn */ }
            }
        }
        return sb.toString()
    }

    override fun cancel() {
        cancelled = true
        connection?.disconnect()
    }

    override fun statusLine() = if (apiToken.isEmpty()) "no API token" else "model: $modelId"
}
