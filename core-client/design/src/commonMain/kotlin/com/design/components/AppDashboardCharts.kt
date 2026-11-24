package com.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import com.design.Dimensions

@Composable
fun AppDonutChart(
    values: List<Float>,
    colors: List<Color>,
    label: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val total = values.sum()
            var startAngle = -90f
            val strokeWidth = size.minDimension * 0.15f
            val arcSize = size.minDimension - strokeWidth
            
            if (total == 0f) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(arcSize, arcSize),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                )
            } else {
                values.forEachIndexed { index, value ->
                    val sweepAngle = (value / total) * 360f * animatedProgress.value
                    drawArc(
                        color = colors[index],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(arcSize, arcSize),
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    )
                    startAngle += sweepAngle
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = values.sum().toInt().toString(),
                style = MaterialTheme.typography.titleLarge,
                fontSize = Dimensions.fontSize(22),
                fontWeight = FontWeight.Black
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = Dimensions.fontSize(10),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppRadialGauge(
    value: Float,
    maxValue: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    val targetRatio = if (maxValue > 0) (value / maxValue).coerceIn(0f, 1f) else 0f
    
    LaunchedEffect(value, maxValue) {
        animatedProgress.animateTo(targetRatio, animationSpec = tween(1200))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.12f
            val arcSize = size.minDimension - strokeWidth
            
            drawArc(
                color = color.copy(alpha = 0.1f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
            )
            
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress.value,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(targetRatio * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontSize = Dimensions.fontSize(22),
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = Dimensions.fontSize(10),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
