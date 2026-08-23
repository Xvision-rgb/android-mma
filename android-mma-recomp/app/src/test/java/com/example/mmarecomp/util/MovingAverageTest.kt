package com.example.mmarecomp.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovingAverageTest {

    @Test
    fun `empty input returns empty output`() {
        assertTrue(MovingAverage.sevenDay(emptyList()).isEmpty())
    }

    @Test
    fun `single point averages to itself`() {
        val day = LocalDate.of(2026, 1, 1)
        val result = MovingAverage.sevenDay(listOf(TrendPoint(day, 80.0)))
        assertEquals(1, result.size)
        assertEquals(80.0, result[0].value, 0.0001)
    }

    @Test
    fun `two consecutive days average only over points seen so far`() {
        val day1 = LocalDate.of(2026, 1, 1)
        val day2 = LocalDate.of(2026, 1, 2)
        val result = MovingAverage.sevenDay(listOf(TrendPoint(day1, 80.0), TrendPoint(day2, 82.0)))

        assertEquals(80.0, result[0].value, 0.0001) // day1: only day1 in its 7-day window
        assertEquals(81.0, result[1].value, 0.0001) // day2: (80 + 82) / 2
    }

    @Test
    fun `points more than 7 days apart do not influence each other`() {
        val early = LocalDate.of(2026, 1, 1)
        val late = LocalDate.of(2026, 1, 10) // 9 days later, outside the 7-day window
        val result = MovingAverage.sevenDay(listOf(TrendPoint(early, 80.0), TrendPoint(late, 90.0)))

        assertEquals(80.0, result[0].value, 0.0001)
        assertEquals(90.0, result[1].value, 0.0001) // early point excluded from late's window
    }

    @Test
    fun `unsorted input is sorted by date before averaging`() {
        val day1 = LocalDate.of(2026, 1, 1)
        val day2 = LocalDate.of(2026, 1, 2)
        val result = MovingAverage.sevenDay(listOf(TrendPoint(day2, 82.0), TrendPoint(day1, 80.0)))

        assertEquals(day1, result[0].date)
        assertEquals(day2, result[1].date)
        assertEquals(81.0, result[1].value, 0.0001)
    }
}
