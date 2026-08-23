package com.example.mmarecomp.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 3, 15)

    @Test
    fun `no activity means a zero streak`() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun `activity every day up to and including today counts each day`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, StreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `no activity yet today does not reset the streak, it counts from yesterday`() {
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))
        assertEquals(3, StreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `a gap stops the streak count`() {
        val dates = setOf(today, today.minusDays(1), today.minusDays(3)) // gap at today-2
        assertEquals(2, StreakCalculator.currentStreak(dates, today))
    }

    @Test
    fun `no activity today or yesterday means a zero streak, even with older activity`() {
        val dates = setOf(today.minusDays(3), today.minusDays(4))
        assertEquals(0, StreakCalculator.currentStreak(dates, today))
    }
}
