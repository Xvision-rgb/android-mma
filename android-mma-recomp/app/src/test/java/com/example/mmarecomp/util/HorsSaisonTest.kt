package com.example.mmarecomp.util

import com.example.mmarecomp.model.ContexteSportif
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.MuscleZone
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.model.toWorkoutTypeOrNull
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun exercice(nom: String, series: Int, reps: Int = 8, charge: Double = 80.0) =
    LoggedExercise(
        nom = nom,
        series = series,
        reps = reps,
        sets = (1..series).map { LoggedSet(index = it, reps = reps, chargeKg = charge) },
    )

private fun seance(
    date: LocalDate,
    type: WorkoutType,
    exercices: List<LoggedExercise> = emptyList(),
) = Workout(
    id = "$date-$type",
    userId = "u",
    date = DateUtils.string(date),
    type = type,
    exercices = exercices,
)

class VolumeLandmarksTest {

    private val lundi: LocalDate = LocalDate.of(2026, 8, 24)

    @Test
    fun `compte les series par zone et non le tonnage`() {
        val workouts = listOf(
            seance(lundi, WorkoutType.TorseForce, listOf(exercice("Rowing barre", 4))),
            seance(lundi.plusDays(2), WorkoutType.TorseForce, listOf(exercice("Tractions", 3))),
        )
        // Deux exercices de tirage : 7 séries au total, quelle que soit la charge.
        assertEquals(7, VolumeLandmarks.seriesParZone(workouts)[MuscleZone.TIRAGE])
    }

    @Test
    fun `une serie sans charge ni reps ne compte pas`() {
        val vide = LoggedExercise(
            nom = "Rowing barre",
            series = 3,
            reps = 8,
            sets = listOf(
                LoggedSet(index = 1, reps = 8, chargeKg = 80.0),
                LoggedSet(index = 2, reps = 0, chargeKg = 80.0),
                LoggedSet(index = 3, reps = 8, chargeKg = 0.0),
            ),
        )
        val workouts = listOf(seance(lundi, WorkoutType.TorseForce, listOf(vide)))
        assertEquals(1, VolumeLandmarks.seriesParZone(workouts)[MuscleZone.TIRAGE])
    }

    @Test
    fun `situe correctement par rapport aux reperes`() {
        assertEquals(ZoneVolume.SOUS_MAINTIEN, VolumeLandmarks.situationPour(3))
        assertEquals(ZoneVolume.MAINTIEN, VolumeLandmarks.situationPour(6))
        assertEquals(ZoneVolume.PRODUCTIF, VolumeLandmarks.situationPour(14))
        assertEquals(ZoneVolume.PLAFOND, VolumeLandmarks.situationPour(22))
        assertEquals(ZoneVolume.AU_DESSUS, VolumeLandmarks.situationPour(30))
    }

    @Test
    fun `une zone jamais travaillee remonte comme deficitaire`() {
        val workouts = listOf(
            seance(lundi, WorkoutType.TorseForce, listOf(exercice("Développé couché", 12))),
        )
        val aDevelopper = VolumeLandmarks.zonesADevelopper(workouts).map { it.zone }
        assertTrue(aDevelopper.contains(MuscleZone.TIRAGE))
        assertTrue(aDevelopper.contains(MuscleZone.COU_POIGNE))
    }

    @Test
    fun `le bilan couvre toutes les zones meme non travaillees`() {
        val bilan = VolumeLandmarks.bilan(emptyList())
        assertEquals(MuscleZone.entries.size, bilan.size)
        assertTrue(bilan.all { it.series == 0 })
    }
}

class EnduranceInterferenceTest {

    private val jour: LocalDate = LocalDate.of(2026, 8, 26)

    @Test
    fun `course et bas du corps lourd le meme jour sont signales`() {
        val workouts = listOf(
            seance(jour, WorkoutType.JambesForce),
            seance(jour, WorkoutType.Hiit),
        )
        val conflits = EnduranceInterference.conflits(jour, workouts)
        assertTrue(conflits.isNotEmpty())
        assertTrue(conflits.any { it.contains("force en premier") })
    }

    @Test
    fun `course la veille d un bas du corps lourd est signalee`() {
        val workouts = listOf(
            seance(jour, WorkoutType.JambesForce),
            seance(jour.minusDays(1), WorkoutType.Hiit),
        )
        assertTrue(EnduranceInterference.conflits(jour, workouts).isNotEmpty())
    }

