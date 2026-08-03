package com.example.healthcheckin.ui.screens.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.healthcheckin.domain.model.WeightRecordItem

@Composable
fun WeightLineChart(
    records: List<WeightRecordItem>,
    targetWeightKg: Double?,
    modifier: Modifier = Modifier,
) {
    if (records.size < 2) return

    val weights = records.map { it.weightKg }
    val minWeight = minOf(weights.minOrNull()!!, targetWeightKg ?: weights.minOrNull()!!) - 1.0
    val maxWeight = maxOf(weights.maxOrNull()!!, targetWeightKg ?: weights.maxOrNull()!!) + 1.0
    val weightRange = (maxWeight - minWeight).coerceAtLeast(0.1)

    // Chart lines use Secondary (sky blue) per design system.
    val lineColor = MaterialTheme.colorScheme.secondary
    val targetColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        val stepX = size.width / (records.size - 1).coerceAtLeast(1)

        fun yFor(weight: Double): Float {
            val ratio = ((weight - minWeight) / weightRange).toFloat()
            return size.height - (ratio * size.height)
        }

        // Subtle horizontal guides
        for (i in 1..3) {
            val y = size.height * i / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }

        targetWeightKg?.let { target ->
            val targetY = yFor(target)
            drawLine(
                color = targetColor,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round,
            )
        }

        val path = Path()
        records.forEachIndexed { index, record ->
            val x = stepX * index
            val y = yFor(record.weightKg)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )

        records.forEachIndexed { index, record ->
            drawCircle(
                color = lineColor,
                radius = 8f,
                center = Offset(stepX * index, yFor(record.weightKg)),
            )
        }
    }
}
