package com.example.healthcheckin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val HealthCheckInShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object HealthCheckInRadius {
    val Button = 12.dp
    val Card = 16.dp
    val Sheet = 28.dp
    val Dialog = 28.dp
    val Chip = 10.dp
    val Input = 12.dp
}
