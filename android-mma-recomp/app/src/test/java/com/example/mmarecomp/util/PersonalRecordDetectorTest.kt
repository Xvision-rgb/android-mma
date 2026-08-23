package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordDetectorTest {

    private fun workout(vararg exercices: LoggedExercise) = Workout(
        id = "id",
        userId = "user",
        date = "2026-01-01",
        type = WorkoutType.JambesForce,
        exercices = exercices.toList(),
    )

    private fun exercise(nom: String, charge: Double?) =
        LoggedExercise(nom = nom, series = 5, reps = 5, chargeReelleKg = charge)

    @Test
    fun `no history means no known best load`() {
        assertNull(PersonalRecordDetector.bestKnownLoad("Squat", emptyList()))
    }

    @Test
    fun `best known load is the max across all past workouts, case and whitespace insensitive`() {
        val history = listOf(
            workout(exercise("Squat", 90.0), exercise("Bench press", 60.0)),
            workout(exercise(" squat ", 100.0)),
            workout(exercise("SQUAT", 95.0)),
        )
        assertEquals(100.0, PersonalRecordDetector.bestKnownLoad("Squat", history)!!, 0.0001)
    }

    @Test
    fun `exceeding the best known load is a new record`() {
        val history = listOf(workout(exercise("Squat", 100.0)))
        assertTrue(PersonalRecordDetector.isNewRecord("Squat", 102.5, history))
    }

    @Test
    fun `matching but not exceeding the best load is not a new record`() {
        val history = listOf(workout(exercise("Squat", 100.0)))
        assertFalse(PersonalRecordDetector.isNewRecord("Squat", 100.0, history))
        assertFalse(PersonalRecordDetector.isNewRecord("Squat", 95.0, history))
    }

    @Test
    fun `a never-before-seen exercise is never flagged as a record, only silence`() {
        val history = listOf(workout(exercise("Squat", 100.0)))
        assertFalse(PersonalRecordDetector.isNewRecord("Développé couché", 40.0, history))
    }

    @Test
    fun `exercises logged without a real load are ignored when computing the best`() {
        val history = listOf(workout(exercise("Squat", null)))
        assertNull(PersonalRecordDetector.bestKnownLoad("Squat", history))
    }
}
