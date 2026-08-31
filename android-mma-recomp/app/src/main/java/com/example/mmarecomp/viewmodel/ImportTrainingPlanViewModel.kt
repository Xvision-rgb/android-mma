package com.example.mmarecomp.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.model.NewTrainingPlanDay
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanCreneau
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.PlannedExercise
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.joursLabels
import com.example.mmarecomp.model.planSlotKey
import com.example.mmarecomp.model.slotKey
import com.example.mmarecomp.util.TrainingPlanParser
import com.example.mmarecomp.util.UiPreferences
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.launch

/** Brouillon d'import pour un créneau détecté dans le texte collé — jamais
 *  enregistré tant que l'utilisateur n'a pas validé explicitement. */
data class ImportDayDraft(
    val jourSemaine: Int,
    val creneau: PlanCreneau = PlanCreneau.Matin,
    val exercices: List<PlannedExercise>,
    /** true = les exercices importés viennent s'ajouter à ceux déjà
     *  programmés pour ce créneau ; false = ils les remplacent. Par défaut
     *  à true dès qu'il y a déjà du contenu, pour ne jamais écraser
     *  silencieusement un créneau déjà programmé. */
    val appendToExisting: Boolean,
    val saved: Boolean = false,
) {
    val slotKey: String get() = planSlotKey(jourSemaine, creneau)
    val label: String get() = "${joursLabels[jourSemaine] ?: ""} · ${creneau.label}"
}

/** Import d'un programme d'entraînement collé en texte libre (ex. généré par
 *  Claude) — parsing best-effort (TrainingPlanParser), jamais d'écriture en
 *  base directement depuis le texte : l'utilisateur voit un aperçu éditable
 *  par créneau (mêmes contrôles que TrainingPlanEditScreen) et choisit
 *  explicitement, pour chaque créneau déjà programmé, de compléter ou de
 *  remplacer, avant de valider créneau par créneau ou en un clic pour tous. */
