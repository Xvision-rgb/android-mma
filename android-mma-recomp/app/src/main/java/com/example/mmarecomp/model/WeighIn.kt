package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Flags contextuels qui faussent le %BF / le poids brut — affichés en note
 *  à côté du point plutôt que cachés, pour désamorcer les fausses alertes. */
@Serializable
data class WeighInContext(
    @SerialName("creatine_recente") val creatineRecente: Boolean = false,
    @SerialName("alcool_recent") val alcoolRecent: Boolean = false,
    @SerialName("post_training") val postTraining: Boolean = false,
) {
    val hasAnyFlag: Boolean get() = creatineRecente || alcoolRecent || postTraining

    val flagLabels: List<String>
        get() = buildList {
            if (creatineRecente) add("créatine récente")
            if (alcoolRecent) add("alcool récent")
            if (postTraining) add("post-training")
        }
}

@Serializable
data class WeighIn(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val heure: String, // "HH:mm:ss"
    val type: WeighInType,
    @SerialName("poids_kg") val poidsKg: Double,
    @SerialName("bf_pct") val bfPct: Double? = null,
    val contexte: WeighInContext = WeighInContext(),
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NewWeighIn(
    val date: String,
    val heure: String,
    val type: WeighInType,
    @SerialName("poids_kg") val poidsKg: Double,
    @SerialName("bf_pct") val bfPct: Double? = null,
    val contexte: WeighInContext,
)
