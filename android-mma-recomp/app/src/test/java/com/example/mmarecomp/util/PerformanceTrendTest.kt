package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceTrendTest {

    private fun workout(date: String, vararg exercices: LoggedExercise) = Workout(
        id = date,
        userId = "u1",
        date = date,
        type = WorkoutType.JambesForce,
        exercices = exercices.toList(),
    )

    private fun squat(chargeReelleKg: Double?) =
        LoggedExercise(nom = "Squat", series = 5, reps = 5, chargeReelleKg = chargeReelleKg)

    @Test
    fun `no workouts never claims progress`() {
        assertFalse(PerformanceTrend.isImproving(emptyList()))
    }

    @Test
    fun `a single logged instance of an exercise is not enough evidence`() {
        val workouts = listOf(workout("2026-03-01", squat(100.0)))
        assertFalse(PerformanceTrend.isImproving(workouts))
    }

    @Test
    fun `charge going up across two sessions counts as improving`() {
        val workouts = listOf(
            workout("2026-03-01", squat(100.0)),
            workout("2026-03-08", squat(105.0)),
        )
        assertTrue(PerformanceTrend.isImproving(workouts))
    }

    @Test
    fun `charge going down across two sessions is not improving`() {
        val workouts = listOf(
            workout("2026-03-01", squat(105.0)),
            workout("2026-03-08", squat(100.0)),
        )
        assertFalse(PerformanceTrend.isImproving(workouts))
    }

    @Test
    fun `charge staying flat is not counted as improving`() {
        val workouts = listOf(
            workout("2026-03-01", squat(100.0)),
            workout("2026-03-08", squat(100.0)),
        )
        assertFalse(PerformanceTrend.isImproving(workouts))
    }

    @Test
    fun `entries with a null charge are ignored rather than crashing`() {
        val workouts = listOf(
            workout("2026-03-01", squat(null)),
            workout("2026-03-08", squat(null)),
        )
        assertFalse(PerformanceTrend.isImproving(workouts))
    }

    @Test
    fun `improving exercises must at least match declining ones`() {
        val bench = { charge: Double? -> LoggedExercise(nom = "Développé couché", series = 5, reps = 5, chargeReelleKg = charge) }
        val workouts = listOf(
            workout("2026-03-01", squat(100.0), bench(60.0)),
            workout("2026-03-08", squat(95.0), bench(65.0)),
        )
        // 1 improving (bench), 1 declining (squat) -> tie counts as improving
        assertEquals(true, PerformanceTrend.isImproving(workouts))
    }
}
