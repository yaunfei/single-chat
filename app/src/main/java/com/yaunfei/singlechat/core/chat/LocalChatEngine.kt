package com.yaunfei.singlechat.core.chat

import androidx.room.withTransaction
import com.yaunfei.singlechat.core.database.AppDatabase
import com.yaunfei.singlechat.core.database.ConversationDao
import com.yaunfei.singlechat.core.database.ConversationEntity
import com.yaunfei.singlechat.core.database.MessageDao
import com.yaunfei.singlechat.core.database.MessageEntity
import com.yaunfei.singlechat.core.database.toModel
import com.yaunfei.singlechat.core.di.ApplicationScope
import com.yaunfei.singlechat.core.model.Contact
import com.yaunfei.singlechat.core.model.Conversation
import com.yaunfei.singlechat.core.model.LocalUser
import com.yaunfei.singlechat.core.model.Message
import com.yaunfei.singlechat.core.model.MessageStatus
import com.yaunfei.singlechat.core.model.MessageType
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class LocalChatEngine @Inject constructor(
    private val database: AppDatabase,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    @ApplicationScope private val scope: CoroutineScope,
) : ChatEngine {

    private val typingConversation = MutableStateFlow<String?>(null)
    private val activeConversation = AtomicReference<String?>(null)
    private val seedMutex = Mutex()

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeConversation(conversationId: String): Flow<Conversation?> =
        conversationDao.observeById(conversationId).map { it?.toModel() }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeForConversation(conversationId).map { list -> list.map { it.toModel() } }

    override fun observeTyping(conversationId: String): Flow<Boolean> =
        typingConversation.map { it == conversationId }

    override suspend fun ensureSeeded() = seedMutex.withLock {
        if (conversationDao.count() > 0) return@withLock
        val seeds = DemoContent.seed(System.currentTimeMillis())
        database.withTransaction {
            seeds.forEach { seed ->
                conversationDao.upsert(seed.conversation)
                messageDao.upsertAll(seed.messages)
            }
        }
    }

    override suspend fun sendText(conversationId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val conversation = conversationDao.findById(conversationId) ?: return
        val now = System.currentTimeMillis()
        val messageId = UUID.randomUUID().toString()
        val message = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderId = LocalUser.ID,
            senderName = LocalUser.NAME,
            text = clean,
            type = MessageType.TEXT.name,
            status = MessageStatus.SENDING.name,
            createdAt = now,
        )
        database.withTransaction {
            messageDao.upsert(message)
            conversationDao.updatePreview(conversationId, clean, now)
        }

        scope.launch {
            delay(SEND_DELAY_MS)
            if (clean == FAIL_TRIGGER) {
                messageDao.updateStatus(messageId, MessageStatus.FAILED.name)
                return@launch
            }
            messageDao.updateStatus(messageId, MessageStatus.SENT.name)
            deliverReply(conversation, clean)
        }
    }

    override suspend fun retry(messageId: String) {
        val message = messageDao.findById(messageId) ?: return
        if (message.status != MessageStatus.FAILED.name) return
        messageDao.updateStatus(messageId, MessageStatus.SENDING.name)
        scope.launch {
            delay(RETRY_DELAY_MS)
            messageDao.updateStatus(messageId, MessageStatus.SENT.name)
        }
    }

    override suspend fun markRead(conversationId: String) {
        conversationDao.clearUnread(conversationId)
    }

    override fun setActiveConversation(conversationId: String?) {
        activeConversation.set(conversationId)
        if (conversationId != null) {
            scope.launch { conversationDao.clearUnread(conversationId) }
        }
    }

    override suspend fun createConversation(contact: Contact): String {
        conversationDao.findByPeerId(contact.id)?.let { return it.id }
        val now = System.currentTimeMillis()
        val conversationId = "c_" + UUID.randomUUID().toString().take(8)
        database.withTransaction {
            conversationDao.upsert(
                ConversationEntity(
                    id = conversationId,
                    title = contact.name,
                    peerId = contact.id,
                    isGroup = false,
                    isPinned = false,
                    unreadCount = 0,
                    lastMessage = "开始聊天吧",
                    lastMessageAt = now,
                    updatedAt = now,
                    avatarSeed = contact.id,
                ),
            )
            messageDao.upsert(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = SYSTEM_SENDER_ID,
                    senderName = "系统",
                    text = "你们已经是好友，打个招呼吧",
                    type = MessageType.SYSTEM.name,
                    status = MessageStatus.SENT.name,
                    createdAt = now,
                ),
            )
        }
        return conversationId
    }

    override suspend fun deleteConversation(conversationId: String) {
        database.withTransaction {
            messageDao.deleteForConversation(conversationId)
            conversationDao.delete(conversationId)
        }
        if (activeConversation.get() == conversationId) {
            activeConversation.set(null)
        }
    }

    override suspend fun availableContacts(): List<Contact> = DemoContent.contacts

    private suspend fun deliverReply(conversation: ConversationEntity, userText: String) {
        typingConversation.value = conversation.id
        try {
            delay(REPLY_MIN_DELAY_MS + Math.floorMod(userText.hashCode(), REPLY_JITTER_MS.toInt()).toLong())
            val replyTime = System.currentTimeMillis()
            val reply = if (conversation.isGroup) {
                val groupReply = ReplyBot.groupReply(userText)
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversation.id,
                    senderId = groupReply.senderId,
                    senderName = groupReply.senderName,
                    text = groupReply.text,
                    type = MessageType.TEXT.name,
                    status = MessageStatus.SENT.name,
                    createdAt = replyTime,
                )
            } else {
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversation.id,
                    senderId = conversation.peerId ?: conversation.id,
                    senderName = conversation.title,
                    text = ReplyBot.reply(conversation.peerId, userText),
                    type = MessageType.TEXT.name,
                    status = MessageStatus.SENT.name,
                    createdAt = replyTime,
                )
            }
            val preview = if (conversation.isGroup) {
                "${reply.senderName}：${reply.text}"
            } else {
                reply.text
            }
            val unreadDelta = if (activeConversation.get() == conversation.id) 0 else 1
            database.withTransaction {
                messageDao.upsert(reply)
                conversationDao.applyIncoming(conversation.id, preview, replyTime, unreadDelta)
            }
        } finally {
            if (typingConversation.value == conversation.id) {
                typingConversation.value = null
            }
        }
    }

    private companion object {
        const val SEND_DELAY_MS = 280L
        const val RETRY_DELAY_MS = 320L
        const val REPLY_MIN_DELAY_MS = 850L
        const val REPLY_JITTER_MS = 700L
        const val FAIL_TRIGGER = "/fail"
        const val SYSTEM_SENDER_ID = "system"
    }
}
