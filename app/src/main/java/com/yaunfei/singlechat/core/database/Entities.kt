package com.yaunfei.singlechat.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val peerId: String?,
    val isGroup: Boolean,
    val isPinned: Boolean,
    val unreadCount: Int,
    val lastMessage: String,
    val lastMessageAt: Long,
    val updatedAt: Long,
    val avatarSeed: String,
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId", "createdAt"])],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val type: String,
    val status: String,
    val createdAt: Long,
    /** 腾讯云 IM 的 msgID，本地发送与漫游消息用它去重。 */
    val remoteId: String = "",
)
