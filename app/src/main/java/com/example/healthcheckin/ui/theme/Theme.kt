package com.example.healthcheckin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.healthcheckin.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = HealthCheckInColors.Primary,
    onPrimary = Color.White,
    primaryContainer = HealthCheckInColors.PrimaryContainerLight,
    onPrimaryContainer = HealthCheckInColors.OnSurfaceLight,
    secondary = HealthCheckInColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = HealthCheckInColors.SecondaryContainerLight,
    onSecondaryContainer = HealthCheckInColors.OnSurfaceLight,
    tertiary = HealthCheckInColors.Tertiary,
    onTertiary = HealthCheckInColors.OnSurfaceLight,
    tertiaryContainer = HealthCheckInColors.TertiaryContainerLight,
    onTertiaryContainer = HealthCheckInColors.OnSurfaceLight,
    error = HealthCheckInColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = HealthCheckInColors.BackgroundLight,
    onBackground = HealthCheckInColors.OnSurfaceLight,
    surface = HealthCheckInColors.BackgroundLight,
    onSurface = HealthCheckInColors.OnSurfaceLight,
    surfaceVariant = HealthCheckInColors.SurfaceVariantLight,
    onSurfaceVariant = HealthCheckInColors.OnSurfaceVariantLight,
    outline = HealthCheckInColors.OutlineLight,
    outlineVariant = HealthCheckInColors.OutlineVariantLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = HealthCheckInColors.PrimaryDark,
    onPrimary = HealthCheckInColors.BackgroundDark,
    primaryContainer = HealthCheckInColors.PrimaryContainerDark,
    onPrimaryContainer = HealthCheckInColors.OnSurfaceDark,
    secondary = HealthCheckInColors.SecondaryDark,
    onSecondary = HealthCheckInColors.BackgroundDark,
    secondaryContainer = HealthCheckInColors.SecondaryContainerDark,
    onSecondaryContainer = HealthCheckInColors.OnSurfaceDark,
    tertiary = HealthCheckInColors.TertiaryDark,
    onTertiary = HealthCheckInColors.BackgroundDark,
    tertiaryContainer = HealthCheckInColors.TertiaryContainerDark,
    onTertiaryContainer = HealthCheckInColors.OnSurfaceDark,
    error = HealthCheckInColors.ErrorDark,
    onError = HealthCheckInColors.BackgroundDark,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = HealthCheckInColors.BackgroundDark,
    onBackground = HealthCheckInColors.OnSurfaceDark,
    surface = HealthCheckInColors.BackgroundDark,
    onSurface = HealthCheckInColors.OnSurfaceDark,
    surfaceVariant = HealthCheckInColors.SurfaceVariantDark,
    onSurfaceVariant = HealthCheckInColors.OnSurfaceVariantDark,
    outline = HealthCheckInColors.OutlineDark,
    outlineVariant = HealthCheckInColors.OutlineVariantDark,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object HealthCheckInThemeExtras {
    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}

@Composable
fun HealthCheckInTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Brand palette is locked; Dynamic Color is intentionally disabled.
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val extended = if (useDarkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HealthCheckInTypography,
            shapes = HealthCheckInShapes,
            content = content,
        )
    }
}
