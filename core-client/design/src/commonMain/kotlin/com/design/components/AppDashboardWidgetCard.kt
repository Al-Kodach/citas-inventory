package com.design.components

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

val LocalApplyDashboardPadding = compositionLocalOf { true }
val LocalIsInsideDashboardCard = compositionLocalOf { false }
const val DASHBOARD_LARGE_SCREEN_WIDTH = 1200

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppDashboardWidgetCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
    val shouldApplyPadding = LocalApplyDashboardPadding.current

    val shape = if (minWidth < DASHBOARD_LARGE_SCREEN_WIDTH) {
        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(16.dp)
    }

    val paddingValues =
        if (shouldApplyPadding && minWidth >= DASHBOARD_LARGE_SCREEN_WIDTH) 32.dp else 0.dp

    CompositionLocalProvider(LocalIsInsideDashboardCard provides true) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues),
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
