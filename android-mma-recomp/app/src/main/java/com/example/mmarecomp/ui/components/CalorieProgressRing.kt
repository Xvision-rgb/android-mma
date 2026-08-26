package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Anneau de progression calorique du jour, inspiré des anneaux de fermeture
 *  d'Apple Fitness — un repère visuel immédiat en plus du texte/barre déjà
 *  existants (TargetVsActualBar), jamais à la place d'une info déjà là.
 *
 *  Toujours une seule teinte de progression positive, jamais de couleur
 *  "danger/échec" : le remplissage est plafonné à 100% qu'on soit en
 *  dessous ou au-dessus de la cible — cohérent avec TargetVsActualBar qui
 *  ne culpabilise jamais sur un déficit ni un dépassement. */
@Composable
fun CalorieProgressRing(actual: Int, target: Int, modifier: Modifier = Modifier) {
    val ratio = if (target > 0) (actual.toFloat() / target).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier
            .size(96.dp)
            .semantics { contentDescription = "$actual sur $target kcal aujourd'hui" },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$actual", style = MaterialTheme.typography.titleMedium)
            Text(
                "/ $target kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
