package com.yaunfei.singlechat.core.database

import com.yaunfei.singlechat.core.model.Conversation
import com.yaunfei.singlechat.core.model.LocalUser
import com.yaunfei.singlechat.core.model.Message
import com.yaunfei.singlechat.core.model.MessageStatus
import com.yaunfei.singlechat.core.model.MessageType

fun ConversationEntity.toModel(): Conversation = Conversation(
    id = id,
    title = title,
    peerId = peerId,
    isGroup = isGroup,
    isPinned = isPinned,
    unreadCount = unreadCount,
    lastMessage = lastMessage,
    lastMessageAt = lastMessageAt,
    updatedAt = updatedAt,
    avatarSeed = avatarSeed,
)

fun MessageEntity.toModel(selfId: String = LocalUser.ID): Message = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    text = text,
    type = enumOrDefault(type, MessageType.TEXT),
    status = enumOrDefault(status, MessageStatus.SENT),
    createdAt = createdAt,
    isMine = senderId == selfId,
)

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(default)
