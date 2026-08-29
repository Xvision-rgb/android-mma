package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
