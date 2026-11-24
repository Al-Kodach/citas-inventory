package com.citacomsys.inventory.admin.billing.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.BillInvoice
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.shared.displayName
import com.citacomsys.types.BillingStatus
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBillingListScreen(
    modifier: Modifier = Modifier,
    onSelected: (id: String) -> Unit,
    viewModel: AdminBillingViewModel = koinViewModel()
) {
    val invoiceState by viewModel.activeInvoiceState.collectAsStateWithLifecycle()
    var selectedInvoiceId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Client Billing & Invoices", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.loadActiveBills() }) {
                        Icon(AppIcons.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding)) {
            // Search & Filter Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search by business id or reference...") },
                leadingIcon = { Icon(AppIcons.Search, null) },
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            UiResultView(
                state = invoiceState,
                onRetry = viewModel::loadActiveBills,
                loadingView = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { invoices ->
                val filteredInvoices = invoices.filter { 
                    it.tenantName.contains(searchQuery, ignoreCase = true) || 
                    it.referenceCode.contains(searchQuery, ignoreCase = true) 
                }

                if (filteredInvoices.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching invoices found.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredInvoices, key = { it.id }) { invoice ->
                            InvoiceCard(
                                invoice = invoice,
                                isSelected = selectedInvoiceId == invoice.id,
                                onClick = {
                                    selectedInvoiceId = invoice.id
                                    onSelected(invoice.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(
    invoice: BillInvoice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = invoice.tenantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "REF: ${invoice.referenceCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                StatusBadge(invoice.status)
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.type.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = invoice.amount.formatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BillingStatus) {
    val (color, label) = when (status) {
        BillingStatus.PENDING -> MaterialTheme.colorScheme.tertiary to status.displayName()
        BillingStatus.PAID -> Color(0xFF2E7D32) to status.displayName()
        BillingStatus.PAYMENT_SUBMITTED -> Color(0xFFF57C00) to "Verifying..."
        BillingStatus.FULFILLED -> Color(0xFF1976D2) to status.displayName()
        else -> MaterialTheme.colorScheme.error to status.displayName()
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = label.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black
        )
    }
}
