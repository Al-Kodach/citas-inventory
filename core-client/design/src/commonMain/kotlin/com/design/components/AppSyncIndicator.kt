package com.design.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppSyncIndicator(
    isSyncing: Boolean,
    hasError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val (icon, color, rotate) = when {
        hasError -> Triple(Icons.Default.SyncProblem, Color.Red, false)
        isSyncing -> Triple(Icons.Default.Sync, Color(0xFF4CAF50), true)
        else -> Triple(Icons.Default.Sync, Color.White.copy(alpha = 0.5f), false)
    }

    Icon(
        imageVector = icon,
        contentDescription = "Sync Status",
        tint = color,
        modifier = modifier
            .size(20.dp)
            .then(if (rotate) Modifier.rotate(rotation) else Modifier)
    )
}
