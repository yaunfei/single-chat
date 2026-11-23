package com.yaunfei.singlechat.core.chat

import android.content.Context
import androidx.room.withTransaction
import com.tencent.imsdk.v2.V2TIMAdvancedMsgListener
import com.tencent.imsdk.v2.V2TIMCallback
import com.tencent.imsdk.v2.V2TIMConversation
import com.tencent.imsdk.v2.V2TIMConversationListener
import com.tencent.imsdk.v2.V2TIMConversationResult
import com.tencent.imsdk.v2.V2TIMManager
import com.tencent.imsdk.v2.V2TIMMessage
import com.tencent.imsdk.v2.V2TIMMessageManager
import com.tencent.imsdk.v2.V2TIMSDKConfig
import com.tencent.imsdk.v2.V2TIMSDKListener
import com.tencent.imsdk.v2.V2TIMSendCallback
import com.tencent.imsdk.v2.V2TIMValueCallback
import com.yaunfei.singlechat.core.database.AppDatabase
import com.yaunfei.singlechat.core.database.ConversationDao
import com.yaunfei.singlechat.core.database.ConversationEntity
import com.yaunfei.singlechat.core.database.MessageDao
import com.yaunfei.singlechat.core.database.MessageEntity
import com.yaunfei.singlechat.core.database.toModel
import com.yaunfei.singlechat.core.data.PreferencesRepository
import com.yaunfei.singlechat.core.di.ApplicationScope
import com.yaunfei.singlechat.core.model.Contact
import com.yaunfei.singlechat.core.model.Conversation
import com.yaunfei.singlechat.core.model.LocalUser
import com.yaunfei.singlechat.core.model.Message
import com.yaunfei.singlechat.core.model.MessageStatus
import com.yaunfei.singlechat.core.model.MessageType
import com.yaunfei.singlechat.core.network.BackendClient
import com.yaunfei.singlechat.core.network.UserSigTicket
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

private class ImException(val code: Int, message: String?) :
    RuntimeException(message ?: "IM error $code")

/**
 * 腾讯云 IM 引擎（M1）。
 *
 * 关键取舍：
 * - 登录态由 BFF 管理，客户端只持有短期 UserSig；
 * - Room 仍是 UI 的唯一数据源，IM 回调只负责把数据写进本地库；
 * - 本地乐观消息用 UUID 主键，收到 msgID 后再回填 remoteId 去重。
 */
