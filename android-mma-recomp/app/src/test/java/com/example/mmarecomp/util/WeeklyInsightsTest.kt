package com.example.mmarecomp.util

import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyInsightsTest {

    private val today = LocalDate.of(2026, 8, 31)

    @Test
    fun `calcule les moyennes sur 7 jours`() {
        val insights = WeeklyInsightsCalculator.compute(
            today = today,
            checkIns = listOf(
                DailyCheckIn("1", "u", "2026-08-31", 4, 4, 4, 4, 4),
                DailyCheckIn("2", "u", "2026-08-30", 3, 3, 3, 3, 3),
            ),
            workouts = listOf(
                Workout("w", "u", "2026-08-31", WorkoutType.TorseForce, emptyList()),
            ),
            mmaSessions = emptyList(),
            meals = listOf(
                Meal("m1", "u", "2026-08-31", 1, 2000, 150.0, 200.0, 60.0),
                Meal("m2", "u", "2026-08-30", 1, 1800, 140.0, 180.0, 55.0),
            ),
            weighIns = listOf(
                WeighIn("wi1", "u", "2026-08-25", "07:00:00", WeighInType.MatinJeun, 80.0),
                WeighIn("wi2", "u", "2026-08-31", "07:00:00", WeighInType.MatinJeun, 80.5),
            ),
            targets = listOf(
                NutritionTarget("t", "u", "2026-08-31", TypeJour.Training, 2000, 150.0),
            ),
        )
        assertEquals(1, insights.workoutsLogged)
        assertEquals(1900, insights.avgCaloriesPerDay)
        assertEquals(17.5, insights.avgReadinessScore!!, 0.01)
        assertTrue(insights.weightDeltaKg != null)
    }
}
