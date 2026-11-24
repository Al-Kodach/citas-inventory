package com.citacomsys.inventory.admin.security.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.citacomsys.feature.api.telemetry.TraceTimelineEntry
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.formatRelativeTime

// Location: adminApp/shared/.../security/components/SecurityEventRow.kt
@Composable
fun SecurityEventRow(
    entry: TraceTimelineEntry,
    onClick: () -> Unit = {}
) {
    val (icon, color) = when (entry.logSource) {
        "NETWORK" -> AppIcons.ShieldOutlined to MaterialTheme.colorScheme.error
        "IDENTITY" -> AppIcons.Person to MaterialTheme.colorScheme.primary
        "ENDPOINT" -> AppIcons.Smartphone to MaterialTheme.colorScheme.tertiary
        "APPLICATION" -> AppIcons.Layers to MaterialTheme.colorScheme.secondary
        else -> AppIcons.Info to MaterialTheme.colorScheme.outline
    }

    val levelColor = when (entry.level) {
        "CRITICAL" -> MaterialTheme.colorScheme.error
        "ERROR" -> MaterialTheme.colorScheme.error
        "WARNING" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(18.dp), tint = color)
                }
            }
        },
        headlineContent = {
            Text(
                text = entry.actionOrComponent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (entry.deviceId != null) {
                    Text(
                        text = "Device: ${entry.deviceId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.createdAt.formatRelativeTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (entry.level != "INFO") {
                    Text(
                        text = entry.level,
                        style = MaterialTheme.typography.labelSmall,
                        color = levelColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    )
}