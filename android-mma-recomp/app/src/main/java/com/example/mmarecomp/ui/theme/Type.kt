package com.example.mmarecomp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MMATypography = Typography(
    // Réservé aux chiffres-clés en direct (calories/protéines du jour, volume
    // hebdo, streak, maintenance calorique) — nettement plus grand que
    // titleLarge pour leur donner une vraie emphase visuelle, cf. les
    // dashboards fitness qui surdimensionnent leurs stats en direct.
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 40.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)
