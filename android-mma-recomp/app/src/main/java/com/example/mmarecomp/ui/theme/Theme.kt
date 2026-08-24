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

private fun lightColors(accent: AccentPreset, highContrast: Boolean) = lightColorScheme(
    primary = accentColors(accent).first,
    secondary = Clay,
    tertiary = Moss,
    background = if (highContrast) Color.White else PaperLight,
    surface = if (highContrast) Color.White else SurfaceLight,
    onBackground = if (highContrast) Color.Black else InkLight,
    onSurface = if (highContrast) Color.Black else InkLight,
    surfaceVariant = TrackLight,
    onSurfaceVariant = if (highContrast) Color.Black else TextSecondaryLight,
)

private fun darkColors(accent: AccentPreset, highContrast: Boolean) = darkColorScheme(
    primary = accentColors(accent).second,
    secondary = ClayDark,
    tertiary = MossDark,
    background = if (highContrast) Color.Black else PaperDark,
    surface = if (highContrast) Color.Black else SurfaceDark,
    onBackground = if (highContrast) Color.White else InkDark,
    onSurface = if (highContrast) Color.White else InkDark,
    surfaceVariant = TrackDark,
    onSurfaceVariant = if (highContrast) Color.White else TextSecondaryDark,
)

@Composable
fun MMARecompTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentPreset = AccentPreset.STEEL,
    textScale: Float = 1f,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors(accent, highContrast) else lightColors(accent, highContrast),
        typography = mmaTypography(textScale),
        content = content,
    )
}
