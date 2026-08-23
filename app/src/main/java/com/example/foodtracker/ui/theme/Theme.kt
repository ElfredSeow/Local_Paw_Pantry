package com.example.foodtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Values chosen to exactly match what InventoryScreen/UpcomingScreen/FoodItemCard used to
// hardcode directly (Color.White / Color.Black / Color.Gray / Color.LightGray), so light mode
// is pixel-identical to before now that those screens read colors from the theme instead.
private val LightColorScheme = lightColorScheme(
    primary = FreshBlue,
    secondary = FreshBlue,
    tertiary = FreshBlue,
    background = BackgroundGray,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Gray,
    outline = Color.LightGray,
)

private val DarkColorScheme = darkColorScheme(
    primary = FreshBlue,
    secondary = FreshBlue,
    tertiary = FreshBlue,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
)

@Composable
fun FoodTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
