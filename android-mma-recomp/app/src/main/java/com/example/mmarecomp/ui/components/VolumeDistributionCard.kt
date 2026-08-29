package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.MuscleZone
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.Formatting

/** Répartition réelle du volume hebdomadaire par zone, face aux parts cibles.
 *
 *  C'est l'écart le plus probable entre la pratique et l'objectif : un
 *  programme construit autour du développé et du squat produit un ratio
 *  tirage:poussée proche de 1:1, là où le physique visé en demande 2:1.
 *  Affiché factuellement — un chiffre et sa cible, jamais un reproche. */
@Composable
fun VolumeDistributionCard(
    repartition: Map<MuscleZone, Double>,
    ratioTiragePoussee: Double?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.cornerMd))
            .padding(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Text(
            "Répartition du volume (7 jours)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (repartition.isEmpty()) {
            Text(
                "Pas encore assez de séances loguées pour calculer la répartition.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        MuscleZone.entries.forEach { zone ->
            val part = repartition[zone] ?: 0.0
            ZoneBar(zone = zone, part = part)
        }

        ratioTiragePoussee?.let { ratio ->
            val cible = MuscleZone.RATIO_TIRAGE_POUSSEE_CIBLE
            Text(
                "Ratio tirage:poussée — ${Formatting.oneDecimal(ratio)}:1 " +
                    "(cible ${Formatting.oneDecimal(cible)}:1)",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (ratio < cible * 0.75) {
                SoftAlertBanner(
                    message = "Le tirage est la zone qui construit le plus le physique visé — " +
                        "il y a de la marge de ce côté.",
                    tone = SoftAlertTone.NEUTRAL,
                )
            }
        }
    }
}

@Composable
private fun ZoneBar(zone: MuscleZone, part: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(zone.label, style = MaterialTheme.typography.bodySmall)
            Text(
                "${(part * 100).toInt()} % · cible ${(zone.partCible * 100).toInt()} %",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(3.dp),
                ),
        ) {
            // La barre sature à 100 % de la largeur : un dépassement de cible
            // se lit dans le chiffre, il n'a pas besoin de déborder.
            val fraction = (part / zone.partCible).coerceIn(0.0, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
            )
        }
    }
}
