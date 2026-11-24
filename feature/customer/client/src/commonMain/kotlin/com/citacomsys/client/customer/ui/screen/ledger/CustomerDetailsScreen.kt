package com.citacomsys.client.customer.ui.screen.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.module.api.customer.CustomerLedgerEntry
import com.citacomsys.module.api.customer.CustomerTransactionType
import com.citacomsys.types.EmailAddress
import com.citacomsys.types.PhoneNumber
import com.design.components.AppListView
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: String,
    workspaceId: String,
    onBack: () -> Unit,
    onEditCustomer: ((String) -> Unit)? = null,
    viewModel: CustomerDetailsViewModel = koinViewModel(parameters = {
        parametersOf(
            customerId,
            workspaceId
        )
    })
) {
    val customerState by viewModel.customerState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val canManagerCustomer = viewModel.canManagerCustomer
    val customer = customerState.getOrNull()

    AppScaffold(
        onBack = onBack,
        title = customer?.fullName ?: "Customer Details",
        actions = {
            if (onEditCustomer != null) {
                IconButton(
                    onClick = { onEditCustomer(customerId) },
                    enabled = canManagerCustomer
                ) {
                    Icon(AppIcons.Edit, contentDescription = "Edit Profile")
                }
            }
            IconButton(onClick = { showDialog = true }) {
                Icon(AppIcons.Add, contentDescription = "Record Transaction")
            }
        }
    ) { padding ->
        UiResultView(
            state = customerState,
            onRetry = { viewModel.loadDetails() },
            modifier = Modifier.padding(padding)
        ) { customer ->
            AppListView(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val (phone, email) = if (canManagerCustomer) {
                        customer.phone to customer.email
                    } else {
                        customer.phone?.let { PhoneNumber(it).mask } to customer.email?.let {
                            EmailAddress(it).getMaskedEmail()
                        }
                    }
                    // Contacts Row / Column
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = phone ?: "No Phone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = email ?: "No Email",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Financial Account Balances Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Financial Account Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.4f
                                )
                            )

                            LedgerRow(
                                label = "Credit Limit",
                                value = customer.creditLimit?.formatted ?: "Unlimited"
                            )
                            LedgerRow(
                                label = "Outstanding Balance / Debt",
                                value = customer.currentBalance?.formatted ?: "GHS 0.00",
                                isHighlight = true
                            )
                        }
                    }
                }

                // Transaction History Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transaction History (${customer.ledgerEntries.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = { showDialog = true },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(
                                horizontal = 12.dp,
                                vertical = 4.dp
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(AppIcons.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Record Payment", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (customer.ledgerEntries.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "No previous ledger transactions recorded for this customerState.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(customer.ledgerEntries) { entry ->
                        LedgerItemRow(entry)
                    }
                }
            }
        }

        if (showDialog) {
            val saleSearchResults by viewModel.saleSearchResults.collectAsState()
            AddTransactionDialog(
                onDismiss = { showDialog = false },
                saleSearchResults = saleSearchResults,
                onSearchSales = viewModel::searchSales,
                onSubmit = { amount, type, method, ref, note ->
                    viewModel.recordTransaction(amount, "GHS", type, method, ref, note)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
private fun LedgerRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LedgerItemRow(entry: CustomerLedgerEntry) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.name.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!entry.note.isNullOrBlank()) {
                    Text(
                        text = entry.note!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Recorded: ${entry.recordedAt.toString().split("T").first()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = entry.amount.formatted,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.type == CustomerTransactionType.CREDIT_PURCHASE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    saleSearchResults: UiResult<List<SearchResult>>,
    onSearchSales: (String) -> Unit,
    onSubmit: (Long, CustomerTransactionType, String?, String?, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CustomerTransactionType.PAYMENT) }
    var selectedMethod by remember { mutableStateOf("CASH") }
    var note by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var isSearchingSales by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Customer Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (GHS)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Transaction Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomerTransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.replace("_", " ")) }
                        )
                    }
                }

                Text("Payment / Settlement Method", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CASH", "MOBILE_MONEY", "BANK_CARD", "ADJUSTMENT").forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method.replace("_", " ")) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = {
                        reference = it
                        onSearchSales(it)
                        isSearchingSales = true
                    },
                    label = { Text("Target Sale ID / Receipt #") },
                    placeholder = { Text("Type receipt # or sale ID to search...") },
                    leadingIcon = { Icon(AppIcons.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isSearchingSales && reference.trim().length >= 2) {
                    UiResultView(saleSearchResults) { sales ->
                        if (sales.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Matching Sales (${sales.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                    sales.take(4).forEach { sale ->
                                        ListItem(
                                            headlineContent = {
                                                Text(
                                                    sale.title,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            supportingContent = { Text(sale.subtitle) },
                                            modifier = Modifier.clickable {
                                                reference = sale.id
                                                sale.metadata["amount"]?.let { amt ->
                                                    val cleanAmt =
                                                        amt.split(" ").firstOrNull() ?: ""
                                                    if (cleanAmt.isNotBlank()) amountText = cleanAmt
                                                }
                                                isSearchingSales = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Particulars") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    onSubmit(
                        amountMinor,
                        selectedType,
                        selectedMethod,
                        reference.takeIf { it.isNotBlank() },
                        note.takeIf { it.isNotBlank() }
                    )
                },
                enabled = amountText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save Transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
