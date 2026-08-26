package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.ui.theme.Dimens

/** Pastille compacte pour un libellé d'état court (ex. "Recommandé",
 *  "✓ atteint") — harmonise les endroits où un simple texte coloré ne se
 *  distinguait pas assez du texte environnant. Réservé aux libellés courts :
 *  un message complet reste un SoftAlertBanner, pas ce composant. */
@Composable
fun StatusBadge(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.tertiary) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = Dimens.spaceSm, vertical = 2.dp),
    )
}
