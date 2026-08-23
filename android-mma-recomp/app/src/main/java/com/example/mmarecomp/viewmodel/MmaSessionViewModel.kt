package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.MmaSessionRepository
import com.example.mmarecomp.model.NewMmaSession
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.ParsedWodMovement
import com.example.mmarecomp.util.WodParser
import com.example.mmarecomp.util.toFriendlyMessage
import java.time.LocalDate
import kotlinx.coroutines.launch

class MmaSessionViewModel(
    private val repository: MmaSessionRepository = MmaSessionRepository(),
) : ViewModel() {
    var date by mutableStateOf(LocalDate.now())
    var wodContent by mutableStateOf("")
    var roundsSets by mutableStateOf("")
    var ressenti by mutableStateOf(3)
    var notesTechnique by mutableStateOf("")
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val parsedMovements: List<ParsedWodMovement> get() = WodParser.parse(wodContent)

    fun save(onResult: (Boolean) -> Unit) {
        errorMessage = null
        isSaving = true
        viewModelScope.launch {
            val session = NewMmaSession(
                date = DateUtils.string(date),
                wodContent = wodContent,
                roundsSets = roundsSets.ifBlank { null },
                ressenti = ressenti,
                notesTechnique = notesTechnique.ifBlank { null },
            )
            try {
                repository.log(session)
                onResult(true)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible d'enregistrer la séance MMA.")
                onResult(false)
            } finally {
                isSaving = false
            }
        }
    }
}
