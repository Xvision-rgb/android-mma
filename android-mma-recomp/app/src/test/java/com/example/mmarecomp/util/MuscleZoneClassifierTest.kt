package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.MuscleZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleZoneClassifierTest {

    @After
    fun tearDown() = MuscleZoneClassifier.effacerOverrides()

    private fun exercice(nom: String, reps: Int, chargeKg: Double, series: Int = 1) =
        LoggedExercise(
            nom = nom,
            series = series,
            reps = reps,
            sets = (1..series).map { LoggedSet(index = it, reps = reps, chargeKg = chargeKg) },
        )

    @Test
    fun `classe les exercices reels de l historique`() {
        assertEquals(MuscleZone.TIRAGE, MuscleZoneClassifier.classifier("Rowing barre"))
        assertEquals(MuscleZone.TIRAGE, MuscleZoneClassifier.classifier("Tractions pronation"))
        assertEquals(MuscleZone.POUSSEE, MuscleZoneClassifier.classifier("Développé couché"))
        assertEquals(MuscleZone.QUADS_BRAS, MuscleZoneClassifier.classifier("Squat Zercher"))
        assertEquals(MuscleZone.CHAINE_POSTERIEURE, MuscleZoneClassifier.classifier("Soulevé de terre roumain"))
        assertEquals(MuscleZone.COU_POIGNE, MuscleZoneClassifier.classifier("Farmer's walk"))
        assertEquals(MuscleZone.COU_POIGNE, MuscleZoneClassifier.classifier("Dead hang"))
    }

    @Test
    fun `la normalisation ignore accents casse et ponctuation`() {
        assertEquals(
            MuscleZoneClassifier.classifier("Développé Couché"),
            MuscleZoneClassifier.classifier("developpe couche"),
        )
        assertEquals(MuscleZone.CHAINE_POSTERIEURE, MuscleZoneClassifier.classifier("SOULEVÉ DE TERRE"))
    }

    @Test
    fun `un nom vide ne casse pas la classification`() {
        assertEquals(MuscleZone.QUADS_BRAS, MuscleZoneClassifier.classifier(""))
    }

    @Test
    fun `l override de l utilisateur prime sur les mots-cles`() {
        MuscleZoneClassifier.override("Rowing barre", MuscleZone.POUSSEE)
        assertEquals(MuscleZone.POUSSEE, MuscleZoneClassifier.classifier("Rowing barre"))
    }

    @Test
    fun `distingue polyarticulaire et isolation`() {
        assertTrue(MuscleZoneClassifier.estPolyarticulaire("Squat Zercher"))
        assertTrue(MuscleZoneClassifier.estPolyarticulaire("Rowing barre"))
        assertFalse(MuscleZoneClassifier.estPolyarticulaire("Curl biceps"))
    }

    @Test
    fun `ratio tirage poussee reflete le volume reel`() {
        val exercices = listOf(
            exercice("Rowing barre", reps = 10, chargeKg = 80.0, series = 4),
            exercice("Développé couché", reps = 10, chargeKg = 80.0, series = 2),
        )
        // 4 séries contre 2, à volume unitaire égal -> ratio 2:1.
        assertEquals(2.0, MuscleZoneClassifier.ratioTiragePoussee(exercices)!!, 0.001)
    }

    @Test
    fun `ratio est null sans poussee loguee`() {
        val exercices = listOf(exercice("Rowing barre", reps = 10, chargeKg = 80.0))
        assertNull(MuscleZoneClassifier.ratioTiragePoussee(exercices))
    }

    @Test
    fun `repartition est vide quand rien n a ete logue`() {
        assertTrue(MuscleZoneClassifier.repartition(emptyList()).isEmpty())
    }

    @Test
    fun `repartition somme a 1`() {
        val exercices = listOf(
            exercice("Rowing barre", reps = 10, chargeKg = 100.0),
            exercice("Développé couché", reps = 10, chargeKg = 100.0),
            exercice("Soulevé de terre", reps = 5, chargeKg = 100.0),
        )
        val total = MuscleZoneClassifier.repartition(exercices).values.sum()
        assertEquals(1.0, total, 0.001)
    }
}
