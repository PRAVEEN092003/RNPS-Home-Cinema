package com.cineplex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CinemaDarkColorScheme = darkColorScheme(
    // Primary — cinematic gold
    primary = CinemaGold,
    onPrimary = TextOnGold,
    primaryContainer = CinemaGoldDark,
    onPrimaryContainer = CinemaGoldLight,

    // Background
    background = BackgroundDeep,
    onBackground = TextPrimary,

    // Surface
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundElevated,
    onSurfaceVariant = TextSecondary,
    surfaceTint = CinemaGold,

    // Error
    error = ErrorRed,
    onError = TextPrimary,

    // Outline
    outline = Divider,
    outlineVariant = BackgroundSurface,

    // Inverse
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDeep,
    inversePrimary = CinemaGoldDark,

    // Scrim
    scrim = BackgroundDeep.copy(alpha = 0.8f),
)

@Composable
fun CinemaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CinemaDarkColorScheme,
        typography = CinemaTypography,
        content = content,
    )
}
