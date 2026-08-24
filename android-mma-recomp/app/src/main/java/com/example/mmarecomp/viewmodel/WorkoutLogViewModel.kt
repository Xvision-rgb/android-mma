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
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.PersonalRecordDetector
import com.example.mmarecomp.util.toFriendlyMessage
import java.time.LocalDate
import kotlinx.coroutines.launch

class WorkoutLogViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
) : ViewModel() {
    var date by mutableStateOf(LocalDate.now())
    var type by mutableStateOf(
        WorkoutType.entries.find { it.name == AppPreferencesState.preferences.value.defaultWorkoutType }
            ?: WorkoutType.JambesForce,
    )
    var exercices by mutableStateOf<List<LoggedExercise>>(emptyList())
    var dureeMin by mutableStateOf("")
    var notes by mutableStateOf("")
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var lastSaved by mutableStateOf<Workout?>(null)
        private set

    /** Noms des exercices qui viennent de battre leur record personnel lors
     *  du dernier enregistrement — jamais l'inverse (pas de "record manqué"). */
    var newRecords by mutableStateOf<List<String>>(emptyList())
        private set

    /** Historique récent — sert uniquement à afficher la meilleure charge
     *  connue par exercice (préférence "Historique rapide") ; ne bloque
     *  jamais la saisie si le chargement échoue. */
    var recentHistory by mutableStateOf<List<Workout>>(emptyList())
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

    fun loadHistoryIfNeeded() {
        if (!AppPreferencesState.preferences.value.showExerciseHistory || recentHistory.isNotEmpty()) return
        viewModelScope.launch {
            recentHistory = runCatching { workoutRepository.fetchRecent(60) }.getOrDefault(emptyList())
        }
    }

    fun addExercise() {
        val defaults = AppPreferencesState.preferences.value.defaultSeriesRepsByType[type.name]
        exercices = exercices + LoggedExercise(nom = "", series = defaults?.series ?: 3, reps = defaults?.reps ?: 10)
    }

    /** Pré-remplit la durée avec celle de la dernière séance du même type —
     *  préférence "Auto-remplissage de la durée", ne fait rien si un champ a
     *  déjà été saisi. */
    fun autoFillLastDurationIfNeeded() {
        if (!AppPreferencesState.preferences.value.autoFillLastDuration || dureeMin.isNotBlank()) return
        viewModelScope.launch {
            val lastOfType = runCatching { workoutRepository.fetchRecent(30) }
                .getOrDefault(emptyList())
                .firstOrNull { it.type == type }
            lastOfType?.dureeMin?.let { dureeMin = it.toString() }
        }
    }

    /** Recopie les exercices de la dernière séance du même type dans la
     *  séance en cours — un point de départ à ajuster, pas un renvoi
     *  automatique. */
    fun duplicateLastWorkout(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val lastOfType = runCatching { workoutRepository.fetchRecent(30) }
                .getOrDefault(emptyList())
                .firstOrNull { it.type == type }
            if (lastOfType == null) {
                onResult(false)
                return@launch
            }
            exercices = lastOfType.exercices
            lastOfType.dureeMin?.let { dureeMin = it.toString() }
            onResult(true)
        }
    }

    fun removeExercise(index: Int) {
        exercices = exercices.filterIndexed { i, _ -> i != index }
    }

    /** Réinsère un exercice à l'index donné — utilisé par le snackbar "Annuler"
     *  après une suppression accidentelle. */
    fun insertExercise(index: Int, exercise: LoggedExercise) {
        exercices = exercices.toMutableList().apply { add(index.coerceIn(0, size), exercise) }
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
            val cleanedExercices = exercices.filter { it.nom.isNotBlank() }

            // Best-effort : si l'historique ne charge pas, on enregistre quand
            // même la séance, on rate juste la détection de record cette fois.
            val history = runCatching { workoutRepository.fetchRecent(60) }.getOrDefault(emptyList())
            val records = cleanedExercices.mapNotNull { exercice ->
                val charge = exercice.chargeReelleKg
                if (charge != null && PersonalRecordDetector.isNewRecord(exercice.nom, charge, history)) {
                    exercice.nom
                } else {
                    null
                }
            }

            val newWorkout = NewWorkout(
                date = DateUtils.string(date),
                type = type,
                // On ignore les lignes d'exercice laissées sans nom plutôt que
                // d'envoyer des entrées vides en base.
                exercices = cleanedExercices,
                dureeMin = duree,
                notes = notes.ifBlank { null },
            )
            try {
                lastSaved = workoutRepository.log(newWorkout)
                newRecords = records
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
