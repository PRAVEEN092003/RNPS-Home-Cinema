package com.cineplex.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CinemaDarkColorScheme = darkColorScheme(
    // Primary — Cinema Gold
    primary = CinemaGold,
    onPrimary = TextOnGold,
    primaryContainer = BackgroundElevated,
    onPrimaryContainer = CinemaGoldLight,

    // Secondary — Gold highlight
    secondary = CinemaGoldLight,
    onSecondary = TextOnGold,
    secondaryContainer = BackgroundElevated,
    onSecondaryContainer = CinemaGold,

    // Background — Near-black #080808
    background = BackgroundDeep,
    onBackground = TextPrimary,

    // Surface — Elevated Cards #202020
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundElevated,
    onSurfaceVariant = TextSecondary,
    surfaceTint = CinemaGold,

    // Error
    error = ErrorRed,
    onError = TextPrimary,

    // Outline
    outline = BorderSubtle,
    outlineVariant = Divider,

    // Inverse
    inverseSurface = TextPrimary,
    inverseOnSurface = BackgroundDeep,
    inversePrimary = CinemaGoldLight,

    // Scrim
    scrim = BackgroundDeep.copy(alpha = 0.85f),
)

@Composable
fun CinemaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CinemaDarkColorScheme,
        typography = CinemaTypography,
        content = content,
    )
}
