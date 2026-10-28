package com.cineplex.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─── Primary Brand Palette — RNPS Home Cinema Black & Gold ────────────────────
val DeepBlack       = Color(0xFF080808)   // Main background
val SecondaryBlack  = Color(0xFF141414)   // Secondary background
val ElevatedCard    = Color(0xFF202020)   // Elevated card surface
val CardBorderGold  = Color(0xFF514331)   // Subtle card borders

// ─── Gold Accent Family ────────────────────────────────────────────────────────
val CinemaGold      = Color(0xFFD6B15E)   // Primary gold accent
val CinemaGoldLight = Color(0xFFFFE3A0)   // Bright gold highlight
val CinemaGoldDark  = Color(0xFF94702F)   // Deep / muted gold

// ─── Legacy aliases so existing screens compile without modification ───────────
val MidnightDarkBlue   = Color(0xFF080808)   // Maps to DeepBlack
val ElectricCinemaBlue = Color(0xFFD6B15E)   // Maps to CinemaGold
val ElectricBlueLight  = Color(0xFFFFE3A0)   // Maps to CinemaGoldLight
val ElectricBlueGlow   = Color(0xFFD6B15E)   // Maps to CinemaGold

// ─── Cinematic Red ────────────────────────────────────────────────────────────
val CinematicRed       = Color(0xFFD91E2B)   // Main cinematic red
val CinematicRedHover  = Color(0xFFB01520)
val CinematicRedSubtle = Color(0x33D91E2B)

// ─── Background & Surfaces ────────────────────────────────────────────────────
val BackgroundDeep      = Color(0xFF080808)   // Main background
val BackgroundSecondary = Color(0xFF141414)   // Secondary background
val BackgroundCard      = Color(0xFF202020)   // Elevated cards
val BackgroundCardDark  = Color(0xFF141414)   // Dark secondary
val BackgroundElevated  = Color(0xFF202020)   // Elevated surface (matches card)
val BackgroundSurface   = Color(0xFF202020)   // Surface = card
val BackgroundGlass     = Color(0xD9141414)   // Frosted glass
val BackgroundGlassHighlight = Color(0x1FD6B15E) // Gold-tinted glass highlight

// ─── Seat Status Colors ────────────────────────────────────────────────────────
val SeatAvailable = Color(0xFF262626)    // Deep slate charcoal — available
val SeatSelected  = Color(0xFFD6B15E)   // Vibrant Cinema Gold — selected
val SeatBooked    = Color(0xFF141414)   // Muted dark charcoal — booked
val SeatVIP       = Color(0xFF332A1D)   // Deep gold tinted obsidian — VIP

// ─── Typography Colors ─────────────────────────────────────────────────────────
val TextPrimary    = Color(0xFFFFFFFF)   // Pure white
val TextSecondary  = Color(0xFFD0C8BA)  // Warm silver
val TextSupporting = Color(0xFFA9A29A)  // Muted text
val TextDisabled   = Color(0xFF6B6460)  // Disabled text
val TextOnGold     = Color(0xFF080808)  // Dark text on gold backgrounds
val TextOnRed      = Color(0xFFFFFFFF)
val TextOnBlue     = Color(0xFFFFFFFF)

// ─── Semantic State Colors ─────────────────────────────────────────────────────
val ErrorRed     = Color(0xFFFF5A5F)   // Error
val SuccessGreen = Color(0xFF42C982)   // Success
val WarningAmber = Color(0xFFFFB547)   // Warning
val InfoBlue     = Color(0xFFD6B15E)   // Info — mapped to gold

// ─── Dividers, Borders & Effects ──────────────────────────────────────────────
val Divider         = Color(0xFF2A2520)   // Subtle divider
val BorderSubtle    = Color(0xFF514331)   // Card borders — gold-tinted
val BorderHighlight = Color(0xFFD6B15E)  // Gold highlight border
val BorderRedSubtle = Color(0x4DD91E2B)
val IconTint        = Color(0xFFD0C8BA)  // Warm silver icon
val IconAccent      = Color(0xFFD6B15E)  // Gold icon accent
val RippleColor     = Color(0xFFD6B15E).copy(alpha = 0.15f)

// ─── Cinematic Gradients ──────────────────────────────────────────────────────
val MidnightBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF080808),
        Color(0xFF141414),
        Color(0xFF080808)
    )
)

val CinematicCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF252018),
        Color(0xFF202020)
    )
)

val TicketPerforationGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF202020),
        Color(0xFF141414)
    )
)

val RedActionGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFD91E2B),
        Color(0xFFB01520)
    )
)

val GoldActionGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFFD6B15E),
        Color(0xFF94702F)
    )
)

// Legacy alias so existing code referencing BlueHighlightGradient keeps compiling
val BlueHighlightGradient = GoldActionGradient
