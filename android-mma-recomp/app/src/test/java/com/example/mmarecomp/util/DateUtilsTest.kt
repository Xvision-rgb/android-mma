package com.example.mmarecomp.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `string formats a date as yyyy-MM-dd`() {
        assertEquals("2026-03-05", DateUtils.string(LocalDate.of(2026, 3, 5)))
    }

    @Test
    fun `date parses a valid yyyy-MM-dd string`() {
        assertEquals(LocalDate.of(2026, 3, 5), DateUtils.date("2026-03-05"))
    }

    @Test
    fun `date returns null for an unparseable string`() {
        assertNull(DateUtils.date("not-a-date"))
        assertNull(DateUtils.date(""))
    }

    @Test
    fun `weekdayIso maps Monday through Sunday to 1 through 7`() {
        // 2024-01-01 is a known Monday.
        assertEquals(1, DateUtils.weekdayIso("2024-01-01"))
        assertEquals(2, DateUtils.weekdayIso("2024-01-02"))
        assertEquals(7, DateUtils.weekdayIso("2024-01-07"))
    }

    @Test
    fun `weekdayIso falls back to Monday for an unparseable date`() {
        assertEquals(1, DateUtils.weekdayIso("garbage"))
    }

    @Test
    fun `startOfWeek returns the Monday of the same week regardless of which day is passed`() {
        val monday = LocalDate.of(2024, 1, 1)
        val wednesday = LocalDate.of(2024, 1, 3)
        val sunday = LocalDate.of(2024, 1, 7)

        assertEquals("2024-01-01", DateUtils.startOfWeek(monday))
        assertEquals("2024-01-01", DateUtils.startOfWeek(wednesday))
        assertEquals("2024-01-01", DateUtils.startOfWeek(sunday))
    }

    @Test
    fun `today and daysAgo(0) agree, and round-trip through date()`() {
        assertEquals(DateUtils.today(), DateUtils.daysAgo(0))
        assertEquals(LocalDate.now(), DateUtils.date(DateUtils.today()))
    }
}
