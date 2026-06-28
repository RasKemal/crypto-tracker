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

private val MidasDarkColorScheme = darkColorScheme(
    primary = MidasOnBackground,
    onPrimary = MidasBlack,
    primaryContainer = MidasSurface,
    onPrimaryContainer = MidasOnBackground,
    secondary = MidasSecondaryText,
    onSecondary = MidasBlack,
    secondaryContainer = MidasSurfaceVariant,
    onSecondaryContainer = MidasOnBackground,
    tertiary = MidasTertiaryText,
    onTertiary = MidasBlack,
    background = MidasBlack,
    onBackground = MidasOnBackground,
    surface = MidasSurface,
    onSurface = MidasOnBackground,
    surfaceVariant = MidasSurfaceVariant,
    onSurfaceVariant = MidasSecondaryText,
    outline = MidasDivider,
    outlineVariant = MidasSurfaceElevated,
    inverseSurface = MidasOnBackground,
    inverseOnSurface = MidasBlack,
    scrim = Color(0x99000000),
)

private val MidasLightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = MidasLightSurface,
    onPrimaryContainer = Color(0xFF000000),
    secondary = MidasLightSecondaryText,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = MidasLightSurfaceVariant,
    onSecondaryContainer = Color(0xFF000000),
    tertiary = MidasLightSecondaryText,
    onTertiary = Color(0xFFFFFFFF),
    background = MidasLightBackground,
    onBackground = Color(0xFF000000),
    surface = MidasLightSurface,
    onSurface = Color(0xFF000000),
    surfaceVariant = MidasLightSurfaceVariant,
    onSurfaceVariant = MidasLightSecondaryText,
    outline = MidasLightDivider,
    outlineVariant = MidasLightSurfaceVariant,
    inverseSurface = MidasBlack,
    inverseOnSurface = MidasOnBackground,
    scrim = Color(0x66000000),
)

@Composable
fun StockTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MidasDarkColorScheme else MidasLightColorScheme

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
