package com.example.mmarecomp.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.joursLabels
import com.example.mmarecomp.ui.components.AppScaffold
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.viewmodel.ImportTrainingPlanViewModel

/** Import d'un programme collé en texte libre (ex. généré par Claude).
 *  Le parsing (TrainingPlanParser) est toujours best-effort et n'écrit
 *  jamais directement en base : cet écran affiche un aperçu éditable par
 *  jour détecté, où corriger ce qui a été mal reconnu avant de valider —
 *  jour par jour ou en un clic pour tous les jours détectés. */
@Composable
fun ImportTrainingPlanScreen(
    viewModel: ImportTrainingPlanViewModel,
    phase: Phase,
    onBack: () -> Unit = {},
) {
    LaunchedEffect(phase) {
        viewModel.updatePhase(phase)
        viewModel.loadExisting()
    }

    AppScaffold(title = "Importer un programme", onBack = onBack) { padding ->
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(padding),
        contentPadding = PaddingValues(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        item {
            Text(
                "Colle un programme (par exemple généré par Claude) : un jour de la " +
                    "semaine par ligne (Lundi, Mardi…), puis les exercices en dessous " +
                    "(ex. \"Squat 4x8 @80kg\"). Le résultat sera à vérifier et corriger " +
                    "avant d'être enregistré — rien n'est écrit tant que tu ne valides pas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            OutlinedTextField(
                value = viewModel.rawText,
                onValueChange = { viewModel.rawText = it },
                label = { Text("Programme collé") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { viewModel.parse() },
                enabled = viewModel.rawText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Analyser le texte")
            }
        }

        viewModel.errorMessage?.let { error ->
            item { ErrorBanner(error, onRetry = { viewModel.loadExisting() }) }
        }

        if (viewModel.hasParsed && viewModel.drafts.isEmpty()) {
            item {
                Text(
                    "Aucun jour ni exercice reconnu dans ce texte — vérifie le format " +
                        "(un jour de la semaine par ligne, puis \"Exercice NxM\" en dessous) " +
                        "ou ajoute les exercices manuellement depuis le Dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(viewModel.drafts, key = { it.jourSemaine }) { draft ->
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider()
                Text(
                    joursLabels[draft.jourSemaine] ?: "",
                    style = MaterialTheme.typography.titleMedium,
                )

                if (draft.saved) {
                    // Une fois enregistré, plus aucun bouton ne permet de
                    // re-soumettre ce jour : le rendre éditable donnerait
                    // l'illusion que d'autres changements seraient pris en
                    // compte alors qu'ils seraient perdus sans le savoir.
                    Text(
                        "Enregistré ✓ — ${draft.exercices.size} exercice(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    draft.exercices.forEach { exercice ->
                        Text(
                            "${exercice.nom} — ${exercice.series}x${exercice.reps}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val existingCount = viewModel.existingDays[draft.jourSemaine]?.exercices?.size ?: 0
                    val hasExisting = existingCount > 0
                    if (hasExisting) {
                        Text(
                            "Ce jour a déjà $existingCount exercice(s) programmé(s) :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                            FilterChip(
                                selected = draft.appendToExisting,
                                onClick = { viewModel.setAppendMode(draft.jourSemaine, true) },
                                label = { Text("Compléter") },
                            )
                            FilterChip(
                                selected = !draft.appendToExisting,
                                onClick = { viewModel.setAppendMode(draft.jourSemaine, false) },
                                label = { Text("Remplacer") },
                            )
                        }
                    }

                    if (draft.exercices.isEmpty()) {
                        Text(
                            "Jour détecté mais aucun exercice reconnu en dessous — ajoute-les manuellement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    draft.exercices.forEachIndexed { index, exercice ->
                        PlannedExerciseRow(
                            exercice = exercice,
                            onChange = { viewModel.updateDraftExercise(draft.jourSemaine, index, it) },
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.removeDraftExercise(draft.jourSemaine, index) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Retirer cet exercice importé",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    TextButton(onClick = { viewModel.addDraftExercise(draft.jourSemaine) }) {
                        Text("Ajouter un exercice")
                    }

                    Button(
                        onClick = { viewModel.saveDay(draft.jourSemaine) {} },
                        enabled = !viewModel.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Enregistrer ce jour") }
                }
            }
        }

        if (viewModel.drafts.count { !it.saved } > 1) {
            item {
                var saveAllSummary by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(saveAllSummary) {
                    if (saveAllSummary != null) {
                        kotlinx.coroutines.delay(4000)
                        saveAllSummary = null
                    }
                }
                Column {
                    Button(
                        onClick = {
                            viewModel.saveAll { successCount, total ->
                                saveAllSummary = if (successCount == total) {
                                    "$successCount jour(s) enregistré(s) ✓"
                                } else {
                                    "$successCount jour(s) sur $total enregistré(s) — vérifie l'erreur ci-dessus pour le reste"
                                }
                            }
                        },
                        enabled = !viewModel.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (viewModel.isSaving) "Enregistrement…" else "Tout enregistrer")
                    }
                    saveAllSummary?.let { summary ->
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    }
}
