package com.straydogs.stray.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

interface TokenCallback {
    fun onToken(token: String)
    fun onDone()
}

@Singleton
class LlamaEngine @Inject constructor() {

    companion object {
        init {
            System.loadLibrary("stray_llama")
        }
    }

    // JNI declarations
    external fun loadModel(modelPath: String, nCtx: Int, nThreads: Int): Boolean
    external fun unloadModel()
    external fun abortGeneration()
    external fun generateTokens(
        prompt: String,
        maxTokens: Int,
        temp: Float,
        topP: Float,
        callback: TokenCallback
    )
    external fun isLoaded(): Boolean

    fun generateStream(
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.8f,
        topP: Float = 0.9f
    ): Flow<String> = callbackFlow {
        val callback = object : TokenCallback {
            override fun onToken(token: String) {
                trySend(token)
            }
            override fun onDone() {
                close()
            }
        }

        val thread = Thread {
            generateTokens(prompt, maxTokens, temperature, topP, callback)
        }
        thread.start()

        awaitClose {
            abortGeneration()
        }
    }.flowOn(Dispatchers.IO)

    fun formatChatPrompt(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        template: PromptTemplate = PromptTemplate.CHATML
    ): String = template.format(systemPrompt, messages)
}

enum class PromptTemplate {
    CHATML {
        override fun format(system: String, messages: List<Pair<String, String>>): String =
            buildString {
                append("<|im_start|>system\n$system<|im_end|>\n")
                for ((role, content) in messages) {
                    append("<|im_start|>$role\n$content<|im_end|>\n")
                }
                append("<|im_start|>assistant\n")
            }
    },
    LLAMA3 {
        override fun format(system: String, messages: List<Pair<String, String>>): String =
            buildString {
                append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n$system<|eot_id|>")
                for ((role, content) in messages) {
                    val header = if (role == "user") "user" else "assistant"
                    append("<|start_header_id|>$header<|end_header_id|>\n$content<|eot_id|>")
                }
                append("<|start_header_id|>assistant<|end_header_id|>\n")
            }
    },
    MISTRAL {
        override fun format(system: String, messages: List<Pair<String, String>>): String =
            buildString {
                var firstUser = true
                for ((role, content) in messages) {
                    if (role == "user") {
                        if (firstUser) {
                            append("[INST] $system\n$content [/INST]")
                            firstUser = false
                        } else {
                            append("[INST] $content [/INST]")
                        }
                    } else {
                        append(" $content ")
                    }
                }
            }
    };

    abstract fun format(system: String, messages: List<Pair<String, String>>): String
}
