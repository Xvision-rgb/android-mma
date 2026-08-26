package com.example.mmarecomp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.compositeOver

// primaryContainer/secondaryContainer (et leurs "on") ne sont fournis nulle
// part ci-dessous historiquement : Material 3 retombe alors sur ses teintes
// violettes par défaut, hors palette — et ce sont justement les couleurs que
// prend l'indicateur d'onglet sélectionné de la NavigationBar et le FAB par
// défaut. Dérivées ici de la palette existante (Steel/Clay) par composition
// alpha sur la surface plutôt que de nouvelles teintes choisies à l'œil.
private val PrimaryContainerLight = Steel.copy(alpha = 0.16f).compositeOver(SurfaceLight)
private val OnPrimaryContainerLight = Steel
private val SecondaryContainerLight = Clay.copy(alpha = 0.16f).compositeOver(SurfaceLight)
private val OnSecondaryContainerLight = Clay

private val PrimaryContainerDark = SteelDark.copy(alpha = 0.20f).compositeOver(SurfaceDark)
private val OnPrimaryContainerDark = SteelDark
private val SecondaryContainerDark = ClayDark.copy(alpha = 0.20f).compositeOver(SurfaceDark)
private val OnSecondaryContainerDark = ClayDark

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
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
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
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
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
