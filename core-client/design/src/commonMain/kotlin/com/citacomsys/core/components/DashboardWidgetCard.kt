package com.citacomsys.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.citacomsys.core.spi.DashboardPlacement

/**
 * CompositionLocal to control whether [DashboardWidgetCard] should apply horizontal padding.
 */
val LocalApplyDashboardPadding = compositionLocalOf { true }

/**
 * CompositionLocal to detect if we are already inside a [DashboardWidgetCard].
 */
val LocalIsInsideDashboardCard = compositionLocalOf { false }

/**
 * Breakpoint for "Large Screen" dashboard layout (Extra Large).
 */
const val DASHBOARD_LARGE_SCREEN_WIDTH = 1200

/**
 * A standard card for dashboard widgets with dynamic surface colors, adaptive shapes, uniform height constraints based on placement, and padding.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DashboardWidgetCard(
    modifier: Modifier = Modifier,
    placement: DashboardPlacement? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    ),
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalIsInsideDashboardCard.current) {
        Column(
            modifier = modifier.fillMaxWidth(),
            content = content
        )
        return
    }

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val minWidth = windowSizeClass.minWidthDp

    val shape = if (minWidth < DASHBOARD_LARGE_SCREEN_WIDTH) {
        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(16.dp)
    }

    val placementModifier = when (placement) {
        DashboardPlacement.ANALYTICS_CAROUSEL -> Modifier.height(220.dp)
        DashboardPlacement.HERO -> Modifier.height(240.dp)
        DashboardPlacement.GRID -> Modifier.height(280.dp)
        DashboardPlacement.LIST -> Modifier.height(360.dp)
        else -> Modifier
    }

    CompositionLocalProvider(LocalIsInsideDashboardCard provides true) {
        Card(
            modifier = modifier
                .then(placementModifier)
                .fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
    }
}
