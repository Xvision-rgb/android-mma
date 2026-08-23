package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.viewmodel.MmaSessionViewModel

@Composable
fun MmaSessionScreen(viewModel: MmaSessionViewModel, onSaved: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Log MMA", style = MaterialTheme.typography.titleLarge) }

        item {
            OutlinedTextField(
                value = viewModel.wodContent,
                onValueChange = { viewModel.wodContent = it },
                label = { Text("WOD du coach (collé depuis WhatsApp)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val parsed = remember(viewModel.wodContent) { viewModel.parsedMovements }
        if (parsed.isNotEmpty()) {
            item {
                Text("Mouvements détectés", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text("Ressenti", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
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

        viewModel.errorMessage?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Button(
                onClick = { viewModel.save { saved -> if (saved) onSaved() } },
                enabled = !viewModel.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (viewModel.isSaving) "Enregistrement…" else "Enregistrer la séance MMA")
            }
        }
    }
}
