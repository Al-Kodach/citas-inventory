package com.citacomsys.module.inventory.ui.screen.stocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResultButton
import com.citacomsys.core.ui.result.getErrorOrNull
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTransferScreen(
    productId: String,
    batchId: String,
    onDismiss: () -> Unit,
    viewModel: StockTransferViewModel = koinInject(
        parameters = { parametersOf(batchId) }
    )
) {
    val batchState by viewModel.selectedBatch.collectAsStateWithLifecycle()
    val branches by viewModel.availableBranches.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()
    val saveState by viewModel.actionState.collectAsStateWithLifecycle()

    val destinationBranchId by viewModel.destinationBranchId.collectAsStateWithLifecycle()
    val quantityToTransfer by viewModel.quantityToTransfer.collectAsStateWithLifecycle()
    val reasonMemo by viewModel.reasonMemo.collectAsStateWithLifecycle()

    var branchDropdownExpanded by remember { mutableStateOf(false) }
    val selectedBranchName =
        branches.find { it.id == destinationBranchId }?.name ?: "Select Destination Location*"

    UiResultView(
        state = batchState,
        onRetry = viewModel::refresh
    ) { batch ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. BANNER TITLE BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Inter-Branch Stock Transit",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Deduct inventory from source batch to spawn an entry at an alternative branch.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isExecuting) {
                    Icon(Icons.Default.Close, contentDescription = "Close Screen")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // --- 2. SOURCE SPECIFICATION PANEL DISPLAY MATRIX ---
            batch?.let { activeBatch ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.3f
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Source Item Blueprint Details",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Batch Identifier Reference Code:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                activeBatch.batchNumber.ifBlank { "Perpetual Line Stub" },
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Available Current Shelf Balance:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${activeBatch.availableQuantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // --- 3. DESTINATION SELECTOR FORM COMPONENT ---
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Target Branch Destination*",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { if (!isExecuting) branchDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = selectedBranchName, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }

                    DropdownMenu(
                        expanded = branchDropdownExpanded,
                        onDismissRequest = { branchDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        branches.forEach { branchUi ->
                            DropdownMenuItem(
                                text = { Text(branchUi.name) },
                                onClick = {
                                    viewModel.destinationBranchId.value = branchUi.id
                                    branchDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // --- 4. QUANTITY INPUT ENTRY LINE ---
            OutlinedTextField(
                value = quantityToTransfer,
                onValueChange = {
                    if (it.all { it.isDigit() }) {
                        viewModel.quantityToTransfer.value = it
                    }
                },
                label = { Text("Transfer Transit Quantity*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isExecuting,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // --- 5. COMPLIANCE AUDIT AUDITOR REASON REASON TEXT AREA ---
            OutlinedTextField(
                value = reasonMemo,
                onValueChange = { viewModel.reasonMemo.value = it },
                label = { Text("Audit Tracking Reason / Memo Note*") },
                placeholder = { Text("e.g., Stock depletion emergency request from Branch B manager.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isExecuting
            )

            Spacer(Modifier.height(16.dp))

            // Display validation or transmission errors
            saveState.getErrorOrNull()?.let { errorMessage ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // --- 6. ACTION CONTROL DOCK DOCK BUTTONS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = !isExecuting
                ) {
                    Text("Cancel")
                }

                UiResultButton(
                    onClick = { viewModel.submitTransferRequest(onSuccess = onDismiss) },
                    result = saveState,
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = destinationBranchId.isNotBlank() && quantityToTransfer.isNotBlank() && reasonMemo.isNotBlank() && !isExecuting,
                    text = "Authorize Live Transit"
                )
            }
        }
    }
}
