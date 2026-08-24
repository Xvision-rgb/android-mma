package com.example.mmarecomp.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.data.AccentPreset
import com.example.mmarecomp.data.DashboardCard
import com.example.mmarecomp.data.DefaultTab
import com.example.mmarecomp.data.MovingAverageWindow
import com.example.mmarecomp.data.ThemeMode
import com.example.mmarecomp.data.ThemePreferenceStore
import com.example.mmarecomp.data.TextScale
import com.example.mmarecomp.data.UserPreferences
import com.example.mmarecomp.data.UserPreferencesStore
import com.example.mmarecomp.data.WeekStart
import com.example.mmarecomp.data.WeightUnit
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.ui.components.LabeledDropdown
import com.example.mmarecomp.ui.components.LabeledSegmentedChoice
import com.example.mmarecomp.ui.components.ToggleRow
import com.example.mmarecomp.ui.theme.AppThemeState
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.displayLabel
import com.example.mmarecomp.util.toFriendlyMessage
import com.example.mmarecomp.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel,
    onPhaseSaved: (Phase) -> Unit,
    onSignOut: () -> Unit,
    onOpenTrainingPlan: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.load() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val prefs by AppPreferencesState.preferences
    fun updatePrefs(transform: (UserPreferences) -> UserPreferences) {
        val updated = transform(prefs)
        AppPreferencesState.preferences.value = updated
        UserPreferencesStore(context).save(updated)
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val csv = viewModel.buildExportCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                exportMessage = "Export enregistré."
            } catch (e: Exception) {
                exportMessage = e.toFriendlyMessage("Échec de l'export.")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Réglages", style = MaterialTheme.typography.titleLarge) }
        item { Text("Objectifs", style = MaterialTheme.typography.titleMedium) }

        item {
            OutlinedTextField(
                value = viewModel.poidsObjectifKg,
                onValueChange = { viewModel.poidsObjectifKg = it },
                label = { Text("Poids objectif (kg)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = viewModel.bfObjectifPct,
                onValueChange = { viewModel.bfObjectifPct = it },
                label = { Text("% BF objectif") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = viewModel.phase.displayLabel(prefs.phaseLabelOverrides),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Phase") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Phase.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayLabel(prefs.phaseLabelOverrides)) },
                            onClick = { viewModel.phase = option; expanded = false },
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = viewModel.coachNotes,
                onValueChange = { viewModel.coachNotes = it },
                label = { Text("Notes du coach (libre)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        viewModel.errorMessage?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Button(
                onClick = { viewModel.save(onPhaseSaved) },
                enabled = !viewModel.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer") }
        }

        item { HorizontalDivider() }
        item { Text("Apparence", style = MaterialTheme.typography.titleMedium) }
        item {
            val themeOptions = listOf(
                ThemeMode.SYSTEM to "Système",
                ThemeMode.LIGHT to "Clair",
                ThemeMode.DARK to "Sombre",
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = AppThemeState.mode.value == mode,
                        onClick = {
                            AppThemeState.mode.value = mode
                            ThemePreferenceStore(context).save(mode)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                    ) { Text(label) }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text("Préférences", style = MaterialTheme.typography.titleMedium) }

        item {
            LabeledSegmentedChoice(
                title = "Unités de poids",
                options = WeightUnit.entries,
                selected = prefs.weightUnit,
                labelFor = { it.label },
                onSelect = { unit -> updatePrefs { it.copy(weightUnit = unit) } },
            )
        }
        item {
            LabeledSegmentedChoice(
                title = "Moyenne mobile (tendance poids)",
                options = MovingAverageWindow.entries,
                selected = prefs.movingAverageWindow,
                labelFor = { it.label },
                onSelect = { window -> updatePrefs { it.copy(movingAverageWindow = window) } },
            )
        }
        item {
            LabeledSegmentedChoice(
                title = "Premier jour de la semaine",
                options = WeekStart.entries,
                selected = prefs.weekStart,
                labelFor = { it.label },
                onSelect = { start -> updatePrefs { it.copy(weekStart = start) } },
            )
        }
        item {
            LabeledSegmentedChoice(
                title = "Taille du texte",
                options = TextScale.entries,
                selected = prefs.textScale,
                labelFor = { it.label },
                onSelect = { scale -> updatePrefs { it.copy(textScale = scale) } },
            )
        }
        item {
            LabeledDropdown(
                title = "Couleur d'accent",
                options = AccentPreset.entries,
                selected = prefs.accent,
                labelFor = { it.label },
                onSelect = { accent -> updatePrefs { it.copy(accent = accent) } },
            )
        }
        item {
            LabeledDropdown(
                title = "Onglet par défaut à l'ouverture",
                options = DefaultTab.entries,
                selected = prefs.defaultTab,
                labelFor = { it.label },
                onSelect = { tab -> updatePrefs { it.copy(defaultTab = tab) } },
            )
        }
        item {
            val defaultWorkoutTypeOptions: List<WorkoutType?> = listOf(null) + WorkoutType.entries
            LabeledDropdown(
                title = "Type de séance par défaut",
                options = defaultWorkoutTypeOptions,
                selected = WorkoutType.entries.find { it.name == prefs.defaultWorkoutType },
                labelFor = { it?.label ?: "Premier de la liste" },
                onSelect = { type -> updatePrefs { it.copy(defaultWorkoutType = type?.name) } },
            )
        }
        item {
            Text("Cartes visibles sur le dashboard", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DashboardCard.entries.forEach { card ->
                    FilterChip(
                        selected = card in prefs.visibleDashboardCards,
                        onClick = {
                            updatePrefs {
                                val updatedCards = if (card in it.visibleDashboardCards) {
                                    it.visibleDashboardCards - card
                                } else {
                                    it.visibleDashboardCards + card
                                }
                                it.copy(visibleDashboardCards = updatedCards)
                            }
                        },
                        label = { Text(card.label) },
                    )
                }
            }
        }
        item {
            ToggleRow(
                "Suivre le % de masse grasse",
                prefs.showBodyFat,
            ) { checked -> updatePrefs { it.copy(showBodyFat = checked) } }
        }
        item {
            ToggleRow(
                "Cibles nutrition calculées automatiquement",
                prefs.autoNutritionTargets,
            ) { checked -> updatePrefs { it.copy(autoNutritionTargets = checked) } }
        }
        item {
            ToggleRow(
                "Afficher la dictée vocale",
                prefs.showVoiceInput,
            ) { checked -> updatePrefs { it.copy(showVoiceInput = checked) } }
        }
        item {
            ToggleRow(
                "Vibrer sur un nouveau record personnel",
                prefs.celebratePrWithVibration,
            ) { checked -> updatePrefs { it.copy(celebratePrWithVibration = checked) } }
        }
        item {
            ToggleRow(
                "Confirmer avant suppression (au lieu du \"Annuler\")",
                prefs.confirmBeforeDelete,
            ) { checked -> updatePrefs { it.copy(confirmBeforeDelete = checked) } }
        }

        item { Text("Renommer les phases", style = MaterialTheme.typography.bodyMedium) }
        Phase.entries.forEach { phase ->
            item {
                OutlinedTextField(
                    value = prefs.phaseLabelOverrides[phase.name] ?: "",
                    onValueChange = { newLabel ->
                        updatePrefs { it.copy(phaseLabelOverrides = it.phaseLabelOverrides + (phase.name to newLabel)) }
                    },
                    label = { Text(phase.label) },
                    placeholder = { Text(phase.label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item { Text("Renommer les créneaux repas", style = MaterialTheme.typography.bodyMedium) }
        RepasSlot.entries.forEach { slot ->
            item {
                OutlinedTextField(
                    value = prefs.mealSlotLabelOverrides[slot.name] ?: "",
                    onValueChange = { newLabel ->
                        updatePrefs { it.copy(mealSlotLabelOverrides = it.mealSlotLabelOverrides + (slot.name to newLabel)) }
                    },
                    label = { Text(slot.label) },
                    placeholder = { Text(slot.label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item { HorizontalDivider() }

        item {
            OutlinedButton(onClick = onOpenTrainingPlan, modifier = Modifier.fillMaxWidth()) {
                Text("Modifier le split hebdomadaire")
            }
        }

        item { HorizontalDivider() }

        item {
            OutlinedButton(
                onClick = { exportLauncher.launch("recomp-mma-export-${DateUtils.today()}.csv") },
                enabled = !viewModel.isExporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (viewModel.isExporting) "Export en cours…" else "Exporter mes données (CSV)")
            }
        }
        exportMessage?.let { message ->
            item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        item { HorizontalDivider() }

        item {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
