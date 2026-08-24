package com.example.mmarecomp.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.data.FoodDatabase
import com.example.mmarecomp.data.FoodItem
import com.example.mmarecomp.util.matchesSearch
import kotlin.math.roundToInt

private data class SelectedFood(val food: FoodItem, val grams: Int)

/** Repères de portion pour qui n'a pas de balance de cuisine — approximatifs
 *  par nature, un point de départ à ajuster, pas une valeur exacte. */
private val PORTION_PRESETS = listOf(
    "Petite portion" to 80,
    "Portion moyenne" to 150,
    "Grosse portion" to 250,
)

/**
 * Aide au calcul quand on ne connaît pas les valeurs d'un repas : on
 * cherche un ou plusieurs aliments dans la base intégrée + les aliments
 * personnels ajoutés, on ajuste le poids, et on applique le total dans le
 * formulaire — jamais de saisie automatique, l'utilisateur valide toujours
 * explicitement. Si rien n'est trouvé, on peut créer l'aliment soi-même
 * (nom + valeurs pour 100g), sauvegardé pour les prochaines recherches.
 */
@Composable
fun FoodCalculatorSection(
    customFoods: List<FoodItem>,
    onSaveCustomFood: (FoodItem) -> Unit,
    onApply: (calories: Int, proteinesG: Double, glucidesG: Double, lipidesG: Double, description: String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<List<SelectedFood>>(emptyList()) }
    var creatingCustomFood by remember { mutableStateOf(false) }
    var newFoodCalories by remember { mutableStateOf("") }
    var newFoodProteines by remember { mutableStateOf("") }
    var newFoodGlucides by remember { mutableStateOf("") }
    var newFoodLipides by remember { mutableStateOf("") }

    val allFoods = remember(customFoods) { FoodDatabase.items + customFoods }
    val matches = remember(query, allFoods) {
        if (query.isBlank()) emptyList() else allFoods.filter { matchesSearch(it.name, query) }.take(6)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Tu ne connais pas les calories ? Cherche un aliment ci-dessous.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; creatingCustomFood = false },
            label = { Text("Chercher un aliment (ex: riz, poulet, œuf)") },
            modifier = Modifier,
        )
        if (matches.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                matches.forEach { food ->
                    AssistChip(
                        onClick = {
                            selected = selected + SelectedFood(food, 100)
                            query = ""
                        },
                        label = { Text(food.name) },
                    )
                }
            }
        } else if (query.isNotBlank() && !creatingCustomFood) {
            TextButton(onClick = { creatingCustomFood = true }) {
                Text("Aucun résultat — créer \"$query\" comme aliment personnalisé")
            }
        }
        if (creatingCustomFood) {
            Text(
                "Valeurs pour 100g de \"$query\"",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newFoodCalories,
                    onValueChange = { newFoodCalories = it },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = newFoodProteines,
                    onValueChange = { newFoodProteines = it },
                    label = { Text("Prot. (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newFoodGlucides,
                    onValueChange = { newFoodGlucides = it },
                    label = { Text("Gluc. (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = newFoodLipides,
                    onValueChange = { newFoodLipides = it },
                    label = { Text("Lip. (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(
                onClick = {
                    val food = FoodItem(
                        name = query,
                        caloriesPer100g = newFoodCalories.toIntOrNull() ?: 0,
                        proteinPer100gG = newFoodProteines.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        carbsPer100gG = newFoodGlucides.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        fatPer100gG = newFoodLipides.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    )
                    onSaveCustomFood(food)
                    selected = selected + SelectedFood(food, 100)
                    query = ""
                    newFoodCalories = ""; newFoodProteines = ""; newFoodGlucides = ""; newFoodLipides = ""
                    creatingCustomFood = false
                },
                enabled = newFoodCalories.isNotBlank(),
            ) { Text("Enregistrer cet aliment") }
        }
        selected.forEachIndexed { index, item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.food.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = item.grams.toString(),
                        onValueChange = { value ->
                            val grams = value.toIntOrNull() ?: item.grams
                            selected = selected.toMutableList().also { it[index] = item.copy(grams = grams) }
                        },
                        label = { Text("g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(90.dp),
                    )
                    IconButton(onClick = { selected = selected.filterIndexed { i, _ -> i != index } }) {
                        Icon(Icons.Filled.Close, contentDescription = "Retirer ${item.food.name} du calcul")
                    }
                }
                // Pas de balance en cuisine ? Des repères de portion approximatifs
                // plutôt que d'obliger à peser précisément.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PORTION_PRESETS.forEach { (label, grams) ->
                        AssistChip(
                            onClick = { selected = selected.toMutableList().also { it[index] = item.copy(grams = grams) } },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }
        if (selected.isNotEmpty()) {
            val totalCalories = selected.sumOf { it.food.caloriesPer100g * it.grams / 100.0 }.roundToInt()
            val totalProteines = selected.sumOf { it.food.proteinPer100gG * it.grams / 100.0 }
            val totalGlucides = selected.sumOf { it.food.carbsPer100gG * it.grams / 100.0 }
            val totalLipides = selected.sumOf { it.food.fatPer100gG * it.grams / 100.0 }
            Text(
                "Total estimé : $totalCalories kcal · %.0fg prot · %.0fg gluc · %.0fg lip".format(totalProteines, totalGlucides, totalLipides),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = {
                val description = selected.joinToString(" + ") { "${it.food.name} (${it.grams}g)" }
                onApply(totalCalories, totalProteines, totalGlucides, totalLipides, description)
                selected = emptyList()
            }) { Text("Utiliser ces valeurs") }
        }
    }
}
