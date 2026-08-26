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
 *  un message complet reste un SoftAlertBanner, pas ce composant.
 *
 *  Le fond est la même couleur que le texte, juste très éclaircie par alpha
 *  — donc plus l'alpha est haut, plus le fond se rapproche de la couleur du
 *  texte et moins il y a de contraste. Mesuré (luminance relative WCAG) :
 *  à 0.14 le badge Moss en mode clair tombait à 4.09:1, sous le seuil AA
 *  4.5:1 pour du texte. Ramené à 0.06 : 4.54:1 en clair, 6.58:1 en sombre —
 *  la teinte reste visible, le texte redevient conforme. */
@Composable
fun StatusBadge(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.tertiary) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
            .padding(horizontal = Dimens.spaceSm, vertical = 2.dp),
    )
}
