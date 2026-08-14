package com.example.healthcheckin.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

/** Material 3 / Apple-inspired motion tokens. Prefer fade + slight slide over hard cuts. */
object HealthCheckInMotion {
    val EaseStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EaseDecelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    const val DurationShort = 180
    const val DurationMedium = 280
    const val DurationLong = 360

    fun <T> standard() = tween<T>(durationMillis = DurationMedium, easing = EaseStandard)
    fun <T> fade() = tween<T>(durationMillis = DurationShort, easing = EaseDecelerate)
}