    @Test
    fun `course et haut du corps ne posent pas de probleme`() {
        val workouts = listOf(
            seance(jour, WorkoutType.TorseForce),
            seance(jour, WorkoutType.Hiit),
        )
        assertTrue(EnduranceInterference.conflits(jour, workouts).isEmpty())
    }

    @Test
    fun `la dose d entretien ne declenche aucune note`() {
        assertNull(EnduranceInterference.noteVolumeCourse(2))
        assertNull(EnduranceInterference.noteVolumeCourse(3))
    }

    @Test
    fun `un volume de course eleve est signale quand la force est prioritaire`() {
        val note = EnduranceInterference.noteVolumeCourse(5)
        assertNotNull(note)
        assertTrue(note!!.contains("2 sorties"))
    }

    @Test
    fun `zero sortie rappelle que deux suffisent a maintenir`() {
        assertNotNull(EnduranceInterference.noteVolumeCourse(0))
    }
}

class OverreachingDetectorTest {

    @Test
    fun `trois baisses consecutives declenchent l alerte`() {
        val charges = listOf(100.0, 97.5, 95.0, 92.5)
        assertEquals(3, OverreachingDetector.seancesConsecutivesEnBaisse(charges))
        assertNotNull(OverreachingDetector.alerte("Squat", charges))
    }

    @Test
    fun `une progression ne declenche rien`() {
        val charges = listOf(90.0, 92.5, 95.0, 97.5)
        assertEquals(0, OverreachingDetector.seancesConsecutivesEnBaisse(charges))
        assertNull(OverreachingDetector.alerte("Squat", charges))
    }

    @Test
    fun `une remontee recente interrompt la serie de baisses`() {
        val charges = listOf(100.0, 95.0, 92.5, 95.0)
        assertEquals(0, OverreachingDetector.seancesConsecutivesEnBaisse(charges))
    }

    @Test
    fun `un historique trop court ne conclut rien`() {
        assertEquals(0, OverreachingDetector.seancesConsecutivesEnBaisse(listOf(100.0)))
        assertNull(OverreachingDetector.alerte("Squat", listOf(100.0, 95.0)))
    }
}

class TypeCourseTest {

    private val jour: LocalDate = LocalDate.of(2026, 8, 26)

    @Test
    fun `une seance mma ne compte plus comme une sortie de course`() {
        val workouts = listOf(
            seance(jour, WorkoutType.JambesForce),
            seance(jour, WorkoutType.MmaWod),
        )
        // Avant l'ajout du type Course, MmaWod servait de proxy : une séance
        // de combat déclenchait donc une alerte d'interférence course/jambes.
        assertTrue(EnduranceInterference.conflits(jour, workouts).isEmpty())
    }

    @Test
    fun `une sortie de course declenche bien l alerte`() {
        val workouts = listOf(
            seance(jour, WorkoutType.JambesForce),
            seance(jour, WorkoutType.Course),
        )
        assertTrue(EnduranceInterference.conflits(jour, workouts).isNotEmpty())
    }

    @Test
    fun `le type course existe dans le plan et se convertit`() {
        assertEquals(
            WorkoutType.Course,
            com.example.mmarecomp.model.PlanDayType.Course.toWorkoutTypeOrNull(),
        )
    }
}

class ContexteSportifTest {

    @Test
    fun `sans combat le multiplicateur d activite baisse`() {
        assertTrue(
            ContexteSportif.SalleUniquement.multiplicateurActivite <
                ContexteSportif.AvecCombat.multiplicateurActivite,
        )
    }

    @Test
    fun `l ecart de maintenance est significatif pour un athlete de 75 kg`() {
        val salle = CalorieCalculator.maintenanceCalories(
            75.0, CalorieCalculator.multiplicateurPour(ContexteSportif.SalleUniquement),
        )
        val combat = CalorieCalculator.maintenanceCalories(
            75.0, CalorieCalculator.multiplicateurPour(ContexteSportif.AvecCombat),
        )
        // 1,4 contre 1,6 sur 75 kg × 30 = 450 kcal d'écart : garder le réglage
        // « combat » alors que le club est fermé transforme une recomposition
        // en surplus sans que rien ne le signale.
        assertEquals(450, combat - salle)
    }

    @Test
    fun `le defaut est le contexte sans combat`() {
        assertTrue(ContexteSportif.SalleUniquement.sansCombat)
        assertTrue(!ContexteSportif.AvecCombat.sansCombat)
    }
}