@Singleton
class TencentChatEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val preferences: PreferencesRepository,
    private val backend: BackendClient,
    @ApplicationScope private val scope: CoroutineScope,
) : ChatEngine {

    private val connectionState = MutableStateFlow(ChatConnectionState.OFFLINE)
    private val selfUserId = MutableStateFlow<String?>(null)
    private var selfName: String = LocalUser.NAME
    private val activeConversation = AtomicReference<String?>(null)
    private val typingConversation = MutableStateFlow<String?>(null)

    private val bootstrapMutex = Mutex()
    private val sdkInitialized = AtomicBoolean(false)
    private val listenersRegistered = AtomicBoolean(false)
    @Volatile
    private var cachedTicket: UserSigTicket? = null

    override fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeConversation(conversationId: String): Flow<Conversation?> =
        conversationDao.observeById(conversationId).map { it?.toModel() }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageDao.observeForConversation(conversationId).map { list ->
            val me = selfUserId.value ?: LocalUser.ID
            list.map { it.toModel(me) }
        }

    override fun observeTyping(conversationId: String): Flow<Boolean> = flowOf(false)

    override fun observeConnectionState(): Flow<ChatConnectionState> = connectionState

    override fun currentUserId(): String? = selfUserId.value

    override suspend fun ensureSeeded() {
        bootstrap()
    }

    override suspend fun sendText(conversationId: String, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val conversation = conversationDao.findById(conversationId) ?: return
        val now = System.currentTimeMillis()
        val localId = UUID.randomUUID().toString()
        val entity = MessageEntity(
            id = localId,
            conversationId = conversationId,
            senderId = selfUserId.value ?: LocalUser.ID,
            senderName = selfName,
            text = clean,
            type = MessageType.TEXT.name,
            status = MessageStatus.SENDING.name,
            createdAt = now,
        )
        database.withTransaction {
            messageDao.upsert(entity)
            conversationDao.updatePreview(conversationId, clean, now)
        }

        scope.launch {
            try {
                val sent = sendImMessage(
                    text = clean,
                    receiverUserId = conversation.peerId.takeIf { !conversation.isGroup },
                    groupId = conversation.peerId.takeIf { conversation.isGroup },
                )
                val remoteId = sent.msgID.orEmpty()
                // 若高级消息监听先一步写入同一条消息，删掉重复行。
                if (remoteId.isNotBlank()) {
                    val duplicate = messageDao.findByRemoteId(remoteId)
                    if (duplicate != null && duplicate.id != localId) {
                        messageDao.deleteById(duplicate.id)
                    }
                }
                messageDao.updateRemoteIdAndStatus(localId, remoteId, MessageStatus.SENT.name)
            } catch (error: Exception) {
                messageDao.updateStatus(localId, MessageStatus.FAILED.name)
            }
        }
    }

    override suspend fun retry(messageId: String) {
        val message = messageDao.findById(messageId) ?: return
        if (message.status != MessageStatus.FAILED.name) return
        val conversation = conversationDao.findById(message.conversationId) ?: return
        messageDao.updateStatus(messageId, MessageStatus.SENDING.name)

        scope.launch {
            try {
                val sent = sendImMessage(
                    text = message.text,
                    receiverUserId = conversation.peerId.takeIf { !conversation.isGroup },
                    groupId = conversation.peerId.takeIf { conversation.isGroup },
                )
                val remoteId = sent.msgID.orEmpty()
                messageDao.updateRemoteIdAndStatus(messageId, remoteId, MessageStatus.SENT.name)
            } catch (error: Exception) {
                messageDao.updateStatus(messageId, MessageStatus.FAILED.name)
            }
        }
    }

    override suspend fun markRead(conversationId: String) {
        val conversation = conversationDao.findById(conversationId) ?: return
        conversationDao.clearUnread(conversationId)
        val peer = conversation.peerId ?: return
        runCatching {
            if (conversation.isGroup) {
                awaitCallback {
                    V2TIMManager.getMessageManager().markGroupMessageAsRead(peer, it)
                }
            } else {
                awaitCallback {
                    V2TIMManager.getMessageManager().markC2CMessageAsRead(peer, it)
                }
            }
        }
    }

    override fun setActiveConversation(conversationId: String?) {
        activeConversation.set(conversationId)
        if (conversationId != null) {
            scope.launch {
                conversationDao.clearUnread(conversationId)
                runCatching { loadRecentHistory(conversationId) }
            }
        }
    }

    override suspend fun createConversation(contact: Contact): String {
        val conversationId = "c2c_${contact.id}"
        conversationDao.findById(conversationId)?.let { return it.id }
        val now = System.currentTimeMillis()
        conversationDao.upsert(
            ConversationEntity(
                id = conversationId,
                title = contact.name.ifBlank { contact.id },
                peerId = contact.id,
                isGroup = false,
                isPinned = false,
                unreadCount = 0,
                lastMessage = "",
                lastMessageAt = now,
                updatedAt = now,
                avatarSeed = contact.id,
            ),
        )
        return conversationId
    }

    override suspend fun deleteConversation(conversationId: String) {
        runCatching {
            awaitCallback {
                V2TIMManager.getConversationManager().deleteConversation(conversationId, it)
            }
        }
        database.withTransaction {
            messageDao.deleteForConversation(conversationId)
            conversationDao.delete(conversationId)
        }
        if (activeConversation.get() == conversationId) {
            activeConversation.set(null)
        }
    }

    override suspend fun availableContacts(): List<Contact> =
        conversationDao.listAll()
            .asSequence()
            .filter { !it.isGroup && !it.peerId.isNullOrBlank() }
            .map { Contact(id = it.peerId!!, name = it.title, bio = "继续和 TA 聊天") }
            .distinctBy { it.id }
            .toList()

    override suspend fun signOut() {
        if (!V2TIMManager.getInstance().getLoginUser().isNullOrBlank()) {
            V2TIMManager.getInstance().logout(
                object : V2TIMCallback {
                    override fun onSuccess() = Unit
                    override fun onError(code: Int, desc: String?) = Unit
                },
            )
        }
        connectionState.value = ChatConnectionState.OFFLINE
        selfUserId.value = null
        activeConversation.set(null)
    }

    // ---------------------------------------------------------------------
    // 启动与连接
    // ---------------------------------------------------------------------

    private suspend fun bootstrap() = bootstrapMutex.withLock {
        val session = preferences.currentSession()
        if (session == null || session.token.isBlank()) {
            connectionState.value = ChatConnectionState.OFFLINE
            return@withLock
        }

        connectionState.value = ChatConnectionState.CONNECTING
        selfName = session.displayName

        try {
            val ticket = cachedTicket
                ?.takeIf { it.expireAt == 0L || it.expireAt > System.currentTimeMillis() / 1000 + 60 }
                ?: backend.fetchUserSig(session.token).also { cachedTicket = it }

            ensureSdkInitialized(ticket.sdkAppId)
            val loginUser = V2TIMManager.getInstance().getLoginUser()
            if (loginUser != ticket.userId) {
                if (!loginUser.isNullOrBlank()) {
                    runCatching { awaitCallback { V2TIMManager.getInstance().logout(it) } }
                }
                awaitCallback { V2TIMManager.getInstance().login(ticket.userId, ticket.userSig, it) }
            }

            // 首次进入 M1 或换账号时，清掉 M0 演示数据 / 上一个账号的数据。
            val previousImUserId = preferences.currentImUserId()
            if (previousImUserId != ticket.userId) {
                database.withTransaction {
                    messageDao.deleteAll()
                    conversationDao.deleteAll()
                }
                preferences.saveImUserId(ticket.userId)
            }

            selfUserId.value = ticket.userId
            registerListeners()
            connectionState.value = ChatConnectionState.ONLINE
            runCatching { syncConversations() }
        } catch (error: Exception) {
            connectionState.value = ChatConnectionState.FAILED
        }
    }

    private fun ensureSdkInitialized(sdkAppId: Int) {
        if (sdkInitialized.get()) return
        val config = V2TIMSDKConfig().apply {
            logLevel = V2TIMSDKConfig.V2TIM_LOG_INFO
        }
        // initSDK 返回 false 时通常表示已初始化过，继续 login 即可。
        V2TIMManager.getInstance().initSDK(context, sdkAppId, config, sdkListener)
        sdkInitialized.set(true)
    }

    private fun registerListeners() {
        if (!listenersRegistered.compareAndSet(false, true)) return
        V2TIMManager.getMessageManager().addAdvancedMsgListener(messageListener)
        V2TIMManager.getConversationManager().addConversationListener(conversationListener)
    }

    private val sdkListener = object : V2TIMSDKListener() {
        override fun onConnecting() {
            connectionState.value = ChatConnectionState.CONNECTING
        }

        override fun onConnectSuccess() {
            connectionState.value = ChatConnectionState.ONLINE
            scope.launch { runCatching { syncConversations() } }
        }

        override fun onConnectFailed(code: Int, error: String?) {
            connectionState.value = ChatConnectionState.FAILED
        }

        override fun onKickedOffline() {
            connectionState.value = ChatConnectionState.OFFLINE
        }

        override fun onUserSigExpired() {
            cachedTicket = null
            connectionState.value = ChatConnectionState.FAILED
            scope.launch {
                runCatching {
                    awaitCallback { V2TIMManager.getInstance().logout(it) }
                    bootstrap()
                }
            }
        }
    }

    private val messageListener = object : V2TIMAdvancedMsgListener() {
        override fun onRecvNewMessage(message: V2TIMMessage) {
            scope.launch { runCatching { persistIncoming(message) } }
        }
    }

    private val conversationListener = object : V2TIMConversationListener() {
        override fun onNewConversation(conversationList: MutableList<V2TIMConversation>?) {
            scope.launch { runCatching { conversationList?.forEach { upsertConversation(it) } } }
        }

        override fun onConversationChanged(conversationList: MutableList<V2TIMConversation>?) {
            scope.launch { runCatching { conversationList?.forEach { upsertConversation(it) } } }
        }

        override fun onConversationDeleted(conversationIDList: MutableList<String>?) {
            scope.launch {
                conversationIDList?.forEach { conversationId ->
                    runCatching {
                        database.withTransaction {
                            messageDao.deleteForConversation(conversationId)
                            conversationDao.delete(conversationId)
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // IM 数据 -> Room
    // ---------------------------------------------------------------------

    private suspend fun syncConversations() {
        val result = awaitValue<V2TIMConversationResult> { callback ->
            V2TIMManager.getConversationManager()
                .getConversationList(0L, CONVERSATION_PAGE_SIZE, callback)
        }
        result.conversationList?.forEach { upsertConversation(it) }
    }

    private suspend fun upsertConversation(conversation: V2TIMConversation) {
        val id = conversation.conversationID ?: return
        val isGroup = conversation.type == V2TIMConversation.V2TIM_GROUP
        val peerId = if (isGroup) conversation.groupID else conversation.userID
        val last = conversation.lastMessage
        val imPreview = previewOf(last) ?: ""
        val imAt = last?.timestamp?.times(1000L) ?: 0L
        val existing = conversationDao.findById(id)

        // 本地乐观发送的预览可能比 IM 快照新，保留较新的那一条。
        val keepLocal = existing != null && existing.lastMessageAt > imAt
        val title = conversation.showName?.takeIf { it.isNotBlank() }
            ?: existing?.title
            ?: peerId.orEmpty()

        conversationDao.upsert(
            ConversationEntity(
                id = id,
                title = title,
                peerId = peerId,
                isGroup = isGroup,
                isPinned = conversation.isPinned,
                unreadCount = conversation.unreadCount,
                lastMessage = if (keepLocal) existing!!.lastMessage else imPreview,
                lastMessageAt = if (keepLocal) existing!!.lastMessageAt else imAt,
                updatedAt = maxOf(imAt, existing?.updatedAt ?: 0L),
                avatarSeed = peerId ?: id,
            ),
        )
    }

    private suspend fun persistIncoming(message: V2TIMMessage) {
        val remoteId = message.msgID ?: return
        if (messageDao.findByRemoteId(remoteId) != null) return

        val groupId = message.groupID?.takeIf { it.isNotBlank() }
        val peer = message.userID?.takeIf { it.isNotBlank() }
        val conversationId = when {
            groupId != null -> "group_$groupId"
            peer != null -> "c2c_$peer"
            else -> return
        }
        val isGroup = groupId != null
        val conversation = conversationDao.findById(conversationId) ?: run {
            val now = System.currentTimeMillis()
            ConversationEntity(
                id = conversationId,
                title = if (isGroup) "群聊 $groupId" else peer.orEmpty(),
                peerId = if (isGroup) groupId else peer,
                isGroup = isGroup,
                isPinned = false,
                unreadCount = 0,
                lastMessage = "",
                lastMessageAt = 0L,
                updatedAt = now,
                avatarSeed = if (isGroup) groupId ?: conversationId else peer ?: conversationId,
            ).also { conversationDao.upsert(it) }
        }

        writeMessage(message, conversation, unreadDelta = if (message.isSelf) 0 else 1)
    }

    private suspend fun loadRecentHistory(conversationId: String) {
        val conversation = conversationDao.findById(conversationId) ?: return
        val peer = conversation.peerId ?: return
        val history = awaitValue<List<V2TIMMessage>> { callback ->
            if (conversation.isGroup) {
                V2TIMManager.getMessageManager()
                    .getGroupHistoryMessageList(peer, HISTORY_PAGE_SIZE, null, callback)
            } else {
                V2TIMManager.getMessageManager()
                    .getC2CHistoryMessageList(peer, HISTORY_PAGE_SIZE, null, callback)
            }
        }
        history.forEach { message ->
            val remoteId = message.msgID ?: return@forEach
            if (messageDao.findByRemoteId(remoteId) != null) return@forEach
            writeMessage(message, conversation, unreadDelta = 0)
        }
    }

    private suspend fun writeMessage(
        message: V2TIMMessage,
        conversation: ConversationEntity,
        unreadDelta: Int,
    ) {
        val remoteId = message.msgID ?: return
        if (messageDao.findByRemoteId(remoteId) != null) return

        val mine = message.isSelf
        val senderId = when {
            mine -> selfUserId.value ?: LocalUser.ID
            conversation.isGroup -> message.sender?.takeIf { it.isNotBlank() } ?: "unknown"
            else -> message.userID?.takeIf { it.isNotBlank() } ?: "unknown"
        }
        val senderName = when {
            mine -> selfName
            !message.nickName.isNullOrBlank() -> message.nickName!!
            else -> senderId
        }
        val createdAt = message.timestamp * 1000L
        val text = message.textElem?.text ?: "[非文本消息]"
        val entity = MessageEntity(
            id = "im_$remoteId",
            conversationId = conversation.id,
            senderId = senderId,
            senderName = senderName,
            text = text,
            type = MessageType.TEXT.name,
            status = MessageStatus.SENT.name,
            createdAt = createdAt,
            remoteId = remoteId,
        )
        val preview = if (conversation.isGroup) "$senderName：$text" else text

        database.withTransaction {
            messageDao.upsert(entity)
            val active = activeConversation.get() == conversation.id
            val unreadDeltaForDb = if (active) 0 else unreadDelta
            when {
                // 只有更新的一条才覆盖会话预览，避免旧消息盖掉本地刚发出的新消息。
                createdAt >= conversation.lastMessageAt ->
                    conversationDao.applyIncoming(conversation.id, preview, createdAt, unreadDeltaForDb)

                unreadDeltaForDb > 0 -> conversationDao.applyIncoming(
                    id = conversation.id,
                    preview = conversation.lastMessage,
                    at = conversation.lastMessageAt,
                    unreadDelta = unreadDeltaForDb,
                )
            }
        }
    }

    private fun previewOf(message: V2TIMMessage?): String? {
        if (message == null) return null
        return when (message.elemType) {
            V2TIMMessage.V2TIM_ELEM_TYPE_TEXT ->
                message.textElem?.text?.takeIf { it.isNotBlank() } ?: "[文本消息]"
            else -> "[非文本消息]"
        }
    }

    // ---------------------------------------------------------------------
    // SDK 回调 -> 挂起函数
    // ---------------------------------------------------------------------

    private suspend fun sendImMessage(
        text: String,
        receiverUserId: String?,
        groupId: String?,
    ): V2TIMMessage = withTimeout(IM_TIMEOUT_MS) {
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val manager: V2TIMMessageManager = V2TIMManager.getMessageManager()
                val message = manager.createTextMessage(text)
                manager.sendMessage(
                    message,
                    receiverUserId,
                    groupId,
                    V2TIMMessage.V2TIM_PRIORITY_DEFAULT,
                    false,
                    null,
                    object : V2TIMSendCallback<V2TIMMessage> {
                        override fun onProgress(progress: Int) = Unit

                        override fun onSuccess(value: V2TIMMessage) {
                            if (continuation.isActive) continuation.resume(value)
                        }

                        override fun onError(code: Int, desc: String?) {
                            if (continuation.isActive) {
                                continuation.resumeWith(Result.failure(ImException(code, desc)))
                            }
                        }
                    },
                )
            }
        }
    }

    private suspend fun <T> awaitValue(
        block: (V2TIMValueCallback<T>) -> Unit,
    ): T = withTimeout(IM_TIMEOUT_MS) {
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                block(
                    object : V2TIMValueCallback<T> {
                        override fun onSuccess(value: T) {
                            if (continuation.isActive) continuation.resume(value)
                        }

                        override fun onError(code: Int, desc: String?) {
                            if (continuation.isActive) {
                                continuation.resumeWith(Result.failure(ImException(code, desc)))
                            }
                        }
                    },
                )
            }
        }
    }

    private suspend fun awaitCallback(block: (V2TIMCallback) -> Unit) =
        withTimeout(IM_TIMEOUT_MS) {
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    block(
                        object : V2TIMCallback {
                            override fun onSuccess() {
                                if (continuation.isActive) continuation.resume(Unit)
                            }

                            override fun onError(code: Int, desc: String?) {
                                if (continuation.isActive) {
                                    continuation.resumeWith(Result.failure(ImException(code, desc)))
                                }
                            }
                        },
                    )
                }
            }
        }

    private companion object {
        const val CONVERSATION_PAGE_SIZE = 100
        const val HISTORY_PAGE_SIZE = 50
        const val IM_TIMEOUT_MS = 20_000L
    }
}
