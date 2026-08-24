package com.example.mmarecomp.util

import com.example.mmarecomp.model.NewWeighIn
import com.example.mmarecomp.model.WeighInContext
import com.example.mmarecomp.model.WeighInType

/** Import best-effort au format produit par [CsvExporter.weighInsToCsv] —
 *  colonnes retrouvées par nom d'en-tête plutôt que par position, une ligne
 *  invalide est ignorée plutôt que de faire échouer tout l'import. */
object CsvImporter {
    data class WeighInImportResult(val parsed: List<NewWeighIn>, val skipped: Int)

    fun parseWeighIns(csv: String): WeighInImportResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return WeighInImportResult(emptyList(), 0)

        val header = splitCsvLine(lines.first()).map { it.trim().lowercase() }
        val dateIdx = header.indexOf("date")
        val heureIdx = header.indexOf("heure")
        val typeIdx = header.indexOf("type")
        val poidsIdx = header.indexOf("poids_kg")
        val bfIdx = header.indexOf("bf_pct")
        val creatineIdx = header.indexOf("creatine_recente")
        val alcoolIdx = header.indexOf("alcool_recent")
        val postIdx = header.indexOf("post_training")

        if (dateIdx < 0 || heureIdx < 0 || typeIdx < 0 || poidsIdx < 0) {
            return WeighInImportResult(emptyList(), (lines.size - 1).coerceAtLeast(0))
        }

        var skipped = 0
        val parsed = mutableListOf<NewWeighIn>()
        for (line in lines.drop(1)) {
            val cols = splitCsvLine(line)
            val date = cols.getOrNull(dateIdx)?.trim().orEmpty()
            val heure = cols.getOrNull(heureIdx)?.trim().orEmpty()
            val typeValue = cols.getOrNull(typeIdx)?.trim().orEmpty()
            val poids = cols.getOrNull(poidsIdx)?.trim()?.toDoubleOrNull()
            val type = WeighInType.entries.find { it.value == typeValue }

            if (date.isBlank() || heure.isBlank() || type == null || poids == null) {
                skipped++
                continue
            }

            val bf = if (bfIdx >= 0) cols.getOrNull(bfIdx)?.trim()?.toDoubleOrNull() else null
            val contexte = WeighInContext(
                creatineRecente = if (creatineIdx >= 0) cols.getOrNull(creatineIdx)?.trim()?.toBooleanStrictOrNull() ?: false else false,
                alcoolRecent = if (alcoolIdx >= 0) cols.getOrNull(alcoolIdx)?.trim()?.toBooleanStrictOrNull() ?: false else false,
                postTraining = if (postIdx >= 0) cols.getOrNull(postIdx)?.trim()?.toBooleanStrictOrNull() ?: false else false,
            )

            parsed += NewWeighIn(date = date, heure = heure, type = type, poidsKg = poids, bfPct = bf, contexte = contexte)
        }

        return WeighInImportResult(parsed, skipped)
    }

    /** Parseur CSV minimal gérant les champs entre guillemets (virgules ou
     *  guillemets échappés à l'intérieur) — l'inverse de CsvExporter.escape(). */
    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
