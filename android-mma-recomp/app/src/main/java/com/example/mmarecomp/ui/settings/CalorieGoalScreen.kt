package com.example.mmarecomp.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.CalorieMode
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.components.SoftAlertTone
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.CalorieGoal
import com.example.mmarecomp.viewmodel.CalorieGoalViewModel

@Composable
fun CalorieGoalScreen(viewModel: CalorieGoalViewModel) {
    LaunchedEffect(Unit) { viewModel.load() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Objectif calorique", style = MaterialTheme.typography.titleLarge) }
        item {
            Text(
                "Calculé à partir de ton poids réel plutôt que d'une formule générique — " +
                    "un pratiquant de sport de combat qui s'entraîne 6-7x/semaine dépense " +
                    "bien plus qu'une personne sédentaire.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            OutlinedTextField(
                value = viewModel.poidsInputKg,
                onValueChange = { viewModel.poidsInputKg = it },
                label = { Text("Poids actuel (kg)") },
                supportingText = if (viewModel.poidsKg != null) {
                    { Text("Pré-rempli depuis ta dernière pesée") }
                } else {
                    { Text("Aucune pesée enregistrée — saisis ton poids manuellement") }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = viewModel.bfInputPct,
                onValueChange = { viewModel.bfInputPct = it },
                label = { Text("%BF actuel (optionnel)") },
                supportingText = { Text("Améliore la précision du calcul protéines, sinon une estimation prudente est utilisée") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val goalRecomposition = viewModel.goalFor(CalorieMode.Recomposition)
        if (goalRecomposition != null) {
            item {
                Text(
                    "Maintenance estimée : ${goalRecomposition.maintenanceCalories} kcal/jour",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                SoftAlertBanner(
                    message = if (viewModel.recommendedMode == CalorieMode.Coupe) {
                        "✓ Coupe recommandée — %BF encore assez élevé pour qu'un léger déficit reste efficace sans risquer le muscle."
                    } else {
                        "✓ Recomposition recommandée — l'option la plus sûre pour rester sec et explosif sans sacrifier de muscle."
                    },
                )
            }
        }

        item { HorizontalDivider() }
        item { Text("Choisis ton mode", style = MaterialTheme.typography.titleMedium) }

        CalorieMode.entries.forEach { mode ->
            item {
                CalorieModeCard(
                    mode = mode,
                    goal = viewModel.goalFor(mode),
                    isRecommended = viewModel.recommendedMode == mode,
                    isApplied = viewModel.appliedMode == mode,
                    isSaving = viewModel.isSaving,
                    onApply = { viewModel.applyMode(mode) },
                )
            }
        }

        if (viewModel.savedConfirmation) {
            item {
                SoftAlertBanner(message = "Mode enregistré — la cible du jour a été mise à jour ✓")
            }
        }
        viewModel.errorMessage?.let { error ->
            item { ErrorBanner(error, onRetry = { viewModel.load() }) }
        }

        item { HorizontalDivider() }
        item { Text("Recalibrage adaptatif", style = MaterialTheme.typography.titleMedium) }
        val recalibration = viewModel.recalibration
        if (recalibration != null) {
            item {
                Text(
                    "Sur les ${recalibration.periodDays} derniers jours, ta tendance de poids suggère une " +
                        "dépense réelle plus proche de ${recalibration.estimatedExpenditureCalories} kcal " +
                        "que l'estimation initiale (${recalibration.staticMaintenanceCalories} kcal).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Button(onClick = { viewModel.applyRecalibration() }, enabled = !viewModel.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text("Ajuster la cible sur cette estimation")
                }
            }
        } else {
            item {
                Text(
                    "Recalibrage disponible dès 14 jours de suivi régulier (pesées + repas loggés) — " +
                        "il compare ta tendance de poids réelle aux calories réellement loguées pour affiner " +
                        "l'estimation au-delà de la formule de départ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { HorizontalDivider() }
        item { Text("Repères pour un lutteur", style = MaterialTheme.typography.titleMedium) }
        item {
            Text(
                "• Recomposition = lent mais optimal pour rester sec et explosif — pas de course à la balance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                "• À maintenance, avec force + cardio léger réguliers, compte 4-6 mois pour atteindre 8-10% de masse grasse en gardant le muscle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                "• En coupe, monte encore un peu les protéines pour préserver le muscle malgré le déficit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                "• Priorise les glucides autour de tes séances (avant/après training) plutôt qu'étalés uniformément sur la journée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                "Le détail de la tendance poids/%BF (moyenne mobile 7 jours) reste visible sur le Dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalorieModeCard(
    mode: CalorieMode,
    goal: CalorieGoal?,
    isRecommended: Boolean,
    isApplied: Boolean,
    isSaving: Boolean,
    onApply: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(Dimens.cornerMd),
        color = if (isApplied) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isApplied) BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Dimens.spaceMd), verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(mode.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (isRecommended) {
                    Text("Recommandé", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            if (goal != null) {
                Text(
                    "${goal.targetCalories} kcal/jour" + when {
                        goal.offsetCalories > 0 -> " (maintenance +${goal.offsetCalories})"
                        goal.offsetCalories < 0 -> " (maintenance ${goal.offsetCalories})"
                        else -> " (= maintenance)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Protéines ${goal.proteinesG}g · Glucides ${goal.glucidesG}g · Lipides ${goal.lipidesG}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                goal.warning?.let { warning ->
                    SoftAlertBanner(message = warning, tone = SoftAlertTone.NEUTRAL)
                }
                Button(onClick = onApply, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isApplied) "Appliqué" else "Appliquer ce mode")
                }
            } else {
                Text(
                    "Renseigne ton poids ci-dessus pour voir le détail de ce mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
