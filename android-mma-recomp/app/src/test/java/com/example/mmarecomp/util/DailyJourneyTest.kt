package com.example.mmarecomp.util

import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyJourneyTest {

    private val today = LocalDate.of(2026, 8, 31)

    @Test
    fun `toutes etapes completees quand donnees presentes`() {
        val journey = DailyJourney.compute(
            today = today,
            checkInToday = DailyCheckIn(
                id = "1", userId = "u", date = "2026-08-31",
                sommeil = 4, courbatures = 4, fatigue = 4, humeur = 4, stress = 4,
            ),
            weighInsToday = listOf(
                WeighIn("w", "u", "2026-08-31", "07:00:00", WeighInType.MatinJeun, 80.0),
            ),
            workoutsToday = listOf(
                Workout("x", "u", "2026-08-31", WorkoutType.JambesForce, emptyList()),
            ),
            mmaSessionsToday = emptyList(),
            mealsToday = listOf(
                Meal("m", "u", "2026-08-31", 1, 500, 30.0, 50.0, 15.0),
            ),
            planToday = TrainingPlanDay("p", "u", 1, PlanDayType.JambesForce, emptyList(), Phase.Ete),
        )
        assertEquals(4, journey.completedCount)
        assertEquals(4, journey.totalCount)
        assertEquals(1f, journey.progress)
    }

    @Test
    fun `jour repos compte la seance comme optionnelle`() {
        val journey = DailyJourney.compute(
            today = today,
            checkInToday = null,
            weighInsToday = emptyList(),
            workoutsToday = emptyList(),
            mmaSessionsToday = emptyList(),
            mealsToday = emptyList(),
            planToday = TrainingPlanDay("p", "u", 1, PlanDayType.Repos, emptyList(), Phase.Ete),
        )
        assertEquals(0, journey.completedCount)
        assertTrue(journey.steps.first { it.id == DailyJourneyStepId.WORKOUT }.optional)
        assertTrue(journey.steps.first { it.id == DailyJourneyStepId.WORKOUT }.done)
    }
}
