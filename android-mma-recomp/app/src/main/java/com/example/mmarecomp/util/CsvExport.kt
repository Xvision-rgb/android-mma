package com.example.mmarecomp.util

import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.Workout

/** Construit des exports CSV conformes RFC 4180, avec neutralisation des
 *  formules pour un usage sûr dans un tableur. */
object CsvExport {

    private fun encodeCell(raw: String): String {
        val neutralized = when {
            raw.startsWith("=") || raw.startsWith("+") ||
                raw.startsWith("-") || raw.startsWith("@") -> "'$raw"
            else -> raw
        }
        return "\"${neutralized.replace("\"", "\"\"")}\""
    }

    private fun row(cells: List<String>): String = cells.joinToString(",") { encodeCell(it) }

    fun weighIns(history: List<WeighIn>): String = buildString {
        appendLine(row(listOf("date", "heure", "type", "poids_kg", "bf_pct")))
        history.sortedBy { it.date }.forEach { w ->
            appendLine(
                row(
                    listOf(
                        w.date,
                        w.heure,
                        w.type.label,
                        Formatting.oneDecimal(w.poidsKg),
                        w.bfPct?.let { Formatting.oneDecimal(it) } ?: "",
                    ),
                ),
            )
        }
    }

    fun workouts(history: List<Workout>): String = buildString {
        appendLine(
            row(
                listOf(
                    "workout_id",
                    "date",
                    "type",
                    "exercise",
                    "set_index",
                    "charge_kg",
                    "reps",
                    "rir",
                    "amrap",
                    "sangles",
                    "limite_poigne",
                    "duree_min",
                    "rpe",
                    "notes",
                ),
            ),
        )
        history.sortedWith(compareBy({ it.date }, { it.id })).forEach { w ->
            w.exercices.forEach { exercice ->
                exercice.effectiveSets.forEach { set ->
                    appendLine(
                        row(
                            listOf(
                                w.id,
                                w.date,
                                w.type.label,
                                exercice.nom,
                                set.index.toString(),
                                Formatting.oneDecimal(set.chargeKg),
                                set.reps.toString(),
                                set.rir?.toString() ?: "",
                                if (set.estAmrap) "1" else "0",
                                if (set.sangles) "1" else "0",
                                if (set.limitePoigne) "1" else "0",
                                w.dureeMin?.toString() ?: "",
                                w.rpe?.toString() ?: "",
                                w.notes ?: "",
                            ),
                        ),
                    )
                }
            }
            if (w.exercices.isEmpty()) {
                appendLine(
                    row(
                        listOf(
                            w.id,
                            w.date,
                            w.type.label,
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            w.dureeMin?.toString() ?: "",
                            w.rpe?.toString() ?: "",
                            w.notes ?: "",
                        ),
                    ),
                )
            }
        }
    }

    fun meals(history: List<Meal>): String = buildString {
        appendLine(
            row(listOf("date", "repas", "description", "calories", "proteines_g", "glucides_g", "lipides_g")),
        )
        history.sortedWith(compareBy({ it.date }, { it.repas })).forEach { m ->
            appendLine(
                row(
                    listOf(
                        m.date,
                        m.repasSlot?.label ?: m.repas.toString(),
                        m.description ?: "",
                        m.calories.toString(),
                        Formatting.oneDecimal(m.proteinesG),
                        Formatting.oneDecimal(m.glucidesG),
                        Formatting.oneDecimal(m.lipidesG),
                    ),
                ),
            )
        }
    }
}
