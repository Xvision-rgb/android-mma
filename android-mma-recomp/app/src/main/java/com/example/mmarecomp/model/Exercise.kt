package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Exercice tel que programmé dans le split hebdo (training_plan.exercices). */
@Serializable
data class PlannedExercise(
    val nom: String,
    val series: Int,
    val reps: Int,
    @SerialName("charge_cible_kg") val chargeCibleKg: Double? = null,
)

/** Exercice tel que loggué après une séance réelle (workouts.exercices). */
@Serializable
data class LoggedExercise(
    val nom: String,
    val series: Int,
    val reps: Int,
    @SerialName("charge_cible_kg") val chargeCibleKg: Double? = null,
    @SerialName("charge_reelle_kg") val chargeReelleKg: Double? = null,
    @SerialName("reps_reelles") val repsReelles: Int? = null,
    /** Toutes les reps faites proprement -> déclenche la suggestion "+2.5kg". */
    val propre: Boolean = false,
) {
    val suggestionProgression: Double?
        get() = if (propre && chargeCibleKg != null) chargeCibleKg + 2.5 else null
}

fun PlannedExercise.toLogged() = LoggedExercise(
    nom = nom,
    series = series,
    reps = reps,
    chargeCibleKg = chargeCibleKg,
)
