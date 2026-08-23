package com.example.foodtracker.ui.theme

import androidx.compose.ui.graphics.Color

val FreshBlue = Color(0xFF3B71F3)
val ExpiryRed = Color(0xFFE74C3C)
val WarningYellow = Color(0xFFF1C40F)

// Dark, high-contrast foreground for text/icons drawn on top of WarningYellow (or a tint of
// it). Solid WarningYellow is a light, high-luminance yellow, so a dark foreground is required
// to clear WCAG's 4.5:1 body-text contrast minimum - white text on it (the old behavior) sat
// around 1.6:1.
val WarningYellowDark = Color(0xFF7A5900)
val GoodGreen = Color(0xFF2ECC71)
val BackgroundGray = Color(0xFFF9FAFB)

// Dark theme neutrals used by Theme.kt's DarkColorScheme. Kept as named constants here,
// matching the existing pattern for this file, instead of inlining hex codes in Theme.kt.
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurfaceVariant = Color(0xFFB0B0B0)
val DarkOutline = Color(0xFF6E6E6E)
