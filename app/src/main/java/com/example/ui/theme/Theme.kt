package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val title: String, val subtitle: String) {
    LIGHT("Light", "Clean high-contrast daylight"),
    DARK("Dark", "Deep obsidian night"),
    GREY("Slate Grey", "Industrial matte steel POS"),
    SYSTEM("Auto System", "Match device preference")
}

// 1. High-Contrast Crisp Light Scheme
private val LightColorScheme = lightColorScheme(
    primary = PosLightPrimary,
    onPrimary = PosLightOnPrimary,
    primaryContainer = PosLightPrimaryContainer,
    onPrimaryContainer = PosLightOnPrimaryContainer,
    secondary = PosLightSecondary,
    onSecondary = PosLightOnSecondary,
    secondaryContainer = PosLightSecondaryContainer,
    onSecondaryContainer = PosLightOnSecondaryContainer,
    tertiary = PosLightTertiary,
    onTertiary = PosLightOnTertiary,
    tertiaryContainer = PosLightTertiaryContainer,
    onTertiaryContainer = PosLightOnTertiaryContainer,
    background = PosLightBackground,
    onBackground = PosLightOnBackground,
    surface = PosLightSurface,
    onSurface = PosLightOnSurface,
    surfaceVariant = PosLightSurfaceVariant,
    onSurfaceVariant = PosLightOnSurfaceVariant,
    outline = PosLightOutline,
    outlineVariant = PosLightOutlineVariant,
    error = PosError,
    errorContainer = PosErrorContainer
)

// 2. Obsidian Deep Dark Scheme
private val DarkColorScheme = darkColorScheme(
    primary = PosDarkPrimary,
    onPrimary = PosDarkOnPrimary,
    primaryContainer = PosDarkPrimaryContainer,
    onPrimaryContainer = PosDarkOnPrimaryContainer,
    secondary = PosDarkSecondary,
    onSecondary = PosDarkOnSecondary,
    secondaryContainer = PosDarkSecondaryContainer,
    onSecondaryContainer = PosDarkOnSecondaryContainer,
    tertiary = PosDarkTertiary,
    onTertiary = PosDarkOnTertiary,
    tertiaryContainer = PosDarkTertiaryContainer,
    onTertiaryContainer = PosDarkOnTertiaryContainer,
    background = PosDarkBackground,
    onBackground = PosDarkOnBackground,
    surface = PosDarkSurface,
    onSurface = PosDarkOnSurface,
    surfaceVariant = PosDarkSurfaceVariant,
    onSurfaceVariant = PosDarkOnSurfaceVariant,
    outline = PosDarkOutline,
    outlineVariant = PosDarkOutlineVariant,
    error = PosError,
    errorContainer = PosErrorContainer
)

// 3. Industrial Slate Grey Scheme
private val GreyColorScheme = darkColorScheme(
    primary = PosGreyPrimary,
    onPrimary = PosGreyOnPrimary,
    primaryContainer = PosGreyPrimaryContainer,
    onPrimaryContainer = PosGreyOnPrimaryContainer,
    secondary = PosGreySecondary,
    onSecondary = PosGreyOnSecondary,
    secondaryContainer = PosGreySecondaryContainer,
    onSecondaryContainer = PosGreyOnSecondaryContainer,
    tertiary = PosGreyTertiary,
    onTertiary = PosGreyOnTertiary,
    tertiaryContainer = PosGreyTertiaryContainer,
    onTertiaryContainer = PosGreyOnTertiaryContainer,
    background = PosGreyBackground,
    onBackground = PosGreyOnBackground,
    surface = PosGreySurface,
    onSurface = PosGreyOnSurface,
    surfaceVariant = PosGreySurfaceVariant,
    onSurfaceVariant = PosGreyOnSurfaceVariant,
    outline = PosGreyOutline,
    outlineVariant = PosGreyOutlineVariant,
    error = PosError,
    errorContainer = PosErrorContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

@Composable
fun RetailPosTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.GREY -> GreyColorScheme
        AppThemeMode.SYSTEM -> if (systemDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
