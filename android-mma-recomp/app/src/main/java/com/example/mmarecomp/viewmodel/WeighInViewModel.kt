package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.offline.SyncManager
import com.example.mmarecomp.model.NewWeighIn
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInContext
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.PlateauDetector
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.util.TrendPoint
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.components.ScreenError
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.launch

class WeighInViewModel(
    private val repository: WeighInRepository = WeighInRepository(),
    private val syncManager: SyncManager? = null,
) : ViewModel() {
    var date by mutableStateOf(LocalDate.now())
    var heure by mutableStateOf(LocalTime.now())
    var type by mutableStateOf(WeighInType.MatinJeun)
    var poidsKg by mutableStateOf("")
    var bfPct by mutableStateOf("")
    var creatineRecente by mutableStateOf(false)
    var alcoolRecent by mutableStateOf(false)
    var postTraining by mutableStateOf(false)
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var errorOperation by mutableStateOf(ErrorOperation.LOAD)
        private set

    val screenError: ScreenError?
        get() = errorMessage?.let { ScreenError(it, errorOperation) }

    private var pendingDelete: WeighIn? = null

    var history by mutableStateOf<List<WeighIn>>(emptyList())
        private set

    /** Pesée déjà enregistrée pour la date et le type sélectionnés. */
    val existingEntryForSelection: WeighIn?
        get() = history.firstOrNull { it.date == DateUtils.string(date) && it.type == type }

    /** Distinction stricte matin/soir : le graphique de tendance ne prend
     *  jamais les pesées du soir, uniquement le matin à jeun. */
    val trend7Day: List<TrendPoint>
        get() {
            val points = history
                .filter { it.type == WeighInType.MatinJeun }
                .mapNotNull { w -> DateUtils.date(w.date)?.let { TrendPoint(it, w.poidsKg) } }
            return MovingAverage.sevenDay(points)
        }

    val eveningWeighIns: List<WeighIn> get() = history.filter { it.type == WeighInType.Soir }

    /** Jours écoulés depuis la dernière pesée matin à jeun — jamais le poids
     *  lui-même, juste un repère temporel pour encourager la régularité. */
    val daysSinceLastMorningEntry: Long?
        get() {
            val last = history.filter { it.type == WeighInType.MatinJeun }.lastOrNull() ?: return null
            val lastDate = DateUtils.date(last.date) ?: return null
            return java.time.temporal.ChronoUnit.DAYS.between(lastDate, LocalDate.now())
        }

    fun loadHistory(days: Long = 60) {
        viewModelScope.launch {
            try {
                val remote = repository.fetch(DateUtils.daysAgo(days))
                val pending = syncManager?.pendingLocalWeighIns().orEmpty()
                history = (remote + pending).distinctBy { "${it.date}-${it.type}-${it.id}" }
                    .groupBy { "${it.date}-${it.type}" }
                    .map { (_, group) -> group.firstOrNull { !it.id.startsWith("local-") } ?: group.first() }
                    .sortedBy { it.date }
            } catch (e: Throwable) {
                rethrowCancellation(e)
                val pending = syncManager?.pendingLocalWeighIns().orEmpty()
                if (pending.isNotEmpty()) {
                    history = pending.sortedBy { it.date }
                }
                errorOperation = ErrorOperation.LOAD
                when (e) {
                    is java.io.IOException -> {
                        errorMessage = "Pas de connexion internet — affichage des pesées locales si disponibles."
                    }
                    else -> {
                        errorMessage = "Impossible de charger l'historique des pesées."
                    }
                }
            }
        }
    }

    /** Pré-remplit le formulaire avec la dernière pesée du même type — utile
     *  quand le poids n'a presque pas bougé, sans avoir à re-saisir. */
    fun prefillFromLastEntry() {
        val last = history.filter { it.type == type }.lastOrNull() ?: return
        loadEntryIntoForm(last)
    }

    /** Charge une pesée existante dans le formulaire (historique ou créneau). */
    fun loadEntryIntoForm(entry: WeighIn) {
        DateUtils.date(entry.date)?.let { date = it }
        type = entry.type
        poidsKg = entry.poidsKg.toString()
        bfPct = entry.bfPct?.toString() ?: ""
        creatineRecente = entry.contexte.creatineRecente
        alcoolRecent = entry.contexte.alcoolRecent
        postTraining = entry.contexte.postTraining
        entry.heure.takeIf { it.isNotBlank() }?.let { heureStr ->
            runCatching {
                heure = java.time.LocalTime.parse(heureStr, DateTimeFormatter.ofPattern("HH:mm:ss"))
            }
        }
    }

    /** Change de créneau en chargeant la pesée du jour ou en vidant le formulaire. */
    fun selectType(newType: WeighInType) {
        if (newType == type) return
        type = newType
        existingEntryForSelection?.let { loadEntryIntoForm(it) } ?: resetForm()
    }

    /** Charge la pesée du jour pour le type sélectionné, si elle existe. */
    fun loadEntryForCurrentSelection() {
        existingEntryForSelection?.let { loadEntryIntoForm(it) }
    }

    /** Vide le formulaire de saisie (garde la date et le type sélectionnés). */
    fun resetForm() {
        poidsKg = ""
        bfPct = ""
        creatineRecente = false
        alcoolRecent = false
        postTraining = false
    }

    /** Suppression optimiste d'une pesée de l'historique, avec restauration
     *  possible (pattern undo côté écran, comme repas/séances). */
    fun deleteFromHistory(weighIn: WeighIn, onDeleted: () -> Unit) {
        val previous = history
        history = history.filterNot { it.id == weighIn.id }
        pendingDelete = weighIn
        viewModelScope.launch {
            try {
                repository.delete(weighIn.id)
                pendingDelete = null
                onDeleted()
            } catch (e: Throwable) {
                rethrowCancellation(e)
                history = previous
                errorOperation = ErrorOperation.DELETE
                errorMessage = "Impossible de supprimer cette pesée."
            }
        }
    }

    fun retryPendingDelete() {
        val entry = pendingDelete ?: return
        deleteFromHistory(entry) { pendingDelete = null }
    }

    /** Réenregistre une pesée supprimée par erreur (action "Annuler" du snackbar). */
    fun restoreToHistory(weighIn: WeighIn) {
        viewModelScope.launch {
            val restored = NewWeighIn(
                date = weighIn.date,
                heure = weighIn.heure,
                type = weighIn.type,
                poidsKg = weighIn.poidsKg,
                bfPct = weighIn.bfPct,
                contexte = weighIn.contexte,
            )
            try {
                val saved = repository.log(restored)
                history = (history.filterNot { it.date == saved.date && it.type == saved.type }) + saved
            } catch (e: Throwable) {
                rethrowCancellation(e)
                errorOperation = ErrorOperation.SAVE
                errorMessage = "Impossible de restaurer cette pesée."
            }
        }
    }

    fun save(onResult: (Boolean) -> Unit) {
        val poids = poidsKg.replace(",", ".").toDoubleOrNull()
        if (poids == null) {
            errorOperation = ErrorOperation.SAVE
            errorMessage = "Poids invalide."
            onResult(false)
            return
        }
        errorMessage = null
        isSaving = true
        viewModelScope.launch {
            val newWeighIn = NewWeighIn(
                date = DateUtils.string(date),
                heure = heure.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                type = type,
                poidsKg = poids,
                bfPct = bfPct.replace(",", ".").toDoubleOrNull(),
                contexte = WeighInContext(
                    creatineRecente = creatineRecente,
                    alcoolRecent = alcoolRecent,
                    postTraining = postTraining,
                ),
            )
            try {
                val saved = repository.log(newWeighIn)
                history = history.filterNot { it.date == saved.date && it.type == saved.type } + saved
                onResult(true)
            } catch (e: Throwable) {
                rethrowCancellation(e)
                errorOperation = ErrorOperation.SAVE
                errorMessage = "Impossible d'enregistrer la pesée."
                onResult(false)
            } finally {
                isSaving = false
            }
        }
    }

    val plateauStatus: PlateauStatus
        get() {
            val points = history
                .filter { it.type == WeighInType.MatinJeun }
                .mapNotNull { w -> DateUtils.date(w.date)?.let { it to w.poidsKg } }
            return PlateauDetector.detect(points, performanceTrendUp = true)
        }
}
