package com.example.mmarecomp.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.ui.components.AppScaffold
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.viewmodel.MmaSessionViewModel
import kotlinx.coroutines.launch

@Composable
fun MmaSessionScreen(viewModel: MmaSessionViewModel, onSaved: () -> Unit, onBack: () -> Unit = {}) {
    val parsed = remember(viewModel.wodContent) { viewModel.parsedMovements }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showSaved by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadRecent() }

    fun deleteWithUndo(session: MmaSession) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.deleteFromHistory(session) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Séance MMA supprimée",
                    actionLabel = "Annuler",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restoreToHistory(session)
            }
        }
    }

    AppScaffold(title = "Log MMA", onBack = onBack) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        item { DateField("Date", viewModel.date, { viewModel.date = it }, modifier = Modifier.fillMaxWidth()) }

        item {
            TextButton(onClick = { viewModel.resetForm() }) { Text("Vider le formulaire") }
        }

        item {
            OutlinedTextField(
                value = viewModel.wodContent,
                onValueChange = { viewModel.wodContent = it },
                label = { Text("WOD du coach (collé depuis WhatsApp)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (parsed.isNotEmpty()) {
            item {
                Text("Mouvements détectés (${parsed.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    parsed.take(4).forEach { movement ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text(if (movement.quantite != null) "${movement.nom} ×${movement.quantite}" else movement.nom) },
                        )
                    }
                    if (parsed.size > 4) {
                        FilterChip(selected = false, onClick = {}, label = { Text("+${parsed.size - 4} autres") })
                    }
                }
            }
        }

        item { HorizontalDivider() }

        item {
            OutlinedTextField(
                value = viewModel.roundsSets,
                onValueChange = { viewModel.roundsSets = it },
                label = { Text("Rounds / Sets (ex: 5 rounds x 3min)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text("Ressenti (1 = très difficile, 5 = facile)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Ressenti : ${viewModel.ressenti} sur 5"
                },
            ) {
                (1..5).forEach { value ->
                    SegmentedButton(
                        selected = viewModel.ressenti == value,
                        onClick = { viewModel.ressenti = value },
                        shape = SegmentedButtonDefaults.itemShape(index = value - 1, count = 5),
                    ) { Text(value.toString()) }
                }
            }
        }

        item {
            OutlinedTextField(
                value = viewModel.notesTechnique,
                onValueChange = { viewModel.notesTechnique = it },
                label = { Text("Notes technique") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (viewModel.recentSessions.isNotEmpty()) {
            item { HorizontalDivider() }
            item {
                var showHistory by remember { mutableStateOf(false) }
                Column {
                    viewModel.averageRessenti?.let { avg ->
                        Text(
                            "Ressenti moyen sur les dernières séances : ${com.example.mmarecomp.util.Formatting.oneDecimal(avg)}/5",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(if (showHistory) "Masquer l'historique MMA" else "Voir l'historique des séances MMA")
                    }
                    if (showHistory) {
                        viewModel.recentSessions.forEach { session ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    session.date + (session.ressenti?.let { " · ressenti $it/5" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                IconButton(onClick = { deleteWithUndo(session) }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Supprimer la séance MMA du ${session.date}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.errorMessage?.let { error ->
            item {
                ErrorBanner(error, onRetry = { viewModel.save { saved -> if (saved) onSaved() } })
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.save { saved ->
                        showSaved = saved
                        if (saved) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                kotlinx.coroutines.delay(500)
                                onSaved()
                            }
                        }
                    }
                },
                enabled = !viewModel.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (viewModel.isSaving) "Enregistrement…" else "Enregistrer la séance MMA")
            }
        }
        item {
            AnimatedVisibility(
                visible = showSaved,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                Text("Séance MMA enregistrée 💪", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
    }
}
