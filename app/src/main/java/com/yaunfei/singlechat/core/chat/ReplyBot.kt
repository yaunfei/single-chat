package com.yaunfei.singlechat.core.chat

/**
 * 本地演示用的人设回复。真实通道接入后由对方设备或服务端产生消息。
 */
object ReplyBot {

    data class GroupReply(
        val senderId: String,
        val senderName: String,
        val text: String,
    )

    private val xiaomanReplies = listOf(
        "嗯，我在。要不要先说说今天最让你分心的那件事？",
        "听上去你已经想得很清楚了，只是还需要一点确认。",
        "先别急着给答案。如果只做一件小事，你会选什么？",
        "这句话挺重要的，我帮你记下来。",
        "辛苦啦。今天有没有哪一刻，让你觉得还挺值得的？",
    )

    private val linyiReplies = listOf(
        "行啊，周六见。这次别再迟到半小时了。",
        "可以，老地方。到了先点两杯冰美式。",
        "我最近也忙，等忙完这阵子一起吃个饭。",
        "收到。你发我的那个东西我晚上看。",
    )

    private val aheReplies = listOf(
        "路线我看了，难度不大，带够水就行。",
        "周六早上会冷，记得穿件外套。",
        "我新买了登山杖，到时候试试。",
        "下山去吃那家面吧，上次那家不错。",
    )

    private val genericReplies = listOf(
        "收到，稍等我回复你。",
        "哈哈，可以。",
        "听起来不错。",
        "我也正想说这个。",
    )

    fun reply(peerId: String?, input: String): String {
        val pool = when (peerId) {
            DemoContent.XIAOMAN_ID -> xiaomanReplies
            DemoContent.LINYI_ID -> linyiReplies
            DemoContent.AHE_ID -> aheReplies
            else -> genericReplies
        }
        val index = Math.floorMod(input.hashCode() + (peerId?.hashCode() ?: 0), pool.size)
        return pool[index]
    }

    fun groupReply(input: String): GroupReply {
        val replies = listOf(
            GroupReply(DemoContent.AHE_ID, "阿禾", "收到，我背炉子。"),
            GroupReply(DemoContent.LINYI_ID, "林一", "那我带咖啡豆，手冲。"),
            GroupReply(DemoContent.QIUQIU_ID, "球球", "我可以负责拍照，顺便当半天后勤。"),
            GroupReply(DemoContent.AHE_ID, "阿禾", "那就这么定了，周六见。"),
        )
        return replies[Math.floorMod(input.hashCode(), replies.size)]
    }
}
