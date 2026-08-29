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
import com.example.mmarecomp.util.BilanVolume
import com.example.mmarecomp.util.VolumeLandmarks
import com.example.mmarecomp.util.ZoneVolume

/** Séries par zone sur 4 semaines, face aux repères de volume.
 *
 *  C'est la carte qui répond à « est-ce que j'en fais assez », question à
 *  laquelle le tonnage en kg ne répondait pas : à volume égal la fréquence
 *  change peu de chose, mais le nombre de séries hebdomadaires, lui, pilote
 *  l'adaptation. */
@Composable
fun VolumeLandmarksCard(
    bilan: List<BilanVolume>,
    alertes: List<String>,
    modifier: Modifier = Modifier,
) {
    if (bilan.isEmpty()) return

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.cornerMd))
            .padding(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Text(
            "Séries par zone et par semaine",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Zone productive : ${VolumeLandmarks.MINIMUM_UTILE} à ${VolumeLandmarks.PRODUCTIF_HAUT} séries.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        bilan.forEach { ligne ->
            val couleur = when (ligne.situation) {
                ZoneVolume.PRODUCTIF -> MaterialTheme.colorScheme.tertiary
                ZoneVolume.MAINTIEN, ZoneVolume.PLAFOND -> MaterialTheme.colorScheme.secondary
                ZoneVolume.SOUS_MAINTIEN, ZoneVolume.AU_DESSUS -> MaterialTheme.colorScheme.primary
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    Box(modifier = Modifier.size(8.dp).background(couleur, CircleShape))
                    Text(ligne.zone.label, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "${ligne.series} séries",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        bilan.firstOrNull { it.situation == ZoneVolume.SOUS_MAINTIEN }?.let {
            SoftAlertBanner(message = it.message, tone = SoftAlertTone.NEUTRAL)
        }

        alertes.take(2).forEach { alerte ->
            SoftAlertBanner(message = alerte, tone = SoftAlertTone.NEUTRAL)
        }
    }
}
