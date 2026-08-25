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
