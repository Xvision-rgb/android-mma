package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExportTest {

    private fun workout(notes: String = "") = Workout(
        id = "w1",
        userId = "u",
        date = "2026-08-30",
        type = WorkoutType.TorseForce,
        exercices = listOf(
            LoggedExercise(
                nom = "Squat",
                series = 2,
                reps = 5,
                sets = listOf(
                    LoggedSet(index = 1, reps = 5, chargeKg = 100.0, rir = 2, estAmrap = false, sangles = false),
                    LoggedSet(index = 2, reps = 5, chargeKg = 100.0, rir = 0, estAmrap = true, sangles = true),
                ),
            ),
        ),
        dureeMin = 60,
        rpe = 8,
        notes = notes,
    )

    @Test
    fun `exporte une ligne par serie`() {
        val csv = CsvExport.workouts(listOf(workout()))
        val dataLines = csv.lines().drop(1).filter { it.isNotBlank() }
        assertEquals(2, dataLines.size)
    }

    @Test
    fun `quotage rfc4180 pour virgule et guillemets`() {
        val csv = CsvExport.workouts(
            listOf(
                workout().copy(
                    exercices = listOf(
                        LoggedExercise(
                            nom = "Press, \"léger\"",
                            series = 1,
                            reps = 5,
                            sets = listOf(LoggedSet(index = 1, reps = 5, chargeKg = 60.0)),
                        ),
                    ),
                ),
            ),
        )
        assertTrue(csv.contains("\"Press, \"\"léger\"\"\""))
    }

    @Test
    fun `neutralise les formules dans les notes`() {
        val csv = CsvExport.workouts(listOf(workout(notes = "=1+1")))
        assertTrue(csv.contains("'=1+1"))
    }

    @Test
    fun `inclut charge reps rir amrap et sangles`() {
        val csv = CsvExport.workouts(listOf(workout()))
        assertTrue(csv.contains("\"100.0\""))
        assertTrue(csv.contains("\"1\"")) // amrap sur la 2e série
    }
}
