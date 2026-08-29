package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Point quotidien sur l'état de forme, saisi le matin.
 *
 *  Les mesures subjectives réagissent souvent plus vite et plus proprement aux
 *  variations de charge que les marqueurs physiologiques — le questionnaire
 *  reste donc le signal principal, la HRV un complément optionnel.
 *
 *  Chaque item va de 1 (mauvais) à 5 (bon), pour que le score total se lise
 *  toujours dans le même sens : plus haut = plus prêt. */
@Serializable
data class DailyCheckIn(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val sommeil: Int,
    val courbatures: Int,
    val fatigue: Int,
    val humeur: Int,
    val stress: Int,
    /** rMSSD au réveil, en ms. Saisie manuelle — aucun SDK santé intégré. */
    @SerialName("hrv_rmssd") val hrvRmssd: Double? = null,
    /** Suspension à la barre, en secondes. Métrique de poigne suivie dans le
     *  temps plutôt que testée une fois (cf. GripBenchmarks). */
    @SerialName("dead_hang_sec") val deadHangSec: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val score: Int get() = sommeil + courbatures + fatigue + humeur + stress
}

@Serializable
data class NewDailyCheckIn(
    val date: String,
    val sommeil: Int,
    val courbatures: Int,
    val fatigue: Int,
    val humeur: Int,
    val stress: Int,
    @SerialName("hrv_rmssd") val hrvRmssd: Double? = null,
    @SerialName("dead_hang_sec") val deadHangSec: Int? = null,
)
