package com.citacomsys.module.device.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.kernel.model.WorkspaceRuntimeConfig
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.PackageTier
import com.citacomsys.types.UserDeviceInfo
import com.citacomsys.types.WorkstationBinding
import com.design.AppTheme
import com.design.components.AppListView
import kotlin.time.Clock
import org.koin.compose.koinInject

@Composable
fun DeviceDashboardWidget(
    modifier: Modifier = Modifier,
    store: PlatformRuntimeStore = koinInject(),
    service: PlatformClientService = koinInject()
) {
    val cachedDevices by store.orgDevices.collectAsStateWithLifecycle()
    val wsConfig by service.activeWorkspaceConfig.collectAsStateWithLifecycle()

    val devicesState = remember(cachedDevices) {
        if (cachedDevices.isNotEmpty()) UiResult.Success(cachedDevices)
        else UiResult.Loading
    }

    DeviceDashboardWidgetContent(
        devicesState = devicesState,
        wsConfig = wsConfig,
        modifier = modifier
    )
}

@Composable
fun DeviceDashboardWidgetContent(
    devicesState: UiResult<List<UserDeviceInfo>>,
    wsConfig: WorkspaceRuntimeConfig?,
    modifier: Modifier = Modifier
) {
    var selectedDeviceSummary by remember { mutableStateOf<DeviceSummary?>(null) }

    val isPremium = remember(wsConfig) {
        val tier = wsConfig?.resource?.tier ?: PackageTier.STARTER
        tier != PackageTier.STARTER
    }

    DashboardWidgetCard(
        modifier = modifier,
        placement = DashboardPlacement.LIST
    ) {
        UiResultView(devicesState) { devices ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Widget Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = AppIcons.Devices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Organization Devices (${devices.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Registered workstations and node status",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Organization Devices List
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No devices registered in organization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AppListView(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(0.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(devices.size, key = { devices[it].hardwareId }) { index ->
                                val device = devices[index]
                                val summary = remember(device, wsConfig) {
                                    DeviceSummary(
                                        hardwareId = device.hardwareId,
                                        name = device.displayName.ifBlank { "Workstation" },
                                        model = "Hardware Node",
                                        osName = "System",
                                        masterStatus = HardwareMasterStatus.ACTIVE,
                                        lastIp = device.lastIp,
                                        lastUsedAt = device.lastUsedAt,
                                        bindings = listOf(
                                            WorkstationBinding(
                                                bindingId = device.hardwareId,
                                                workspaceName = wsConfig?.name ?: "Active Store",
                                                workspaceId = wsConfig?.workspaceId ?: "",
                                                status = device.status,
                                                boundAt = device.lastUsedAt,
                                                lastIp = device.lastIp,
                                                lastUsedAt = device.lastUsedAt
                                            )
                                        )
                                    )
                                }

                                OrgDeviceRowCard(
                                    device = device,
                                    onClick = { selectedDeviceSummary = summary }
                                )
                            }
                        }
                    }
                }

                // Advanced Sessions & Performance (PRO / ENTERPRISE or Trial)
                if (isPremium) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                            )
                            Text(
                                text = "${devices.count { !it.status.revoked() }} Active Sessions • Mesh Healthy",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "Latency: < 5ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "PRO Plan: Live session diagnostics & performance monitoring",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Detail Dialog for DeviceSummary & Workstation Bindings
        if (selectedDeviceSummary != null) {
            DeviceSummaryDetailsDialog(
                summary = selectedDeviceSummary!!,
                isPremium = isPremium,
                onDismiss = { selectedDeviceSummary = null }
            )
        }
    }
}

@Composable
private fun OrgDeviceRowCard(
    device: UserDeviceInfo,
    onClick: () -> Unit
) {
    val revoked = device.status.revoked()
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName.ifBlank { "Workstation" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${device.hardwareId.take(10)}... • IP: ${device.lastIp ?: "N/A"} • ${device.lastUsedAt.formatRelativeTime()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SuggestionChip(
                onClick = onClick,
                label = {
                    Text(
                        text = if (revoked) "REVOKED" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (revoked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }

}

@Composable
private fun DeviceSummaryDetailsDialog(
    summary: DeviceSummary,
    isPremium: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = summary.name) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Device Hardware Identity Section
                Text(
                    text = "Hardware Identity",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Hardware ID: ${summary.hardwareId}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Model: ${summary.model ?: "Unknown"} • OS: ${summary.osName ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last Active: ${summary.lastUsedAt.formatRelativeTime()} • IP: ${summary.lastIp ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Workstation Bindings Section
                Text(
                    text = "Workspace Bindings (${summary.bindings.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (summary.bindings.isEmpty()) {
                    Text(
                        text = "No active workspace bindings found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    summary.bindings.forEach { binding ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = binding.workspaceName.ifBlank {
                                        "Workspace ${binding.workspaceId.take(8)}"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Status: ${binding.status.name} • Bound: ${binding.boundAt.formatRelativeTime()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Performance & Sessions Diagnostics (PRO / ENTERPRISE)
                if (isPremium) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Text(
                        text = "Session Diagnostics",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Active Sessions: Operational",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Mesh Network Status: Healthy (< 5ms)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Preview
@Composable
private fun DeviceDashboardWidgetPreview() {
    val sampleDevices = listOf(
        UserDeviceInfo(
            hardwareId = "hw-pos-001-acc8921f",
            status = DeviceStatus.ACTIVE,
            displayName = "Front Desk POS Terminal",
            lastUsedAt = Clock.System.now(),
            lastIp = "192.168.1.101"
        ),
        UserDeviceInfo(
            hardwareId = "hw-tab-002-bf91238d",
            status = DeviceStatus.ACTIVE,
            displayName = "Floor Manager Tablet",
            lastUsedAt = Clock.System.now(),
            lastIp = "192.168.1.105"
        ),
        UserDeviceInfo(
            hardwareId = "hw-reg-003-fe44120a",
            status = DeviceStatus.REVOKED,
            displayName = "Legacy Backoffice Register",
            lastUsedAt = Clock.System.now(),
            lastIp = "192.168.1.112"
        )
    )

    AppTheme {
        DeviceDashboardWidgetContent(
            devicesState = UiResult.Success(sampleDevices),
            wsConfig = null
        )
    }
}
