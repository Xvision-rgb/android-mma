package com.example.mmarecomp.util

import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingLoadTest {

    private val aujourdhui: LocalDate = LocalDate.of(2026, 8, 29)

    private fun workout(date: LocalDate, rpe: Int?, dureeMin: Int?) = Workout(
        id = date.toString(),
        userId = "u",
        date = DateUtils.string(date),
        type = WorkoutType.TorseForce,
        exercices = emptyList(),
        dureeMin = dureeMin,
        rpe = rpe,
    )

    private fun checkIn(date: LocalDate, score5: Int, hrv: Double? = null) = DailyCheckIn(
        id = date.toString(),
        userId = "u",
        date = DateUtils.string(date),
        sommeil = score5,
        courbatures = score5,
        fatigue = score5,
        humeur = score5,
        stress = score5,
        hrvRmssd = hrv,
    )

    @Test
    fun `charge de seance est le produit rpe par duree`() {
        assertEquals(480.0, TrainingLoad.chargeSeance(workout(aujourdhui, 8, 60)), 0.001)
    }

    @Test
    fun `seance sans rpe ou sans duree ne compte pas`() {
        assertEquals(0.0, TrainingLoad.chargeSeance(workout(aujourdhui, null, 60)), 0.001)
        assertEquals(0.0, TrainingLoad.chargeSeance(workout(aujourdhui, 8, null)), 0.001)
    }

    @Test
    fun `acwr vaut 1 quand la charge est constante`() {
        val workouts = (0 until 28).map { workout(aujourdhui.minusDays(it.toLong()), 7, 60) }
        val acwr = TrainingLoad.acwr(TrainingLoad.chargesParJour(workouts), aujourdhui)
        assertNotNull(acwr)
        assertEquals(1.0, acwr!!, 0.001)
    }

    @Test
    fun `une seance mma tres difficile charge plus qu une seance facile`() {
        assertTrue(TrainingLoad.intensiteMma(1)!! > TrainingLoad.intensiteMma(5)!!)
    }

    @Test
    fun `le ressenti mma hors echelle est refuse`() {
        assertNull(TrainingLoad.intensiteMma(0))
        assertNull(TrainingLoad.intensiteMma(6))
    }

    @Test
    fun `charge mma utilise l intensite convertie`() {
        val difficile = MmaSession("1", "u", "2026-08-29", "wod", ressenti = 1)
        val facile = MmaSession("2", "u", "2026-08-29", "wod", ressenti = 5)
        assertTrue(TrainingLoad.chargeSeance(difficile) > TrainingLoad.chargeSeance(facile))
    }

    @Test
    fun `acwr indisponible avec moins de 21 jours de couverture chronique`() {
        val workouts = listOf(workout(aujourdhui, 8, 60))
        val acwr = TrainingLoad.acwr(TrainingLoad.chargesParJour(workouts), aujourdhui)
        assertNull(acwr)
    }

    @Test
    fun `acwr monte quand la semaine est plus chargee que le mois`() {
        // 21 jours légers puis 7 jours pleins : couverture chronique OK.
        val workouts = (0 until 28).map { offset ->
            val rpe = if (offset < 7) 8 else 1
            workout(aujourdhui.minusDays(offset.toLong()), rpe, 60)
        }
        val acwr = TrainingLoad.acwr(TrainingLoad.chargesParJour(workouts), aujourdhui)!!
        assertTrue(acwr > TrainingLoad.ACWR_ALERTE)
    }

    @Test
    fun `les jours sans seance comptent comme zero et non comme absents`() {
        // 21 jours avec charge + 7 repos : la moyenne chronique dilue la semaine.
        val workouts = (0 until 21).map { workout(aujourdhui.minusDays(it.toLong()), 10, 60) }
        val acwr = TrainingLoad.acwr(TrainingLoad.chargesParJour(workouts), aujourdhui)!!
        assertTrue(acwr > 1.0)
    }

    @Test
    fun `acwr est null sans historique chronique`() {
        assertNull(TrainingLoad.acwr(emptyMap(), aujourdhui))
    }

    // --- Modulation ---

    @Test
    fun `score haut et acwr dans la zone donne une seance nominale`() {
        val m = TrainingLoad.moduler(score = 22, acwr = 1.0)
        assertEquals(ReadinessAction.NOMINALE, m.action)
        assertEquals(1.0, m.facteurVolume, 0.001)
        assertEquals(1.0, m.facteurCharge, 0.001)
    }

    @Test
    fun `score moyen reduit le volume sans toucher la charge`() {
        val m = TrainingLoad.moduler(score = 17, acwr = 1.0)
        assertEquals(ReadinessAction.VOLUME_REDUIT, m.action)
        assertTrue(m.facteurVolume < 1.0)
        assertEquals(1.0, m.facteurCharge, 0.001)
    }

    @Test
    fun `hrv sous la tendance suffit a reduire le volume`() {
        val m = TrainingLoad.moduler(score = 22, acwr = 1.0, ecartHrvSigma = -1.2)
        assertEquals(ReadinessAction.VOLUME_REDUIT, m.action)
    }

    @Test
    fun `score bas allege charge et volume`() {
        val m = TrainingLoad.moduler(score = 12, acwr = 1.0)
        assertEquals(ReadinessAction.ALLEGEE, m.action)
        assertTrue(m.facteurCharge < 1.0)
        assertEquals(2, m.rirSupplementaire)
    }

    @Test
    fun `acwr au dessus du seuil d alerte allege la seance`() {
        val m = TrainingLoad.moduler(score = 22, acwr = 1.8)
        assertEquals(ReadinessAction.ALLEGEE, m.action)
    }

    @Test
    fun `trois jours en rouge declenchent un deload a charges maintenues`() {
        val m = TrainingLoad.moduler(score = 12, acwr = 1.0, joursConsecutifsEnRouge = 3)
        assertEquals(ReadinessAction.DELOAD, m.action)
        assertEquals(0.5, m.facteurVolume, 0.001)
        // Les charges se maintiennent : c'est la fatigue qu'on purge, pas
        // l'adaptation de force.
        assertEquals(1.0, m.facteurCharge, 0.001)
    }

    @Test
    fun `aucune modulation ne supprime jamais la seance`() {
        val cas = listOf(
            TrainingLoad.moduler(null, null),
            TrainingLoad.moduler(5, 3.0),
            TrainingLoad.moduler(5, 3.0, -2.0, 5),
        )
        cas.forEach { assertTrue("volume nul interdit", it.facteurVolume > 0.0) }
    }

    @Test
    fun `absence totale de donnees reste une seance nominale`() {
        assertEquals(ReadinessAction.NOMINALE, TrainingLoad.moduler(null, null).action)
    }

    // --- Jours consécutifs en rouge ---

    @Test
    fun `compte les jours en rouge consecutifs jusqu a aujourd hui`() {
        val checkIns = listOf(
            checkIn(aujourdhui, 2),
            checkIn(aujourdhui.minusDays(1), 2),
            checkIn(aujourdhui.minusDays(2), 4),
        )
        assertEquals(2, TrainingLoad.joursConsecutifsEnRouge(checkIns, aujourdhui))
    }

    @Test
    fun `un trou dans l historique interrompt le comptage`() {
        val checkIns = listOf(checkIn(aujourdhui.minusDays(1), 2))
        assertEquals(0, TrainingLoad.joursConsecutifsEnRouge(checkIns, aujourdhui))
    }

    // --- HRV ---

    @Test
    fun `ecart hrv est null tant que l historique est trop court`() {
        val checkIns = (0 until 3).map { checkIn(aujourdhui.minusDays(it.toLong()), 3, 60.0) }
        assertNull(TrainingLoad.ecartHrvEnSigma(checkIns, aujourdhui))
    }

    @Test
    fun `pas de hrv du jour sans check in aujourd hui`() {
        val historique = (1..7).map { checkIn(aujourdhui.minusDays(it.toLong()), 3, 60.0) }
        assertNull(TrainingLoad.ecartHrvEnSigma(historique, aujourdhui))
    }

    @Test
    fun `une hrv basse isolee ressort comme un ecart negatif marque`() {
        val stable = (1..7).map { checkIn(aujourdhui.minusDays(it.toLong()), 3, 60.0 + it % 2) }
        val checkIns = stable + checkIn(aujourdhui, 3, 40.0)
        val ecart = TrainingLoad.ecartHrvEnSigma(checkIns, aujourdhui)
        assertNotNull(ecart)
        assertTrue(ecart!! < -0.5)
    }
}
