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
import com.example.mmarecomp.util.toFriendlyMessage
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
        viewModelScope.launch {
            val jour = DateUtils.weekdayIso(DateUtils.string(date))
            val plan = runCatching { trainingPlanRepository.fetchWeek(phase) }
                .getOrNull()
                ?.firstOrNull { it.jourSemaine == jour } ?: return@launch

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
        val duree = dureeMin.toIntOrNull()
        if (dureeMin.isNotBlank() && (duree == null || duree !in 1..300)) {
            errorMessage = "La durée doit être un nombre de minutes entre 1 et 300."
            onResult(false)
            return
        }

        errorMessage = null
        isSaving = true
        viewModelScope.launch {
            val newWorkout = NewWorkout(
                date = DateUtils.string(date),
                type = type,
                // On ignore les lignes d'exercice laissées sans nom plutôt que
                // d'envoyer des entrées vides en base.
                exercices = exercices.filter { it.nom.isNotBlank() },
                dureeMin = duree,
                notes = notes.ifBlank { null },
            )
            try {
                lastSaved = workoutRepository.log(newWorkout)
                onResult(true)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible d'enregistrer la séance.")
                onResult(false)
            } finally {
                isSaving = false
            }
        }
    }
}
