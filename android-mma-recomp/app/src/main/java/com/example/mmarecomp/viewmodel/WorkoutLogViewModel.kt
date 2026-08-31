package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.DailyCheckInRepository
import com.example.mmarecomp.data.MmaSessionRepository
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.NewWorkout
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.components.ScreenError
import com.example.mmarecomp.model.toLogged
import com.example.mmarecomp.model.toWorkoutTypeOrNull
import com.example.mmarecomp.util.ExerciseName
import com.example.mmarecomp.util.ApreEngine
import com.example.mmarecomp.util.ApreProtocol
import com.example.mmarecomp.util.ChargeHistory
import com.example.mmarecomp.util.ModulationApplier
import com.example.mmarecomp.util.ModulationSeance
import com.example.mmarecomp.util.ReadinessAction
import com.example.mmarecomp.util.ReadinessCalculator
import com.example.mmarecomp.util.DateUtils
import java.time.LocalDate
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.launch

class WorkoutLogViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
    private val dailyCheckInRepository: DailyCheckInRepository = DailyCheckInRepository(),
    private val mmaSessionRepository: MmaSessionRepository = MmaSessionRepository(),
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
    var errorOperation by mutableStateOf(ErrorOperation.LOAD)
        private set

    val screenError: ScreenError?
        get() = errorMessage?.let { ScreenError(it, errorOperation) }

    private var pendingDeleteWorkout: Workout? = null

    private fun reportError(message: String, operation: ErrorOperation) {
        errorMessage = message
        errorOperation = operation
    }
    var lastSaved by mutableStateOf<Workout?>(null)
        private set
    var recentWorkouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    var prefilledFromPlan by mutableStateOf(false)
        private set
    var workoutsForDate by mutableStateOf<List<Workout>>(emptyList())
        private set

    var modulation by mutableStateOf(
        ModulationSeance(
            action = ReadinessAction.NOMINALE,
            facteurVolume = 1.0,
            facteurCharge = 1.0,
            rirSupplementaire = 0,
            explication = "Chargement…",
        ),
    )
        private set
    var scoreReadiness by mutableStateOf<Int?>(null)
        private set
    var aCheckInAujourdhui by mutableStateOf(false)
        private set
    var modulationApplied by mutableStateOf(false)
        private set
    var rirBonusModulation by mutableStateOf(0)
        private set
    var dernierResumeModulation by mutableStateOf<List<String>>(emptyList())
        private set
    var modulationSnackbarMessage by mutableStateOf<String?>(null)
        private set
    private var checkInsRecents by mutableStateOf<List<DailyCheckIn>>(emptyList())

    /** Séance déjà enregistrée pour la date et le type sélectionnés. */
    val existingWorkoutForType: Workout?
        get() = workoutsForDate.firstOrNull { it.type == type }

    /** Types de séance déjà logués pour la date sélectionnée. */
    val loggedTypesForDate: Set<WorkoutType>
        get() = workoutsForDate.map { it.type }.toSet()

    /** Charge le check-in du jour et la modulation associée. */
    fun loadReadiness() {
        viewModelScope.launch {
            try {
                refreshReadiness()
                maybeAutoApplyModulation()
            } catch (e: Throwable) {
                rethrowCancellation(e)
            }
        }
    }

    private suspend fun refreshReadiness() {
        val fenetre28j = DateUtils.inclusiveStart(28)
        checkInsRecents = dailyCheckInRepository.fetchSince(fenetre28j)
        val workouts = (workoutRepository.fetchWeek(fenetre28j) + recentWorkouts)
            .distinctBy { it.id }
        val mmaSessions = runCatching {
            mmaSessionRepository.fetchSince(fenetre28j)
        }.getOrDefault(emptyList())
        val dateStr = DateUtils.string(date)
        val checkInForDate = checkInsRecents.firstOrNull { it.date == dateStr }
        scoreReadiness = checkInForDate?.score
        aCheckInAujourdhui = checkInForDate != null
        modulation = ReadinessCalculator.modulation(
            checkInToday = checkInForDate,
            checkInsRecents = checkInsRecents,
            workouts = workouts,
            mmaSessions = mmaSessions,
            today = date,
        )
    }

    /** Applique automatiquement la modulation si la séance est chargée et non nominale. */
    private fun maybeAutoApplyModulation(): Boolean {
        if (modulationApplied || exercices.isEmpty()) return false
        if (modulation.action == ReadinessAction.NOMINALE) return false
        if (!applyModulationToWorkout()) return false
        modulationSnackbarMessage = dernierResumeModulation.joinToString(" · ").ifBlank { null }
        return true
    }

    fun clearModulationSnackbar() {
        modulationSnackbarMessage = null
    }

    /** Applique la modulation du jour sur les exercices du formulaire. */
    fun applyModulationToWorkout(): Boolean {
        if (modulationApplied || exercices.isEmpty()) return false
        val result = ModulationApplier.apply(modulation, exercices)
        exercices = result.exercices
        rirBonusModulation = result.rirSupplementaire
        dernierResumeModulation = result.resume
        modulationApplied = true
        return true
    }

    /** Charge les séances de la date sélectionnée (pour chips ✓ et édition). */
    fun loadWorkoutsForDate() {
        viewModelScope.launch {
            val dateStr = DateUtils.string(date)
            workoutsForDate = try {
                workoutRepository.fetchWeek(dateStr).filter { it.date == dateStr }
            } catch (e: Throwable) {
                rethrowCancellation(e)
                emptyList()
            }
        }
    }

    /** Charge une séance de l'historique dans le formulaire. */
    fun loadWorkoutIntoForm(workout: Workout) {
        DateUtils.date(workout.date)?.let { date = it }
        type = workout.type
        exercices = workout.exercices
        dureeMin = workout.dureeMin?.toString() ?: ""
        rpe = workout.rpe?.toString() ?: ""
        notes = workout.notes.orEmpty()
        prefilledFromPlan = false
        viewModelScope.launch { loadWorkoutsForDate() }
    }

    /** Change de type en chargeant la séance existante ou en vidant le brouillon. */
    fun selectType(newType: WorkoutType) {
        if (newType == type) return
        type = newType
        workoutsForDate.firstOrNull { it.type == newType }?.let { loadWorkoutIntoForm(it) } ?: resetForm()
    }

    /** Charge les dernières séances loguées, pour l'historique dépliable. */
    fun loadRecent() {
        viewModelScope.launch {
            recentWorkouts = try {
                workoutRepository.fetchRecent()
            } catch (e: Throwable) {
                rethrowCancellation(e)
                when (e) {
                    is java.io.IOException -> {
                reportError("Pas de connexion internet — réessaie dès que le réseau revient.", ErrorOperation.LOAD)
                emptyList()
                    }
                    else -> {
                reportError("Impossible de charger l'historique des séances.", ErrorOperation.LOAD)
                emptyList()
                    }
                }
            }
        }
    }

    /** Suppression optimiste d'une séance de l'historique, avec restauration
     *  possible via le callback (pattern undo côté écran). */
    fun deleteFromHistory(workout: Workout, onDeleted: () -> Unit) {
        val previous = recentWorkouts
        recentWorkouts = recentWorkouts.filterNot { it.id == workout.id }
        pendingDeleteWorkout = workout
        viewModelScope.launch {
            try {
                workoutRepository.delete(workout.id)
                pendingDeleteWorkout = null
                onDeleted()
            } catch (e: Throwable) {
                rethrowCancellation(e)
                recentWorkouts = previous
                reportError("Impossible de supprimer cette séance.", ErrorOperation.DELETE)
            }
        }
    }

    fun retryPendingDelete() {
        val workout = pendingDeleteWorkout ?: return
        deleteFromHistory(workout) { pendingDeleteWorkout = null }
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
            } catch (e: Throwable) {
                rethrowCancellation(e)
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
        modulationApplied = false
        rirBonusModulation = 0
        dernierResumeModulation = emptyList()
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
            } catch (e: Throwable) {
                rethrowCancellation(e)
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
            } catch (e: Throwable) {
                rethrowCancellation(e)
                when (e) {
                    is java.io.IOException -> {
                reportError("Pas de connexion internet — réessaie dès que le réseau revient.", ErrorOperation.LOAD)
                return@launch
                    }
                    else -> {
                reportError("Impossible de charger le plan de séance.", ErrorOperation.LOAD)
                return@launch
                    }
                }
            } ?: return@launch

            plan.type.toWorkoutTypeOrNull()?.let { type = it }
            exercices = plan.exercices.map { it.toLogged() }
            prefilledFromPlan = true
            modulationApplied = false
            rirBonusModulation = 0
            dernierResumeModulation = emptyList()
            refreshReadiness()
            maybeAutoApplyModulation()
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
        // Garde de bornes : un index périmé (recomposition Compose après un
        // retrait/réordonnancement) ne doit jamais faire crasher la saisie.
        if (index !in exercices.indices) return
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
                // Nettoyé à l'enregistrement, pas à la frappe : couper les
                // espaces pendant que l'utilisateur tape l'empêcherait d'en
                // saisir un entre deux mots.
                exercices = exercices.map { it.copy(nom = ExerciseName.propre(it.nom)) },
                dureeMin = dureeMin.toIntOrNull()?.coerceAtLeast(0),
                rpe = rpe.toIntOrNull()?.coerceIn(1, 10),
                notes = notes.ifBlank { null },
            )
            try {
                val existing = workoutsForDate.firstOrNull { it.type == type }
                if (existing != null) {
                    workoutRepository.delete(existing.id)
                }
                val saved = workoutRepository.log(newWorkout)
                lastSaved = saved
                workoutsForDate = (workoutsForDate.filterNot { it.type == type } + saved)
                recentWorkouts = (listOf(saved) + recentWorkouts.filterNot { it.id == saved.id })
                    .sortedByDescending { it.date }
                onResult(true)
            } catch (e: Throwable) {
                rethrowCancellation(e)
                reportError("Impossible d'enregistrer la séance.", ErrorOperation.SAVE)
                onResult(false)
            } finally {
                isSaving = false
            }
        }
    }
}
