package com.yaunfei.singlechat.core.chat

import com.yaunfei.singlechat.core.model.Contact
import com.yaunfei.singlechat.core.model.Conversation
import com.yaunfei.singlechat.core.model.LocalUser
import com.yaunfei.singlechat.core.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

enum class ChatConnectionState {
    OFFLINE,
    CONNECTING,
    ONLINE,
    FAILED,
}

/**
 * 消息能力的唯一边界。
 *
 * M0 由 [LocalChatEngine] 实现，M1 换成腾讯云 IM 实现时，UI 与 ViewModel 不需要改动。
 */
interface ChatEngine {

    fun observeConversations(): Flow<List<Conversation>>

    fun observeConversation(conversationId: String): Flow<Conversation?>

    fun observeMessages(conversationId: String): Flow<List<Message>>

    fun observeTyping(conversationId: String): Flow<Boolean>

    suspend fun ensureSeeded()

    suspend fun sendText(conversationId: String, text: String)

    suspend fun retry(messageId: String)

    suspend fun markRead(conversationId: String)

    fun setActiveConversation(conversationId: String?)

    suspend fun createConversation(contact: Contact): String

    suspend fun deleteConversation(conversationId: String)

    suspend fun availableContacts(): List<Contact>

    fun observeConnectionState(): Flow<ChatConnectionState> = flowOf(ChatConnectionState.ONLINE)

    /** 连接失败的详细信息（含 IM 错误码），便于手机端排查。 */
    fun observeConnectionDetail(): Flow<String?> = flowOf(null)

    /** 最近一次发送失败的详细信息（含 IM 错误码）。 */
    fun observeLastSendError(): Flow<String?> = flowOf(null)

    fun currentUserId(): String? = LocalUser.ID

    suspend fun signOut() = Unit
}
