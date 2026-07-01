package com.example.stocktracker.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CryptoDarkColorScheme = darkColorScheme(
    primary = CryptoOnBackground,
    onPrimary = CryptoBlack,
    primaryContainer = CryptoSurface,
    onPrimaryContainer = CryptoOnBackground,
    secondary = CryptoSecondaryText,
    onSecondary = CryptoBlack,
    secondaryContainer = CryptoSurfaceVariant,
    onSecondaryContainer = CryptoOnBackground,
    tertiary = CryptoTertiaryText,
    onTertiary = CryptoBlack,
    background = CryptoBlack,
    onBackground = CryptoOnBackground,
    surface = CryptoSurface,
    onSurface = CryptoOnBackground,
    surfaceVariant = CryptoSurfaceVariant,
    onSurfaceVariant = CryptoSecondaryText,
    outline = CryptoDivider,
    outlineVariant = CryptoSurfaceElevated,
    inverseSurface = CryptoOnBackground,
    inverseOnSurface = CryptoBlack,
    scrim = Color(0x99000000),
)

private val CryptoLightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CryptoLightSurface,
    onPrimaryContainer = Color(0xFF000000),
    secondary = CryptoLightSecondaryText,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CryptoLightSurfaceVariant,
    onSecondaryContainer = Color(0xFF000000),
    tertiary = CryptoLightSecondaryText,
    onTertiary = Color(0xFFFFFFFF),
    background = CryptoLightBackground,
    onBackground = Color(0xFF000000),
    surface = CryptoLightSurface,
    onSurface = Color(0xFF000000),
    surfaceVariant = CryptoLightSurfaceVariant,
    onSurfaceVariant = CryptoLightSecondaryText,
    outline = CryptoLightDivider,
    outlineVariant = CryptoLightSurfaceVariant,
    inverseSurface = CryptoBlack,
    inverseOnSurface = CryptoOnBackground,
    scrim = Color(0x66000000),
)

@Composable
fun StockTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CryptoDarkColorScheme else CryptoLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
