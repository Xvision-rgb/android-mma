package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NutritionTarget(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    @SerialName("type_jour") val typeJour: TypeJour,
    @SerialName("calories_cible") val caloriesCible: Int,
    @SerialName("proteines_cible_g") val proteinesCibleG: Double,
    /** Nullables : l'historique antérieur à la périodisation glucidique n'en
     *  a pas, et une valeur inventée fausserait les récapitulatifs. */
    @SerialName("glucides_cible_g") val glucidesCibleG: Double? = null,
    @SerialName("lipides_cible_g") val lipidesCibleG: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NewNutritionTarget(
    val date: String,
    @SerialName("type_jour") val typeJour: TypeJour,
    @SerialName("calories_cible") val caloriesCible: Int,
    @SerialName("proteines_cible_g") val proteinesCibleG: Double,
    @SerialName("glucides_cible_g") val glucidesCibleG: Double? = null,
    @SerialName("lipides_cible_g") val lipidesCibleG: Double? = null,
)
