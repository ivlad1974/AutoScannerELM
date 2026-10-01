package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AutomotiveBlue,
    onPrimary = Color.White,
    primaryContainer = SoftBlueContainer,
    onPrimaryContainer = OnSoftBlueText,
    secondary = ClearRed,
    onSecondary = Color.White,
    secondaryContainer = SoftRedContainer,
    onSecondaryContainer = OnSoftRedText,
    tertiary = StatusGreen,
    background = HighDensityBackground,
    onBackground = TextPrimary,
    surface = HighDensitySurface,
    onSurface = TextPrimary,
    surfaceVariant = HighDensitySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = HighDensityBorder,
    error = ClearRed,
    onError = Color.White
)

private val DarkSportColorScheme = darkColorScheme(
    primary = CarbonBlue,
    onPrimary = CarbonBackground,
    primaryContainer = Color(0xFF003865),
    onPrimaryContainer = Color(0xFFC2E8FF),
    secondary = Color(0xFFFF6B6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF491010),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF4ADE80),
    background = CarbonBackground,
    onBackground = CarbonTextPrimary,
    surface = CarbonSurface,
    onSurface = CarbonTextPrimary,
    surfaceVariant = CarbonSurfaceVariant,
    onSurfaceVariant = CarbonTextSecondary,
    outline = CarbonBorder,
    error = Color(0xFFFF6B6B)
)

private val NeonHudColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = NeonBackground,
    primaryContainer = Color(0xFF003B46),
    onPrimaryContainer = NeonCyan,
    secondary = NeonGreen,
    onSecondary = NeonBackground,
    secondaryContainer = Color(0xFF003816),
    onSecondaryContainer = NeonGreen,
    tertiary = NeonGreen,
    background = NeonBackground,
    onBackground = NeonTextPrimary,
    surface = NeonSurface,
    onSurface = NeonTextPrimary,
    surfaceVariant = NeonSurfaceVariant,
    onSurfaceVariant = NeonTextSecondary,
    outline = NeonBorder,
    error = Color(0xFFFF3366)
)

private val OemClassicColorScheme = darkColorScheme(
    primary = OemAccent,
    onPrimary = OemSurface,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFF87171),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF450A0A),
    onSecondaryContainer = Color(0xFFFEE2E2),
    tertiary = Color(0xFF34D399),
    background = OemBackground,
    onBackground = OemTextPrimary,
    surface = OemSurface,
    onSurface = OemTextPrimary,
    surfaceVariant = OemSurfaceVariant,
    onSurfaceVariant = OemTextSecondary,
    outline = OemBorder,
    error = Color(0xFFF87171)
)

@Composable
fun AutoScanTheme(
    appTheme: AppThemeMode = AppThemeMode.DARK_SPORT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppThemeMode.HIGH_CONTRAST_LIGHT -> LightColorScheme
        AppThemeMode.DARK_SPORT -> DarkSportColorScheme
        AppThemeMode.NEON_HUD -> NeonHudColorScheme
        AppThemeMode.OEM_CLASSIC -> OemClassicColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
