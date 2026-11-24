package com.citacomsys.feature.billing.ui.screen.history

import TransactionHistoryViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.util.formatted
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.core.ui.layout.SectionWrapper
import com.citacomsys.shared.displayName
import com.citacomsys.types.BillingStatus
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TransactionHistoryScreen(
    onBack: () -> Unit,
    onInvoiceClick: (String) -> Unit,
    viewModel: TransactionHistoryViewModel = koinViewModel()
) {
    val historyResult by viewModel.history.collectAsStateWithLifecycle()


    AppScaffold(
        title = "Transaction History",
        onBack = onBack
    ) {
        UiResultGridView(
            state = historyResult,
            onRetry = { viewModel.loadHistory() },
            emptyPlaceholder = {
                DetailsPlaceholder(
                    icon = AppIcons.History,
                    title = "No Transactions Yet",
                    description = "Your subscription payments and SMS top-ups will appear here once generated."
                )
            }
        ) { history ->
            items(history) { record ->
                Surface(
                    modifier = Modifier.clickable { onInvoiceClick(record.id) }
                ) {
                    TransactionHistoryItem(record)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TransactionHistoryItem(record: TransactionHistory) {
    val statusColor = when (record.status) {
        BillingStatus.PAID, BillingStatus.PAID -> Color(0xFF4CAF50) // Success Green
        BillingStatus.REJECTED, BillingStatus.CANCELED -> MaterialTheme.colorScheme.error
        BillingStatus.EXPIRED -> MaterialTheme.colorScheme.outline
        BillingStatus.PAYMENT_SUBMITTED -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary // PENDING
    }

    ListItem(
        headlineContent = { Text(record.summary, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Text(
                "${record.createdAt.formatted("MEDIUM")} • Ref: ${record.referenceCode}",
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(record.amount.formatted, fontWeight = FontWeight.Black)
                Text(
                    text = record.status.displayName().uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = statusColor,
                    letterSpacing = 0.5.sp
                )
            }
        },
        leadingContent = {
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (record.status) {
                            BillingStatus.PAID, BillingStatus.PAID -> AppIcons.CheckCircle
                            BillingStatus.REJECTED, BillingStatus.CANCELED -> AppIcons.Cancel
                            BillingStatus.PAYMENT_SUBMITTED -> AppIcons.History
                            else -> AppIcons.Schedule
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}