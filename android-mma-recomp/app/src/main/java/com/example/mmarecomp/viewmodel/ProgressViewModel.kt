package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.TrendPoint
import java.time.LocalDate
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.launch

data class ChargePoint(val date: LocalDate, val chargeKg: Double)

class ProgressViewModel(
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val mealRepository: MealRepository = MealRepository(),
) : ViewModel() {
    var weighIns by mutableStateOf<List<WeighIn>>(emptyList())
        private set
    var workouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    var meals by mutableStateOf<List<Meal>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var windowWeeks by mutableStateOf(4)

    val weightTrend: List<TrendPoint>
        get() {
            val points = weighIns
                .filter { it.type == WeighInType.MatinJeun }
                .mapNotNull { w -> DateUtils.date(w.date)?.let { TrendPoint(it, w.poidsKg) } }
            return MovingAverage.sevenDay(points)
        }

    val bfTrend: List<TrendPoint>
        get() {
            val points = weighIns
                .filter { it.type == WeighInType.MatinJeun }
                .mapNotNull { w ->
                    val d = DateUtils.date(w.date)
                    val bf = w.bfPct
                    if (d != null && bf != null) TrendPoint(d, bf) else null
                }
            return MovingAverage.sevenDay(points)
        }

    /** Apport calorique quotidien moyenné sur 7 jours — jamais le total brut
     *  d'un seul jour, pour rester cohérent avec le principe de lissage déjà
     *  appliqué au poids (pas de comparaison jour à jour culpabilisante). */
    val caloriesTrend: List<TrendPoint>
        get() {
            val dailyTotals = meals.groupBy { it.date }.mapValues { (_, dayMeals) -> dayMeals.sumOf { it.calories }.toDouble() }
            val points = dailyTotals.mapNotNull { (date, total) -> DateUtils.date(date)?.let { TrendPoint(it, total) } }
            return MovingAverage.sevenDay(points)
        }

    /** Répartition des séances loguées par type sur la fenêtre sélectionnée,
     *  triée par fréquence décroissante — vue d'ensemble de la régularité
     *  par type d'entraînement. */
    val workoutTypeBreakdown: List<Pair<WorkoutType, Int>>
        get() = workouts.groupingBy { it.type }.eachCount().entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

    /** Progression de charge par exercice sur la fenêtre sélectionnée. */
    val chargeProgressionByExercise: Map<String, List<ChargePoint>>
        get() {
            val result = mutableMapOf<String, MutableList<ChargePoint>>()
            for (workout in workouts) {
                val date = DateUtils.date(workout.date) ?: continue
                for (exercice in workout.exercices) {
                    val charge = exercice.chargeMaxKg ?: exercice.chargeReelleKg ?: continue
                    result.getOrPut(exercice.nom) { mutableListOf() }.add(ChargePoint(date, charge))
                }
            }
            return result.mapValues { (_, points) -> points.sortedBy { it.date } }
        }

    /** Volume total d'entraînement (séries × reps × charge réelle) agrégé par
     *  semaine (lundi au dimanche) sur la fenêtre sélectionnée — vue
     *  d'ensemble de la progression du volume global, pas seulement de la
     *  charge par exercice. */
    val weeklyVolumeTrend: List<TrendPoint>
        get() {
            val byWeek = mutableMapOf<LocalDate, Double>()
            for (workout in workouts) {
                val date = DateUtils.date(workout.date) ?: continue
                val weekStart = date.with(
                    java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY),
                )
                val volume = workout.exercices.sumOf { it.volumeTotal }
                byWeek[weekStart] = (byWeek[weekStart] ?: 0.0) + volume
            }
            return byWeek.entries.sortedBy { it.key }.map { (date, vol) -> TrendPoint(date, vol) }
        }

    val performanceTrendUp: Boolean
        get() = chargeProgressionByExercise.values.any { series ->
            val first = series.firstOrNull()?.chargeKg
            val last = series.lastOrNull()?.chargeKg
            first != null && last != null && last > first
        }

    fun load() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            val since = DateUtils.daysAgo(windowWeeks * 7L)
            try {
                weighIns = weighInRepository.fetch(since)
                workouts = workoutRepository.fetchWeek(since)
                meals = mealRepository.fetchSince(since)
            } catch (e: Throwable) {
                rethrowCancellation(e)
                when (e) {
                    is java.io.IOException -> {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
                    }
                    else -> {
                errorMessage = "Impossible de charger la progression."
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }
}
