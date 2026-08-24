package com.example.mmarecomp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** `scale` vient de la préférence "Taille du texte" — 1f reproduit
 *  exactement les tailles historiques de l'app. */
fun mmaTypography(scale: Float = 1f): Typography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = (24 * scale).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (18 * scale).sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = (16 * scale).sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = (11 * scale).sp),
)
