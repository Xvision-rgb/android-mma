package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.withSets
import kotlin.math.roundToInt

/** Applique concrètement la modulation de readiness sur une séance en cours. */
object ModulationApplier {

    data class Result(
        val exercices: List<LoggedExercise>,
        val rirSupplementaire: Int,
        val resume: List<String>,
    )

    fun estAccessoire(nom: String): Boolean =
        nom.isNotBlank() && !MuscleZoneClassifier.estPolyarticulaire(nom)

    fun actionsConcretes(modulation: ModulationSeance): List<String> = when (modulation.action) {
        ReadinessAction.NOMINALE -> listOf("Séance comme prévu — aucun ajustement nécessaire.")
        ReadinessAction.VOLUME_REDUIT -> listOf(
            "Retirer la dernière série de chaque accessoire (curl, face pull, etc.)",
            "Charges inchangées",
        )
        ReadinessAction.ALLEGEE -> listOf(
            "Retirer 1 série sur les accessoires et les gros mouvements",
            "Baisser les charges d'environ 10 %",
            "Ajouter 2 reps de marge (RIR +2)",
        )
        ReadinessAction.DELOAD -> listOf(
            "Garder environ la moitié des séries",
            "Maintenir les charges — la récupération passe par le volume",
        )
    }

    fun apply(modulation: ModulationSeance, exercices: List<LoggedExercise>): Result {
        if (exercices.isEmpty()) {
            return Result(exercices, modulation.rirSupplementaire, listOf("Ajoute d'abord des exercices à ta séance."))
        }
        val resume = mutableListOf<String>()
        var updated = exercices
        var rirBonus = modulation.rirSupplementaire

        when (modulation.action) {
            ReadinessAction.NOMINALE -> {
                resume += "Aucun ajustement nécessaire."
            }
            ReadinessAction.VOLUME_REDUIT -> {
                val (ex, count) = retirerDerniereSerieAccessoires(updated)
                updated = ex
                resume += if (count > 0) {
                    "$count accessoire(s) allégé(s) — dernière série retirée"
                } else {
                    "Pas d'accessoire à alléger sur cette séance"
                }
            }
            ReadinessAction.ALLEGEE -> {
                val (ex1, countAccessoires) = retirerDerniereSerieAccessoires(updated)
                val (ex2, countPoly) = retirerDerniereSeriePolyarticulaires(ex1, minSetsAvant = 2)
                updated = appliquerFacteurCharge(ex2, modulation.facteurCharge)
                val total = countAccessoires + countPoly
                if (total > 0) resume += "$total exercice(s) allégé(s) — série(s) retirée(s)"
                if (modulation.facteurCharge < 1.0) resume += "Charges baissées d'environ 10 %"
                if (rirBonus > 0) resume += "RIR +$rirBonus sur chaque série"
            }
            ReadinessAction.DELOAD -> {
                updated = reduireVolumeMoitie(updated)
                resume += "Volume réduit à ~50 % — charges maintenues"
                rirBonus = 0
            }
        }

        if (rirBonus > 0) {
            updated = updated.map { ex ->
                ex.withSets(
                    ex.effectiveSets.map { set ->
                        set.copy(rir = (set.rir ?: 0) + rirBonus)
                    },
                )
            }
        }

        return Result(updated, rirBonus, resume)
    }

    private fun retirerDerniereSerieAccessoires(
        exercices: List<LoggedExercise>,
    ): Pair<List<LoggedExercise>, Int> {
        var count = 0
        val result = exercices.map { ex ->
            if (!estAccessoire(ex.nom) || ex.effectiveSets.size <= 1) {
                ex
            } else {
                count++
                ex.withSets(ex.effectiveSets.dropLast(1))
            }
        }
        return result to count
    }

    private fun retirerDerniereSeriePolyarticulaires(
        exercices: List<LoggedExercise>,
        minSetsAvant: Int,
    ): Pair<List<LoggedExercise>, Int> {
        var count = 0
        val result = exercices.map { ex ->
            if (estAccessoire(ex.nom) || ex.effectiveSets.size <= minSetsAvant) {
                ex
            } else {
                count++
                ex.withSets(ex.effectiveSets.dropLast(1))
            }
        }
        return result to count
    }

    private fun appliquerFacteurCharge(
        exercices: List<LoggedExercise>,
        facteur: Double,
    ): List<LoggedExercise> {
        if (facteur >= 1.0) return exercices
        return exercices.map { ex ->
            val chargeCible = ex.chargeCibleKg?.let { arrondirCharge(it * facteur) }
            val newSets = ex.effectiveSets.map { set ->
                set.copy(chargeKg = arrondirCharge(set.chargeKg * facteur))
            }
            ex.withSets(newSets).copy(chargeCibleKg = chargeCible)
        }
    }

    private fun reduireVolumeMoitie(exercices: List<LoggedExercise>): List<LoggedExercise> =
        exercices.map { ex ->
            val sets = ex.effectiveSets
            val cible = (sets.size / 2.0).roundToInt().coerceAtLeast(1)
            ex.withSets(sets.take(cible))
        }

    private fun arrondirCharge(valeur: Double): Double =
        (valeur * 10).roundToInt() / 10.0
}
