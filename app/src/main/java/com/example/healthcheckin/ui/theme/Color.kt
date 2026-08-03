package com.example.healthcheckin.ui.theme

import androidx.compose.ui.graphics.Color

/** Brand & semantic color tokens — light/dark pairs for the mint health theme. */
object HealthCheckInColors {
    // Brand
    val Primary = Color(0xFF36B37E)
    val PrimaryDark = Color(0xFF4CCD90)
    val Secondary = Color(0xFF65B8E0)
    val SecondaryDark = Color(0xFF7EC8EA)
    val Tertiary = Color(0xFFFFB770)
    val TertiaryDark = Color(0xFFFFC48A)

    // Status (calorie / feedback)
    val Success = Color(0xFF36B37E)
    val SuccessDark = Color(0xFF4CCD90)
    val Warning = Color(0xFFFFC145)
    val WarningDark = Color(0xFFFFD06A)
    val Error = Color(0xFFF26260)
    val ErrorDark = Color(0xFFF57B79)

    // Neutrals — light
    val BackgroundLight = Color(0xFFF7FBF9)
    val OnSurfaceLight = Color(0xFF333836)
    val OnSurfaceVariantLight = Color(0xFF666D6B)
    val SurfaceVariantLight = Color(0xFFE8F2ED)
    val OutlineLight = Color(0xFFB7C4BE)
    val OutlineVariantLight = Color(0xFFD5E0DB)

    // Neutrals — dark
    val BackgroundDark = Color(0xFF1A2320)
    val OnSurfaceDark = Color(0xFFD4DDDA)
    val OnSurfaceVariantDark = Color(0xFFA3ADA9)
    val SurfaceVariantDark = Color(0xFF2A3531)
    val OutlineDark = Color(0xFF5A6863)
    val OutlineVariantDark = Color(0xFF3D4A45)

    // Containers
    val PrimaryContainerLight = Color(0xFFD4F5E6)
    val PrimaryContainerDark = Color(0xFF1E4A38)
    val SecondaryContainerLight = Color(0xFFD6EFF8)
    val SecondaryContainerDark = Color(0xFF1E3A48)
    val TertiaryContainerLight = Color(0xFFFFE8D0)
    val TertiaryContainerDark = Color(0xFF4A3520)

    // Legacy aliases used by calorie status helpers
    val CalorieNormal get() = Success
    val CalorieWarn get() = Warning
    val CalorieOver get() = Error
}

data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val tertiaryAccent: Color,
)

val LightExtendedColors = ExtendedColors(
    success = HealthCheckInColors.Success,
    onSuccess = Color.White,
    warning = HealthCheckInColors.Warning,
    onWarning = HealthCheckInColors.OnSurfaceLight,
    tertiaryAccent = HealthCheckInColors.Tertiary,
)

val DarkExtendedColors = ExtendedColors(
    success = HealthCheckInColors.SuccessDark,
    onSuccess = HealthCheckInColors.BackgroundDark,
    warning = HealthCheckInColors.WarningDark,
    onWarning = HealthCheckInColors.BackgroundDark,
    tertiaryAccent = HealthCheckInColors.TertiaryDark,
)
