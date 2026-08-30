package com.example.mmarecomp.util

import com.example.mmarecomp.model.CalorieMode
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.PlannedExercise
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInContext
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsInclusiveStartTest {

    @Test
    fun `7 jours inclusifs se terminent au jour de reference sans jour de trop`() {
        val from = LocalDate.of(2026, 8, 30)
        assertEquals("2026-08-24", DateUtils.inclusiveStart(7, from))
        // daysAgo(7) depuis le 30 = 23 août : 8 jours calendaires, trop large.
        assertNotEquals("2026-08-23", DateUtils.inclusiveStart(7, from))
    }

    @Test
    fun `28 jours inclusifs partent 27 jours plus tot`() {
        assertEquals("2026-08-03", DateUtils.inclusiveStart(28, LocalDate.of(2026, 8, 30)))
    }
}

class WeighInSelectorTest {

    private fun weighIn(date: String, type: WeighInType, poids: Double) = WeighIn(
        id = "$date-$type",
        userId = "u",
        date = date,
        heure = "07:00:00",
        type = type,
        poidsKg = poids,
        bfPct = 12.0,
        contexte = WeighInContext(),
    )

    @Test
    fun `une pesee du soir plus recente ne prime pas sur le matin a jeun`() {
        val ins = listOf(
            weighIn("2026-08-29", WeighInType.MatinJeun, 80.0),
            weighIn("2026-08-30", WeighInType.Soir, 82.4),
        )
        val ref = WeighInSelector.latestReference(ins)!!
        assertEquals(WeighInType.MatinJeun, ref.type)
        assertEquals(80.0, ref.poidsKg, 0.001)
    }

    @Test
    fun `sans matin on retombe sur la derniere pesee disponible`() {
        val ins = listOf(weighIn("2026-08-30", WeighInType.Soir, 81.0))
        assertEquals(81.0, WeighInSelector.latestReference(ins)!!.poidsKg, 0.001)
    }

    @Test
    fun `un poids nul n est jamais une reference`() {
        val ins = listOf(weighIn("2026-08-30", WeighInType.MatinJeun, 0.0))
        assertNull(WeighInSelector.latestReference(ins))
    }
}

class ActivityStreakTest {

    @Test
    fun `la serie depasse 7 jours quand l historique le permet`() {
        val today = LocalDate.of(2026, 8, 30)
        val dates = (0L..13L).map { DateUtils.string(today.minusDays(it)) }.toSet()
        assertEquals(14, ActivityStreak.days(dates, today))
    }

    @Test
    fun `un trou casse la serie sans descendre sous zero`() {
        val today = LocalDate.of(2026, 8, 30)
        val dates = setOf("2026-08-30", "2026-08-29", "2026-08-27")
        assertEquals(2, ActivityStreak.days(dates, today))
    }
}

class MovingAverageDirectionTest {

    @Test
    fun `la direction ignore une hausse ancienne hors de la fenetre 7 jours`() {
        val points = listOf(
            TrendPoint(LocalDate.of(2026, 7, 1), 90.0),
            TrendPoint(LocalDate.of(2026, 8, 23), 80.0),
            TrendPoint(LocalDate.of(2026, 8, 30), 79.5),
        )
        // Premier → dernier = baisse de 10 kg (2 mois). Sur 7 jours : légère baisse.
        assertEquals(TrendDirection.BAISSE, MovingAverage.direction(points, lookbackDays = 7))
    }

    @Test
    fun `une serie trop courte reste indeterminee`() {
        val one = listOf(TrendPoint(LocalDate.of(2026, 8, 30), 80.0))
        assertEquals(TrendDirection.INDETERMINE, MovingAverage.direction(one))
    }
}

class TodayPlanResolverTest {

    private val lundi = LocalDate.of(2026, 8, 24) // ISO weekday 1

    private fun plan(jour: Int, type: PlanDayType, vararg noms: String) = TrainingPlanDay(
        id = "p$jour",
        userId = "u",
        jourSemaine = jour,
        type = type,
        exercices = noms.map { PlannedExercise(it, 3, 8) },
        phase = Phase.Ete,
    )

    private fun workout(date: LocalDate, type: WorkoutType) = Workout(
        id = "$date-$type",
        userId = "u",
        date = DateUtils.string(date),
        type = type,
        exercices = emptyList(),
    )

