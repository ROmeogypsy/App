package com.straydogs.stray.ui.screens.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.straydogs.stray.data.model.Conversation
import com.straydogs.stray.data.repository.ChatRepository
import com.straydogs.stray.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsUiState(
    val conversations: List<Conversation> = emptyList(),
    val activeModelId: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                chatRepository.getAllConversations(),
                modelRepository.activeLocalModelId
            ) { conversations, modelId ->
                ConversationsUiState(conversations = conversations, activeModelId = modelId)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun createNewConversation(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val modelId = modelRepository.activeLocalModelId.first()
            val conversation = chatRepository.createConversation(modelId = modelId)
            onCreated(conversation.id)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
        }
    }
}
