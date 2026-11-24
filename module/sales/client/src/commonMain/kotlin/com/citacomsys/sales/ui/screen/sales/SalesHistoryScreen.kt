package com.citacomsys.sales.ui.screen.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.citacomsys.core.database.dao.sale.totalAmount
import com.citacomsys.core.domain.security.HistoryVisibilityStatus
import com.citacomsys.core.domain.security.WorkspaceResourceQuotaGuard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.management.SaaSListScreenScaffold
import com.citacomsys.database.SaleTransactionView
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SalesHistoryScreen(
    onSaleSelected: (String, String) -> Unit,
    onBack: () -> Unit,
    viewModel: SalesHistoryViewModel = koinViewModel()
) {
    val isCompact = LocalBackButtonVisibility.current

    SaaSListScreenScaffold(
        viewModel = viewModel,
        title = "Sales History",
//        onBack = if (LocalBackButtonVisibility.current) onBack else null,
        searchPlaceholder = "Search by ID or staff...",
        onClick = { sale ->
            onSaleSelected(sale.id, sale.workspace_id)
        }
    ) { sale, isSelected ->
        SaleListItem(
            sale = sale,
            isSelected = isSelected && !isCompact,
            onClick = {
                onSaleSelected(sale.id, sale.workspace_id)
            }
        )
    }
}

@Composable
private fun QuotaInfoSection(
    quotaGuard: WorkspaceResourceQuotaGuard = koinInject()
) {
    val ledgerStatus = produceState(null as HistoryVisibilityStatus?) {
        value = quotaGuard.getSalesVisibilityStatus()
    }

    ledgerStatus.value?.let {
        if (!it.isUnrestricted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Info, null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Viewing ${it.label}")
                        it.upgradeNudge?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleListItem(
    sale: SaleTransactionView,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "REF: ${sale.id.takeLast(8).uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sale.createdAt.toString().split("T").first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "By ${sale.actorName}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = sale.totalAmount.formatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
