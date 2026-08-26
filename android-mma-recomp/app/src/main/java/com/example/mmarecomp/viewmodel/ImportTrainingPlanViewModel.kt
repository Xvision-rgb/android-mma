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
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.joursLabels
import com.example.mmarecomp.util.TrainingPlanParser
import kotlinx.coroutines.launch

/** Brouillon d'import pour un jour détecté dans le texte collé — jamais
 *  enregistré tant que l'utilisateur n'a pas validé explicitement. */
data class ImportDayDraft(
    val jourSemaine: Int,
    val exercices: List<PlannedExercise>,
    /** true = les exercices importés viennent s'ajouter à ceux déjà
     *  programmés pour ce jour ; false = ils les remplacent. Par défaut
     *  à true dès qu'il y a déjà du contenu, pour ne jamais écraser
     *  silencieusement un jour déjà programmé. */
    val appendToExisting: Boolean,
    val saved: Boolean = false,
)

/** Import d'un programme d'entraînement collé en texte libre (ex. généré par
 *  Claude) — parsing best-effort (TrainingPlanParser), jamais d'écriture en
 *  base directement depuis le texte : l'utilisateur voit un aperçu éditable
 *  par jour (mêmes contrôles que TrainingPlanEditScreen) et choisit
 *  explicitement, pour chaque jour déjà programmé, de compléter ou de
 *  remplacer, avant de valider jour par jour ou en un clic pour tous. */
class ImportTrainingPlanViewModel(
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
) : ViewModel() {
    var rawText by mutableStateOf("")
    var phase by mutableStateOf(Phase.Ete)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var hasParsed by mutableStateOf(false)
        private set

    var existingDays by mutableStateOf<Map<Int, TrainingPlanDay>>(emptyMap())
        private set
    var drafts by mutableStateOf<List<ImportDayDraft>>(emptyList())
        private set

    fun updatePhase(newPhase: Phase) {
        phase = newPhase
    }

    /** Charge le programme existant — pour savoir quels jours ont déjà des
     *  exercices et proposer compléter/remplacer plutôt que d'écraser à
     *  l'aveugle. */
    fun loadExisting() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                existingDays = trainingPlanRepository.fetchWeek(phase).associateBy { it.jourSemaine }
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
            } catch (e: Exception) {
                errorMessage = "Impossible de charger le programme existant."
            } finally {
                isLoading = false
            }
        }
    }

    fun parse() {
        hasParsed = true
        val parsed = TrainingPlanParser.parse(rawText)
        drafts = parsed.map { day ->
            val hasExisting = existingDays[day.jourSemaine]?.exercices?.isNotEmpty() == true
            ImportDayDraft(jourSemaine = day.jourSemaine, exercices = day.exercices, appendToExisting = hasExisting)
        }
    }

    fun setAppendMode(jourSemaine: Int, append: Boolean) {
        drafts = drafts.map { if (it.jourSemaine == jourSemaine) it.copy(appendToExisting = append) else it }
    }

    fun updateDraftExercise(jourSemaine: Int, index: Int, updated: PlannedExercise) {
        drafts = drafts.map { draft ->
            if (draft.jourSemaine != jourSemaine) draft
            else draft.copy(exercices = draft.exercices.toMutableList().also { it[index] = updated })
        }
    }

    fun removeDraftExercise(jourSemaine: Int, index: Int) {
        drafts = drafts.map { draft ->
            if (draft.jourSemaine != jourSemaine) draft
            else draft.copy(exercices = draft.exercices.filterIndexed { i, _ -> i != index })
        }
    }

    fun addDraftExercise(jourSemaine: Int) {
        drafts = drafts.map { draft ->
            if (draft.jourSemaine != jourSemaine) draft
            else draft.copy(exercices = draft.exercices + PlannedExercise(nom = "", series = 3, reps = 10))
        }
    }

    private enum class SaveOutcome { SUCCESS, NETWORK_ERROR, OTHER_ERROR }

    private suspend fun saveDayInternal(jourSemaine: Int): SaveOutcome {
        val draft = drafts.firstOrNull { it.jourSemaine == jourSemaine } ?: return SaveOutcome.OTHER_ERROR
        if (draft.exercices.any { it.nom.isBlank() }) {
            errorMessage = "${joursLabels[jourSemaine] ?: "Un jour"} a un exercice avec un nom vide — complète-le ou retire-le avant d'enregistrer."
            return SaveOutcome.OTHER_ERROR
        }
        val existing = existingDays[jourSemaine]
        val finalExercices = if (draft.appendToExisting) {
            (existing?.exercices ?: emptyList()) + draft.exercices
        } else {
            draft.exercices
        }
        return try {
            trainingPlanRepository.upsert(
                NewTrainingPlanDay(
                    jourSemaine = jourSemaine,
                    type = existing?.type ?: PlanDayType.Repos,
                    exercices = finalExercices,
                    phase = phase,
                    notes = existing?.notes,
                ),
            )
            drafts = drafts.map { if (it.jourSemaine == jourSemaine) it.copy(saved = true) else it }
            SaveOutcome.SUCCESS
        } catch (e: java.io.IOException) {
            errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
            SaveOutcome.NETWORK_ERROR
        } catch (e: Exception) {
            errorMessage = "Impossible d'enregistrer ce jour."
            SaveOutcome.OTHER_ERROR
        }
    }

    fun saveDay(jourSemaine: Int, onResult: (Boolean) -> Unit) {
        isSaving = true
        errorMessage = null
        viewModelScope.launch {
            val outcome = saveDayInternal(jourSemaine)
            isSaving = false
            onResult(outcome == SaveOutcome.SUCCESS)
        }
    }

    /** Enregistre tous les jours pas encore sauvegardés, l'un après l'autre.
     *  S'arrête dès qu'une coupure réseau est détectée (pas la peine
     *  d'insister sur les jours suivants sans connexion) ; une erreur
     *  propre à un jour (ex. nom d'exercice vide) n'empêche pas de tenter
     *  les autres jours. */
    fun saveAll(onResult: (successCount: Int, total: Int) -> Unit) {
        isSaving = true
        errorMessage = null
        viewModelScope.launch {
            val pending = drafts.filter { !it.saved }.map { it.jourSemaine }
            var success = 0
            for (jour in pending) {
                val outcome = saveDayInternal(jour)
                if (outcome == SaveOutcome.SUCCESS) success++
                if (outcome == SaveOutcome.NETWORK_ERROR) break
            }
            isSaving = false
            onResult(success, pending.size)
        }
    }
}
