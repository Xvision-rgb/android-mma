package com.example.mmarecomp.ui.settings

import com.example.mmarecomp.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.mmarecomp.data.DateFormatStyle
import com.example.mmarecomp.data.DefaultTab
import com.example.mmarecomp.data.DisplayDensity
import com.example.mmarecomp.data.MovingAverageWindow
import com.example.mmarecomp.data.QuickAddSnack
import com.example.mmarecomp.data.SeriesReps
import com.example.mmarecomp.data.ThemeMode
import com.example.mmarecomp.data.UndoDuration
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
import com.example.mmarecomp.util.cancelDailyReminder
import com.example.mmarecomp.util.displayLabel
import com.example.mmarecomp.util.hasNotificationPermission
import com.example.mmarecomp.util.needsNotificationPermission
import com.example.mmarecomp.util.scheduleDailyReminder
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val csv = viewModel.buildExportCsv()
                context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                exportMessage = "Export enregistré."
                updatePrefs { it.copy(lastCsvExportEpochMillis = System.currentTimeMillis()) }
            } catch (e: Exception) {
                exportMessage = e.toFriendlyMessage("Échec de l'export.")
            }
        }
    }

    var importMessage by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val csv = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (csv.isNullOrBlank()) {
                    importMessage = "Fichier vide ou illisible."
                } else {
                    val (imported, skipped) = viewModel.importWeighInsCsv(csv)
                    importMessage = if (skipped == 0) {
                        "$imported pesée(s) importée(s)."
                    } else {
                        "$imported pesée(s) importée(s), $skipped ligne(s) ignorée(s)."
                    }
                }
            } catch (e: Exception) {
                importMessage = e.toFriendlyMessage("Échec de l'import.")
            }
        }
    }

    var showResetConfirm by remember { mutableStateOf(false) }

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
            LabeledDropdown(
                title = "Afficher la carte Constance à partir de",
                options = listOf(1, 2, 3, 5, 7),
                selected = prefs.streakDisplayThreshold,
                labelFor = { if (it == 1) "1 jour" else "$it jours" },
                onSelect = { days -> updatePrefs { it.copy(streakDisplayThreshold = days) } },
            )
        }
        item {
            ToggleRow(
                "Verrouiller l'app par biométrie/PIN",
                prefs.appLockEnabled,
            ) { checked -> updatePrefs { it.copy(appLockEnabled = checked) } }
        }
        item {
            Text(
                "Utilise le déverrouillage d'écran de ton téléphone — n'a d'effet que si un appareil a une " +
                    "empreinte, un visage ou un code configuré.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            ToggleRow(
                "Rappel quotidien",
                prefs.dailyReminderEnabled,
            ) { checked ->
                updatePrefs { it.copy(dailyReminderEnabled = checked) }
                if (checked) {
                    if (needsNotificationPermission && !hasNotificationPermission(context)) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    scheduleDailyReminder(context, prefs.dailyReminderHour, prefs.dailyReminderMinute)
                } else {
                    cancelDailyReminder(context)
                }
            }
        }
        if (prefs.dailyReminderEnabled) {
            item {
                Text(
                    "Un simple rappel générique pour logger ta journée — jamais lié à la pesée, jamais " +
                        "culpabilisant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledDropdown(
                        title = "Heure",
                        options = (0..23).toList(),
                        selected = prefs.dailyReminderHour,
                        labelFor = { "%02dh".format(it) },
                        onSelect = { hour ->
                            updatePrefs { it.copy(dailyReminderHour = hour) }
                            scheduleDailyReminder(context, hour, prefs.dailyReminderMinute)
                        },
                    )
                    LabeledDropdown(
                        title = "Minute",
                        options = listOf(0, 15, 30, 45),
                        selected = prefs.dailyReminderMinute,
                        labelFor = { "%02d".format(it) },
                        onSelect = { minute ->
                            updatePrefs { it.copy(dailyReminderMinute = minute) }
                            scheduleDailyReminder(context, prefs.dailyReminderHour, minute)
                        },
                    )
                }
            }
            item {
                ToggleRow(
                    "Rappel silencieux (sans son ni vibration)",
                    prefs.dailyReminderSilent,
                ) { checked -> updatePrefs { it.copy(dailyReminderSilent = checked) } }
            }
        }
        item {
            ToggleRow(
                "Rappel après séance manquée sur le dashboard",
                prefs.missedWorkoutReminderEnabled,
            ) { checked -> updatePrefs { it.copy(missedWorkoutReminderEnabled = checked) } }
        }
        if (prefs.missedWorkoutReminderEnabled) {
            item {
                Text(
                    "Un message neutre sur le dashboard, jamais une notification — juste un rappel doux.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                LabeledDropdown(
                    title = "Après combien de jours",
                    options = listOf(2, 3, 5, 7, 10, 14),
                    selected = prefs.missedWorkoutReminderDays,
                    labelFor = { "$it jours" },
                    onSelect = { days -> updatePrefs { it.copy(missedWorkoutReminderDays = days) } },
                )
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
                "Cible nutrition ajustée selon la phase",
                prefs.nutritionTargetByPhase,
            ) { checked -> updatePrefs { it.copy(nutritionTargetByPhase = checked) } }
        }
        item {
            ToggleRow(
                "Répartition macro personnalisée (%)",
                prefs.macroSplitEnabled,
            ) { checked -> updatePrefs { it.copy(macroSplitEnabled = checked) } }
        }
        if (prefs.macroSplitEnabled) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prefs.macroProteinPct.toString(),
                        onValueChange = { text -> text.toIntOrNull()?.let { pct -> updatePrefs { it.copy(macroProteinPct = pct) } } },
                        label = { Text("% protéines") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = prefs.macroCarbsPct.toString(),
                        onValueChange = { text -> text.toIntOrNull()?.let { pct -> updatePrefs { it.copy(macroCarbsPct = pct) } } },
                        label = { Text("% glucides") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = prefs.macroFatPct.toString(),
                        onValueChange = { text -> text.toIntOrNull()?.let { pct -> updatePrefs { it.copy(macroFatPct = pct) } } },
                        label = { Text("% lipides") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Text(
                    "Idéalement, les trois pourcentages totalisent 100.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            ToggleRow(
                "Suivi de l'hydratation (carte dashboard)",
                prefs.hydrationEnabled,
            ) { checked -> updatePrefs { it.copy(hydrationEnabled = checked) } }
        }
        item {
            ToggleRow(
                "Résumé hebdomadaire (carte dashboard, partageable)",
                prefs.weeklySummaryCardEnabled,
            ) { checked -> updatePrefs { it.copy(weeklySummaryCardEnabled = checked) } }
        }
        item {
            ToggleRow(
                "Cibles nutrition affichées en %",
                prefs.showTargetsAsPercent,
            ) { checked -> updatePrefs { it.copy(showTargetsAsPercent = checked) } }
        }
        item {
            ToggleRow(
                "Historique des cibles nutrition (60 derniers jours)",
                prefs.nutritionTargetHistoryEnabled,
            ) { checked -> updatePrefs { it.copy(nutritionTargetHistoryEnabled = checked) } }
        }
        item {
            ToggleRow(
                "Estimation de date d'objectif de poids",
                prefs.weightGoalEtaEnabled,
            ) { checked -> updatePrefs { it.copy(weightGoalEtaEnabled = checked) } }
        }
        item {
            LabeledDropdown(
                title = "Sensibilité du plateau",
                options = listOf(7, 10, 14, 21, 28),
                selected = prefs.plateauSensitivityDays,
                labelFor = { "$it jours" },
                onSelect = { days -> updatePrefs { it.copy(plateauSensitivityDays = days) } },
            )
        }
        item { Text("Collations rapides", style = MaterialTheme.typography.bodyMedium) }
        prefs.quickAddSnacks.forEach { snack ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${snack.name} · ${snack.calories} kcal · ${snack.proteinesG.toInt()}g", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        updatePrefs { it.copy(quickAddSnacks = it.quickAddSnacks.filterNot { s -> s.name == snack.name }) }
                    }) { Text("Retirer") }
                }
            }
        }
        item {
            var newSnackName by remember { mutableStateOf("") }
            var newSnackCalories by remember { mutableStateOf("") }
            var newSnackProteines by remember { mutableStateOf("") }
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSnackName,
                        onValueChange = { newSnackName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = newSnackCalories,
                        onValueChange = { newSnackCalories = it },
                        label = { Text("kcal") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = newSnackProteines,
                        onValueChange = { newSnackProteines = it },
                        label = { Text("Protéines") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(
                    onClick = {
                        val calories = newSnackCalories.toIntOrNull()
                        val proteines = newSnackProteines.replace(",", ".").toDoubleOrNull()
                        if (newSnackName.isNotBlank() && calories != null && proteines != null) {
                            updatePrefs {
                                it.copy(quickAddSnacks = it.quickAddSnacks + QuickAddSnack(newSnackName, calories, proteines))
                            }
                            newSnackName = ""; newSnackCalories = ""; newSnackProteines = ""
                        }
                    },
                ) { Text("Ajouter une collation") }
            }
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
        item {
            LabeledSegmentedChoice(
                title = "Durée du snackbar \"Annuler\"",
                options = UndoDuration.entries,
                selected = prefs.undoDuration,
                labelFor = { it.label },
                onSelect = { duration -> updatePrefs { it.copy(undoDuration = duration) } },
            )
        }
        item {
            LabeledSegmentedChoice(
                title = "Format de date",
                options = DateFormatStyle.entries,
                selected = prefs.dateFormat,
                labelFor = { it.label },
                onSelect = { format -> updatePrefs { it.copy(dateFormat = format) } },
            )
        }
        item {
            LabeledSegmentedChoice(
                title = "Densité d'affichage",
                options = DisplayDensity.entries,
                selected = prefs.displayDensity,
                labelFor = { it.label },
                onSelect = { density -> updatePrefs { it.copy(displayDensity = density) } },
            )
        }
        item {
            ToggleRow(
                "Mode contraste élevé",
                prefs.highContrast,
            ) { checked -> updatePrefs { it.copy(highContrast = checked) } }
        }
        item {
            ToggleRow(
                "Garder l'écran allumé pendant la saisie",
                prefs.keepScreenOnWhileLogging,
            ) { checked -> updatePrefs { it.copy(keepScreenOnWhileLogging = checked) } }
        }
        item {
            ToggleRow(
                "Confirmer avant de quitter un formulaire modifié",
                prefs.confirmDiscardUnsavedChanges,
            ) { checked -> updatePrefs { it.copy(confirmDiscardUnsavedChanges = checked) } }
        }
        item {
            ToggleRow(
                "Vibrer à chaque enregistrement réussi",
                prefs.vibrateOnAnySave,
            ) { checked -> updatePrefs { it.copy(vibrateOnAnySave = checked) } }
        }

        item { Text("Onglets — ordre et visibilité", style = MaterialTheme.typography.bodyMedium) }
        item {
            val order = if (prefs.tabOrder.isEmpty()) DefaultTab.entries.map { it.route } else prefs.tabOrder
            Column {
                order.forEachIndexed { index, route ->
                    val tab = DefaultTab.entries.find { it.route == route } ?: return@forEachIndexed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val newOrder = order.toMutableList()
                                        newOrder[index] = newOrder[index - 1].also { newOrder[index - 1] = newOrder[index] }
                                        updatePrefs { it.copy(tabOrder = newOrder) }
                                    }
                                },
                                enabled = index > 0,
                            ) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Monter ${tab.label}") }
                            IconButton(
                                onClick = {
                                    if (index < order.size - 1) {
                                        val newOrder = order.toMutableList()
                                        newOrder[index] = newOrder[index + 1].also { newOrder[index + 1] = newOrder[index] }
                                        updatePrefs { it.copy(tabOrder = newOrder) }
                                    }
                                },
                                enabled = index < order.size - 1,
                            ) { Icon(Icons.Filled.ArrowDownward, contentDescription = "Descendre ${tab.label}") }
                            Text(tab.label, style = MaterialTheme.typography.bodyMedium)
                        }
                        ToggleRow(
                            label = "",
                            checked = tab.route !in prefs.hiddenTabs,
                            onCheckedChange = { visible ->
                                updatePrefs {
                                    it.copy(hiddenTabs = if (visible) it.hiddenTabs - tab.route else it.hiddenTabs + tab.route)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text("Entraînement", style = MaterialTheme.typography.titleMedium) }
        item {
            ToggleRow(
                "Minuteur de repos entre séries",
                prefs.restTimerEnabled,
            ) { checked -> updatePrefs { it.copy(restTimerEnabled = checked) } }
        }
        if (prefs.restTimerEnabled) {
            item {
                LabeledDropdown(
                    title = "Durée du repos",
                    options = listOf(30, 45, 60, 90, 120, 180),
                    selected = prefs.restTimerSeconds,
                    labelFor = { "${it}s" },
                    onSelect = { seconds -> updatePrefs { it.copy(restTimerSeconds = seconds) } },
                )
            }
        }
        item {
            ToggleRow(
                "Auto-remplissage de la durée (dernière séance du même type)",
                prefs.autoFillLastDuration,
            ) { checked -> updatePrefs { it.copy(autoFillLastDuration = checked) } }
        }
        item {
            ToggleRow(
                "Historique rapide (meilleure charge connue par exercice)",
                prefs.showExerciseHistory,
            ) { checked -> updatePrefs { it.copy(showExerciseHistory = checked) } }
        }
        item { Text("Séries/reps par défaut par type de séance", style = MaterialTheme.typography.bodyMedium) }
        WorkoutType.entries.forEach { type ->
            item {
                val defaults = prefs.defaultSeriesRepsByType[type.name]
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = defaults?.series?.toString() ?: "",
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { series ->
                                updatePrefs {
                                    val updated = it.defaultSeriesRepsByType +
                                        (type.name to SeriesReps(series, defaults?.reps ?: 10))
                                    it.copy(defaultSeriesRepsByType = updated)
                                }
                            }
                        },
                        label = { Text("${type.label} · séries") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = defaults?.reps?.toString() ?: "",
                        onValueChange = { text ->
                            text.toIntOrNull()?.let { reps ->
                                updatePrefs {
                                    val updated = it.defaultSeriesRepsByType +
                                        (type.name to SeriesReps(defaults?.series ?: 3, reps))
                                    it.copy(defaultSeriesRepsByType = updated)
                                }
                            }
                        },
                        label = { Text("reps") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
        val daysSinceExport = if (prefs.lastCsvExportEpochMillis == 0L) {
            null
        } else {
            (System.currentTimeMillis() - prefs.lastCsvExportEpochMillis) / (1000L * 60 * 60 * 24)
        }
        if (daysSinceExport == null || daysSinceExport > 30) {
            item {
                Text(
                    if (daysSinceExport == null) {
                        "Tu n'as jamais exporté tes données — pense à faire une sauvegarde de temps en temps."
                    } else {
                        "Dernière sauvegarde il y a $daysSinceExport jours — pense à en refaire une."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        exportMessage?.let { message ->
            item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        item {
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("text/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Importer des pesées (CSV)") }
        }
        importMessage?.let { message ->
            item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        item { HorizontalDivider() }

        item {
            OutlinedButton(
                onClick = { showResetConfirm = true },
                enabled = !viewModel.isResetting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (viewModel.isResetting) "Réinitialisation…" else "Réinitialiser toutes mes données",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item { HorizontalDivider() }

        item {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
            }
        }

        item { HorizontalDivider() }
        item { Text("À propos", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                "Dernières nouveautés : préférences utilisateur étendues (minuteur de repos, " +
                    "favoris repas et collations, verrouillage biométrique, rappels, import/export CSV, " +
                    "personnalisation de l'affichage et de la navigation).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Tout réinitialiser ?") },
            text = {
                Text(
                    "Toutes tes séances, repas, pesées et ton split programmé seront supprimés " +
                        "définitivement. Tes objectifs et ta phase restent inchangés.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    viewModel.resetAllData { }
                }) { Text("Réinitialiser", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Annuler") }
            },
        )
    }
}
