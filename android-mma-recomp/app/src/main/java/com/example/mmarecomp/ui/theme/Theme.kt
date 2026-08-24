package com.example.mmarecomp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mmarecomp.data.AccentPreset
import com.example.mmarecomp.data.ThemeMode

private fun accentColors(accent: AccentPreset): Pair<Color, Color> = when (accent) {
    AccentPreset.STEEL -> Steel to SteelDark
    AccentPreset.CLAY -> Clay to ClayDark
    AccentPreset.MOSS -> Moss to MossDark
    AccentPreset.EMBER -> Ember to EmberDark
    AccentPreset.OCEAN -> Ocean to OceanDark
}

private fun lightColors(accent: AccentPreset) = lightColorScheme(
    primary = accentColors(accent).first,
    secondary = Clay,
    tertiary = Moss,
    background = PaperLight,
    surface = SurfaceLight,
    onBackground = InkLight,
    onSurface = InkLight,
    surfaceVariant = TrackLight,
    onSurfaceVariant = TextSecondaryLight,
)

private fun darkColors(accent: AccentPreset) = darkColorScheme(
    primary = accentColors(accent).second,
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
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentPreset = AccentPreset.STEEL,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors(accent) else lightColors(accent),
        typography = mmaTypography(textScale),
        content = content,
    )
}
