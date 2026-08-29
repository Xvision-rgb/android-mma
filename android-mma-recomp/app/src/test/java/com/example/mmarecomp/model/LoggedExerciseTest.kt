package com.example.mmarecomp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rétrocompatibilité du logging par série.
 *
 *  La colonne `exercices` est du JSONB : tout l'historique antérieur existe
 *  sans champ `sets`. Ces séances doivent continuer à se lire et à produire
 *  un volume correct, sinon la migration efface silencieusement des mois de
 *  données. */
class LoggedExerciseTest {

    @Test
    fun `un exercice historique sans sets derive ses series`() {
        val ancien = LoggedExercise(
            nom = "Développé couché",
            series = 3,
            reps = 10,
            chargeReelleKg = 80.0,
        )
        val sets = ancien.effectiveSets
        assertEquals(3, sets.size)
        assertTrue(ancien.setsSontDerives)
        assertTrue(sets.all { it.reps == 10 && it.chargeKg == 80.0 })
        // La dernière série reconstruite sert de référence à l'autorégulation.
        assertTrue(sets.last().estAmrap)
        assertEquals(listOf(1, 2, 3), sets.map { it.index })
    }

    @Test
    fun `reps reelles priment sur les reps planifiees dans la derivation`() {
        val ancien = LoggedExercise(
            nom = "Squat",
            series = 2,
            reps = 10,
            repsReelles = 8,
            chargeReelleKg = 100.0,
        )
        assertTrue(ancien.effectiveSets.all { it.reps == 8 })
    }

    @Test
    fun `la charge cible sert de repli quand la charge reelle manque`() {
        val ancien = LoggedExercise(nom = "Rowing", series = 2, reps = 10, chargeCibleKg = 60.0)
        assertEquals(60.0, ancien.effectiveSets.first().chargeKg, 0.001)
    }

    @Test
    fun `un exercice sans aucune charge ne fabrique pas de series`() {
        val vide = LoggedExercise(nom = "Gainage", series = 3, reps = 30)
        assertTrue(vide.effectiveSets.isEmpty())
        assertEquals(0.0, vide.volumeTotal, 0.001)
    }

    @Test
    fun `les sets explicites font foi sur les agregats`() {
        val moderne = LoggedExercise(
            nom = "Rowing barre",
            series = 3,
            reps = 10,
            chargeReelleKg = 80.0,
            sets = listOf(
                LoggedSet(index = 1, reps = 10, chargeKg = 80.0),
                LoggedSet(index = 2, reps = 8, chargeKg = 80.0),
            ),
        )
        assertFalse(moderne.setsSontDerives)
        assertEquals(2, moderne.effectiveSets.size)
        assertEquals(80.0 * 18, moderne.volumeTotal, 0.001)
    }

    @Test
    fun `withSets resynchronise les agregats et renumerote`() {
        val ex = LoggedExercise(nom = "Traction", series = 1, reps = 5)
            .withSets(
                listOf(
                    LoggedSet(index = 9, reps = 6, chargeKg = 10.0),
                    LoggedSet(index = 4, reps = 4, chargeKg = 12.5),
                ),
            )
        assertEquals(listOf(1, 2), ex.sets.map { it.index })
        assertEquals(2, ex.series)
        // Les agrégats restent lus par le code antérieur : les laisser dériver
        // produirait des volumes faux côté historique.
        assertEquals(12.5, ex.chargeReelleKg!!, 0.001)
        assertEquals(6, ex.repsReelles)
        assertEquals(6 * 10.0 + 4 * 12.5, ex.volumeTotal, 0.001)
    }

    @Test
    fun `charge max lit bien la serie la plus lourde`() {
        val ex = LoggedExercise(
            nom = "Squat",
            series = 2,
            reps = 5,
            sets = listOf(
                LoggedSet(index = 1, reps = 5, chargeKg = 100.0),
                LoggedSet(index = 2, reps = 3, chargeKg = 120.0),
            ),
        )
        assertEquals(120.0, ex.chargeMaxKg!!, 0.001)
    }
}
