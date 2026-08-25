package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Food(
    val id: String,
    val nom: String,
    val categorie: String,
    @SerialName("kcal_100g") val kcal100g: Double,
    @SerialName("proteines_100g") val proteines100g: Double,
    @SerialName("glucides_100g") val glucides100g: Double,
    @SerialName("lipides_100g") val lipides100g: Double,
) {
    fun caloriesFor(grams: Double): Int = (kcal100g * grams / 100.0).toInt()
    fun proteinesFor(grams: Double): Double = proteines100g * grams / 100.0
    fun glucidesFor(grams: Double): Double = glucides100g * grams / 100.0
    fun lipidesFor(grams: Double): Double = lipides100g * grams / 100.0
}
