package com.example.mmarecomp.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.data.DisplayDensity

/** Espacement des listes principales selon la préférence "Densité
 *  d'affichage" — un seul point de réglage réutilisé par tous les écrans
 *  plutôt qu'une constante dupliquée cinq fois. */
fun densitySpacing(density: DisplayDensity): Dp = if (density == DisplayDensity.COMPACT) 8.dp else 16.dp

fun densityItemGap(density: DisplayDensity): Dp = if (density == DisplayDensity.COMPACT) 8.dp else 14.dp
