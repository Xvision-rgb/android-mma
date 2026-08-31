package com.example.mmarecomp.util

import com.example.mmarecomp.model.PlanCreneau
import com.example.mmarecomp.model.PlannedExercise
import com.example.mmarecomp.model.PlannedExerciseUnit
import kotlin.math.roundToInt

data class ParsedPlanDay(
    val jourSemaine: Int,
    val exercices: List<PlannedExercise>,
    val creneau: PlanCreneau = PlanCreneau.Matin,
)

data class PlanParseResult(
    val days: List<ParsedPlanDay>,
    val ignoredLines: List<String>,
)

/** Parsing best-effort d'un programme d'entraînement collé en texte libre
 *  (ex. généré par Claude) — même esprit que WodParser : ne bloque jamais,
 *  une ligne non reconnue est signalée dans [PlanParseResult.ignoredLines]
 *  plutôt que de faire échouer l'import. Le résultat est toujours présenté
 *  dans un aperçu éditable avant tout enregistrement (jamais écrit en base
 *  directement depuis le parsing).
 *
 *  Créneaux : "Lundi — Soir", "Mardi matin", ou sous-titres "Soir" / "Matin"
 *  sous un jour. Unités : `3x8`, `3x45s`, `20 min`, `2x40m`. */
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

    /** NxM @charge — reps (défaut). */
    private val repsRegex = Regex(
        """^\s*(?:[-•*]|\d{1,2}[.):])?\s*([\p{L}][\p{L}0-9''\-. ]{1,40}?)\s*[:\-]?\s*(\d{1,2})\s*(?:x|X|séries?\s*(?:de|x)?)\s*(\d{1,3})(?:\s*-\s*(\d{1,3}))?\s*(?:reps?)?(?:\s*(?:@|à|a)?\s*(\d{1,3}(?:[.,]\d+)?)\s*(kg|lbs?))?\s*$""",
    )

    /** NxMs / NxM sec — secondes (isométrie, farmer hold…). */
    private val secondesRegex = Regex(
        """^\s*(?:[-•*]|\d{1,2}[.):])?\s*([\p{L}][\p{L}0-9''\-. ]{1,40}?)\s*[:\-]?\s*(\d{1,2})\s*(?:x|X)\s*(\d{1,3})\s*(?:s|sec|secs|secondes?)\b(?:\s*(?:@|à|a)?\s*(\d{1,3}(?:[.,]\d+)?)\s*(kg|lbs?))?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /** NxNm / NxN m — mètres. */
    private val metresRegex = Regex(
        """^\s*(?:[-•*]|\d{1,2}[.):])?\s*([\p{L}][\p{L}0-9''\-. ]{1,40}?)\s*[:\-]?\s*(\d{1,2})\s*(?:x|X)\s*(\d{1,4})\s*(?:m|mètres?|metres?)\b(?:\s*(?:@|à|a)?\s*(\d{1,3}(?:[.,]\d+)?)\s*(kg|lbs?))?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    /** N min / NxN min — minutes (cardio). */
    private val minutesRegex = Regex(
        """^\s*(?:[-•*]|\d{1,2}[.):])?\s*([\p{L}][\p{L}0-9''\-. ]{1,40}?)\s*[:\-]?\s*(?:(\d{1,2})\s*(?:x|X)\s*)?(\d{1,3})\s*(?:min|minutes?)\b(?:\s*(?:@|à|a)?\s*(\d{1,3}(?:[.,]\d+)?)\s*(kg|lbs?))?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private data class DayHeader(val jour: Int, val creneau: PlanCreneau?)

    private fun matchCreneauToken(text: String): PlanCreneau? {
        val t = text.lowercase()
        return when {
            t.contains("soir") || t.contains("maison") || t.contains("evening") -> PlanCreneau.Soir
            t.contains("matin") || t.contains("salle") || t.contains("morning") -> PlanCreneau.Matin
            else -> null
        }
    }

    private fun matchDay(line: String): DayHeader? {
        val trimmed = line.trim().trimStart('-', '•', '*', ' ').lowercase()
        for ((name, jour) in dayNames) {
            if (trimmed == name ||
                trimmed.startsWith("$name ") ||
                trimmed.startsWith("$name:") ||
                trimmed.startsWith("$name-") ||
                trimmed.startsWith("$name—") ||
                trimmed.startsWith("$name–")
            ) {
                val rest = trimmed.removePrefix(name).trim().trimStart(':', '-', '—', '–', ' ', '(')
                val creneau = matchCreneauToken(rest)
                return DayHeader(jour, creneau)
            }
        }
        return null
    }

    /** Sous-titre de créneau sans jour : "Soir", "Matin — maison", "SOIR — MAISON". */
    private fun matchCreneauOnly(line: String): PlanCreneau? {
        val trimmed = line.trim().trimStart('-', '•', '*', ' ')
        if (trimmed.length > 40) return null
        // Ne pas confondre avec un exercice qui commence par un chiffre / NxM.
        if (Regex("""\d\s*[xX]""").containsMatchIn(trimmed)) return null
        val lower = trimmed.lowercase()
        val onlyCreneau = lower in setOf(
            "soir", "matin", "maison", "salle",
            "soir — maison", "soir - maison", "soir maison",
            "matin — salle", "matin - salle", "matin salle",
            "soir — maison", "créneau soir", "creneau soir",
            "créneau matin", "creneau matin",
        ) || (
            (lower.startsWith("soir") || lower.startsWith("matin") ||
                lower.startsWith("maison") || lower.startsWith("salle")) &&
                !lower.any { it.isDigit() }
            )
        if (!onlyCreneau) return null
        return matchCreneauToken(lower)
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

    private fun cleanName(raw: String): String =
        raw.trim().trimEnd(':', '-').trim()

    private fun parseSegment(segment: String): PlannedExercise? {
        secondesRegex.find(segment)?.let { match ->
            val (nameRaw, seriesRaw, qtyRaw, weightRaw, unitRaw) = match.destructured
            val series = seriesRaw.toIntOrNull()?.takeIf { it > 0 } ?: return@let
            val qty = qtyRaw.toIntOrNull()?.takeIf { it > 0 } ?: return@let
            val name = cleanName(nameRaw)
            if (name.isBlank()) return@let
            return PlannedExercise(
                nom = name,
                series = series,
                reps = qty,
                chargeCibleKg = weightInKg(weightRaw, unitRaw),
                unite = PlannedExerciseUnit.Secondes,
            )
        }
        metresRegex.find(segment)?.let { match ->
            val (nameRaw, seriesRaw, qtyRaw, weightRaw, unitRaw) = match.destructured
            val series = seriesRaw.toIntOrNull()?.takeIf { it > 0 } ?: return@let
            val qty = qtyRaw.toIntOrNull()?.takeIf { it > 0 } ?: return@let
            val name = cleanName(nameRaw)
            if (name.isBlank()) return@let
            return PlannedExercise(
                nom = name,
                series = series,
                reps = qty,
                chargeCibleKg = weightInKg(weightRaw, unitRaw),
                unite = PlannedExerciseUnit.Metres,
            )
        }
        minutesRegex.find(segment)?.let { match ->
            val (nameRaw, seriesRaw, qtyRaw, weightRaw, unitRaw) = match.destructured
            val series = seriesRaw.toIntOrNull()?.takeIf { it > 0 } ?: 1
            val qty = qtyRaw.toIntOrNull()?.takeIf { it > 0 } ?: return@let
            val name = cleanName(nameRaw)
            if (name.isBlank()) return@let
            return PlannedExercise(
                nom = name,
                series = series,
                reps = qty,
                chargeCibleKg = weightInKg(weightRaw, unitRaw),
                unite = PlannedExerciseUnit.Minutes,
            )
        }
        repsRegex.find(segment)?.let { match ->
            val (nameRaw, seriesRaw, repsLowRaw, repsHighRaw, weightRaw, unitRaw) = match.destructured
            val series = seriesRaw.toIntOrNull() ?: return@let
            val repsLow = repsLowRaw.toIntOrNull() ?: return@let
            if (series <= 0 || repsLow <= 0) return@let
            val name = cleanName(nameRaw)
            if (name.isBlank()) return@let
            val repsHigh = repsHighRaw.toIntOrNull()
            val reps = if (repsHigh != null && repsHigh > repsLow) {
                ((repsLow + repsHigh) / 2.0).roundToInt()
            } else {
                repsLow
            }
            return PlannedExercise(
                nom = name,
                series = series,
                reps = reps,
                chargeCibleKg = weightInKg(weightRaw, unitRaw),
                unite = PlannedExerciseUnit.Reps,
            )
        }
        return null
    }

    private fun parseExerciseLine(line: String): List<PlannedExercise> =
        splitSegments(line).mapNotNull { parseSegment(it) }

    fun parse(text: String): PlanParseResult {
        if (text.isBlank()) return PlanParseResult(emptyList(), emptyList())
        val result = linkedMapOf<Pair<Int, PlanCreneau>, MutableList<PlannedExercise>>()
        val ignored = mutableListOf<String>()
        var currentDay: Int? = null
        var currentCreneau: PlanCreneau = PlanCreneau.Matin

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val dayHeader = matchDay(line)
            if (dayHeader != null) {
                currentDay = dayHeader.jour
                currentCreneau = dayHeader.creneau ?: PlanCreneau.Matin
                result.getOrPut(currentDay!! to currentCreneau) { mutableListOf() }
                continue
            }

            val creneauOnly = matchCreneauOnly(line)
            if (creneauOnly != null && currentDay != null) {
                currentCreneau = creneauOnly
                result.getOrPut(currentDay to currentCreneau) { mutableListOf() }
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
            result.getOrPut(jour to currentCreneau) { mutableListOf() }.addAll(exercices)
        }

        val days = result.entries
            .sortedWith(compareBy({ it.key.first }, { it.key.second.ordinal }))
            .map { (key, exos) -> ParsedPlanDay(key.first, exos, key.second) }
        return PlanParseResult(days, ignored)
    }
}
