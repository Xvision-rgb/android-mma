package com.example.mmarecomp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** `scale` vient de la préférence "Taille du texte" — 1f reproduit
 *  exactement les tailles historiques de l'app. `accessibleFont` bascule sur
 *  une police à chasse fixe avec un espacement de lettres élargi — sans
 *  police tierce à embarquer, ça reste une aide réelle à la lecture pour
 *  certains troubles (confusion b/d/p/q notamment). */
fun mmaTypography(scale: Float = 1f, accessibleFont: Boolean = false): Typography {
    val family = if (accessibleFont) FontFamily.Monospace else FontFamily.Default
    val letterSpacing = if (accessibleFont) 0.06.em else 0.em
    return Typography(
        titleLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = (24 * scale).sp, letterSpacing = letterSpacing),
        titleMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (18 * scale).sp, letterSpacing = letterSpacing),
        bodyLarge = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (16 * scale).sp, letterSpacing = letterSpacing),
        bodyMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp, letterSpacing = letterSpacing),
        labelSmall = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = (11 * scale).sp, letterSpacing = letterSpacing),
    )
}
