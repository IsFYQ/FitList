package com.example.healthcheckin.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DateTimeUtilTest {
    @Test fun parseFlexibleLocalDate_accepts_common_formats() {
        val expected = LocalDate.of(2026, 8, 14)
        assertEquals(expected, DateTimeUtil.parseFlexibleLocalDate("2026-08-14"))
        assertEquals(expected, DateTimeUtil.parseFlexibleLocalDate("2026/8/14"))
        assertEquals(expected, DateTimeUtil.parseFlexibleLocalDate("2026.08.14"))
        assertEquals(expected, DateTimeUtil.parseFlexibleLocalDate("20260814"))
        assertEquals(expected, DateTimeUtil.parseFlexibleLocalDate("2026年8月14日"))
        assertEquals(expected, DateTimeUtil.parseFlexibleLocalDate("8月14日", referenceYear = 2026))
        assertNull(DateTimeUtil.parseFlexibleLocalDate(""))
        assertNull(DateTimeUtil.parseFlexibleLocalDate("下周过期"))
    }
}