    @Test
    fun `un HIIT du matin ne masque pas la seance de force prevue`() {
        val plan = listOf(plan(1, PlanDayType.JambesForce, "Squat"))
        val workouts = listOf(workout(lundi, WorkoutType.Hiit))
        val unresolved = TodayPlanResolver.unresolvedToday(plan, workouts, lundi)
        assertNotNull(unresolved)
        assertEquals(PlanDayType.JambesForce, unresolved!!.type)
    }

    @Test
    fun `un jour de repos reste visible meme si une autre seance a ete loguee`() {
        val plan = listOf(plan(1, PlanDayType.Repos))
        val workouts = listOf(workout(lundi, WorkoutType.Hiit))
        assertEquals(PlanDayType.Repos, TodayPlanResolver.unresolvedToday(plan, workouts, lundi)?.type)
    }

    @Test
    fun `le plan disparait seulement si le type prevu est deja logue`() {
        val plan = listOf(plan(1, PlanDayType.JambesForce, "Squat"))
        val workouts = listOf(workout(lundi, WorkoutType.JambesForce))
        assertNull(TodayPlanResolver.unresolvedToday(plan, workouts, lundi))
    }

    @Test
    fun `la suggestion est le premier exercice du plan du jour, jamais aleatoire`() {
        val plan = listOf(plan(1, PlanDayType.TorseForce, "Développé couché", "Rowing"))
        val suggestion = TodayPlanResolver.suggestedExercise(plan, emptyList(), lundi)
        assertEquals("Développé couché" to "Torse force", suggestion)
        assertEquals(suggestion, TodayPlanResolver.suggestedExercise(plan, emptyList(), lundi))
    }

    @Test
    fun `si le jour est fait on propose le prochain jour d entrainement`() {
        val plan = listOf(
            plan(1, PlanDayType.JambesForce, "Squat"),
            plan(2, PlanDayType.TorseForce, "Développé couché"),
        )
        val workouts = listOf(workout(lundi, WorkoutType.JambesForce))
        val suggestion = TodayPlanResolver.suggestedExercise(plan, workouts, lundi)
        assertEquals("Développé couché" to "Torse force", suggestion)
    }
}

class ChargeHistoryTest {

    private fun workout(date: String, nom: String, vararg charges: Double) = Workout(
        id = "$date-$nom",
        userId = "u",
        date = date,
        type = WorkoutType.TorseForce,
        exercices = listOf(
            LoggedExercise(
                nom = nom,
                series = charges.size,
                reps = 5,
                chargeReelleKg = charges.first(),
                sets = charges.mapIndexed { i, c ->
                    LoggedSet(index = i + 1, reps = 5, chargeKg = c)
                },
            ),
        ),
    )

    @Test
    fun `le record lit la serie la plus lourde, pas l agregat fige`() {
        val workouts = listOf(workout("2026-08-20", "Squat", 100.0, 120.0))
        assertEquals(120.0, ChargeHistory.personalRecordKg(workouts, "squat")!!, 0.001)
    }

    @Test
    fun `la derniere charge connue vient de la seance la plus recente`() {
        val workouts = listOf(
            workout("2026-08-10", "Squat", 140.0),
            workout("2026-08-24", "Squat", 110.0, 115.0),
        )
        assertEquals(115.0, ChargeHistory.lastKnownChargeKg(workouts, "Squat")!!, 0.001)
    }
}

class NutritionTargetDraftTest {

    @Test
    fun `une cible manuelle conserve glucides et lipides deja enregistres`() {
        val existing = NutritionTarget(
            id = "t",
            userId = "u",
            date = "2026-08-30",
            typeJour = TypeJour.Training,
            caloriesCible = 2800,
            proteinesCibleG = 160.0,
            glucidesCibleG = 350.0,
            lipidesCibleG = 80.0,
        )
        val draft = NutritionTargetDraft.custom("2026-08-30", 3000, 170.0, existing)
        assertEquals(350.0, draft.glucidesCibleG!!, 0.001)
        assertEquals(80.0, draft.lipidesCibleG!!, 0.001)
        assertEquals(3000, draft.caloriesCible)
    }

    @Test
    fun `fromGoal pousse les macros calculees au lieu de les laisser nuls`() {
        val goal = CalorieCalculator.goal(80.0, 12.0, CalorieMode.Recomposition)
        val draft = NutritionTargetDraft.fromGoal("2026-08-30", TypeJour.Training, goal)
        assertTrue(draft.glucidesCibleG != null && draft.glucidesCibleG!! > 0)
        assertTrue(draft.lipidesCibleG != null && draft.lipidesCibleG!! > 0)
        assertEquals(goal.targetCalories, draft.caloriesCible)
    }
}
