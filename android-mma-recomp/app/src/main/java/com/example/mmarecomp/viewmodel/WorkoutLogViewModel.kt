package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.NewWorkout
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.model.toLogged
import com.example.mmarecomp.model.toWorkoutTypeOrNull
import com.example.mmarecomp.util.ApreEngine
import com.example.mmarecomp.util.ApreProtocol
import com.example.mmarecomp.util.ChargeHistory
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
    var rpe by mutableStateOf("")
    var notes by mutableStateOf("")

    /** Protocole d'autorégulation appliqué à la séance. Le type de séance
     *  donne le défaut : une séance de force vise moins de reps par série
     *  qu'une séance d'hypertrophie. */
    val protocoleApre: ApreProtocol
        get() = when (type) {
            WorkoutType.JambesForce, WorkoutType.TorseForce -> ApreProtocol.APRE_6
            else -> ApreProtocol.APRE_10
        }

    /** Incrément de charge disponible en salle. Réglable — tous les gymnases
     *  n'ont pas de disques de 1,25 kg. */
    var incrementChargeKg by mutableStateOf(ApreEngine.INCREMENT_DEFAUT)

    /** Biais d'estimation du RIR, injecté par l'écran depuis RirCalibration
     *  (qui a besoin d'un Context que le ViewModel n'a pas ici). */
    var biaisRir by mutableStateOf(0.0)
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var lastSaved by mutableStateOf<Workout?>(null)
        private set
    var recentWorkouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    var prefilledFromPlan by mutableStateOf(false)
        private set

    /** Charge les dernières séances loguées, pour l'historique dépliable. */
    fun loadRecent() {
        viewModelScope.launch {
            recentWorkouts = try {
                workoutRepository.fetchRecent()
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
                emptyList()
            } catch (e: Exception) {
                errorMessage = "Impossible de charger l'historique des séances."
                emptyList()
            }
        }
    }

    /** Suppression optimiste d'une séance de l'historique, avec restauration
     *  possible via le callback (pattern undo côté écran). */
    fun deleteFromHistory(workout: Workout, onDeleted: () -> Unit) {
        val previous = recentWorkouts
        recentWorkouts = recentWorkouts.filterNot { it.id == workout.id }
        viewModelScope.launch {
            try {
                workoutRepository.delete(workout.id)
                onDeleted()
            } catch (e: Exception) {
                recentWorkouts = previous
                errorMessage = "Impossible de supprimer cette séance."
            }
        }
    }

    /** Réenregistre une séance supprimée par erreur (action "Annuler" du snackbar). */
    fun restoreToHistory(workout: Workout) {
        viewModelScope.launch {
            val restored = NewWorkout(
                date = workout.date,
                type = workout.type,
                exercices = workout.exercices,
                dureeMin = workout.dureeMin,
                rpe = workout.rpe,
                notes = workout.notes,
            )
            try {
                val saved = workoutRepository.log(restored)
                recentWorkouts = (recentWorkouts + saved).sortedByDescending { it.date }
            } catch (e: Exception) {
                errorMessage = "Impossible de restaurer cette séance."
            }
        }
    }

    /** Vide le formulaire en cours (garde la date) — pour repartir d'une
     *  séance vierge sans naviguer ailleurs et revenir. */
    fun resetForm() {
        exercices = emptyList()
        dureeMin = ""
        rpe = ""
        notes = ""
        prefilledFromPlan = false
    }

    /** Reprend le type/exercices/durée de la séance loguée hier, si elle
     *  existe. Ne fait rien sinon. Les exercices d'hier sont ajoutés à ceux
     *  déjà présents dans le formulaire (jamais remplacés) pour ne pas
     *  perdre des exercices déjà saisis manuellement avant l'appui sur ce
     *  bouton. */
    fun duplicateFromYesterday(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val yesterday = DateUtils.string(date.minusDays(1))
                val match = workoutRepository.fetchWeek(yesterday).firstOrNull { it.date == yesterday }
                if (match == null) {
                    onResult(false)
                    return@launch
                }
                type = match.type
                exercices = exercices + match.exercices
                if (dureeMin.isBlank()) dureeMin = match.dureeMin?.toString() ?: ""
                prefilledFromPlan = false
                onResult(true)
            } catch (e: Exception) {
                errorMessage = "Impossible de reprendre la séance d'hier."
                onResult(false)
            }
        }
    }

    /** Pré-remplit la séance depuis le split programmé pour le jour choisi. */
    fun loadPlan(phase: Phase) {
        errorMessage = null
        viewModelScope.launch {
            val jour = DateUtils.weekdayIso(DateUtils.string(date))
            val plan = try {
                trainingPlanRepository.fetchWeek(phase).firstOrNull { it.jourSemaine == jour }
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
                return@launch
            } catch (e: Exception) {
                errorMessage = "Impossible de charger le plan de séance."
                return@launch
            } ?: return@launch

            plan.type.toWorkoutTypeOrNull()?.let { type = it }
            exercices = plan.exercices.map { it.toLogged() }
            prefilledFromPlan = true
        }
    }

    fun addExercise() {
        // Trois séries vides d'emblée : un exercice sans série n'a rien à
        // afficher, et le cas le plus courant reste 3 séries.
        val sets = (1..3).map { i ->
            LoggedSet(index = i, reps = 10, chargeKg = 0.0, estAmrap = i == 3)
        }
        exercices = exercices + LoggedExercise(nom = "", series = 3, reps = 10, sets = sets)
    }

    /** Charge cible pré-remplie avec la dernière charge réelle connue pour ce
     *  nom d'exercice — réduit la friction de saisie (cf. Strong/Hevy qui
     *  pré-chargent le poids de la dernière séance). N'écrase jamais une
     *  charge cible déjà saisie par l'utilisateur : appelé uniquement au
     *  moment où le nom change et que chargeCibleKg est encore vide. */
    fun prefillChargeFromLastKnown(index: Int, exerciseName: String) {
        val exercice = exercices.getOrNull(index) ?: return
        if (exercice.chargeCibleKg != null) return
        val charge = lastKnownCharge(exerciseName) ?: return
        updateExercise(index, exercice.copy(chargeCibleKg = charge))
    }

    /** Record personnel (MAX historique, pas seulement la dernière séance)
     *  de charge réelle pour ce nom d'exercice, sur l'historique déjà
     *  chargé (recentWorkouts). Distinct de lastKnownCharge qui ne renvoie
     *  que la dernière séance — sert à détecter un nouveau record quand la
     *  charge saisie aujourd'hui le dépasse. */
    fun personalRecordCharge(exerciseName: String): Double? =
        ChargeHistory.personalRecordKg(recentWorkouts, exerciseName)

    /** Duplique un exercice juste après lui — pratique pour les supersets
     *  ou une variante de la même série de mouvements. */
    fun duplicateExercise(index: Int) {
        val source = exercices.getOrNull(index) ?: return
        exercices = exercices.toMutableList().also { it.add(index + 1, source.copy()) }
    }

    fun removeExercise(index: Int) {
        exercices = exercices.filterIndexed { i, _ -> i != index }
    }

    /** Réinsère un exercice supprimé par erreur, à sa position d'origine
     *  (ou en fin de liste si l'index n'est plus valide). */
    fun restoreExercise(index: Int, exercice: LoggedExercise) {
        val safeIndex = index.coerceIn(0, exercices.size)
        exercices = exercices.toMutableList().also { it.add(safeIndex, exercice) }
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

    fun updateExercise(index: Int, updated: LoggedExercise) {
        exercices = exercices.toMutableList().also { it[index] = updated }
    }

    /** Dernière charge réelle loguée pour un exercice du même nom, dans
     *  l'historique déjà chargé — simple repère, jamais imposé. */
    /** Volume total (séries × reps × charge réelle) de la dernière séance du
     *  même type que celle en cours (hors séance du jour) — repère de
     *  comparaison factuel, jamais un jugement sur la progression. */
    val previousSessionVolume: Double?
        get() {
            val previous = recentWorkouts
                .filter { it.type == type && it.date != DateUtils.string(date) }
                .maxByOrNull { it.date } ?: return null
            return previous.exercices.sumOf { it.volumeTotal }
        }

    fun lastKnownCharge(exerciseName: String): Double? =
        ChargeHistory.lastKnownChargeKg(recentWorkouts, exerciseName)

    fun save(onResult: (Boolean) -> Unit) {
        errorMessage = null
        isSaving = true
        viewModelScope.launch {
            val newWorkout = NewWorkout(
                date = DateUtils.string(date),
                type = type,
                exercices = exercices,
                dureeMin = dureeMin.toIntOrNull()?.coerceAtLeast(0),
                rpe = rpe.toIntOrNull()?.coerceIn(1, 10),
                notes = notes.ifBlank { null },
            )
            try {
                val saved = workoutRepository.log(newWorkout)
                lastSaved = saved
                recentWorkouts = (listOf(saved) + recentWorkouts.filterNot { it.id == saved.id })
                    .sortedByDescending { it.date }
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
