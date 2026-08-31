package com.example.mmarecomp.util

import com.example.mmarecomp.model.Meal

/** Ligne d'aliment reconstituée depuis la description d'un repas enregistré. */
data class ParsedMealLine(
    val label: String,
    val calories: Int,
    val proteinesG: Double,
    val glucidesG: Double,
    val lipidesG: Double,
)

/** Reconstitue les aliments affichés à partir d'un repas stocké (description
 *  comma-séparée). Les macros sont réparties à parts égales quand plusieurs
 *  aliments partagent une seule ligne en base — suffisant pour éditer sans
 *  tout ressaisir. */
object MealDescriptionParser {

    fun linesFromMeal(meal: Meal): List<ParsedMealLine> {
        val labels = meal.description
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        if (labels.isEmpty()) {
            return listOf(
                ParsedMealLine(
                    label = "Repas",
                    calories = meal.calories,
                    proteinesG = meal.proteinesG,
                    glucidesG = meal.glucidesG,
                    lipidesG = meal.lipidesG,
                ),
            )
        }
        val count = labels.size
        return labels.map { label ->
            ParsedMealLine(
                label = label,
                calories = meal.calories / count,
                proteinesG = meal.proteinesG / count,
                glucidesG = meal.glucidesG / count,
                lipidesG = meal.lipidesG / count,
            )
        }
    }

    fun summaryLabel(meal: Meal): String {
        val desc = meal.description?.trim().orEmpty()
        if (desc.isNotBlank()) return desc
        return "${meal.calories} kcal"
    }
}
