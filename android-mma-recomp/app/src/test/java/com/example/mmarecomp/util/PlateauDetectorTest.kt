package com.example.mmarecomp.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PlateauDetectorTest {

    private val today = LocalDate.of(2026, 3, 15)

    @Test
    fun `fewer than 2 recent weigh-ins never triggers the plateau message`() {
        val result = PlateauDetector.detect(
            morningWeighIns = listOf(today to 85.0),
            performanceTrendUp = true,
            today = today,
        )
        assertEquals(PlateauStatus.NONE, result)
    }

    @Test
    fun `stable weight with rising performance is a positive recomposition signal`() {
        val weighIns = listOf(
            today.minusDays(13) to 85.0,
            today.minusDays(7) to 85.2,
            today to 84.9,
        )
        val result = PlateauDetector.detect(weighIns, performanceTrendUp = true, today = today)
        assertEquals(PlateauStatus.RECOMPOSITION_EN_COURS, result)
    }

    @Test
    fun `stable weight without rising performance is never flagged as stagnation`() {
        val weighIns = listOf(
            today.minusDays(13) to 85.0,
            today to 85.2,
        )
        // Non-negotiable UX rule: even a genuine plateau never surfaces as an alert —
        // it either reads as positive, or it says nothing at all.
        val result = PlateauDetector.detect(weighIns, performanceTrendUp = false, today = today)
        assertEquals(PlateauStatus.NONE, result)
    }

    @Test
    fun `weight swinging more than half a kilo is not considered stable`() {
        val weighIns = listOf(
            today.minusDays(13) to 84.0,
            today to 85.0, // 1.0kg swing, over the 0.5kg stability threshold
        )
        val result = PlateauDetector.detect(weighIns, performanceTrendUp = true, today = today)
        assertEquals(PlateauStatus.NONE, result)
    }

    @Test
    fun `weigh-ins older than 14 days are excluded from the window`() {
        val weighIns = listOf(
            today.minusDays(20) to 90.0, // outside the 14-day window, ignored
            today.minusDays(6) to 85.0,
            today to 85.1,
        )
        val result = PlateauDetector.detect(weighIns, performanceTrendUp = true, today = today)
        assertEquals(PlateauStatus.RECOMPOSITION_EN_COURS, result)
    }

    @Test
    fun `exactly half a kilo swing is still considered stable`() {
        val weighIns = listOf(
            today.minusDays(13) to 85.0,
            today to 85.5,
        )
        val result = PlateauDetector.detect(weighIns, performanceTrendUp = true, today = today)
        assertEquals(PlateauStatus.RECOMPOSITION_EN_COURS, result)
    }
}
