package com.example.mmarecomp.util

import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.Workout

/** Export CSV best-effort pour sauvegarde personnelle — pas un format
 *  d'échange, juste de quoi garder une copie lisible de ses données. */
object CsvExporter {
    fun workoutsToCsv(workouts: List<Workout>): String {
        val header = listOf("date", "type", "exercice", "series", "reps", "charge_cible_kg", "charge_reelle_kg", "propre")
        val rows = workouts.flatMap { workout ->
            if (workout.exercices.isEmpty()) {
                listOf(listOf(workout.date, workout.type.value, "", "", "", "", "", ""))
            } else {
                workout.exercices.map { ex ->
                    listOf(
                        workout.date,
                        workout.type.value,
                        ex.nom,
                        ex.series.toString(),
                        ex.reps.toString(),
                        ex.chargeCibleKg?.toString().orEmpty(),
                        ex.chargeReelleKg?.toString().orEmpty(),
                        ex.propre.toString(),
                    )
                }
            }
        }
        return buildCsv(header, rows)
    }

    fun mealsToCsv(meals: List<Meal>): String {
        val header = listOf("date", "repas", "calories", "proteines_g", "glucides_g", "lipides_g", "description")
        val rows = meals.map { meal ->
            listOf(
                meal.date,
                meal.repas.toString(),
                meal.calories.toString(),
                meal.proteinesG.toString(),
                meal.glucidesG.toString(),
                meal.lipidesG.toString(),
                meal.description.orEmpty(),
            )
        }
        return buildCsv(header, rows)
    }

    fun weighInsToCsv(weighIns: List<WeighIn>): String {
        val header = listOf("date", "heure", "type", "poids_kg", "bf_pct", "creatine_recente", "alcool_recent", "post_training")
        val rows = weighIns.map { w ->
            listOf(
                w.date,
                w.heure,
                w.type.value,
                w.poidsKg.toString(),
                w.bfPct?.toString().orEmpty(),
                w.contexte.creatineRecente.toString(),
                w.contexte.alcoolRecent.toString(),
                w.contexte.postTraining.toString(),
            )
        }
        return buildCsv(header, rows)
    }

    private fun buildCsv(header: List<String>, rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",") { escape(it) }).append('\n')
        rows.forEach { row -> sb.append(row.joinToString(",") { escape(it) }).append('\n') }
        return sb.toString()
    }

    private fun escape(value: String): String {
        val needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
