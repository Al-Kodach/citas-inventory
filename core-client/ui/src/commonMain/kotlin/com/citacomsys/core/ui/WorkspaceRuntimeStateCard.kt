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
import com.citacomsys.core.util.formatted
import com.citacomsys.kernel.model.WorkspaceRuntimeState

@Composable
fun WorkspaceRuntimeStateCard(
    state: WorkspaceRuntimeState,
    modifier: Modifier = Modifier
) {
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
                Text(
                    text = "Workspace State",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v${state.configVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusRow(
                    label = "Workspace Status",
                    status = state.workspaceStatus.name,
                    icon = AppIcons.Business
                )
                StatusRow(
                    label = "Device Status",
                    status = state.deviceStatus.name,
                    icon = AppIcons.Devices
                )
            }

            if (state.runtimeFlags.maintenanceMode || state.runtimeFlags.showSubscriptionWarning || state.runtimeFlags.hasPendingInvoice) {
                HorizontalDivider(Modifier.alpha(0.1f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Runtime Flags",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.runtimeFlags.maintenanceMode) {
                            FlagChip(
                                label = "Maintenance",
                                icon = AppIcons.HomeRepairService,
                                isError = true
                            )
                        }
                        if (state.runtimeFlags.showSubscriptionWarning) {
                            FlagChip(label = "Sub Warning", icon = AppIcons.Warning, isError = true)
                        }
                        if (state.runtimeFlags.hasPendingInvoice) {
                            FlagChip(
                                label = "Pending Invoice",
                                icon = AppIcons.Payment,
                                isError = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceEnforcementPolicyCard(
    state: WorkspaceRuntimeState,
    modifier: Modifier = Modifier
) {
    val policy = state.enforcement

    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Enforcement Policy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PolicyItem(label = "Allow Login", enabled = policy.allowLogin)
                PolicyItem(label = "Allow Sync", enabled = policy.allowSync)
                PolicyItem(label = "Allow Writes", enabled = policy.allowWrites)
                PolicyItem(label = "Offline Auth", enabled = policy.allowOfflineAuth)
                PolicyItem(label = "Online Validation", enabled = !policy.requireOnlineValidation)
            }

            HorizontalDivider(Modifier.alpha(0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Lease Expires: ${policy.leaseExpiresAt.formatted()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, status: String, icon: ImageVector) {
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
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FlagChip(label: String, icon: ImageVector, isError: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
private fun PolicyItem(label: String, enabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Icon(
            imageVector = if (enabled) AppIcons.CheckCircle else AppIcons.Cancel,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}
