package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.model.withSets
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentExerciseCatalogTest {

    private fun exercice(nom: String, series: Int, charge: Double) = LoggedExercise(
        nom = nom,
        series = series,
        reps = 8,
        chargeCibleKg = charge,
        sets = (1..series).map { i ->
            LoggedSet(index = i, reps = 8, chargeKg = charge, estAmrap = i == series)
        },
    )

    @Test
    fun `fromWorkouts deduplique et garde la plus recente`() {
        val workouts = listOf(
            Workout("w1", "u", "2026-08-30", WorkoutType.JambesForce, listOf(exercice("Squat", 3, 100.0))),
            Workout("w2", "u", "2026-08-31", WorkoutType.JambesForce, listOf(exercice("Squat", 4, 110.0))),
        )
        val entries = RecentExerciseCatalog.fromWorkouts(workouts)
        assertEquals(1, entries.size)
        assertEquals(110.0, entries.first().derniereChargeKg!!, 0.01)
        assertEquals(4, entries.first().nbSeries)
    }

    @Test
    fun `replaceKeepingStructure conserve le nombre de series du plan`() {
        val plan = exercice("Squat", 4, 100.0)
        val historique = exercice("Presse a cuisses", 3, 140.0)
        val remplace = RecentExerciseCatalog.replaceKeepingStructure(plan, historique)
        assertEquals("Presse a cuisses", remplace.nom)
        assertEquals(4, remplace.effectiveSets.size)
        assertEquals(140.0, remplace.effectiveSets.first().chargeKg, 0.01)
    }
}
