package com.citacomsys.sales.ui.screen.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.module.api.customer.Customer
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.module.api.customer.CustomerLedgerEntry
import com.citacomsys.module.api.customer.CustomerTransactionType
import com.citacomsys.sales.ui.screen.share.SmsReceiptDialogState
import com.design.components.AppListView
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailsScreen(
    saleId: String,
    wsId: String?,
    onBack: () -> Unit,
    onSmsTop: () -> Unit,
    onViewReceipt: (String) -> Unit,
    viewModel: SaleDetailsViewModel = koinViewModel { parametersOf(saleId, wsId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUser by viewModel.actor.collectAsStateWithLifecycle()
    val smsState by viewModel.smsReceiptState.collectAsStateWithLifecycle()
    val customerState by viewModel.associatedCustomer.collectAsStateWithLifecycle()
    val ledger by viewModel.ledgerEntries.collectAsStateWithLifecycle()

    val details = state.saleDetailsState.getOrNull()

    LaunchedEffect(details) {
        if (details != null) {
            viewModel.loadCustomerLedger(details.sale.customerId, viewModel.activeWorkspaceId)
        }
    }

    var showSmsReceiptDialog by remember { mutableStateOf(false) }

    if (showSmsReceiptDialog) {
        DigitalReceiptDialog(
            state = smsState,
            onDismiss = { showSmsReceiptDialog = false },
            onPhoneChange = { viewModel.onPhoneChange(it) },
            onSend = {
                viewModel.sendDigitalReceipt {
                    showSmsReceiptDialog = false
                }
            },
            onSmsTop = onSmsTop
        )
    }

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    if (LocalBackButtonVisibility.current) {
                        BackButton(onBack)
                    }
                },
                actions = {
                    if (details != null) {
                        Button(
                            onClick = { onViewReceipt(saleId) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(AppIcons.Print, null)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Receipt")
                        }
                    }
                    IconButton(onClick = { viewModel.loadDetails() }) {
                        Icon(AppIcons.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            UiResultView(
                state = state.saleDetailsState,
                onRetry = { viewModel.loadDetails() }
            ) { sale ->
                SaleDetailsContent(
                    details = sale,
                    actorName = currentUser?.name ?: "Unknown",
                    smsState = smsState,
                    customerState = customerState,
                    ledger = ledger,
                    loadCustomer = { id ->
                        viewModel.loadCustomerLedger(
                            id,
                            viewModel.activeWorkspaceId
                        )
                    },
                    onSendSms = { showSmsReceiptDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


@Composable
internal fun SaleDetailsContent(
    details: SaleDetails,
    actorName: String,
    smsState: SmsReceiptDialogState,
    customerState: UiResult<CustomerItem>,
    ledger: List<CustomerLedgerEntry>,
    loadCustomer: (String) -> Unit,
    onSendSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppListView(
        modifier = modifier,
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("Total Amount Paid", style = MaterialTheme.typography.labelMedium)
                            Text(
                                details.sale.totalAmount.formatted,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        StatusBadge(details.sale.status.name)
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    Spacer(Modifier.height(16.dp))

                    DetailInfoRow("Transaction ID", details.sale.id)
                    DetailInfoRow(
                        "Date & Time",
                        details.sale.createdAt.toString().replace("T", " ").split(" ").first()
                    )
                    DetailInfoRow("Staff Attendant", actorName)
                }
            }
        }



        item { SectionHeader("Itemized Breakdown") }
        items(details.items) { item ->
            SaleItemDetailRow(item)
            HorizontalDivider(
                Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }

        item { SectionHeader("Payment History") }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                details.payments.forEach { payment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                payment.method.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            payment.reference?.let {
                                Text(
                                    "Ref: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            payment.amount.formatted,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (details.sale.customerId != null) {
            item { SectionHeader("Customer Account Ledger") }

            customerAndLedgerInfo(
                customerState = customerState,
                ledger = ledger,
                getCustomer = { loadCustomer(details.sale.customerId!!) },
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Digital Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Send a secure digital summary of this transaction to the customer.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = onSendSms,
                        enabled = smsState.state.isLoading.not(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send via SMS")
                    }
                }
            }
        }
    }
}

@Composable
internal fun SaleItemDetailRow(item: SaleDetails.Item) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Batch: ${item.batchNumber ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.expiryDate != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Exp: ${item.expiryDate.toString().split("T").first()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    "${item.unitPrice.formatted} x ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                item.totalPrice.formatted,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
    )
}

@Composable
internal fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun StatusBadge(status: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = CircleShape
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}


private fun LazyListScope.customerAndLedgerInfo(
    customerState: UiResult<CustomerItem>,
    ledger: List<CustomerLedgerEntry>,
    getCustomer: () -> Unit
) {
    item {
        UiResultView(
            state = customerState,
            onRetry = getCustomer,
            modifier = Modifier.fillMaxWidth(),
        ) { customer ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Customer Account Ledger",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                            alpha = 0.4f
                        )
                    )

                    DetailInfoRow("Customer Name", customer.fullName)
                    DetailInfoRow(
                        "Phone Number",
                        customer.phone ?: "No Phone"
                    )
                    DetailInfoRow(
                        "Email",
                        customer.email ?: "No Email"
                    )
                }
            }
        }
    }

    item {
        if (ledger.isEmpty()) {
            Text(
                text = "No ledger entries found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@item
        }

        Text(
            text = "Associated Ledger Entries (${ledger.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        ledger.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = entry.type.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    entry.note?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = entry.amount.formatted,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.type == CustomerTransactionType.CREDIT_PURCHASE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
