package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Lot 7 — Post-Workout Macro Timing banner */
@Composable
fun MacroTimingBanner(
    show: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!show) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("⏱️ Dans 30min: 40-60g carbs + 20-30g protéine pour la récupération", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiary)
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Fermer")
        }
    }
}

/** Lot 10 — 1RM Estimate card */
@Composable
fun OneRMCard(
    exerciseName: String,
    estimatedOneRM: Double?,
    modifier: Modifier = Modifier,
) {
    if (estimatedOneRM == null) return
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        Column {
            Text("Est. 1RM: $exerciseName", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${"%.1f".format(estimatedOneRM)}kg", style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Lot 13 — Share Session mock modal */
@Composable
fun SessionShareCard(
    summary: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Partager cette séance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onCopyClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Copier vers presse-papiers")
        }
    }
}

/** Lot 15 — Conversational Summary card */
@Composable
fun AskClaudeCard(
    mockSummary: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✨", style = MaterialTheme.typography.headlineSmall)
            Text("Demande à Claude", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiary)
        }
        Text(mockSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiary)
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("En savoir plus")
        }
    }
}
