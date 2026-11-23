package com.yaunfei.singlechat.core.model

object LocalUser {
    const val ID = "u_me"
    const val NAME = "我"
}

data class Session(
    val userId: String,
    val displayName: String,
    val phone: String,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class MessageType {
    TEXT,
    IMAGE,
    SYSTEM,
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED,
}

data class Conversation(
    val id: String,
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

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val type: MessageType,
    val status: MessageStatus,
    val createdAt: Long,
) {
    val isMine: Boolean get() = senderId == LocalUser.ID
}

data class Contact(
    val id: String,
    val name: String,
    val bio: String,
)
