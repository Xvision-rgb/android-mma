package com.example.mmarecomp.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.DailyCheckInRepository
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.MmaSessionRepository
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.components.ScreenError
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.TrendPoint
import com.example.mmarecomp.util.UiPreferences
import com.example.mmarecomp.util.WeeklyInsights
import com.example.mmarecomp.util.WeeklyInsightsCalculator
import java.time.LocalDate
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.launch

data class ChargePoint(val date: LocalDate, val chargeKg: Double)

class ProgressViewModel(
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val mealRepository: MealRepository = MealRepository(),
    private val dailyCheckInRepository: DailyCheckInRepository = DailyCheckInRepository(),
    private val nutritionTargetRepository: NutritionTargetRepository = NutritionTargetRepository(),
    private val mmaSessionRepository: MmaSessionRepository = MmaSessionRepository(),
    context: Context? = null,
) : ViewModel() {
    private val uiPreferences = context?.let { UiPreferences(it) }
    var weighIns by mutableStateOf<List<WeighIn>>(emptyList())
        private set
    var workouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    var meals by mutableStateOf<List<Meal>>(emptyList())
        private set
    var checkIns by mutableStateOf<List<com.example.mmarecomp.model.DailyCheckIn>>(emptyList())
        private set
    var nutritionTargets by mutableStateOf<List<com.example.mmarecomp.model.NutritionTarget>>(emptyList())
        private set
    var mmaSessions by mutableStateOf<List<com.example.mmarecomp.model.MmaSession>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var errorOperation by mutableStateOf(ErrorOperation.LOAD)
        private set
    var windowWeeks by mutableStateOf(4)

    val screenError: ScreenError?
        get() = errorMessage?.let { ScreenError(it, errorOperation) }

    val weeklyInsights: WeeklyInsights
        get() = WeeklyInsightsCalculator.compute(
            checkIns = checkIns,
            workouts = workouts,
            mmaSessions = mmaSessions,
            meals = meals,
            weighIns = weighIns,
            targets = nutritionTargets,
        )

    fun applyWindowWeeks(weeks: Int) {
        windowWeeks = weeks
        uiPreferences?.progressWindowWeeks = weeks
        load()
    }

    fun markExport(exportKey: String) {
        uiPreferences?.markExport(exportKey)
    }

    fun lastExportLabel(exportKey: String): String? {
        val millis = uiPreferences?.lastExportMillis(exportKey) ?: return null
        val instant = java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        return "Dernier export : ${instant.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))}"
    }

    init {
        uiPreferences?.let { windowWeeks = it.progressWindowWeeks }
    }

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

    val caloriesTrend: List<TrendPoint>
        get() {
            val dailyTotals = meals.groupBy { it.date }.mapValues { (_, dayMeals) -> dayMeals.sumOf { it.calories }.toDouble() }
            val points = dailyTotals.mapNotNull { (date, total) -> DateUtils.date(date)?.let { TrendPoint(it, total) } }
            return MovingAverage.sevenDay(points)
        }

    val workoutTypeBreakdown: List<Pair<WorkoutType, Int>>
        get() = workouts.groupingBy { it.type }.eachCount().entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

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

    val hasAnyData: Boolean
        get() = weighIns.isNotEmpty() || workouts.isNotEmpty() || meals.isNotEmpty()

    fun load() {
        isLoading = true
        errorMessage = null
        errorOperation = ErrorOperation.LOAD
        viewModelScope.launch {
            val since = DateUtils.daysAgo(windowWeeks * 7L)
            val weekSince = DateUtils.inclusiveStart(7)
            try {
                weighIns = weighInRepository.fetch(since)
                workouts = workoutRepository.fetchWeek(since)
                meals = mealRepository.fetchSince(since)
                checkIns = dailyCheckInRepository.fetchSince(weekSince)
                nutritionTargets = nutritionTargetRepository.fetchSince(weekSince)
                mmaSessions = runCatching { mmaSessionRepository.fetchSince(weekSince) }.getOrDefault(emptyList())
            } catch (e: Throwable) {
                rethrowCancellation(e)
                when (e) {
                    is java.io.IOException -> {
                        errorMessage = "Pas de connexion internet — données en cache si disponibles."
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
