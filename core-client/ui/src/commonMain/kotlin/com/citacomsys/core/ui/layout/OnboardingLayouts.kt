package com.citacomsys.core.ui.layout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.ResponsiveScreenWrapper
import com.design.components.AppScaffold

@Composable
fun SectionWrapper(
    modifier: Modifier = Modifier,
    title: String,
    onBack: () -> Unit,
    maxContentWidth: Int = 850,
    bottomBar: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable () -> Unit
) {
    AppScaffold(
        title = title,
        onBack = onBack,
        actions = actions,
        bottomBar = bottomBar,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        ResponsiveScreenWrapper(
            maxContentWidth = maxContentWidth.dp,
            modifier = Modifier
                .padding(padding)
                .animateContentSize(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp
            ) {
                Column(Modifier.padding(24.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun ResourceSpecItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
