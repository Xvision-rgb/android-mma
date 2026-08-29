package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Log réel d'une séance (jambes/torse/HIIT). `date` reste en "yyyy-MM-dd"
 *  pour matcher directement la colonne Postgres `date`. */
@Serializable
data class Workout(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val type: WorkoutType,
    val exercices: List<LoggedExercise>,
    @SerialName("duree_min") val dureeMin: Int? = null,
    /** RPE global de la séance (1-10). Multiplié par la durée, il donne la
     *  charge interne (session-RPE) dont dérive l'ACWR. */
    val rpe: Int? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NewWorkout(
    val date: String,
    val type: WorkoutType,
    val exercices: List<LoggedExercise>,
    @SerialName("duree_min") val dureeMin: Int? = null,
    val rpe: Int? = null,
    val notes: String? = null,
)
