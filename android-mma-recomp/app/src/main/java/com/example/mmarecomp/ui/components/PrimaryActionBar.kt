package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mmarecomp.ui.theme.Dimens

/** Action principale d'un écran de saisie, ancrée en bas et hors défilement.
 *
 *  Elle était jusqu'ici le dernier `item` de la `LazyColumn` : après six
 *  exercices, valider sa séance demandait de retraverser tout l'écran. C'est
 *  précisément le geste qu'on fait en salle, une main sur le téléphone, entre
 *  deux séries — donc dans la zone du pouce, sans défilement, avec une cible
 *  d'au moins 48dp pour rester atteignable les doigts moites. */
@Composable
fun PrimaryActionBar(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimens.cardElevationRaised,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spaceMd)
                .defaultMinSize(minHeight = Dimens.minTouchTarget),
        ) {
            Text(label)
        }
    }
}
