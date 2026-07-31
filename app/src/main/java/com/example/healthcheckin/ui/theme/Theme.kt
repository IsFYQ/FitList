package com.example.healthcheckin.ui.theme

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
import com.example.healthcheckin.domain.model.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = HealthCheckInColors.CalorieNormal,
    secondary = HealthCheckInColors.CalorieWarn,
    error = HealthCheckInColors.CalorieOver,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFFFFB74D),
    error = Color(0xFFEF5350),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
)

object HealthCheckInColors {
    val CalorieNormal = Color(0xFF4CAF50)
    val CalorieWarn = Color(0xFFFF9800)
    val CalorieOver = Color(0xFFF44336)
}

@Composable
fun HealthCheckInTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
