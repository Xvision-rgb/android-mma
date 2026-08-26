package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("poids_objectif_kg") val poidsObjectifKg: Double,
    @SerialName("bf_objectif_pct") val bfObjectifPct: Double,
    val phase: Phase,
    @SerialName("objectif_calorie_mode") val objectifCalorieMode: CalorieMode = CalorieMode.Recomposition,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ProfileUpdate(
    @SerialName("poids_objectif_kg") val poidsObjectifKg: Double? = null,
    @SerialName("bf_objectif_pct") val bfObjectifPct: Double? = null,
    val phase: Phase? = null,
    @SerialName("objectif_calorie_mode") val objectifCalorieMode: CalorieMode? = null,
)
