package com.example.mmarecomp.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.notification.WeighInReminder
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.ui.theme.ThemeMode
import com.example.mmarecomp.ui.theme.ThemePreference
import com.example.mmarecomp.viewmodel.ProfileViewModel

@Composable
fun SettingsScreen(
    viewModel: ProfileViewModel,
    userEmail: String,
    onPhaseSaved: (Phase) -> Unit,
    onSignOut: () -> Unit,
    onImportPlan: () -> Unit = {},
    onOpenCalorieGoal: () -> Unit = {},
) {
    LaunchedEffect(Unit) { viewModel.load() }

    val context = LocalContext.current
    var reminderEnabled by remember { mutableStateOf(WeighInReminder.isEnabled(context)) }
    var reminderHour by remember { mutableStateOf(WeighInReminder.hour(context)) }
    var reminderMinute by remember { mutableStateOf(WeighInReminder.minute(context)) }
    var mealReminderEnabled by remember { mutableStateOf(com.example.mmarecomp.notification.MealReminder.isEnabled(context)) }
    var mealReminderHour by remember { mutableStateOf(com.example.mmarecomp.notification.MealReminder.hour(context)) }
    var mealReminderMinute by remember { mutableStateOf(com.example.mmarecomp.notification.MealReminder.minute(context)) }
    var pendingReminder by remember { mutableStateOf<String?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            permissionDenied = false
            when (pendingReminder) {
                "weighin" -> { reminderEnabled = true; WeighInReminder.setEnabled(context, true) }
                "meal" -> { mealReminderEnabled = true; com.example.mmarecomp.notification.MealReminder.setEnabled(context, true) }
            }
        } else {
            permissionDenied = true
        }
        pendingReminder = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        item { Text("Réglages", style = MaterialTheme.typography.titleLarge) }
        if (userEmail.isNotBlank()) {
            item {
                Text(
                    "Connecté en tant que $userEmail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { Text("Objectifs", style = MaterialTheme.typography.titleMedium) }

        item {
            OutlinedButton(onClick = onOpenCalorieGoal, modifier = Modifier.fillMaxWidth()) {
                Text("Objectif calorique (Bulk / Recomposition / Coupe)")
            }
        }
        item {
            val poidsValeur = viewModel.poidsObjectifKg.replace(",", ".").toDoubleOrNull()
            val poidsInvalide = viewModel.poidsObjectifKg.isNotBlank() && (poidsValeur == null || poidsValeur < 20 || poidsValeur > 400)
            OutlinedTextField(
                value = viewModel.poidsObjectifKg,
                onValueChange = { viewModel.poidsObjectifKg = it },
                label = { Text("Poids objectif (kg)") },
                isError = poidsInvalide,
                supportingText = if (poidsInvalide) { { Text("Entre un nombre valide, ex. 78") } } else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val bfInvalide = viewModel.bfObjectifPct.isNotBlank() &&
                (viewModel.bfObjectifPct.replace(",", ".").toDoubleOrNull() ?: -1.0).let { it < 0 || it > 60 }
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            OutlinedTextField(
                value = viewModel.bfObjectifPct,
                onValueChange = { viewModel.bfObjectifPct = it },
                label = { Text("% BF objectif") },
                isError = bfInvalide,
                supportingText = if (bfInvalide) { { Text("Entre un pourcentage entre 0 et 60") } } else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = viewModel.phase.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Phase") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Phase.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) }, onClick = { viewModel.phase = option; expanded = false })
                    }
                }
            }
        }

        viewModel.errorMessage?.let { error ->
            item { ErrorBanner(error, onRetry = { viewModel.load() }) }
        }

        item {
            Button(
                onClick = { viewModel.save(onPhaseSaved) },
                enabled = !viewModel.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer") }
        }

        item { HorizontalDivider() }
        item { Text("Programme d'entraînement", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                "Modifie les exercices d'un jour depuis le Dashboard (\"Voir le programme " +
                    "de la semaine\"), ou importe un programme complet en une fois.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedButton(onClick = onImportPlan, modifier = Modifier.fillMaxWidth()) {
                Text("Importer un programme")
            }
        }

        item { HorizontalDivider() }
        item { Text("Apparence", style = MaterialTheme.typography.titleMedium) }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = ThemePreference.mode == mode,
                        onClick = { ThemePreference.updateMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                    ) { Text(mode.label) }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text("Rappels", style = MaterialTheme.typography.titleMedium) }
        if (permissionDenied) {
            item {
                Text(
                    "Permission de notification refusée — active-la dans les réglages système de l'app pour utiliser les rappels.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = reminderEnabled,
                    role = Role.Switch,
                    onValueChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pendingReminder = "weighin"
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            reminderEnabled = checked
                            WeighInReminder.setEnabled(context, checked)
                        }
                    },
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Rappel doux pesée du matin (%02d:%02d)".format(reminderHour, reminderMinute),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = reminderEnabled, onCheckedChange = null)
            }
        }
        if (reminderEnabled) {
            item {
                TextButton(onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            reminderHour = h
                            reminderMinute = m
                            WeighInReminder.setTime(context, h, m)
                        },
                        reminderHour,
                        reminderMinute,
                        true,
                    ).show()
                }) { Text("Changer l'heure du rappel pesée") }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = mealReminderEnabled,
                    role = Role.Switch,
                    onValueChange = { checked ->
                        if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pendingReminder = "meal"
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            mealReminderEnabled = checked
                            com.example.mmarecomp.notification.MealReminder.setEnabled(context, checked)
                        }
                    },
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Rappel doux repas du soir (%02d:%02d)".format(mealReminderHour, mealReminderMinute),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = mealReminderEnabled, onCheckedChange = null)
            }
        }
        if (mealReminderEnabled) {
            item {
                TextButton(onClick = {
                    TimePickerDialog(
                        context,
                        { _, h, m ->
                            mealReminderHour = h
                            mealReminderMinute = m
                            com.example.mmarecomp.notification.MealReminder.setTime(context, h, m)
                        },
                        mealReminderHour,
                        mealReminderMinute,
                        true,
                    ).show()
                }) { Text("Changer l'heure du rappel repas") }
            }
        }

        item { HorizontalDivider() }
        item { Text("À propos", style = MaterialTheme.typography.titleMedium) }
        item {
            val versionName = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "—"
            }
            Text(
                "Recomp & MMA · version $versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { HorizontalDivider() }

        item {
            var showSignOutConfirm by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { showSignOutConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
            }
            if (showSignOutConfirm) {
                AlertDialog(
                    onDismissRequest = { showSignOutConfirm = false },
                    title = { Text("Se déconnecter ?") },
                    text = { Text("Tes données restent sauvegardées, tu pourras te reconnecter à tout moment.") },
                    confirmButton = {
                        TextButton(onClick = { showSignOutConfirm = false; onSignOut() }) { Text("Se déconnecter") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSignOutConfirm = false }) { Text("Annuler") }
                    },
                )
            }
        }
    }
}
