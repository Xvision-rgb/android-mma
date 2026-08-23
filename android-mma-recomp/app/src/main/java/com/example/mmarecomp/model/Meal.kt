package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Meal(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val repas: Int,
    val calories: Int,
    @SerialName("proteines_g") val proteinesG: Double,
    @SerialName("glucides_g") val glucidesG: Double,
    @SerialName("lipides_g") val lipidesG: Double,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    val repasSlot: RepasSlot? get() = RepasSlot.fromValue(repas)
}

@Serializable
data class NewMeal(
    val date: String,
    val repas: Int,
    val calories: Int,
    @SerialName("proteines_g") val proteinesG: Double,
    @SerialName("glucides_g") val glucidesG: Double,
    @SerialName("lipides_g") val lipidesG: Double,
    val description: String? = null,
)
