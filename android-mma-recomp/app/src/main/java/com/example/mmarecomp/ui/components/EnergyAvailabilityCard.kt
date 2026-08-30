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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.ConfianceSuivi
import com.example.mmarecomp.util.DisponibiliteEnergetique
import com.example.mmarecomp.util.EaStatut
import com.example.mmarecomp.util.Formatting

/** Disponibilité énergétique du jour et fiabilité du suivi.
 *
 *  Deux informations que les totaux caloriques ne portent pas : ce qui reste
 *  une fois l'entraînement payé, et la confiance qu'on peut accorder au
 *  calcul. Ton factuel — un chiffre, un seuil, une action possible. */
@Composable
fun EnergyAvailabilityCard(
    ea: DisponibiliteEnergetique?,
    confiance: ConfianceSuivi?,
    modifier: Modifier = Modifier,
) {
    if (ea == null && confiance == null) return

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.cornerMd))
            .padding(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        ea?.let {
            Text(
                "Disponibilité énergétique",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                val couleur = when (it.statut) {
                    EaStatut.CORRECTE -> MaterialTheme.colorScheme.tertiary
                    EaStatut.VIGILANCE -> MaterialTheme.colorScheme.secondary
                    EaStatut.BASSE -> MaterialTheme.colorScheme.primary
                }
                Box(modifier = Modifier.size(10.dp).background(couleur, CircleShape))
                Text(
                    "${Formatting.oneDecimal(it.kcalParKgMasseMaigre)} kcal/kg de masse maigre",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                it.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (it.depenseExerciceKcal > 0) {
                Text(
                    "Estimation : ~${it.depenseExerciceKcal} kcal dépensés à l'entraînement aujourd'hui.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        confiance?.let {
            Text(
                "${it.niveau.label} — ${it.message}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
