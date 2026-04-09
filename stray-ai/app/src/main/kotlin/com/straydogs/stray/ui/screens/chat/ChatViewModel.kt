package com.straydogs.stray.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.straydogs.stray.data.local.LlamaEngine
import com.straydogs.stray.data.local.PromptTemplate
import com.straydogs.stray.data.model.Conversation
import com.straydogs.stray.data.model.InferenceMode
import com.straydogs.stray.data.model.Message
import com.straydogs.stray.data.model.MessageRole
import com.straydogs.stray.data.remote.HFInferenceService
import com.straydogs.stray.data.repository.ChatRepository
import com.straydogs.stray.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val streamingMessageId: String? = null,
    val error: String? = null,
    val inferenceMode: InferenceMode = InferenceMode.LOCAL
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val llamaEngine: LlamaEngine,
    private val hfInferenceService: HFInferenceService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            val conversation = chatRepository.getConversation(conversationId)
            _uiState.update { it.copy(conversation = conversation) }
        }

        viewModelScope.launch {
            chatRepository.getMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        viewModelScope.launch {
            modelRepository.inferenceMode.collect { mode ->
                _uiState.update { it.copy(inferenceMode = mode) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val input = _uiState.value.inputText.trim()
        if (input.isEmpty() || _uiState.value.isGenerating) return

        val conversation = _uiState.value.conversation ?: return

        _uiState.update { it.copy(inputText = "", isGenerating = true, error = null) }

        generationJob = viewModelScope.launch {
            // Save user message
            chatRepository.addMessage(conversation.id, MessageRole.USER, input)

            // Update title after first message
            val messages = chatRepository.getMessagesSync(conversation.id)
            if (messages.size == 1) {
                val title = input.take(40).trim()
                chatRepository.updateConversationTitle(conversation.id, title)
            }

            when (_uiState.value.inferenceMode) {
                InferenceMode.LOCAL -> generateLocal(conversation, messages + listOf(
                    Message(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversation.id,
                        role = MessageRole.USER,
                        content = input,
                        timestamp = System.currentTimeMillis()
                    )
                ))
                InferenceMode.REMOTE -> generateRemote(conversation, messages)
            }
        }
    }

    private suspend fun generateLocal(conversation: Conversation, messages: List<Message>) {
        val streamingId = UUID.randomUUID().toString()
        _uiState.update { it.copy(streamingMessageId = streamingId) }

        val messagePairs = messages.map { it.role.value to it.content }
        val prompt = llamaEngine.formatChatPrompt(
            systemPrompt = conversation.systemPrompt,
            messages = messagePairs,
            template = PromptTemplate.CHATML
        )

        val maxTokens = modelRepository.maxTokens.first()
        val temperature = modelRepository.temperature.first()
        val topP = modelRepository.topP.first()

        val responseBuilder = StringBuilder()
        var savedMessageId: String? = null

        llamaEngine.generateStream(prompt, maxTokens, temperature, topP)
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isGenerating = false, streamingMessageId = null) }
            }
            .onCompletion {
                if (savedMessageId == null && responseBuilder.isNotEmpty()) {
                    chatRepository.addMessage(conversation.id, MessageRole.ASSISTANT, responseBuilder.toString())
                }
                _uiState.update { it.copy(isGenerating = false, streamingMessageId = null) }
            }
            .collect { token ->
                responseBuilder.append(token)
                // Update streaming message in UI state
                val streamMsg = Message(
                    id = streamingId,
                    conversationId = conversation.id,
                    role = MessageRole.ASSISTANT,
                    content = responseBuilder.toString(),
                    timestamp = System.currentTimeMillis(),
                    isStreaming = true
                )
                _uiState.update { state ->
                    val updatedMessages = state.messages.filter { it.id != streamingId } + streamMsg
                    state.copy(messages = updatedMessages)
                }
            }

        if (responseBuilder.isNotEmpty()) {
            chatRepository.addMessage(conversation.id, MessageRole.ASSISTANT, responseBuilder.toString())
        }
    }

    private suspend fun generateRemote(conversation: Conversation, messages: List<Message>) {
        val apiToken = modelRepository.hfApiToken.first()
        val remoteModelId = modelRepository.remoteModelId.first()
        val maxTokens = modelRepository.maxTokens.first()
        val temperature = modelRepository.temperature.first()
        val topP = modelRepository.topP.first()

        hfInferenceService.chat(
            modelId = remoteModelId,
            apiToken = apiToken,
            messages = messages,
            systemPrompt = conversation.systemPrompt,
            maxTokens = maxTokens,
            temperature = temperature,
            topP = topP
        ).collect { result ->
            result.onSuccess { content ->
                chatRepository.addMessage(conversation.id, MessageRole.ASSISTANT, content)
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isGenerating = false, streamingMessageId = null) }
        }
    }

    fun stopGeneration() {
        llamaEngine.abortGeneration()
        generationJob?.cancel()
        _uiState.update { it.copy(isGenerating = false, streamingMessageId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
