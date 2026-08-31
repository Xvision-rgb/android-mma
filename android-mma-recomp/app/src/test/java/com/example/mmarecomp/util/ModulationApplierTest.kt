package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModulationApplierTest {

    private fun exercice(nom: String, series: Int, charge: Double = 80.0) = LoggedExercise(
        nom = nom,
        series = series,
        reps = 8,
        chargeCibleKg = charge,
        sets = (1..series).map { i ->
            LoggedSet(index = i, reps = 8, chargeKg = charge, estAmrap = i == series)
        },
    )

    @Test
    fun `volume reduit retire la derniere serie des accessoires`() {
        val exercices = listOf(
            exercice("Squat", 4),
            exercice("Curl biceps", 3),
        )
        val modulation = TrainingLoad.moduler(score = 18, acwr = 1.0)
        val result = ModulationApplier.apply(modulation, exercices)
        assertEquals(4, result.exercices[0].effectiveSets.size)
        assertEquals(2, result.exercices[1].effectiveSets.size)
        assertTrue(result.resume.any { it.contains("accessoire") })
    }

    @Test
    fun `allegee baisse les charges et ajoute du rir`() {
        val exercices = listOf(exercice("Developpe couche", 3, charge = 100.0))
        val modulation = TrainingLoad.moduler(score = 12, acwr = 1.0)
        val result = ModulationApplier.apply(modulation, exercices)
        assertEquals(90.0, result.exercices[0].chargeCibleKg!!, 0.01)
        assertEquals(90.0, result.exercices[0].effectiveSets.first().chargeKg, 0.01)
        assertEquals(2, result.rirSupplementaire)
    }

    @Test
    fun `deload garde au moins une serie par exercice`() {
        val exercices = listOf(exercice("Rowing", 4))
        val modulation = TrainingLoad.moduler(score = 10, acwr = 1.0, joursConsecutifsEnRouge = 3)
        val result = ModulationApplier.apply(modulation, exercices)
        assertEquals(2, result.exercices[0].effectiveSets.size)
    }
}
