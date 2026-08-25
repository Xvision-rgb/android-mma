package com.example.mmarecomp.ui.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.Food
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.EmptyState
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.TargetVsActualBar
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.Formatting
import com.example.mmarecomp.viewmodel.MealLogViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MealLogScreen(viewModel: MealLogViewModel) {
    LaunchedEffect(viewModel.date) { viewModel.load() }
    LaunchedEffect(Unit) { viewModel.loadFoods() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    fun deleteWithUndo(meal: Meal) {
        viewModel.deleteMeal(meal) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Repas supprimé",
                    actionLabel = "Annuler",
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restoreMeal(meal)
            }
        }
    }

    var selectedSlot by remember { mutableStateOf(RepasSlot.Matin) }
    var calories by remember { mutableStateOf("") }
    var proteines by remember { mutableStateOf("") }
    var glucides by remember { mutableStateOf("") }
    var lipides by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var slotFilter by remember { mutableStateOf<RepasSlot?>(null) }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var quantiteG by remember { mutableStateOf("100") }
    var showSaved by remember { mutableStateOf(false) }

    fun applyFood(food: Food, grams: String) {
        val g = grams.replace(",", ".").toDoubleOrNull() ?: return
        calories = food.caloriesFor(g).toString()
        proteines = Formatting.oneDecimal(food.proteinesFor(g))
        glucides = Formatting.oneDecimal(food.glucidesFor(g))
        lipides = Formatting.oneDecimal(food.lipidesFor(g))
        if (description.isBlank()) description = food.nom
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
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
            item {
                Text(
                    "Répartition indicative par créneau",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    viewModel.indicativeSplit.forEach { (slot, slotTarget) ->
                        Column {
                            Text(slot.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("~${slotTarget.calories} kcal", style = MaterialTheme.typography.bodySmall)
                        }
                    }
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

        if (viewModel.mealsForDay.isNotEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = slotFilter == null, onClick = { slotFilter = null }, label = { Text("Tous") })
                    RepasSlot.entries.forEach { slot ->
                        FilterChip(
                            selected = slotFilter == slot,
                            onClick = { slotFilter = if (slotFilter == slot) null else slot },
                            label = { Text(slot.label) },
                        )
                    }
                }
            }
        }

        val visibleMeals = viewModel.mealsForDay.filter { slotFilter == null || it.repasSlot == slotFilter }
        if (visibleMeals.isEmpty()) {
            item {
                EmptyState(
                    title = if (viewModel.mealsForDay.isEmpty()) "Aucun repas loggé pour l'instant" else "Aucun repas pour ce créneau",
                    subtitle = if (viewModel.mealsForDay.isEmpty()) "Ajoute ton premier repas ci-dessous, ça prend 10 secondes." else null,
                )
            }
        }
        val mealsBySlot = visibleMeals.groupBy { it.repasSlot }
        RepasSlot.entries.forEach { slot ->
            val slotMeals = mealsBySlot[slot].orEmpty()
            if (slotMeals.isNotEmpty()) {
                stickyHeader(key = "header-${slot.value}") {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Text(
                            slot.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        )
                    }
                }
                items(slotMeals, key = { it.id }) { meal ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${meal.calories} kcal · ${meal.proteinesG.toInt()}g prot",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = { deleteWithUndo(meal) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Supprimer le repas ${slot.label}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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
            var duplicateFeedback by remember(selectedSlot) { mutableStateOf<Boolean?>(null) }
            TextButton(onClick = {
                viewModel.duplicateFromYesterday(selectedSlot) { found -> duplicateFeedback = found }
            }) { Text("Reprendre le repas d'hier sur ce créneau") }
            duplicateFeedback?.let { found ->
                Text(
                    if (found) "Repas d'hier repris ✓" else "Rien à reprendre — pas de repas loggé hier sur ce créneau",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { Text("Aliment préchargé (optionnel)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            OutlinedTextField(
                value = viewModel.foodQuery,
                onValueChange = { viewModel.foodQuery = it; selectedFood = null },
                label = { Text("Rechercher un aliment") },
                trailingIcon = {
                    if (viewModel.foodQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.foodQuery = ""; selectedFood = null }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Effacer la recherche")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (viewModel.foodQuery.isNotBlank() && viewModel.filteredFoods.isEmpty()) {
            item {
                Text(
                    "Aucun aliment ne correspond — tu peux saisir les valeurs manuellement ci-dessous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(viewModel.filteredFoods, key = { it.id }) { food ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = Dimens.minTouchTarget)
                    .clickable {
                        selectedFood = food
                        viewModel.foodQuery = food.nom
                        applyFood(food, quantiteG)
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(food.nom)
                Text(
                    "${food.kcal100g.toInt()} kcal/100g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        selectedFood?.let { food ->
            item {
                OutlinedTextField(
                    value = quantiteG,
                    onValueChange = { quantiteG = it; applyFood(food, it) },
                    label = { Text("Quantité (g)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
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
            item { ErrorBanner(error, onRetry = { viewModel.load() }) }
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
                        showSaved = saved
                        if (saved) {
                            calories = ""; proteines = ""; glucides = ""; lipides = ""; description = ""
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer ce repas") }
        }
        item {
            AnimatedVisibility(visible = showSaved, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
                Text("Repas enregistré ✓", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
