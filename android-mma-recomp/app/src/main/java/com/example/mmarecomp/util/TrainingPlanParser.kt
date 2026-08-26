package com.example.mmarecomp.util

import com.example.mmarecomp.model.PlannedExercise
import kotlin.math.roundToInt

data class ParsedPlanDay(val jourSemaine: Int, val exercices: List<PlannedExercise>)

/** Parsing best-effort d'un programme d'entraînement collé en texte libre
 *  (ex. généré par Claude) — même esprit que WodParser : ne bloque jamais,
 *  une ligne non reconnue est simplement ignorée plutôt que de faire
 *  échouer l'import. Le résultat est toujours présenté dans un aperçu
 *  éditable avant tout enregistrement (jamais écrit en base directement
 *  depuis le parsing). */
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

    /** Capture : (1) nom, (2) séries, (3) reps (borne basse), (4) reps borne
     *  haute optionnelle ("8-12" -> moyenne arrondie), (5) charge cible
     *  (valeur), (6) unité (kg ou lb/lbs, convertie en kg). Un préfixe de
     *  liste ("-", "•", "1.", "2)") avant le nom est ignoré. Exemples
     *  reconnus : "Squat 4x8 @80kg", "1. Développé couché 3x10",
     *  "- Tractions 3 séries de 8-10", "Fentes 4x12 185lbs". */
    private val exerciseRegex = Regex(
        """^\s*(?:[-•*]|\d{1,2}[.):])?\s*([\p{L}][\p{L}0-9'’\-. ]{1,40}?)\s*[:\-]?\s*(\d{1,2})\s*(?:x|X|séries?\s*(?:de|x)?)\s*(\d{1,3})(?:\s*-\s*(\d{1,3}))?\s*(?:reps?)?(?:\s*(?:@|à|a)?\s*(\d{1,3}(?:[.,]\d+)?)\s*(kg|lbs?))?\s*$""",
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

    /** Une ligne peut contenir plusieurs exercices séparés par une virgule
     *  ou un point-virgule (Claude compacte parfois plusieurs mouvements sur
     *  une même ligne) — chaque segment est tenté indépendamment. */
    private fun parseExerciseLine(line: String): List<PlannedExercise> {
        val results = mutableListOf<PlannedExercise>()
        for (segment in line.split(",", ";")) {
            val trimmedSegment = segment.trim()
            if (trimmedSegment.isEmpty()) continue
            val match = exerciseRegex.find(trimmedSegment) ?: continue
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
            val exercices = parseExerciseLine(line)
            if (exercices.isEmpty()) continue
            result.getOrPut(jour) { mutableListOf() }.addAll(exercices)
        }

        return result.entries.sortedBy { it.key }.map { (jour, exos) -> ParsedPlanDay(jour, exos) }
    }
}
