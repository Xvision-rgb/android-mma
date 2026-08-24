package com.example.mmarecomp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.mmarecomp.util.timerEndVibration
import kotlinx.coroutines.delay

/** Minuteur de repos optionnel entre deux séries — démarré manuellement,
 *  vibre une fois à zéro puis se réinitialise. Purement un confort de
 *  saisie, aucune donnée associée. */
@Composable
fun RestTimerButton(seconds: Int) {
    var remaining by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    LaunchedEffect(remaining) {
        val current = remaining
        if (current == null) return@LaunchedEffect
        if (current <= 0) {
            timerEndVibration(context)
            remaining = null
            return@LaunchedEffect
        }
        delay(1000)
        remaining = current - 1
    }

    val current = remaining
    if (current != null) {
        TextButton(onClick = { remaining = null }) {
            Text("Repos : ${current}s (annuler)", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        TextButton(onClick = { remaining = seconds }) {
            Text("Démarrer repos (${seconds}s)", style = MaterialTheme.typography.bodySmall)
        }
    }
}
