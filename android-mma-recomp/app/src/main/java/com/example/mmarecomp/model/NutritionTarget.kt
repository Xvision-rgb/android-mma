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
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NewNutritionTarget(
    val date: String,
    @SerialName("type_jour") val typeJour: TypeJour,
    @SerialName("calories_cible") val caloriesCible: Int,
    @SerialName("proteines_cible_g") val proteinesCibleG: Double,
)
