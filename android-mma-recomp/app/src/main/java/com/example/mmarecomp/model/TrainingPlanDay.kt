package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Une journée du split hebdo programmé (training_plan). */
@Serializable
data class TrainingPlanDay(
    val id: String,
    @SerialName("user_id") val userId: String,
    /** 1 = lundi ... 7 = dimanche */
    @SerialName("jour_semaine") val jourSemaine: Int,
    val type: PlanDayType,
    val exercices: List<PlannedExercise>,
    val phase: Phase,
    val notes: String? = null,
    val actif: Boolean = true,
)

val joursLabels = mapOf(
    1 to "Lundi", 2 to "Mardi", 3 to "Mercredi", 4 to "Jeudi",
    5 to "Vendredi", 6 to "Samedi", 7 to "Dimanche",
)

@Serializable
data class NewTrainingPlanDay(
    @SerialName("jour_semaine") val jourSemaine: Int,
    val type: PlanDayType,
    val exercices: List<PlannedExercise>,
    val phase: Phase,
    val notes: String? = null,
)
