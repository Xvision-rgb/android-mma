package com.example.mmarecomp.util

import com.example.mmarecomp.model.NewNutritionTarget
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.TypeJour

/**
 * Construction des cibles nutritionnelles à enregistrer.
 *
 * Un upsert PostgREST envoie toutes les colonnes : omettre glucides/lipides
 * les écrit à NULL et efface la périodisation glucidique déjà calculée.
 */
object NutritionTargetDraft {

    fun fromGoal(
        date: String,
        typeJour: TypeJour,
        goal: CalorieGoal,
    ): NewNutritionTarget = NewNutritionTarget(
        date = date,
        typeJour = typeJour,
        caloriesCible = goal.targetCalories,
        proteinesCibleG = goal.proteinesG.toDouble(),
        glucidesCibleG = goal.glucidesG.toDouble().takeIf { it > 0 },
        lipidesCibleG = goal.lipidesG.toDouble().takeIf { it > 0 },
    )

    /** Cible saisie à la main : on garde les macros déjà enregistrées. */
    fun custom(
        date: String,
        calories: Int,
        proteinesG: Double,
        existing: NutritionTarget?,
    ): NewNutritionTarget = NewNutritionTarget(
        date = date,
        typeJour = existing?.typeJour ?: TypeJour.Training,
        caloriesCible = calories,
        proteinesCibleG = proteinesG,
        glucidesCibleG = existing?.glucidesCibleG,
        lipidesCibleG = existing?.lipidesCibleG,
    )
}
