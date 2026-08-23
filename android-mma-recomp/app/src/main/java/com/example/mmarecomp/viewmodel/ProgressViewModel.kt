package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.PersonalRecordDetector
import com.example.mmarecomp.util.TrendPoint
import com.example.mmarecomp.util.toFriendlyMessage
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class ChargePoint(val date: LocalDate, val chargeKg: Double)

class ProgressViewModel(
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
) : ViewModel() {
    var weighIns by mutableStateOf<List<WeighIn>>(emptyList())
        private set
    var workouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    var allTimeWorkouts by mutableStateOf<List<Workout>>(emptyList())
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

    /** Progression de charge par exercice sur la fenêtre sélectionnée. */
    val chargeProgressionByExercise: Map<String, List<ChargePoint>>
        get() {
            val result = mutableMapOf<String, MutableList<ChargePoint>>()
            for (workout in workouts) {
                val date = DateUtils.date(workout.date) ?: continue
                for (exercice in workout.exercices) {
                    val charge = exercice.chargeReelleKg ?: continue
                    result.getOrPut(exercice.nom) { mutableListOf() }.add(ChargePoint(date, charge))
                }
            }
            return result.mapValues { (_, points) -> points.sortedBy { it.date } }
        }

    val performanceTrendUp: Boolean
        get() = chargeProgressionByExercise.values.any { series ->
            val first = series.firstOrNull()?.chargeKg
            val last = series.lastOrNull()?.chargeKg
            first != null && last != null && last > first
        }

    /** Meilleure charge jamais loggée par exercice, sur tout l'historique
     *  (pas seulement la fenêtre 4/8 semaines) — toujours une célébration,
     *  jamais affiché comme "manquant" pour un exercice jamais fait. */
    val personalBests: List<Pair<String, Double>>
        get() {
            val displayNameByKey = LinkedHashMap<String, String>()
            for (workout in allTimeWorkouts) {
                for (exercice in workout.exercices) {
                    val key = exercice.nom.trim().lowercase()
                    displayNameByKey.putIfAbsent(key, exercice.nom.trim())
                }
            }
            return displayNameByKey.values
                .mapNotNull { name -> PersonalRecordDetector.bestKnownLoad(name, allTimeWorkouts)?.let { name to it } }
                .sortedBy { it.first }
        }

    fun load() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            val since = DateUtils.daysAgo(windowWeeks * 7L)
            try {
                coroutineScope {
                    val weighInsDeferred = async { weighInRepository.fetch(since) }
                    val workoutsDeferred = async { workoutRepository.fetchWeek(since) }
                    val allTimeDeferred = async { workoutRepository.fetchAll() }
                    weighIns = weighInsDeferred.await()
                    workouts = workoutsDeferred.await()
                    allTimeWorkouts = allTimeDeferred.await()
                }
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de charger la progression.")
            } finally {
                isLoading = false
            }
        }
    }

    /** Séances les plus récentes en premier — pour l'historique avec suppression. */
    val recentWorkoutsDescending: List<Workout>
        get() = workouts.sortedByDescending { it.date }

    fun deleteWorkout(workout: Workout, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                workoutRepository.delete(workout.id)
                workouts = workouts.filterNot { it.id == workout.id }
                onResult(true)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de supprimer cette séance.")
                onResult(false)
            }
        }
    }
}
