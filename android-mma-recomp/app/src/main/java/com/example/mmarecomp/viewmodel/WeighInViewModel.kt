package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.WeighInRepository
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
import kotlinx.coroutines.launch

class WeighInViewModel(
    private val repository: WeighInRepository = WeighInRepository(),
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

    var history by mutableStateOf<List<WeighIn>>(emptyList())
        private set

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

    fun loadHistory(days: Long = 60) {
        viewModelScope.launch {
            try {
                history = repository.fetch(DateUtils.daysAgo(days))
            } catch (e: Exception) {
                errorMessage = "Impossible de charger l'historique des pesées."
            }
        }
    }

    fun save(onResult: (Boolean) -> Unit) {
        val poids = poidsKg.replace(",", ".").toDoubleOrNull()
        if (poids == null) {
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
            } catch (e: Exception) {
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
