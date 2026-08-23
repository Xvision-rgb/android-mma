package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WorkoutType(val value: String, val label: String) {
    @SerialName("jambes_force") JambesForce("jambes_force", "Jambes force"),
    @SerialName("torse_force") TorseForce("torse_force", "Torse force"),
    @SerialName("jambes_hypertrophie") JambesHypertrophie("jambes_hypertrophie", "Jambes hypertrophie"),
    @SerialName("torse_hypertrophie") TorseHypertrophie("torse_hypertrophie", "Torse hypertrophie"),
    @SerialName("hiit") Hiit("hiit", "HIIT"),
    @SerialName("mma_wod") MmaWod("mma_wod", "MMA (WOD)"),
}

/** Type de jour dans le split hebdo programmé — inclut "repos", contrairement
 *  à WorkoutType qui ne couvre que les séances effectivement logguées. */
@Serializable
enum class PlanDayType(val value: String, val label: String) {
    @SerialName("jambes_force") JambesForce("jambes_force", "Jambes force"),
    @SerialName("torse_force") TorseForce("torse_force", "Torse force"),
    @SerialName("jambes_hypertrophie") JambesHypertrophie("jambes_hypertrophie", "Jambes hypertrophie"),
    @SerialName("torse_hypertrophie") TorseHypertrophie("torse_hypertrophie", "Torse hypertrophie"),
    @SerialName("hiit") Hiit("hiit", "HIIT"),
    @SerialName("mma_wod") MmaWod("mma_wod", "MMA (WOD)"),
    @SerialName("repos") Repos("repos", "Repos"),
}

fun PlanDayType.toWorkoutTypeOrNull(): WorkoutType? = when (this) {
    PlanDayType.JambesForce -> WorkoutType.JambesForce
    PlanDayType.TorseForce -> WorkoutType.TorseForce
    PlanDayType.JambesHypertrophie -> WorkoutType.JambesHypertrophie
    PlanDayType.TorseHypertrophie -> WorkoutType.TorseHypertrophie
    PlanDayType.Hiit -> WorkoutType.Hiit
    PlanDayType.MmaWod -> WorkoutType.MmaWod
    PlanDayType.Repos -> null
}

@Serializable
enum class WeighInType(val value: String, val label: String) {
    @SerialName("matin_jeun") MatinJeun("matin_jeun", "Matin à jeun"),
    @SerialName("soir") Soir("soir", "Soir"),
}

enum class RepasSlot(val value: Int, val label: String, val shareIndicatif: Double) {
    Matin(1, "Matin", 0.25),
    PostTraining(2, "Post-training", 0.30),
    ApresMidi(3, "Après-midi", 0.20),
    Soir(4, "Soir", 0.25);

    companion object {
        fun fromValue(value: Int): RepasSlot? = entries.firstOrNull { it.value == value }
    }
}

@Serializable
enum class TypeJour(val value: String, val label: String) {
    @SerialName("training") Training("training", "Jour training"),
    @SerialName("repos") Repos("repos", "Jour repos"),
}

/** Prépare l'extension post-septembre (métriques MMA spécifiques : explosivité,
 *  cardio) sans refondre le schéma ni les modèles. */
@Serializable
enum class Phase(val value: String, val label: String) {
    @SerialName("ete") Ete("ete", "Été"),
    @SerialName("curriculum_mma") CurriculumMma("curriculum_mma", "Curriculum MMA"),
}
