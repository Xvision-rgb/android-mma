package com.example.mmarecomp.ui.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.TargetVsActualBar
import com.example.mmarecomp.ui.components.VoiceInputButton
import com.example.mmarecomp.viewmodel.MealLogViewModel
import kotlinx.coroutines.launch

@Composable
fun MealLogScreen(viewModel: MealLogViewModel) {
    LaunchedEffect(viewModel.date) { viewModel.load() }

    val context = LocalContext.current
    var repeatConfirmation by remember { mutableStateOf<String?>(null) }
    var selectedSlot by remember { mutableStateOf(RepasSlot.Matin) }
    var calories by remember { mutableStateOf("") }
    var proteines by remember { mutableStateOf("") }
    var glucides by remember { mutableStateOf("") }
    var lipides by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var manualCalories by remember { mutableStateOf("") }
    var manualProteines by remember { mutableStateOf("") }
    var newPresetName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val prefs by AppPreferencesState.preferences

    LaunchedEffect(Unit) { viewModel.loadPresets(context) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(scaffoldPadding),
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
            } else if (prefs.autoNutritionTargets) {
                item {
                    Text("Pas encore de cible définie pour ce jour.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.setTarget(TypeJour.Training) }) { Text("Jour training") }
                        OutlinedButton(onClick = { viewModel.setTarget(TypeJour.Repos) }) { Text("Jour repos") }
                    }
                }
            } else {
                item { Text("Pas encore de cible définie pour ce jour.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            if (!prefs.autoNutritionTargets) {
                item {
                    Text(
                        "Cible manuelle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualCalories,
                            onValueChange = { manualCalories = it },
                            label = { Text("Calories") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = manualProteines,
                            onValueChange = { manualProteines = it },
                            label = { Text("Protéines (g)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            val calories = manualCalories.toIntOrNull()
                            val proteines = manualProteines.replace(",", ".").toDoubleOrNull()
                            if (calories != null && proteines != null) {
                                viewModel.setManualTarget(calories, proteines)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Définir la cible") }
                }
            }

            item { HorizontalDivider() }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Repas déjà loggés", style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = {
                            viewModel.repeatYesterday { count ->
                                repeatConfirmation = if (count > 0) {
                                    if (count == 1) "1 repas recopié depuis hier" else "$count repas recopiés depuis hier"
                                } else {
                                    "Rien à recopier depuis hier"
                                }
                            }
                        },
                    ) { Text("Répéter hier") }
                }
            }
            repeatConfirmation?.let { message ->
                item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            if (viewModel.mealsForDay.isEmpty()) {
                item {
                    Text(
                        "Aucun repas loggué pour l'instant — ajoute ton premier repas ci-dessous, ou récupère ceux d'hier avec le bouton au-dessus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(viewModel.mealsForDay, key = { it.id }) { meal ->
                meal.repasSlot?.let { slot ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(slot.label)
                            Text(
                                "${meal.calories} kcal · ${meal.proteinesG.toInt()}g prot",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = {
                            viewModel.removeMealLocally(meal)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Repas supprimé",
                                    actionLabel = "Annuler",
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreMeal(meal)
                                } else {
                                    viewModel.commitDeleteMeal(meal)
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer le repas ${slot.label}")
                        }
                    }
                }
            }

            item { HorizontalDivider() }
            item { Text("Ajouter / modifier un repas", style = MaterialTheme.typography.titleMedium) }

            if (viewModel.presets.isNotEmpty()) {
                item {
                    Text("Favoris", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        viewModel.presets.forEach { preset ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    calories = preset.calories.toString()
                                    proteines = preset.proteinesG.toString()
                                    glucides = preset.glucidesG.toString()
                                    lipides = preset.lipidesG.toString()
                                    description = preset.description.orEmpty()
                                },
                                label = { Text(preset.name) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Supprimer le favori ${preset.name}",
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .clickable { viewModel.deletePreset(preset, context) },
                                    )
                                },
                            )
                        }
                    }
                }
            }

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
                    trailingIcon = {
                        VoiceInputButton { spoken ->
                            description = if (description.isBlank()) spoken else "$description $spoken"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Nom du favori (ex: Shake post-training)") },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            viewModel.saveCurrentAsPreset(
                                name = newPresetName,
                                calories = calories.toIntOrNull() ?: 0,
                                proteinesG = proteines.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                glucidesG = glucides.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                lipidesG = lipides.replace(",", ".").toDoubleOrNull() ?: 0.0,
                                description = description,
                                context = context,
                            )
                            newPresetName = ""
                        },
                        enabled = newPresetName.isNotBlank() && calories.isNotBlank(),
                    ) { Text("Sauver") }
                }
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
}
