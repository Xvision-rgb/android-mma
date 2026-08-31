package com.example.mmarecomp.util

import com.example.mmarecomp.model.ExerciseModality
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardioEnergyTest {

    @Test
    fun `detecte marche course et velo comme cardio`() {
        assertTrue(CardioEnergy.looksLikeCardio("Marche rapide"))
        assertTrue(CardioEnergy.looksLikeCardio("Marche inclinée 8%"))
        assertTrue(CardioEnergy.looksLikeCardio("Course à pied"))
        assertTrue(CardioEnergy.looksLikeCardio("Vélo"))
        assertFalse(CardioEnergy.looksLikeCardio("Squat"))
        assertFalse(CardioEnergy.looksLikeCardio("Développé couché"))
    }

    @Test
    fun `kcal MET avec poids`() {
        val exo = CardioEnergy.fromPreset(CardioEnergy.PRESETS.first { it.nom == "Course à pied" })
            .copy(dureeMin = 30)
        // 9.0 MET × 80 kg × 0.5 h = 360
        assertEquals(360, CardioEnergy.kcalForExercise(exo, 80.0))
    }

    @Test
    fun `charge interne cardio sans RPE seance`() {
        val workout = Workout(
            id = "1",
            userId = "u",
            date = "2026-08-31",
            type = WorkoutType.Course,
            exercices = listOf(
                LoggedExercise(
                    nom = "Marche rapide",
                    series = 0,
                    reps = 0,
                    modality = ExerciseModality.Cardio,
                    dureeMin = 40,
                    intensite = 5,
                ),
            ),
            dureeMin = null,
            rpe = null,
        )
        assertEquals(200.0, TrainingLoad.chargeSeance(workout), 0.001)
        assertTrue(TrainingLoad.depenseKcal(workout, 75.0) > 0)
    }

    @Test
    fun `volume force ignore le cardio`() {
        val cardio = LoggedExercise(
            nom = "Vélo",
            series = 3,
            reps = 10,
            modality = ExerciseModality.Cardio,
            dureeMin = 30,
            sets = listOf(LoggedSet(1, 10, 50.0)),
        )
        assertEquals(0.0, cardio.volumeTotal, 0.001)
        assertTrue(cardio.effectiveSets.isEmpty())
    }

    @Test
    fun `autodetect bascule un nom cardio sans series chargees`() {
        val exo = LoggedExercise(nom = "Course à pied", series = 3, reps = 10, sets = emptyList())
        val detected = CardioEnergy.maybeAutodetect(exo)
        assertTrue(detected.isCardio)
        assertEquals(30, detected.dureeMin)
    }
}
