package com.citacomsys.module.device.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.device.service.DeviceService
import com.design.components.AppSettingsItem
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DeviceRenameSettingsWidget(
    modifier: Modifier = Modifier,
    deviceService: DeviceService = koinInject()
) {
    val deviceInfo by deviceService.deviceInfo.collectAsStateWithLifecycle()
    var showRenameDialog by remember { mutableStateOf(false) }

    AppSettingsItem(
        title = "Workstation Display Name",
        subtitle = deviceInfo.displayName.ifBlank { "Local Workstation" },
        icon = AppIcons.Devices,
        onClick = { showRenameDialog = true }
    )

    if (showRenameDialog) {
        var nameText by remember { mutableStateOf(deviceInfo.displayName) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Workstation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Set a friendly ID for this workstation (e.g. 'Front Desk 1', 'Main Register').",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            deviceService.updateDeviceInfo(deviceInfo.copy(displayName = nameText))
                        }
                        showRenameDialog = false
                    },
                    enabled = nameText.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
