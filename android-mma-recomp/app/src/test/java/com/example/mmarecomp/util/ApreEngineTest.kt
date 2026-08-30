package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApreEngineTest {

    private fun exercice(
        reps: Int,
        chargeKg: Double = 100.0,
        limitePoigne: Boolean = false,
    ) = LoggedExercise(
        nom = "Rowing barre",
        series = 1,
        reps = reps,
        sets = listOf(
            LoggedSet(
                index = 1,
                reps = reps,
                chargeKg = chargeKg,
                limitePoigne = limitePoigne,
                estAmrap = true,
            ),
        ),
    )

    // --- Table d'ajustement : les cinq branches ---

    @Test
    fun `tres en dessous de la cible fait baisser la charge de 6 pourcent`() {
        val p = ApreEngine.prescrire(exercice(reps = 6), ApreProtocol.APRE_10)
        assertNotNull(p)
        // 100 * 0,94 = 94 -> arrondi à 95 (incrément 2,5)
        assertEquals(95.0, p!!.chargeKg, 0.001)
        assertTrue(p.deltaKg < 0)
    }

    @Test
    fun `legerement en dessous de la cible fait baisser de 2 virgule 5 pourcent`() {
        val p = ApreEngine.prescrire(exercice(reps = 8), ApreProtocol.APRE_10)
        // 100 * 0,975 = 97,5
        assertEquals(97.5, p!!.chargeKg, 0.001)
    }

    @Test
    fun `pile sur la cible laisse la charge inchangee`() {
        val p = ApreEngine.prescrire(exercice(reps = 10), ApreProtocol.APRE_10)
        assertEquals(100.0, p!!.chargeKg, 0.001)
        assertEquals(0.0, p.deltaKg, 0.001)
    }

    @Test
    fun `legerement au dessus de la cible fait monter de 2 virgule 5 pourcent`() {
        val p = ApreEngine.prescrire(exercice(reps = 12), ApreProtocol.APRE_10)
        assertEquals(102.5, p!!.chargeKg, 0.001)
    }

    @Test
    fun `nettement au dessus de la cible fait monter de 5 pourcent`() {
        val p = ApreEngine.prescrire(exercice(reps = 15), ApreProtocol.APRE_10)
        assertEquals(105.0, p!!.chargeKg, 0.001)
    }

    // --- Le cas qui compte pour un tirage limité par la poigne ---

    @Test
    fun `serie coupee par la poigne ne fait pas baisser la charge`() {
        // 4 reps sur une cible de 10 ferait normalement -6 %.
        val p = ApreEngine.prescrire(exercice(reps = 4, limitePoigne = true), ApreProtocol.APRE_10)
        assertNotNull(p)
        assertEquals(100.0, p!!.chargeKg, 0.001)
        assertEquals(0.0, p.deltaKg, 0.001)
        assertTrue(p.justification.contains("poigne"))
    }

    // --- Arrondi à l'incrément réellement chargeable ---

    @Test
    fun `arrondi respecte l increment disponible`() {
        assertEquals(105.0, ApreEngine.arrondir(104.3, 5.0), 0.001)
        assertEquals(102.5, ApreEngine.arrondir(102.0, 2.5), 0.001)
        assertEquals(104.0, ApreEngine.arrondir(104.3, 1.0), 0.001)
    }

    @Test
    fun `increment grossier peut absorber l ajustement sans changer la charge`() {
        // −2,5 % de 100 kg = 97,5, qui retombe sur 100 avec un incrément de 5 :
        // la salle n'a pas de disque assez fin pour exprimer l'ajustement.
        val p = ApreEngine.prescrire(exercice(reps = 9), ApreProtocol.APRE_10, incrementKg = 5.0)
        assertEquals(100.0, p!!.chargeKg, 0.001)
        assertEquals(0.0, p.deltaKg, 0.001)
    }

    // --- Biais RIR ---

    @Test
    fun `biais positif surestimation reduit la hausse de charge`() {
        val sans = ApreEngine.prescrire(exercice(reps = 12), ApreProtocol.APRE_10, incrementKg = 0.5)
        val avec = ApreEngine.prescrire(exercice(reps = 12), ApreProtocol.APRE_10, incrementKg = 0.5, biaisRir = 2.0)
        assertTrue(avec!!.chargeKg <= sans!!.chargeKg)
    }

    @Test
    fun `biais negatif sous estimation augmente la hausse de charge`() {
        val sans = ApreEngine.prescrire(exercice(reps = 12), ApreProtocol.APRE_10, incrementKg = 0.5)
        val avec = ApreEngine.prescrire(exercice(reps = 12), ApreProtocol.APRE_10, incrementKg = 0.5, biaisRir = -2.0)
        assertTrue(avec!!.chargeKg >= sans!!.chargeKg)
    }

    @Test
    fun `biais rir ne touche pas une baisse de charge`() {
        val sans = ApreEngine.prescrire(exercice(reps = 6), ApreProtocol.APRE_10)
        val avec = ApreEngine.prescrire(exercice(reps = 6), ApreProtocol.APRE_10, biaisRir = 2.0)
        assertEquals(sans!!.chargeKg, avec!!.chargeKg, 0.001)
    }

    // --- Cas dégradés ---

    @Test
    fun `exercice sans serie ne produit aucune prescription`() {
        val vide = LoggedExercise(nom = "Squat", series = 0, reps = 0)
        assertNull(ApreEngine.prescrire(vide, ApreProtocol.APRE_6))
    }

    @Test
    fun `charge nulle ne produit aucune prescription`() {
        assertNull(ApreEngine.prescrire(exercice(reps = 10, chargeKg = 0.0), ApreProtocol.APRE_10))
    }

    @Test
    fun `la serie amrap prime sur la derniere serie`() {
        val ex = LoggedExercise(
            nom = "Développé couché",
            series = 3,
            reps = 6,
            sets = listOf(
                LoggedSet(index = 1, reps = 6, chargeKg = 80.0),
                LoggedSet(index = 2, reps = 9, chargeKg = 80.0, estAmrap = true),
                LoggedSet(index = 3, reps = 3, chargeKg = 80.0),
            ),
        )
        // La série AMRAP (9 reps sur une cible de 6) doit faire monter la
        // charge, alors que la dernière série (3 reps) la ferait baisser.
        val p = ApreEngine.prescrire(ex, ApreProtocol.APRE_6)
        assertTrue(p!!.deltaKg > 0)
    }

    @Test
    fun `sous performance d une rep ne fait pas monter la charge`() {
        val p = ApreEngine.prescrire(exercice(reps = 9), ApreProtocol.APRE_10, incrementKg = 1.0)
        assertNotNull(p)
        assertTrue(p!!.chargeKg <= 100.0)
    }
}
