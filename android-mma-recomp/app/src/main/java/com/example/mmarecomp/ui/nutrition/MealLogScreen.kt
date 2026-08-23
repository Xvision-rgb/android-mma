package com.example.mmarecomp.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.TargetVsActualBar
import com.example.mmarecomp.viewmodel.MealLogViewModel

@Composable
fun MealLogScreen(viewModel: MealLogViewModel) {
    LaunchedEffect(viewModel.date) { viewModel.load() }

    var selectedSlot by remember { mutableStateOf(RepasSlot.Matin) }
    var calories by remember { mutableStateOf("") }
    var proteines by remember { mutableStateOf("") }
    var glucides by remember { mutableStateOf("") }
    var lipides by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Log repas", style = MaterialTheme.typography.titleLarge) }

        item { DateField("Date", viewModel.date, { viewModel.date = it }, modifier = Modifier.fillMaxWidth()) }

        val target = viewModel.target
        if (target != null) {
            item {
                Text("Cible du jour", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TargetVsActualBar("Calories", viewModel.totalCalories.toDouble(), target.caloriesCible.toDouble(), "kcal")
            }
            item {
                TargetVsActualBar("Protéines", viewModel.totalProteines, target.proteinesCibleG, "g")
            }
            if (viewModel.totalCalories < target.caloriesCible) {
                item {
                    Text(
                        "Il reste ${target.caloriesCible - viewModel.totalCalories} kcal aujourd'hui — pas de panique, juste une indication.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                Text("Pas encore de cible définie pour ce jour.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.setTarget(TypeJour.Training) }) { Text("Jour training") }
                    OutlinedButton(onClick = { viewModel.setTarget(TypeJour.Repos) }) { Text("Jour repos") }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text("Repas déjà loggés", style = MaterialTheme.typography.titleMedium) }

        if (viewModel.mealsForDay.isEmpty()) {
            item { Text("Aucun repas pour l'instant.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(viewModel.mealsForDay) { meal ->
            meal.repasSlot?.let { slot ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(slot.label)
                    Text(
                        "${meal.calories} kcal · ${meal.proteinesG.toInt()}g prot",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { HorizontalDivider() }
        item { Text("Ajouter / modifier un repas", style = MaterialTheme.typography.titleMedium) }

        item {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedSlot.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Créneau") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    RepasSlot.entries.forEach { slot ->
                        DropdownMenuItem(text = { Text(slot.label) }, onClick = { selectedSlot = slot; expanded = false })
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = calories, onValueChange = { calories = it }, label = { Text("Calories") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = proteines, onValueChange = { proteines = it }, label = { Text("Protéines (g)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = glucides, onValueChange = { glucides = it }, label = { Text("Glucides (g)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = lipides, onValueChange = { lipides = it }, label = { Text("Lipides (g)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = description, onValueChange = { description = it }, label = { Text("Description (optionnel)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        viewModel.errorMessage?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Button(
                onClick = {
                    viewModel.logMeal(
                        slot = selectedSlot,
                        calories = calories.toIntOrNull() ?: 0,
                        proteinesG = proteines.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        glucidesG = glucides.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        lipidesG = lipides.replace(",", ".").toDoubleOrNull() ?: 0.0,
                        description = description,
                    ) { saved ->
                        if (saved) {
                            calories = ""; proteines = ""; glucides = ""; lipides = ""; description = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer ce repas") }
        }
    }
}
