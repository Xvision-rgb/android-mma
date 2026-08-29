package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
