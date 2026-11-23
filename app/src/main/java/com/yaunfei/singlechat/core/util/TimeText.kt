package com.yaunfei.singlechat.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ClockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
private val Weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

fun formatConversationTime(
    timestamp: Long,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val time = Instant.ofEpochMilli(timestamp).atZone(zone)
    val today = now.atZone(zone).toLocalDate()
    val date = time.toLocalDate()
    return when {
        date == today -> time.format(ClockFormatter)
        date == today.minusDays(1) -> "昨天"
        date.isAfter(today.minusDays(7)) -> Weekdays[date.dayOfWeek.value - 1]
        else -> time.format(DateFormatter)
    }
}

fun formatMessageTime(
    timestamp: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = Instant.ofEpochMilli(timestamp).atZone(zone).format(ClockFormatter)

fun formatDateDivider(
    timestamp: Long,
    now: Instant = Instant.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val time = Instant.ofEpochMilli(timestamp).atZone(zone)
    val today = now.atZone(zone).toLocalDate()
    val date = time.toLocalDate()
    return when {
        date == today -> "今天"
        date == today.minusDays(1) -> "昨天"
        date.year == today.year ->
            "${date.monthValue}月${date.dayOfMonth}日 ${Weekdays[date.dayOfWeek.value - 1]}"
        else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }
}

fun isSameDay(
    first: Long,
    second: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean {
    val a = Instant.ofEpochMilli(first).atZone(zone).toLocalDate()
    val b = Instant.ofEpochMilli(second).atZone(zone).toLocalDate()
    return a == b
}
