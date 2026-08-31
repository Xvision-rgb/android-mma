package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Créneau d'une séance programmée — un jour peut en avoir deux (salle + maison). */
@Serializable
enum class PlanCreneau(val value: String, val label: String) {
    @SerialName("matin") Matin("matin", "Matin"),
    @SerialName("soir") Soir("soir", "Soir"),
    ;

    companion object {
        fun fromValue(raw: String?): PlanCreneau =
            entries.firstOrNull { it.value.equals(raw, ignoreCase = true) } ?: Matin
    }
}

/** Une journée (ou un créneau) du split hebdo programmé (training_plan). */
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
    /** Matin par défaut — rétrocompatible avec les lignes antérieures à 010. */
    val creneau: PlanCreneau = PlanCreneau.Matin,
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
    val creneau: PlanCreneau = PlanCreneau.Matin,
)

/** Clé stable jour + créneau pour maps / brouillons d'import. */
fun planSlotKey(jourSemaine: Int, creneau: PlanCreneau): String =
    "${jourSemaine}_${creneau.value}"

fun TrainingPlanDay.slotKey(): String = planSlotKey(jourSemaine, creneau)

fun TrainingPlanDay.displayLabel(): String =
    "${joursLabels[jourSemaine] ?: "Jour $jourSemaine"} · ${creneau.label}"
