package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val REST_PRESETS_SECONDS = listOf(60, 90, 120, 150)

/** Minuteur de repos manuel entre deux exercices/séries — déclenché par
 *  l'utilisateur (notre modèle logue un exercice global, pas set par set,
 *  donc pas de déclenchement automatique possible comme sur Strong/Hevy).
 *  Vibration courte à la fin, jamais de son pour rester discret en salle. */
@Composable
fun RestTimer(modifier: Modifier = Modifier) {
    var remainingSeconds by remember { mutableStateOf<Int?>(null) }
    var activePreset by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(remainingSeconds) {
        val current = remainingSeconds
        if (current == null) {
            activePreset = null
            return@LaunchedEffect
        }
        if (current <= 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            remainingSeconds = null
            return@LaunchedEffect
        }
        delay(1000)
        remainingSeconds = current - 1
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Minuteur de repos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            REST_PRESETS_SECONDS.forEach { seconds ->
                FilterChip(
                    selected = activePreset == seconds,
                    onClick = { activePreset = seconds; remainingSeconds = seconds },
                    label = { Text("${seconds}s") },
                )
            }
        }
        remainingSeconds?.let { seconds ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Repos : ${seconds}s restantes", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { remainingSeconds = null }) { Text("Arrêter") }
            }
        }
    }
}
