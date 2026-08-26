package com.example.mmarecomp.util

import com.example.mmarecomp.model.PlannedExercise

data class ParsedPlanDay(val jourSemaine: Int, val exercices: List<PlannedExercise>)

/** Parsing best-effort d'un programme d'entraînement collé en texte libre
 *  (ex. généré par Claude) — même esprit que WodParser : ne bloque jamais,
 *  une ligne non reconnue est simplement ignorée plutôt que de faire
 *  échouer l'import. Le résultat est toujours présenté dans un aperçu
 *  éditable avant tout enregistrement (jamais écrit en base directement
 *  depuis le parsing). */
object TrainingPlanParser {
    private val dayNames = linkedMapOf(
        "lundi" to 1, "mardi" to 2, "mercredi" to 3, "jeudi" to 4,
        "vendredi" to 5, "samedi" to 6, "dimanche" to 7,
    )

    /** Capture : (1) nom, (2) séries, (3) reps, (4) charge cible en kg (optionnelle).
     *  Exemples reconnus : "Squat 4x8 @80kg", "Développé couché 3x10",
     *  "- Tractions 3 séries de 8", "Fentes 4x12 100kg". */
    private val exerciseRegex = Regex(
        """^\s*[-•*]?\s*([\p{L}][\p{L}0-9'’\-. ]{1,40}?)\s*[:\-]?\s*(\d{1,2})\s*(?:x|X|séries?\s*(?:de|x)?)\s*(\d{1,3})\s*(?:reps?)?(?:\s*(?:@|à|a)?\s*(\d{1,3}(?:[.,]\d+)?)\s*kg)?\s*$""",
    )

    private fun matchDay(line: String): Int? {
        val trimmed = line.trim().trimStart('-', '•', '*', ' ').lowercase()
        for ((name, jour) in dayNames) {
            if (trimmed == name || trimmed.startsWith("$name ") || trimmed.startsWith("$name:") || trimmed.startsWith("$name-")) {
                return jour
            }
        }
        return null
    }

    fun parse(text: String): List<ParsedPlanDay> {
        if (text.isBlank()) return emptyList()
        val result = linkedMapOf<Int, MutableList<PlannedExercise>>()
        var currentDay: Int? = null

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val day = matchDay(line)
            if (day != null) {
                currentDay = day
                result.getOrPut(day) { mutableListOf() }
                continue
            }

            val jour = currentDay ?: continue
            val match = exerciseRegex.find(line) ?: continue
            val (nameRaw, seriesRaw, repsRaw, weightRaw) = match.destructured
            val series = seriesRaw.toIntOrNull() ?: continue
            val reps = repsRaw.toIntOrNull() ?: continue
            if (series <= 0 || reps <= 0) continue
            val name = nameRaw.trim().trimEnd(':', '-').trim()
            if (name.isBlank()) continue
            val weight = weightRaw.replace(",", ".").toDoubleOrNull()

            result.getOrPut(jour) { mutableListOf() }.add(
                PlannedExercise(nom = name, series = series, reps = reps, chargeCibleKg = weight),
            )
        }

        return result.entries.sortedBy { it.key }.map { (jour, exos) -> ParsedPlanDay(jour, exos) }
    }
}
