package com.citacomsys.core.modifiers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Higher-order modifier that applies a consistent glassmorphism effect.
 */
@Composable
fun Modifier.glassEffect(
    shape: Shape = MaterialTheme.shapes.large,
    opacity: Float = 0.15f,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(
        MaterialTheme.colorScheme.surface.copy(alpha = opacity)
    )
    .border(
        borderWidth,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        shape
    )


@Composable
fun Modifier.glassSurface(
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    borderAlpha: Float = 0.1f,
    surfaceAlpha: Float = 0.15f,
    borderWidth: Dp = 0.dp,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    elevation: Dp = 2.dp
): Modifier =
    this
        .clip(shape)
        .background(
            MaterialTheme.colorScheme.surface.copy(
                alpha = surfaceAlpha
            )
        )
        .border(
            borderWidth,
            borderColor.copy(alpha = borderAlpha),
            shape
        )
        .shadow(
            elevation = elevation,
            shape = shape
        )
