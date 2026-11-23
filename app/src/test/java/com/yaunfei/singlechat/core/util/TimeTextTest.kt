package com.yaunfei.singlechat.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeTextTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-11-23T04:00:00Z") // 2026-11-23 12:00 +08:00

    private fun timestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `today uses clock time`() {
        val result = formatConversationTime(
            timestamp = timestamp(2026, 11, 23, 9, 5),
            now = now,
            zone = zone,
        )
        assertEquals("09:05", result)
    }

    @Test
    fun `yesterday uses label`() {
        val result = formatConversationTime(
            timestamp = timestamp(2026, 11, 22, 23, 59),
            now = now,
            zone = zone,
        )
        assertEquals("昨天", result)
    }

    @Test
    fun `within a week uses weekday`() {
        val result = formatConversationTime(
            timestamp = timestamp(2026, 11, 20, 18, 30),
            now = now,
            zone = zone,
        )
        assertEquals("周五", result)
    }

    @Test
    fun `older than a week uses month and day`() {
        val result = formatConversationTime(
            timestamp = timestamp(2026, 10, 28, 8, 0),
            now = now,
            zone = zone,
        )
        assertEquals("10月28日", result)
    }

    @Test
    fun `date divider distinguishes today yesterday and older`() {
        assertEquals("今天", formatDateDivider(timestamp(2026, 11, 23, 0, 1), now, zone))
        assertEquals("昨天", formatDateDivider(timestamp(2026, 11, 22, 23, 59), now, zone))
        assertEquals("11月20日 周五", formatDateDivider(timestamp(2026, 11, 20, 12, 0), now, zone))
    }

    @Test
    fun `same day ignores time`() {
        assertTrue(isSameDay(timestamp(2026, 11, 23, 0, 0), timestamp(2026, 11, 23, 23, 59), zone))
        assertFalse(isSameDay(timestamp(2026, 11, 22, 23, 59), timestamp(2026, 11, 23, 0, 1), zone))
    }
}
