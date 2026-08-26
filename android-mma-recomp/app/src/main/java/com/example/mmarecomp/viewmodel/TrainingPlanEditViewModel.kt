package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.model.NewTrainingPlanDay
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.PlannedExercise
import kotlinx.coroutines.launch

/** Édite les exercices programmés d'un jour du split hebdo (training_plan) —
 *  jusqu'ici seul le type de séance du jour était modifiable depuis le
 *  Dashboard, la liste d'exercices elle-même n'avait aucune UI d'édition.
 *  Brouillon local modifiable librement, un seul enregistrement explicite
 *  via save() — pas d'écriture à chaque frappe. */
class TrainingPlanEditViewModel(
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
) : ViewModel() {
    var jourSemaine by mutableStateOf(1)
        private set
    var phase by mutableStateOf(Phase.Ete)
        private set
    var type by mutableStateOf(PlanDayType.Repos)
    var exercices by mutableStateOf<List<PlannedExercise>>(emptyList())
        private set
    var notes by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** true si le dernier errorMessage vient d'un enregistrement raté plutôt
     *  que d'un chargement raté — pour que "Réessayer" relance la bonne
     *  action. Un retry qui rechargerait depuis le serveur après un
     *  échec de save() effacerait silencieusement les modifications que
     *  l'utilisateur vient de faire. */
    var errorIsFromSave by mutableStateOf(false)
        private set

    // Instantané du dernier état chargé/enregistré, pour savoir si le
    // brouillon en cours contient des modifications non enregistrées.
    private var loadedSnapshot: Triple<PlanDayType, List<PlannedExercise>, String> =
        Triple(PlanDayType.Repos, emptyList(), "")

    val hasUnsavedChanges: Boolean
        get() = loadedSnapshot != Triple(type, exercices, notes)

    fun load(jourSemaine: Int, phase: Phase) {
        this.jourSemaine = jourSemaine
        this.phase = phase
        isLoading = true
        errorMessage = null
        errorIsFromSave = false
        viewModelScope.launch {
            try {
                val day = trainingPlanRepository.fetchWeek(phase).firstOrNull { it.jourSemaine == jourSemaine }
                type = day?.type ?: PlanDayType.Repos
                exercices = day?.exercices ?: emptyList()
                notes = day?.notes ?: ""
                loadedSnapshot = Triple(type, exercices, notes)
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
            } catch (e: Exception) {
                errorMessage = "Impossible de charger le programme de ce jour."
            } finally {
                isLoading = false
            }
        }
    }

    fun addExercise() {
        exercices = exercices + PlannedExercise(nom = "", series = 3, reps = 10)
    }

    fun updateExercise(index: Int, updated: PlannedExercise) {
        exercices = exercices.toMutableList().also { it[index] = updated }
    }

    fun removeExercise(index: Int) {
        exercices = exercices.filterIndexed { i, _ -> i != index }
    }

    fun duplicateExercise(index: Int) {
        val source = exercices.getOrNull(index) ?: return
        exercices = exercices.toMutableList().also { it.add(index + 1, source.copy()) }
    }

    fun moveExerciseUp(index: Int) {
        if (index <= 0) return
        exercices = exercices.toMutableList().also {
            val tmp = it[index - 1]; it[index - 1] = it[index]; it[index] = tmp
        }
    }

    fun moveExerciseDown(index: Int) {
        if (index >= exercices.size - 1) return
        exercices = exercices.toMutableList().also {
            val tmp = it[index + 1]; it[index + 1] = it[index]; it[index] = tmp
        }
    }

    /** Un exercice avec un nom vide ne peut pas être enregistré — jamais
     *  silencieusement ignoré (l'utilisateur perdrait ses séries/reps/charge
     *  déjà saisies sans le savoir) : on bloque avec un message explicite. */
    val hasBlankExerciseName: Boolean get() = exercices.any { it.nom.isBlank() }

    fun save(onResult: (Boolean) -> Unit) {
        if (hasBlankExerciseName) {
            errorMessage = "Un exercice a un nom vide — complète-le ou retire-le avant d'enregistrer."
            errorIsFromSave = true
            onResult(false)
            return
        }
        isSaving = true
        errorMessage = null
        viewModelScope.launch {
            val newDay = NewTrainingPlanDay(
                jourSemaine = jourSemaine,
                type = type,
                exercices = exercices,
                phase = phase,
                notes = notes.ifBlank { null },
            )
            try {
                trainingPlanRepository.upsert(newDay)
                loadedSnapshot = Triple(type, exercices, notes)
                onResult(true)
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
                errorIsFromSave = true
                onResult(false)
            } catch (e: Exception) {
                errorMessage = "Impossible d'enregistrer le programme."
                errorIsFromSave = true
                onResult(false)
            } finally {
                isSaving = false
            }
        }
    }
}
