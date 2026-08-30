package com.example.mmarecomp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mmarecomp.ui.theme.Dimens

/** Niveau d'emphase d'une carte. Trois variantes, et pas plus : au-delà, la
 *  hiérarchie cesse d'être lisible et on retombe sur le défaut d'avant, où
 *  quatorze blocs avaient exactement le même poids visuel. */
enum class AppCardTone {
    /** Le cas courant. */
    STANDARD,

    /** La « grande chose » de l'écran — une seule par écran. */
    HERO,

    /** Bannières et états, teinte d'accent. */
    ACCENT,
}

/** Conteneur de carte unique de l'app.
 *
 *  Avant, chaque carte était un `Column` avec `.background()` recopié, et les
 *  surfaces divergeaient d'un composant à l'autre (`surface`, `surfaceVariant`,
 *  `tertiary`), sans jamais d'élévation ni de bordure. En sombre, `SurfaceDark`
 *  (#1C1C1E) sur `PaperDark` (#14171C) ne séparait presque rien — les cartes se
 *  fondaient dans le fond. On s'appuie ici sur `Card` de Material 3, avec une
 *  bordure fine en plus de l'élévation : l'élévation seule reste peu lisible
 *  sur fond sombre. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: AppCardTone = AppCardTone.STANDARD,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor = when (tone) {
        AppCardTone.STANDARD -> scheme.surface
        AppCardTone.HERO -> scheme.surfaceContainerHigh
        AppCardTone.ACCENT -> scheme.secondaryContainer
    }
    val elevation = when (tone) {
        AppCardTone.HERO -> Dimens.cardElevationRaised
        else -> Dimens.cardElevation
    }
    val borderColor = when (tone) {
        AppCardTone.HERO -> scheme.primary.copy(alpha = 0.35f)
        else -> scheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cornerMd),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(Dimens.borderHairline, borderColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardContentSpacing),
            content = content,
        )
    }
}
