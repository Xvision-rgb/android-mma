package com.example.mmarecomp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Police du corps de texte. Aucune fonte n'est embarquée dans l'app : on
 *  déclare explicitement la sans-serif système plutôt que de laisser le
 *  défaut implicite, pour que le jour où une fonte est ajoutée il n'y ait
 *  qu'une constante à changer ici. */
private val AppFontFamily = FontFamily.SansSerif

/** Chiffres à chasse fixe (`tnum`). Sans ça, les chiffres proportionnels
 *  changent de largeur d'une valeur à l'autre et les métriques tressautent
 *  quand elles se mettent à jour — visible sur le compteur de calories, le
 *  volume hebdo et le timer de repos. Appliqué aux styles qui portent des
 *  nombres, pas au texte courant. */
private const val TabularFigures = "tnum"

/** Échelle complète Material 3 — les 15 styles.
 *
 *  Avant, seuls 6 étaient définis : `bodySmall`, pourtant le style le plus
 *  utilisé de l'app (76 occurrences), retombait sur le défaut Material et
 *  échappait donc à toute maîtrise. Idem pour `titleSmall`, `headlineSmall`,
 *  `headlineMedium` (nom de l'app sur l'écran d'auth) et `labelMedium`. */
val MMATypography = Typography(
    // Réservé aux chiffres-clés en direct (calories/protéines du jour, volume
    // hebdo, série d'activité, maintenance calorique) — nettement plus grand
    // que titleLarge pour leur donner une vraie emphase visuelle, cf. les
    // dashboards fitness qui surdimensionnent leurs stats en direct.
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = TabularFigures,
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp,
        fontFeatureSettings = TabularFigures,
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontFeatureSettings = TabularFigures,
    ),
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Le style le plus employé de l'app : lignes de contexte sous les
    // métriques, libellés d'historique, explications de carte.
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // En-têtes de carte ("Cette semaine", "Nutrition", "État du jour").
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
    ),
)
