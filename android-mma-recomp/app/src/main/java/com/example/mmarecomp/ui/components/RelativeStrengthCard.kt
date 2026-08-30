package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.ForceRelative
import com.example.mmarecomp.util.Formatting

/** Indicateur directeur : 1RM estimé rapporté au poids de corps.
 *
 *  Placé au-dessus du poids dans la hiérarchie du dashboard, parce que c'est
 *  lui qui répond à la question posée — devenir plus fort pour son poids — là
 *  où la balance seule ne dit rien dans un sens ou dans l'autre. */
@Composable
fun RelativeStrengthCard(
    forces: List<ForceRelative>,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, tone = AppCardTone.HERO) {
        Text(
            "Force relative — 1RM estimé / poids de corps",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (forces.isEmpty()) {
            Text(
                "Logue quelques séries sur tes mouvements principaux pour voir ce chiffre apparaître.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@AppCard
        }

        // UN chiffre domine, celui du meilleur mouvement. Une carte "Hero"
        // qui aligne quatre lignes de même taille est prioritaire sans être
        // dominante — le pire des deux mondes. Le détail reste dessous, en
        // retrait, pour qui veut comparer.
        val meilleur = forces.first()
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Text(
                "×${Formatting.oneDecimal(meilleur.ratio)}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                meilleur.exercice,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Dimens.spaceXs).weight(1f, fill = false),
            )
        }
        Text(
            "1RM estimé ${Formatting.oneDecimal(meilleur.unRmEstimeKg)}kg " +
                "· poids ${Formatting.oneDecimal(meilleur.poidsCorpsKg)}kg (moyenne 7j)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Les autres mouvements : une ligne compacte chacun, sans le détail
        // 1RM/poids qui triplait la hauteur de la carte.
        forces.drop(1).take(3).forEach { force ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    force.exercice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    "×${Formatting.oneDecimal(force.ratio)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Text(
            "C'est ce ratio qui mesure la progression, pas la balance seule.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
