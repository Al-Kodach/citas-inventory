package com.citacomsys.inventory.admin.device.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.types.HardwareTechInfo
import com.citacomsys.types.UserCapability
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeviceHardwareView(
    modifier: Modifier = Modifier,
    device: HardwareTechInfo
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IdentityCard(device)
        HardwareCard(device)
    }
}

@Composable
fun DeviceHardwareMetadataView(
    deviceId: String,
    modifier: Modifier = Modifier,
    showSecurityLink: Boolean = true,
    onNavigateToSecurity: (String) -> Unit = {},
    capabilityManager: UserCapabilityManager = koinInject(),
    viewModel: DeviceHardwareMetaInfoViewModel = koinViewModel()
) {
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    val canSeeSecurity by capabilityManager.capabilities.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDeviceHardwareInfo(deviceId)
    }

    UiResultView(
        deviceState,
        onRetry = { viewModel.loadDeviceHardwareInfo(deviceId) },
        modifier = modifier.fillMaxSize()
    ) { device ->
        LazyListView(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IdentityCard(device)
            }

            item {
                HardwareCard(device)
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (showSecurityLink && canSeeSecurity.contains(UserCapability.ACCESS_SYSTEM_SETTINGS)) {
                        TextButton(
                            onClick = { onNavigateToSecurity(device.hardwareId) },
                        ) {
                            Icon(AppIcons.Security, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("View Endpoint Security Logs")
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun IdentityCard(device: HardwareTechInfo) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AppIcons.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("Hardware Identity", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            DetailRow("Hardware ID", device.hardwareId)
            DetailRow("Master Status", device.masterStatus.name)
            DetailRow("Fingerprint", device.fingerprint ?: "N/A")
        }
    }
}


@Composable
private fun HardwareCard(device: HardwareTechInfo) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AppIcons.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("Specifications", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            DetailRow("Model", device.model ?: "N/A")
            DetailRow("OS Name", device.osName ?: "N/A")
            DetailRow("Architecture", device.architecture ?: "N/A")
            DetailRow("Last Updated", device.updatedAt.formatRelativeTime())
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = if (label == "Hardware ID") FontFamily.Monospace else null
        )
    }
}
