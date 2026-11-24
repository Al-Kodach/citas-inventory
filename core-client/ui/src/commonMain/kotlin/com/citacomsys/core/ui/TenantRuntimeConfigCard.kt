package com.citacomsys.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.kernel.model.WorkspaceRuntimeConfig
import com.citacomsys.types.ProductModule

@Composable
fun WorkspaceRuntimeConfigCard(
    config: WorkspaceRuntimeConfig,
    modifier: Modifier = Modifier
) {
    val res = config.resource
    val usage = config.usage

    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tenant Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Package: ${res.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(
                    containerColor = when (config.status.name) {
                        "ACTIVE" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(config.status.name)
                }
            }

            HorizontalDivider(Modifier.alpha(0.1f))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                UsageItem(
                    icon = AppIcons.People,
                    label = "Users",
                    current = usage.userCount,
                    max = res.maxUsers
                )
                UsageItem(
                    icon = AppIcons.Device,
                    label = "Devices",
                    current = usage.deviceCount,
                    max = res.maxDevice
                )
                UsageItem(
                    icon = AppIcons.Place,
                    label = "Branches",
                    current = usage.branchCount,
                    max = res.maxBranches
                )
            }

            HorizontalDivider(Modifier.alpha(0.1f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enabled Modules",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    res.modules.forEach { module ->
                        AssistChip(
                            onClick = {},
                            label = { Text(module.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getModuleIcon(module),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageItem(
    icon: ImageVector,
    label: String,
    current: Int,
    max: Int,
    unit: String = ""
) {
    val progress = if (max > 0) current.toFloat() / max.toFloat() else 0f
    val color = when {
        progress > 0.9f -> MaterialTheme.colorScheme.error
        progress > 0.75f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            // DATA HIGHLIGHT: Use labelLarge and Black weight
            Text(
                text = "$current / $max $unit",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

private fun getModuleIcon(module: ProductModule): ImageVector {
    return when (module.name) {
        "INVENTORY" -> AppIcons.Inventory
        "SALES" -> AppIcons.PointOfSale
        "BILLING" -> AppIcons.Payment
        "REPORTS" -> AppIcons.Analytics
        else -> AppIcons.Layers
    }
}
