package com.example.foodtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = FreshBlue,
    secondary = FreshBlue,
    tertiary = FreshBlue,
    background = BackgroundGray,
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F2F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A2130),
    onSurface = Color(0xFF1A2130),
    // Readable muted tone (~6:1 on white) — replaces the old low-contrast LightGray text (U2)
    onSurfaceVariant = Color(0xFF5A6474),
)

private val DarkColorScheme = darkColorScheme(
    primary = FreshBlue,
    secondary = FreshBlue,
    tertiary = FreshBlue,
    background = Color(0xFF121417),
    surface = Color(0xFF1C2026),
    surfaceVariant = Color(0xFF2A2F37),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE6E8EB),
    onSurface = Color(0xFFE6E8EB),
    onSurfaceVariant = Color(0xFFB4BAC2),
)

@Composable
fun FoodTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // U4: actually honour the system dark/light setting instead of forcing light.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
