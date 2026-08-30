package com.example.mmarecomp.util

import com.example.mmarecomp.model.PlannedExercise
import kotlin.math.roundToInt

data class ParsedPlanDay(val jourSemaine: Int, val exercices: List<PlannedExercise>)

data class PlanParseResult(
    val days: List<ParsedPlanDay>,
    val ignoredLines: List<String>,
)

/** Parsing best-effort d'un programme d'entraînement collé en texte libre
 *  (ex. généré par Claude) — même esprit que WodParser : ne bloque jamais,
 *  une ligne non reconnue est signalée dans [PlanParseResult.ignoredLines]
 *  plutôt que de faire échouer l'import. Le résultat est toujours présenté
 *  dans un aperçu éditable avant tout enregistrement (jamais écrit en base
 *  directement depuis le parsing). */
object TrainingPlanParser {
    private val dayNames = linkedMapOf(
        "lundi" to 1, "lun" to 1,
        "mardi" to 2, "mar" to 2,
        "mercredi" to 3, "mer" to 3,
        "jeudi" to 4, "jeu" to 4,
        "vendredi" to 5, "ven" to 5,
        "samedi" to 6, "sam" to 6,
        "dimanche" to 7, "dim" to 7,
    )

    private val exerciseRegex = Regex(
        """^\s*(?:[-•*]|\d{1,2}[.):])?\s*([\p{L}][\p{L}0-9''\-. ]{1,40}?)\s*[:\-]?\s*(\d{1,2})\s*(?:x|X|séries?\s*(?:de|x)?)\s*(\d{1,3})(?:\s*-\s*(\d{1,3}))?\s*(?:reps?)?(?:\s*(?:@|à|a)?\s*(\d{1,3}(?:[.,]\d+)?)\s*(kg|lbs?))?\s*$""",
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

    private fun weightInKg(valueRaw: String, unitRaw: String): Double? {
        if (valueRaw.isBlank()) return null
        val value = valueRaw.replace(",", ".").toDoubleOrNull() ?: return null
        return if (unitRaw.lowercase().startsWith("lb")) {
            (value * 0.453592 * 10).roundToInt() / 10.0
        } else {
            value
        }
    }

    /** Ne découpe sur virgule/point-virgule que si ce n'est pas une virgule
     *  décimale entre chiffres (ex. 82,5 kg). */
    private fun splitSegments(line: String): List<String> {
        val segments = mutableListOf<String>()
        var current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == ',' || c == ';') {
                val prev = if (i > 0) line[i - 1] else ' '
                val next = if (i + 1 < line.length) line[i + 1] else ' '
                val betweenDigits = prev.isDigit() && next.isDigit()
                if (!betweenDigits) {
                    val segment = current.toString().trim()
                    if (segment.isNotEmpty()) segments += segment
                    current = StringBuilder()
                    i++
                    continue
                }
            }
            current.append(c)
            i++
        }
        val last = current.toString().trim()
        if (last.isNotEmpty()) segments += last
        return segments
    }

    private fun parseExerciseLine(line: String): List<PlannedExercise> {
        val results = mutableListOf<PlannedExercise>()
        for (segment in splitSegments(line)) {
            val match = exerciseRegex.find(segment) ?: continue
            val (nameRaw, seriesRaw, repsLowRaw, repsHighRaw, weightRaw, unitRaw) = match.destructured
            val series = seriesRaw.toIntOrNull() ?: continue
            val repsLow = repsLowRaw.toIntOrNull() ?: continue
            if (series <= 0 || repsLow <= 0) continue
            val name = nameRaw.trim().trimEnd(':', '-').trim()
            if (name.isBlank()) continue
            val repsHigh = repsHighRaw.toIntOrNull()
            val reps = if (repsHigh != null && repsHigh > repsLow) ((repsLow + repsHigh) / 2.0).roundToInt() else repsLow
            val weight = weightInKg(weightRaw, unitRaw)
            results.add(PlannedExercise(nom = name, series = series, reps = reps, chargeCibleKg = weight))
        }
        return results
    }

    fun parse(text: String): PlanParseResult {
        if (text.isBlank()) return PlanParseResult(emptyList(), emptyList())
        val result = linkedMapOf<Int, MutableList<PlannedExercise>>()
        val ignored = mutableListOf<String>()
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

            val jour = currentDay
            if (jour == null) {
                ignored += line
                continue
            }

            val exercices = parseExerciseLine(line)
            if (exercices.isEmpty()) {
                ignored += line
                continue
            }
            result.getOrPut(jour) { mutableListOf() }.addAll(exercices)
        }

        val days = result.entries.sortedBy { it.key }.map { (jour, exos) -> ParsedPlanDay(jour, exos) }
        return PlanParseResult(days, ignored)
    }
}
