package com.example.mmarecomp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.mmarecomp.util.TrendPoint

/** N'affiche QUE la moyenne mobile 7 jours — jamais le point brut du jour —
 *  conformément au principe UX non négociable : ne jamais montrer un poids
 *  brut comme donnée principale sans le contextualiser par la tendance. */
@Composable
fun WeightTrendChart(points: List<TrendPoint>, modifier: Modifier = Modifier, lineColor: Color = MaterialTheme.colorScheme.primary) {
    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "Pas encore de pesée — ajoute une pesée du matin pour voir la tendance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val values = points.map { it.value }
    val minV = values.min()
    val maxV = values.max()
    val range = (maxV - minV).let { if (it < 0.01) 1.0 else it }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stepX = if (points.size > 1) w / (points.size - 1) else 0f

        fun yFor(value: Double): Float = h - ((value - minV) / range * h).toFloat()

        val linePath = Path()
        val areaPath = Path()
        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = yFor(point.value)
            if (index == 0) {
                linePath.moveTo(x, y)
                areaPath.moveTo(x, h)
                areaPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                areaPath.lineTo(x, y)
            }
        }
        areaPath.lineTo((points.size - 1) * stepX, h)
        areaPath.close()

        drawPath(areaPath, color = lineColor.copy(alpha = 0.10f), style = Fill)
        drawPath(linePath, color = lineColor, style = Stroke(width = 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}
