package com.example.mmarecomp.util

import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.Workout

/** Construit un CSV simple de l'historique des pesées, pour export/partage
 *  (tableur perso, backup) — colonnes stables, point décimal. */
object CsvExport {
    fun weighIns(history: List<WeighIn>): String = buildString {
        appendLine("date,heure,type,poids_kg,bf_pct")
        history.sortedBy { it.date }.forEach { w ->
            append(w.date).append(',')
            append(w.heure).append(',')
            append(w.type.label).append(',')
            append(Formatting.oneDecimal(w.poidsKg)).append(',')
            append(w.bfPct?.let { Formatting.oneDecimal(it) } ?: "")
            appendLine()
        }
    }

    fun workouts(history: List<Workout>): String = buildString {
        appendLine("date,type,nb_exercices,duree_min,notes")
        history.sortedBy { it.date }.forEach { w ->
            append(w.date).append(',')
            append(w.type.label).append(',')
            append(w.exercices.size).append(',')
            append(w.dureeMin?.toString() ?: "").append(',')
            append((w.notes ?: "").replace(",", ";").replace("\n", " "))
            appendLine()
        }
    }
}