class ImportTrainingPlanViewModel(
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
    context: Context? = null,
) : ViewModel() {
    private val uiPreferences = context?.let { UiPreferences(it) }
    var rawText by mutableStateOf(uiPreferences?.importPlanDraftText.orEmpty())
        private set

    init {
        if (rawText.isBlank()) {
            uiPreferences?.importPlanDraftText?.takeIf { it.isNotBlank() }?.let { rawText = it }
        }
    }

    fun updateRawText(value: String) {
        rawText = value
        uiPreferences?.importPlanDraftText = value
    }
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

    var existingDays by mutableStateOf<Map<String, TrainingPlanDay>>(emptyMap())
        private set
    var drafts by mutableStateOf<List<ImportDayDraft>>(emptyList())
        private set

    fun updatePhase(newPhase: Phase) {
        phase = newPhase
    }

    /** Charge le programme existant — pour savoir quels créneaux ont déjà des
     *  exercices et proposer compléter/remplacer plutôt que d'écraser à
     *  l'aveugle. */
    fun loadExisting() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                existingDays = trainingPlanRepository.fetchWeek(phase).associateBy { it.slotKey() }
            } catch (e: Throwable) {
                rethrowCancellation(e)
                when (e) {
                    is java.io.IOException -> {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
                    }
                    else -> {
                errorMessage = "Impossible de charger le programme existant."
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    /** Ré-analyser le texte (ex. après une correction) ne doit jamais faire
     *  disparaître un créneau déjà enregistré dans cette session d'import : ces
     *  jours gardent leur état "Enregistré ✓" tel quel plutôt que d'être
     *  remplacés par un nouveau brouillon non enregistré. Les brouillons pas
     *  encore enregistrés sont, eux, régénérés depuis le nouveau texte —
     *  d'éventuelles modifications manuelles faites dessus avant une
     *  ré-analyse ne sont pas préservées (cas plus rare, non géré ici). */
    fun parse() {
        hasParsed = true
        val parsed = TrainingPlanParser.parse(rawText)
        val alreadySaved = drafts.filter { it.saved }.associateBy { it.slotKey }
        val fromNewParse = parsed.days
            .filterNot { alreadySaved.containsKey(planSlotKey(it.jourSemaine, it.creneau)) }
            .map { day ->
                val key = planSlotKey(day.jourSemaine, day.creneau)
                val hasExisting = existingDays[key]?.exercices?.isNotEmpty() == true
                ImportDayDraft(
                    jourSemaine = day.jourSemaine,
                    creneau = day.creneau,
                    exercices = day.exercices,
                    appendToExisting = hasExisting,
                )
            }
        drafts = (alreadySaved.values + fromNewParse)
            .sortedWith(compareBy({ it.jourSemaine }, { it.creneau.ordinal }))
    }

    fun setAppendMode(slotKey: String, append: Boolean) {
        drafts = drafts.map { if (it.slotKey == slotKey) it.copy(appendToExisting = append) else it }
    }

    fun updateDraftExercise(slotKey: String, index: Int, updated: PlannedExercise) {
        drafts = drafts.map { draft ->
            if (draft.slotKey != slotKey) draft
            else draft.copy(exercices = draft.exercices.toMutableList().also { it[index] = updated })
        }
    }

    fun removeDraftExercise(slotKey: String, index: Int) {
        drafts = drafts.map { draft ->
            if (draft.slotKey != slotKey) draft
            else draft.copy(exercices = draft.exercices.filterIndexed { i, _ -> i != index })
        }
    }

    fun addDraftExercise(slotKey: String) {
        drafts = drafts.map { draft ->
            if (draft.slotKey != slotKey) draft
            else draft.copy(exercices = draft.exercices + PlannedExercise(nom = "", series = 3, reps = 10))
        }
    }

    private enum class SaveOutcome { SUCCESS, NETWORK_ERROR, OTHER_ERROR }

    private suspend fun saveDayInternal(slotKey: String): SaveOutcome {
        val draft = drafts.firstOrNull { it.slotKey == slotKey } ?: return SaveOutcome.OTHER_ERROR
        if (draft.exercices.any { it.nom.isBlank() }) {
            errorMessage = "${draft.label} a un exercice avec un nom vide — complète-le ou retire-le avant d'enregistrer."
            return SaveOutcome.OTHER_ERROR
        }
        val existing = existingDays[slotKey]
        val finalExercices = if (draft.appendToExisting) {
            (existing?.exercices ?: emptyList()) + draft.exercices
        } else {
            draft.exercices
        }
        return try {
            trainingPlanRepository.upsert(
                NewTrainingPlanDay(
                    jourSemaine = draft.jourSemaine,
                    type = existing?.type ?: PlanDayType.TorseForce,
                    exercices = finalExercices,
                    phase = phase,
                    notes = existing?.notes,
                    creneau = draft.creneau,
                ),
            )
            drafts = drafts.map { if (it.slotKey == slotKey) it.copy(saved = true) else it }
            SaveOutcome.SUCCESS
        } catch (e: Throwable) {
            rethrowCancellation(e)
            when (e) {
                is java.io.IOException -> {
            errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
            SaveOutcome.NETWORK_ERROR
                }
                else -> {
            errorMessage = "Impossible d'enregistrer ce créneau."
            SaveOutcome.OTHER_ERROR
                }
            }
        }
    }

    fun saveDay(slotKey: String, onResult: (Boolean) -> Unit) {
        isSaving = true
        errorMessage = null
        viewModelScope.launch {
            val outcome = saveDayInternal(slotKey)
            isSaving = false
            onResult(outcome == SaveOutcome.SUCCESS)
        }
    }

    /** Enregistre tous les créneaux pas encore sauvegardés, l'un après l'autre.
     *  S'arrête dès qu'une coupure réseau est détectée (pas la peine
     *  d'insister sur les suivants sans connexion) ; une erreur
     *  propre à un créneau (ex. nom d'exercice vide) n'empêche pas de tenter
     *  les autres. */
    fun saveAll(onResult: (successCount: Int, total: Int) -> Unit) {
        isSaving = true
        errorMessage = null
        viewModelScope.launch {
            val pending = drafts.filter { !it.saved }.map { it.slotKey }
            var success = 0
            for (key in pending) {
                val outcome = saveDayInternal(key)
                if (outcome == SaveOutcome.SUCCESS) success++
                if (outcome == SaveOutcome.NETWORK_ERROR) break
            }
            isSaving = false
            onResult(success, pending.size)
        }
    }
}
