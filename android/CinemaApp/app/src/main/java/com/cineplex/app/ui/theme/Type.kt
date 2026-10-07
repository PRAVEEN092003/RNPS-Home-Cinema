package com.cineplex.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using system default sans-serif for reliability without bundled fonts.
// Replace with downloaded Inter/Outfit font files for production.
val CinemaFontFamily = FontFamily.Default

val CinemaTypography = Typography(
    // Display — movie titles on home screen
    displayLarge = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = TextPrimary,
    ),
    displayMedium = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        color = TextPrimary,
    ),

    // Headlines
    headlineLarge = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        color = TextPrimary,
    ),
    headlineMedium = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = TextPrimary,
    ),
    headlineSmall = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary,
    ),

    // Title — section labels, card titles
    titleLarge = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        color = TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary,
    ),
    titleSmall = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = TextSecondary,
    ),

    // Body
    bodyLarge = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary,
    ),
    bodySmall = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = TextDisabled,
    ),

    // Label
    labelLarge = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
        color = TextPrimary,
    ),
    labelMedium = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = TextSecondary,
    ),
    labelSmall = TextStyle(
        fontFamily = CinemaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        color = TextDisabled,
    ),
)
