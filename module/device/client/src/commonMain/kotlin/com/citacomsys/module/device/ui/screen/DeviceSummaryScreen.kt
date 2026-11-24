package com.citacomsys.module.device.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.auth.WorkstationSession
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.types.HardwareMasterStatus
import com.citacomsys.types.WorkstationBinding
import com.design.components.AppListView
import com.design.components.AppScaffold
import eu.anifantakis.lib.ksafe.biometrics.KSafeBiometrics
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSummaryScreen(
    hardwareId: String,
    onBack: () -> Unit,
    viewModel: WorkstationSummaryViewModel = koinViewModel(parameters = { parametersOf(hardwareId) })
) {
    val summaryState by viewModel.deviceSummaryState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showRenameDialog by remember { mutableStateOf(false) }
    var bindingToUnbind by remember { mutableStateOf<WorkstationBinding?>(null) }
    var showRevokeConfirmDialog by remember { mutableStateOf(false) }

    val details = summaryState.getOrNull()

    AppScaffold(
        title = details?.name ?: "Device Details",
        onBack = if (LocalBackButtonVisibility.current) onBack else null,
        actions = {
            IconButton(onClick = {
                viewModel.loadDeviceSummary(hardwareId)
            }) {
                Icon(AppIcons.Refresh, contentDescription = "Refresh Data")
            }
        }
    ) { padding ->
        UiResultView(
            state = summaryState,
            onRetry = {
                viewModel.loadDeviceSummary(hardwareId)
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { summary ->
            val isMasterREVOKED = summary.masterStatus == HardwareMasterStatus.REVOKED

            AppListView(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Primary Device Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (isMasterREVOKED) MaterialTheme.colorScheme.errorContainer.copy(
                                            alpha = 0.5f
                                        )
                                        else MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = AppIcons.Devices,
                                                contentDescription = null,
                                                tint = if (isMasterREVOKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = summary.name.ifBlank { "Store Device" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isMasterREVOKED) "Device Status: BLOCKED / DISABLED" else "Device Status: ACTIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMasterREVOKED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showRenameDialog = true },
                                    enabled = !isMasterREVOKED,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(
                                        AppIcons.Edit,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Rename")
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.4f
                                )
                            )

                            DetailRow(
                                label = "Device Model",
                                value = summary.model ?: "Standard Terminal/Tablet"
                            )
                            DetailRow(label = "Operating System", value = summary.osName ?: "N/A")
                            DetailRow(label = "Last Known IP", value = summary.lastIp ?: "N/A")
                            DetailRow(
                                label = "Last Active",
                                value = summary.lastUsedAt.formatRelativeTime()
                            )
                        }
                    }
                }

                // Assigned Business Stores Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assigned Business Stores (${summary.bindings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Unlinking a store frees device capacity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (summary.bindings.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "This device is not currently assigned to any store.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(summary.bindings, key = { it.bindingId }) { binding ->
                        WorkstationBindingItem(
                            binding = binding,
                            isMasterLocked = isMasterREVOKED,
                            onUnbindClick = { bindingToUnbind = binding },
                            onRelinkClick = {
                                viewModel.relinkWorkstation(
                                    binding.bindingId,
                                    summary.hardwareId
                                )
                            }
                        )
                    }
                }

                // Active User Sessions Section
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Active User Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Block / Disable Device Security Card
                item {
                    Spacer(Modifier.height(12.dp))
                    AppCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Device Security Controls",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = "Blocking this device will immediately disable it from accessing any company store, the Citas Platform, or network services, and log out all active sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { showRevokeConfirmDialog = true },
                                enabled = !isMasterREVOKED,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(AppIcons.Delete, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isMasterREVOKED) "Device Currently Blocked" else "Block & Disable Device Company-Wide",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        val summary = (summaryState as? UiResult.Success)?.data
        var newName by remember { mutableStateOf(summary?.name ?: "") }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Device") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameDevice(hardwareId, newName)
                        showRenameDialog = false
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unlink Store Confirmation
    if (bindingToUnbind != null) {
        AlertDialog(
            onDismissRequest = { bindingToUnbind = null },
            title = { Text("Unlink Store Access?") },
            text = {
                Text("Unlinking '${bindingToUnbind?.workspaceName}' will disconnect this device from that store and free up device capacity slots. Biometric confirmation required.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetBinding = bindingToUnbind!!
                        bindingToUnbind = null
                        coroutineScope.launch {
                            val authorized =
                                KSafeBiometrics.verifyBiometric("Authorize store unlinking for ${targetBinding.workspaceName}")
                            if (authorized) {
                                viewModel.unbindWorkstation(targetBinding.bindingId, hardwareId)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { bindingToUnbind = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Block Device Globally Confirmation
    if (showRevokeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeConfirmDialog = false },
            title = { Text("Block & Disable Device?") },
            text = {
                Text("This action will block this device from accessing any store in your company, the Citas Platform, or network services. Biometric confirmation required.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRevokeConfirmDialog = false
                        coroutineScope.launch {
                            val authorized =
                                KSafeBiometrics.verifyBiometric("Authorize device security block")
                            if (authorized) {
                                viewModel.revokeDeviceGlobally(hardwareId)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block Device")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkstationBindingItem(
    binding: WorkstationBinding,
    isMasterLocked: Boolean,
    onUnbindClick: () -> Unit,
    onRelinkClick: () -> Unit
) {
    val isRevoked = binding.status.revoked() || isMasterLocked

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRevoked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            0.5.dp,
            if (isRevoked) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = binding.workspaceName.ifBlank { "Business Store" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Assigned: ${binding.boundAt.formatRelativeTime()} • Last Active: ${binding.lastUsedAt.formatRelativeTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isRevoked) {
                OutlinedButton(
                    onClick = onUnbindClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(AppIcons.Delete, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Unlink Store")
                }
            } else if (!isMasterLocked) {
                Button(
                    onClick = onRelinkClick,
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(AppIcons.Refresh, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Relink Store", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "UNLINKED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkstationSessionItem(
    session: WorkstationSession,
    onTerminate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isActive) MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.2f
            )
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        )
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Logged-in User: #${session.userId.takeLast(8)}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (session.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (session.isActive) "ACTIVE" else "ENDED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (session.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Started: ${session.createdAt.formatRelativeTime()} • Last Active: ${session.lastUsedAt.formatRelativeTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onTerminate,
                enabled = session.isActive,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out User", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
