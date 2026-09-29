package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = QuoteForgeDarkPrimary,
    onPrimary = Color(0xFF00382E),
    primaryContainer = QuoteForgeDarkPrimaryContainer,
    onPrimaryContainer = Color(0xFF99F0DC),
    secondary = QuoteForgeDarkSecondary,
    onSecondary = Color(0xFF1D352F),
    secondaryContainer = Color(0xFF344B45),
    onSecondaryContainer = Color(0xFFCDE8DF),
    tertiary = QuoteForgeDarkTertiary,
    onTertiary = Color(0xFF482900),
    tertiaryContainer = Color(0xFF653F05),
    onTertiaryContainer = Color(0xFFFFDDB6),
    background = QuoteForgeDarkBackground,
    onBackground = QuoteForgeDarkTextPrimary,
    surface = QuoteForgeDarkSurface,
    onSurface = QuoteForgeDarkTextPrimary,
    surfaceVariant = QuoteForgeDarkSurfaceVariant,
    onSurfaceVariant = QuoteForgeDarkTextSecondary,
    outline = QuoteForgeDarkOutline,
    outlineVariant = Color(0xFF2C3935),
    error = RoseError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = QuoteForgePrimaryTeal,
    onPrimary = Color.White,
    primaryContainer = QuoteForgePrimaryContainer,
    onPrimaryContainer = QuoteForgeOnPrimaryContainer,
    secondary = QuoteForgeSecondaryMuted,
    onSecondary = Color.White,
    secondaryContainer = QuoteForgeSecondaryContainer,
    onSecondaryContainer = QuoteForgeOnSecondaryContainer,
    tertiary = QuoteForgeTertiaryGold,
    onTertiary = Color(0xFF3C2300),
    tertiaryContainer = QuoteForgeTertiaryContainer,
    onTertiaryContainer = QuoteForgeOnTertiaryContainer,
    background = QuoteForgeNeutralCanvas,
    onBackground = QuoteForgeOnSurfaceCharcoal,
    surface = QuoteForgeSurfaceLight,
    onSurface = QuoteForgeOnSurfaceCharcoal,
    surfaceVariant = Color(0xFFE5EDE9),
    onSurfaceVariant = QuoteForgeOnSurfaceVariant,
    outline = QuoteForgeOutlineLight,
    outlineVariant = QuoteForgeOutlineVariantLight,
    error = RoseError,
    onError = Color.White
)

@Composable
fun QuoteForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our crisp architectural brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
