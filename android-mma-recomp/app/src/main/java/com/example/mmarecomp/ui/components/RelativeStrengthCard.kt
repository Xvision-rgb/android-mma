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
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.cornerMd))
            .padding(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
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
            return@Column
        }

        forces.take(4).forEach { force ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    force.exercice,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    "×${Formatting.oneDecimal(force.ratio)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                "1RM estimé ${Formatting.oneDecimal(force.unRmEstimeKg)}kg " +
                    "· poids ${Formatting.oneDecimal(force.poidsCorpsKg)}kg (moyenne 7j)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "C'est ce ratio qui mesure la progression, pas la balance seule.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
