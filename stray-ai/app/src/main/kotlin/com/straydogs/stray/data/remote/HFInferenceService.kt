package com.straydogs.stray.data.remote

import com.straydogs.stray.data.model.Message
import com.straydogs.stray.data.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HFInferenceService @Inject constructor(
    private val api: HuggingFaceApi
) {

    fun chat(
        modelId: String,
        apiToken: String,
        messages: List<Message>,
        systemPrompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.8f,
        topP: Float = 0.9f
    ): Flow<Result<String>> = flow {
        val hfMessages = buildList {
            add(HFChatMessage(role = "system", content = systemPrompt))
            addAll(messages.map { msg ->
                HFChatMessage(
                    role = msg.role.value,
                    content = msg.content
                )
            })
        }

        val request = HFChatRequest(
            model = modelId,
            messages = hfMessages,
            maxTokens = maxTokens,
            temperature = temperature,
            topP = topP
        )

        val response = api.chatCompletion(
            modelId = modelId,
            authHeader = "Bearer $apiToken",
            request = request
        )

        if (response.isSuccessful) {
            val body = response.body()
            val content = body?.choices?.firstOrNull()?.message?.content ?: ""
            emit(Result.success(content))
        } else {
            // Fallback to text generation endpoint
            val prompt = buildTextPrompt(systemPrompt, messages)
            val textRequest = HFTextGenRequest(
                inputs = prompt,
                parameters = HFParameters(
                    maxNewTokens = maxTokens,
                    temperature = temperature,
                    topP = topP
                )
            )
            val textResponse = api.textGeneration(
                modelId = modelId,
                authHeader = "Bearer $apiToken",
                request = textRequest
            )
            if (textResponse.isSuccessful) {
                val text = textResponse.body()?.generatedText ?: ""
                emit(Result.success(text))
            } else {
                emit(Result.failure(Exception("HF API error: ${response.code()} ${response.message()}")))
            }
        }
    }

    private fun buildTextPrompt(systemPrompt: String, messages: List<Message>): String =
        buildString {
            append("<|im_start|>system\n$systemPrompt<|im_end|>\n")
            for (msg in messages) {
                append("<|im_start|>${msg.role.value}\n${msg.content}<|im_end|>\n")
            }
            append("<|im_start|>assistant\n")
        }
}
