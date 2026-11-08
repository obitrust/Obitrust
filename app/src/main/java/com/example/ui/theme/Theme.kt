package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = NavyDark,
    primaryContainer = GoldContainer,
    onPrimaryContainer = OnGoldContainer,
    secondary = EmeraldProfit,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = GoldLight,
    onTertiary = NavyDark,
    background = NavyDark,
    onBackground = Color(0xFFF1F5F9),
    surface = NavySurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NavyCard,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = NavyBorder,
    error = CrimsonExpense,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = GoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = EmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = SlateLightBg,
    onBackground = SlateTextPrimary,
    surface = SlateLightSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateLightCard,
    onSurfaceVariant = SlateTextSecondary,
    outline = Color(0xFFE2E8F0),
    error = CrimsonExpense,
    onError = Color.White
)

@Composable
fun ObiTrustTheme(
    darkTheme: Boolean = true, // Default to premium navy dark for business prestige
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
