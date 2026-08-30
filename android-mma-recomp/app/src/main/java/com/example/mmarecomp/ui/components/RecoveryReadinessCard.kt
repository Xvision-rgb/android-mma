package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.ui.theme.readinessColor
import com.example.mmarecomp.util.Formatting
import com.example.mmarecomp.util.ModulationSeance
import com.example.mmarecomp.util.TrainingLoad

/** État de forme du jour et modulation qui en découle.
 *
 *  Deux principes tiennent toute la carte : l'ajustement porte sur le VOLUME
 *  avant la charge, et il n'existe aucun état « repos complet » — la dose
 *  minimale entretient l'adaptation, l'arrêt total la perd. Une mauvaise
 *  journée allège la séance, elle ne la supprime pas. */
@Composable
fun RecoveryReadinessCard(
    modulation: ModulationSeance,
    score: Int?,
    acwr: Double?,
    aCheckInAujourdhui: Boolean,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Le mapping état -> couleur vit désormais dans SemanticColors : les
    // trois tons sont partagés avec le reste de l'app plutôt que redéfinis ici.
    val couleur = readinessColor(modulation.action)

    AppCard(modifier = modifier) {
        Text(
            "État du jour",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            Box(modifier = Modifier.size(Dimens.dotMd).background(couleur, CircleShape))
            Text(modulation.action.label, style = MaterialTheme.typography.titleMedium)
        }

        Text(
            modulation.explication,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!aCheckInAujourdhui) {
            Text(
                "Pas encore de point ce matin — la modulation se base sur ta charge seule.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onCheckIn) { Text("Faire le point du jour (20 s)") }
        } else {
            score?.let {
                Text(
                    "Score de forme : $it/25",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        acwr?.let {
            val zone = when {
                it > TrainingLoad.ACWR_ALERTE -> "montée de charge marquée"
                it > TrainingLoad.ACWR_MAX -> "au-dessus de ta zone habituelle"
                it < TrainingLoad.ACWR_MIN -> "en dessous de ta zone habituelle"
                else -> "dans ta zone habituelle"
            }
            Text(
                "Charge 7j / 28j : ${Formatting.oneDecimal(it)} — $zone.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
