package com.citacomsys.module.device.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.types.UserDeviceInfo
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    onDeviceClick: (String) -> Unit,
    viewModel: WorkstationListViewModel = koinViewModel(),
    onBack: (() -> Unit)? = null
) {
    val devicesState by viewModel.devices.collectAsStateWithLifecycle()
    var selectedDeviceToRename by remember { mutableStateOf<UserDeviceInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    AppScaffold(
        title = "Device Management",
        supportContent = {
            Text(
                text = "Manage devices that have access to your store or organization account. Click a device to view workstation bindings, active sessions, and provision capacity slots.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        },
        onBack = onBack
    ) { padding ->
        UiResultGridView(
            state = devicesState,
            onRetry = viewModel::loadDevices,
            modifier = Modifier.fillMaxSize()
                .padding(padding),
        ) { devices ->
            items(items = devices, key = { it.hardwareId }) { device ->
                DeviceCard(
                    device = device,
                    onClick = { onDeviceClick(device.hardwareId) },
                    onRename = { selectedDeviceToRename = device }
                )
            }
        }
    }

    if (selectedDeviceToRename != null) {
        RenameDeviceDialog(
            device = selectedDeviceToRename!!,
            onConfirm = { newName ->
                viewModel.renameDevice(selectedDeviceToRename!!.hardwareId, newName)
                selectedDeviceToRename = null
            },
            onDismiss = { selectedDeviceToRename = null }
        )
    }
}

@Composable
private fun DeviceCard(
    device: UserDeviceInfo,
    onClick: () -> Unit,
    onRename: () -> Unit
) {
    val revoked = device.status.revoked()
    AppCard(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        isSelected = revoked.not()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (revoked) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Devices,
                        contentDescription = null,
                        tint = if (revoked) Color.Unspecified else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.displayName.ifBlank { "Unnamed Device" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "IP: ${device.lastIp ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val lastSeen = device.lastUsedAt.formatRelativeTime()

                Text(
                    text = "Last seen: $lastSeen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRename, enabled = !revoked) {
                    Icon(AppIcons.Edit, "Rename")
                }
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RenameDeviceDialog(
    device: UserDeviceInfo,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(device.displayName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Give this device a recognizable ID like 'Reception Tablet' or 'Backoffice PC'.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter ID...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
