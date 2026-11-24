package com.citacomsys.inventory.admin.security.device

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.management.FilterBar
import com.citacomsys.inventory.admin.common.ui.AdminListScreenScaffold
import com.citacomsys.inventory.admin.device.presentation.DeviceListViewModel
import com.citacomsys.shared.displayName
import com.citacomsys.types.DeviceStatus
import com.citacomsys.types.DeviceSummary
import com.citacomsys.types.HardwareMasterStatus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDeviceListScreen(
    viewModel: DeviceListViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current
    val queryState by viewModel.queryState.collectAsStateWithLifecycle()

    AdminListScreenScaffold(
        viewModel = viewModel,
        onBack = { navigator.goBack() },
        title = "Devices",
        searchPlaceholder = "Search workstations by id or model...",
        emptyTitle = "No Devices Registered",
        emptyDescription = "There are no endpoints currently linked to the platform. New workstations will appear here after binding.",
        emptyIcon = AppIcons.Devices,
        filterBar = {
            FilterBar(
                DeviceStatus.entries.toList(),
                toString = { it?.displayName() },
                selectedStatus = queryState.filters["status"],
                onStatusSelected = { viewModel.filtersChanged("status", it) }
            )
        },
        onClick = { device ->
            viewModel.onSelect(device)
//            navigator.navigateToSingleTop(SecurityDeviceDetailsNav(device.id))
        }
    ) { device, isSelected ->
        DeviceSummaryItem(device = device, isSelected)
    }
}

@Composable
fun DeviceSummaryItem(
    device: DeviceSummary,
    isSelected: Boolean
) {
    val color = if (device.masterStatus == HardwareMasterStatus.ACTIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = color.copy(alpha = 0.1f),
                    contentColor = color,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = device.masterStatus.displayName(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        supportingContent = {
            Column {
                Text(
                    text = "Model: ${device.model ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Last seen: ${device.lastUsedAt.formatRelativeTime()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified
        )
    )
}

@Composable
fun StatusBadge(status: DeviceStatus) {
    val color = when (status) {
        DeviceStatus.ACTIVE -> MaterialTheme.colorScheme.primary
        DeviceStatus.LOCKED -> MaterialTheme.colorScheme.error
        DeviceStatus.ACTIVE -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = status.displayName(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
