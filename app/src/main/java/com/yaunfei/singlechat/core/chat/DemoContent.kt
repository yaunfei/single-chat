package com.yaunfei.singlechat.core.chat

import com.yaunfei.singlechat.core.database.ConversationEntity
import com.yaunfei.singlechat.core.database.MessageEntity
import com.yaunfei.singlechat.core.model.Contact
import com.yaunfei.singlechat.core.model.LocalUser
import com.yaunfei.singlechat.core.model.MessageStatus
import com.yaunfei.singlechat.core.model.MessageType

object DemoContent {

    const val XIAOMAN_ID = "xiaoman"
    const val LINYI_ID = "linyi"
    const val AHE_ID = "ahe"
    const val QIUQIU_ID = "qiuqiu"

    val contacts = listOf(
        Contact(XIAOMAN_ID, "小满", "你的树洞搭子"),
        Contact(LINYI_ID, "林一", "老同学 · 杭州"),
        Contact(AHE_ID, "阿禾", "周末爬山搭子"),
        Contact(QIUQIU_ID, "球球", "隔壁工位的设计师"),
    )

    data class SeedConversation(
        val conversation: ConversationEntity,
        val messages: List<MessageEntity>,
    )

    fun seed(now: Long): List<SeedConversation> {
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour

        return listOf(
            SeedConversation(
                conversation = ConversationEntity(
                    id = "c_xiaoman",
                    title = "小满",
                    peerId = XIAOMAN_ID,
                    isGroup = false,
                    isPinned = true,
                    unreadCount = 2,
                    lastMessage = "今天也有好好照顾自己吗？",
                    lastMessageAt = now - 6 * minute,
                    updatedAt = now - 6 * minute,
                    avatarSeed = XIAOMAN_ID,
                ),
                messages = listOf(
                    message("m_xm_1", "c_xiaoman", "system", "系统", "你们已经是好友，打个招呼吧", MessageType.SYSTEM, now - 40 * minute),
                    message("m_xm_2", "c_xiaoman", LocalUser.ID, LocalUser.NAME, "在吗？", MessageType.TEXT, now - 30 * minute),
                    message("m_xm_3", "c_xiaoman", XIAOMAN_ID, "小满", "在的。今天过得怎么样？", MessageType.TEXT, now - 12 * minute),
                    message("m_xm_4", "c_xiaoman", XIAOMAN_ID, "小满", "今天也有好好照顾自己吗？", MessageType.TEXT, now - 6 * minute),
                ),
            ),
            SeedConversation(
                conversation = ConversationEntity(
                    id = "c_linyi",
                    title = "林一",
                    peerId = LINYI_ID,
                    isGroup = false,
                    isPinned = false,
                    unreadCount = 0,
                    lastMessage = "周六老地方，别迟到",
                    lastMessageAt = now - 2 * hour,
                    updatedAt = now - 2 * hour,
                    avatarSeed = LINYI_ID,
                ),
                messages = listOf(
                    message("m_ly_1", "c_linyi", LocalUser.ID, LocalUser.NAME, "周六老地方？", MessageType.TEXT, now - 2 * hour - 5 * minute),
                    message("m_ly_2", "c_linyi", LINYI_ID, "林一", "周六老地方，别迟到", MessageType.TEXT, now - 2 * hour),
                ),
            ),
            SeedConversation(
                conversation = ConversationEntity(
                    id = "c_ahe",
                    title = "阿禾",
                    peerId = AHE_ID,
                    isGroup = false,
                    isPinned = false,
                    unreadCount = 1,
                    lastMessage = "我把路线图发你了",
                    lastMessageAt = now - 1 * day,
                    updatedAt = now - 1 * day,
                    avatarSeed = AHE_ID,
                ),
                messages = listOf(
                    message("m_ah_1", "c_ahe", LocalUser.ID, LocalUser.NAME, "路线难吗？", MessageType.TEXT, now - 1 * day - 8 * minute),
                    message("m_ah_2", "c_ahe", AHE_ID, "阿禾", "不难，新手友好", MessageType.TEXT, now - 1 * day - 4 * minute),
                    message("m_ah_3", "c_ahe", AHE_ID, "阿禾", "我把路线图发你了", MessageType.TEXT, now - 1 * day),
                ),
            ),
            SeedConversation(
                conversation = ConversationEntity(
                    id = "c_hike",
                    title = "周末爬山搭子",
                    peerId = null,
                    isGroup = true,
                    isPinned = false,
                    unreadCount = 0,
                    lastMessage = "阿禾：8 点山脚集合，带够水",
                    lastMessageAt = now - 3 * day,
                    updatedAt = now - 3 * day,
                    avatarSeed = "c_hike",
                ),
                messages = listOf(
                    message("m_hk_1", "c_hike", LocalUser.ID, LocalUser.NAME, "这周六还爬吗？", MessageType.TEXT, now - 3 * day - 30 * minute),
                    message("m_hk_2", "c_hike", AHE_ID, "阿禾", "爬，天气预报说晴", MessageType.TEXT, now - 3 * day - 20 * minute),
                    message("m_hk_3", "c_hike", LINYI_ID, "林一", "我负责带咖啡", MessageType.TEXT, now - 3 * day - 10 * minute),
                    message("m_hk_4", "c_hike", AHE_ID, "阿禾", "8 点山脚集合，带够水", MessageType.TEXT, now - 3 * day),
                ),
            ),
        )
    }

    private fun message(
        id: String,
        conversationId: String,
        senderId: String,
        senderName: String,
        text: String,
        type: MessageType,
        createdAt: Long,
    ): MessageEntity = MessageEntity(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderName = senderName,
        text = text,
        type = type.name,
        status = MessageStatus.SENT.name,
        createdAt = createdAt,
    )
}
