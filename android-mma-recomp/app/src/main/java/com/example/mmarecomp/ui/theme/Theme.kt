package com.example.mmarecomp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Steel,
    secondary = Clay,
    tertiary = Moss,
    background = PaperLight,
    surface = SurfaceLight,
    onBackground = InkLight,
    onSurface = InkLight,
    surfaceVariant = TrackLight,
    onSurfaceVariant = TextSecondaryLight,
    // PaperLightAlt/PaperDarkAlt existaient déjà dans la palette mais
    // n'étaient jamais câblés à un rôle Material 3. Défini ici par robustesse :
    // tant qu'aucun token de surface tonale (surfaceContainerHigh, etc.)
    // n'est explicitement fourni à lightColorScheme/darkColorScheme, il
    // retombe sur la teinte violette par défaut de Material 3 — hors palette
    // si jamais un composant venait à l'utiliser. Volontairement PAS
    // utilisé pour DashCard : à la vérification, PaperLightAlt/PaperDarkAlt
    // sont trop proches de background pour créer une vraie séparation
    // visuelle avec les cartes — colorScheme.surface (blanc / gris foncé,
    // déjà utilisé par DashCard) offre un contraste carte/fond nettement
    // meilleur et reste donc le bon choix, inchangé.
    surfaceContainerHigh = PaperLightAlt,
)

private val DarkColors = darkColorScheme(
    primary = SteelDark,
    secondary = ClayDark,
    tertiary = MossDark,
    background = PaperDark,
    surface = SurfaceDark,
    onBackground = InkDark,
    onSurface = InkDark,
    surfaceVariant = TrackDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = PaperDarkAlt,
)

@Composable
fun MMARecompTheme(
    darkTheme: Boolean = when (ThemePreference.mode) {
        ThemeMode.Systeme -> isSystemInDarkTheme()
        ThemeMode.Clair -> false
        ThemeMode.Sombre -> true
    },
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MMATypography,
        content = content,
    )
}
