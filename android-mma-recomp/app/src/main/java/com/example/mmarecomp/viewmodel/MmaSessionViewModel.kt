package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.MmaSessionRepository
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.NewMmaSession
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.ParsedWodMovement
import com.example.mmarecomp.util.WodParser
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
    var recentSessions by mutableStateOf<List<MmaSession>>(emptyList())
        private set

    val parsedMovements: List<ParsedWodMovement> get() = WodParser.parse(wodContent)

    /** Ressenti moyen sur les séances récemment loguées (1 = très difficile,
     *  5 = facile) — repère factuel sur la charge perçue, jamais un objectif
     *  à atteindre. Null tant qu'aucune séance avec ressenti n'est loguée. */
    val averageRessenti: Double?
        get() {
            val values = recentSessions.mapNotNull { it.ressenti }
            if (values.isEmpty()) return null
            return values.average()
        }

    /** Vide le formulaire (garde la date sélectionnée). */
    fun resetForm() {
        wodContent = ""
        roundsSets = ""
        ressenti = 3
        notesTechnique = ""
    }

    fun loadRecent() {
        viewModelScope.launch {
            recentSessions = try {
                repository.fetchRecent()
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
                emptyList()
            } catch (e: Exception) {
                errorMessage = "Impossible de charger l'historique des séances MMA."
                emptyList()
            }
        }
    }

    /** Suppression optimiste avec restauration possible (pattern undo,
     *  comme repas/séances/pesées). */
    fun deleteFromHistory(session: MmaSession, onDeleted: () -> Unit) {
        val previous = recentSessions
        recentSessions = recentSessions.filterNot { it.id == session.id }
        viewModelScope.launch {
            try {
                repository.delete(session.id)
                onDeleted()
            } catch (e: Exception) {
                recentSessions = previous
                errorMessage = "Impossible de supprimer cette séance MMA."
            }
        }
    }

    fun restoreToHistory(session: MmaSession) {
        viewModelScope.launch {
            val restored = NewMmaSession(
                date = session.date,
                wodContent = session.wodContent,
                roundsSets = session.roundsSets,
                ressenti = session.ressenti,
                notesTechnique = session.notesTechnique,
            )
            try {
                val saved = repository.log(restored)
                recentSessions = (recentSessions + saved).sortedByDescending { it.date }
            } catch (e: Exception) {
                errorMessage = "Impossible de restaurer cette séance MMA."
            }
        }
    }

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
                errorMessage = "Impossible d'enregistrer la séance MMA."
                onResult(false)
            } finally {
                isSaving = false
            }
        }
    }
}
