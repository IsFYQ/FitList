package com.example.healthcheckin.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.ui.theme.HealthCheckInRadius

@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 60.dp,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    val base: Color = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(HealthCheckInRadius.Card))
            .background(base.copy(alpha = alpha)),
    )
}

@Composable
fun DashboardSkeletonContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(HealthCheckInDimens.PagePadding),
        verticalArrangement = Arrangement.spacedBy(HealthCheckInDimens.Space4),
    ) {
        SkeletonBlock(height = 140.dp)
        SkeletonBlock(height = 72.dp)
        SkeletonBlock(height = 72.dp)
        SkeletonBlock(height = 96.dp)
    }
}
