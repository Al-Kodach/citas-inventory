package com.citacomsys.inventory.admin.billing.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.productPackage.BillingQuotePayloadCard
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.ui.sudo.ActorSudoAuthDialogs
import com.citacomsys.shared.displayName
import com.citacomsys.types.BillingStatus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailsScreen(
    modifier: Modifier = Modifier,
    invoiceId: String,
    onBackClick: () -> Unit,
    viewModel: AdminBillingDetailViewModel = koinViewModel()
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    val selectedRecordState by viewModel.recordState.collectAsStateWithLifecycle()

    val sudoManager = viewModel.sudoAuthHandler

    val record = selectedRecordState?.getOrNull()

    // Actions logic based on BillingStatus
    val canVerify =
        record != null && (record.status == BillingStatus.PENDING || record.status == BillingStatus.PAYMENT_SUBMITTED)
    val canReject = record != null && !record.status.isTerminal() &&
            record.status != BillingStatus.PAID &&
            record.status != BillingStatus.FULFILLED

    LaunchedEffect(invoiceId) {
        viewModel.loadDetails(invoiceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "INVOICE REVIEW",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(AppIcons.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (canVerify || canReject) {
                Surface(tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (canReject) {
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Reject")
                            }
                        }
                        if (canVerify) {
                            Button(
                                onClick = { viewModel.verify(record) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Verify Payment")
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        if (selectedRecordState == null) return@Scaffold

        UiResultView(
            state = selectedRecordState!!,
            onRetry = { viewModel.loadDetails(invoiceId) },
            modifier = Modifier.padding(padding)
        ) { invoice ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Status Badge & Ref
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = invoice.referenceCode,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tenant: ${invoice.wsName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Badge(
                        containerColor = when (invoice.status) {
                            BillingStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                            BillingStatus.PAYMENT_SUBMITTED -> Color(0xFFFFF3E0) // Orange 50
                            BillingStatus.PAID -> MaterialTheme.colorScheme.primaryContainer
                            BillingStatus.FULFILLED -> Color(0xFFE8F5E9) // Green 50
                            BillingStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                            BillingStatus.REFUNDED -> MaterialTheme.colorScheme.secondaryContainer
                            BillingStatus.FULFILMENT_FAILED -> MaterialTheme.colorScheme.errorContainer
                            BillingStatus.CANCELED, BillingStatus.EXPIRED -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = when (invoice.status) {
                            BillingStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                            BillingStatus.PAYMENT_SUBMITTED -> Color(0xFFE65100) // Orange 900
                            BillingStatus.PAID -> MaterialTheme.colorScheme.onPrimaryContainer
                            BillingStatus.FULFILLED -> Color(0xFF2E7D32) // Green 800
                            BillingStatus.REJECTED -> MaterialTheme.colorScheme.onErrorContainer
                            BillingStatus.REFUNDED -> MaterialTheme.colorScheme.onSecondaryContainer
                            BillingStatus.FULFILMENT_FAILED -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                        Text(
                            text = invoice.status.displayName(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Unified Quota & Price Card
                BillingQuotePayloadCard(
                    quote = invoice.quote,
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PAYMENT INFORMATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        InfoRow(label = "Channel", value = invoice.paymentChannel.name)
                        InfoRow(label = "Target", value = invoice.paymentTarget)
                        InfoRow(label = "Recipient", value = invoice.paymentRecipientName)

                        invoice.paymentSubmission?.let { submission ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "SUBMISSION DETAILS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            InfoRow(
                                label = "Transaction ID",
                                value = submission.transactionId ?: "N/A"
                            )
                            InfoRow(label = "Sender Name", value = submission.payerName ?: "N/A")
                            InfoRow(label = "Note", value = submission.note ?: "N/A")
                        }
                    }
                }

                // Metadata
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Audit Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    InfoRow(label = "Type", value = invoice.type.displayName())
                    InfoRow(label = "Reason", value = invoice.reason.displayName())
                    InfoRow(label = "Origin", value = invoice.origin.displayName())
                    InfoRow(label = "Created By", value = invoice.createdBy)
                    InfoRow(label = "Created At", value = invoice.createdAt.formatRelativeTime())
                    invoice.expiresAt?.let { InfoRow(label = "Expires At", value = it.toString()) }
                    invoice.fulfilledAt?.let {
                        InfoRow(label = "Fulfilled At", value = it.formatRelativeTime())
                    }
                }
            }
        }
    }

    ActorSudoAuthDialogs(sudoManager)

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Invoice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Please provide a reason for rejecting this payment.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Transaction ID not found") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reject(invoiceId, rejectReason)
                        showRejectDialog = false
                    },
                    enabled = rejectReason.isNotBlank()
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}
