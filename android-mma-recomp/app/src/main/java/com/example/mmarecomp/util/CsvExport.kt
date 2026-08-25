package com.example.mmarecomp.util

import com.example.mmarecomp.model.WeighIn

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
}
