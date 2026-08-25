package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.NewWorkout
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.model.toLogged
import com.example.mmarecomp.model.toWorkoutTypeOrNull
import com.example.mmarecomp.util.DateUtils
import java.time.LocalDate
import kotlinx.coroutines.launch

class WorkoutLogViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
) : ViewModel() {
    var date by mutableStateOf(LocalDate.now())
    var type by mutableStateOf(WorkoutType.JambesForce)
    var exercices by mutableStateOf<List<LoggedExercise>>(emptyList())
    var dureeMin by mutableStateOf("")
    var notes by mutableStateOf("")
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var lastSaved by mutableStateOf<Workout?>(null)
        private set

    /** Pré-remplit la séance depuis le split programmé pour le jour choisi. */
    fun loadPlan(phase: Phase) {
        errorMessage = null
        viewModelScope.launch {
            val jour = DateUtils.weekdayIso(DateUtils.string(date))
            val plan = try {
                trainingPlanRepository.fetchWeek(phase).firstOrNull { it.jourSemaine == jour }
            } catch (e: Exception) {
                errorMessage = "Impossible de charger le plan de séance."
                return@launch
            } ?: return@launch

            plan.type.toWorkoutTypeOrNull()?.let { type = it }
            exercices = plan.exercices.map { it.toLogged() }
        }
    }

    fun addExercise() {
        exercices = exercices + LoggedExercise(nom = "", series = 3, reps = 10)
    }

    fun removeExercise(index: Int) {
        exercices = exercices.filterIndexed { i, _ -> i != index }
    }

    fun updateExercise(index: Int, updated: LoggedExercise) {
        exercices = exercices.toMutableList().also { it[index] = updated }
    }

    fun save(onResult: (Boolean) -> Unit) {
        errorMessage = null
        isSaving = true
        viewModelScope.launch {
            val newWorkout = NewWorkout(
                date = DateUtils.string(date),
                type = type,
                exercices = exercices,
                dureeMin = dureeMin.toIntOrNull(),
                notes = notes.ifBlank { null },
            )
            try {
                lastSaved = workoutRepository.log(newWorkout)
                onResult(true)
            } catch (e: Exception) {
                errorMessage = "Impossible d'enregistrer la séance."
                onResult(false)
            } finally {
                isSaving = false
            }
        }
    }
}
