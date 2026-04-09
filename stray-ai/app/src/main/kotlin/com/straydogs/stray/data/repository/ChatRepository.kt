package com.straydogs.stray.data.repository

import com.straydogs.stray.data.db.ConversationDao
import com.straydogs.stray.data.db.ConversationEntity
import com.straydogs.stray.data.db.MessageDao
import com.straydogs.stray.data.db.MessageEntity
import com.straydogs.stray.data.model.Conversation
import com.straydogs.stray.data.model.Message
import com.straydogs.stray.data.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    fun getAllConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getConversation(id: String): Conversation? =
        conversationDao.getConversationById(id)?.toDomain()

    suspend fun createConversation(
        modelId: String,
        systemPrompt: String = Conversation.DEFAULT_SYSTEM_PROMPT
    ): Conversation {
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            createdAt = now,
            updatedAt = now,
            modelId = modelId,
            systemPrompt = systemPrompt
        )
        conversationDao.insertConversation(conversation.toEntity())
        return conversation
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        val entity = conversationDao.getConversationById(id) ?: return
        conversationDao.updateConversation(entity.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(id: String) {
        conversationDao.deleteConversationById(id)
    }

    fun getMessages(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getMessagesSync(conversationId: String): List<Message> =
        messageDao.getMessagesForConversationSync(conversationId).map { it.toDomain() }

    suspend fun addMessage(
        conversationId: String,
        role: MessageRole,
        content: String
    ): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(message.toEntity())
        conversationDao.getConversationById(conversationId)?.let { conv ->
            conversationDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }
        return message
    }

    suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(message.toEntity())
    }

    suspend fun clearConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
    }

    // Mappers
    private fun ConversationEntity.toDomain() = Conversation(
        id = id, title = title, createdAt = createdAt, updatedAt = updatedAt,
        modelId = modelId, systemPrompt = systemPrompt
    )

    private fun Conversation.toEntity() = ConversationEntity(
        id = id, title = title, createdAt = createdAt, updatedAt = updatedAt,
        modelId = modelId, systemPrompt = systemPrompt
    )

    private fun MessageEntity.toDomain() = Message(
        id = id, conversationId = conversationId,
        role = MessageRole.from(role), content = content,
        timestamp = timestamp, tokenCount = tokenCount
    )

    private fun Message.toEntity() = MessageEntity(
        id = id, conversationId = conversationId,
        role = role.value, content = content,
        timestamp = timestamp, tokenCount = tokenCount
    )
}
