package com.citacomsys.sales.ui.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.sales.nav.SalesListNav
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.database.SaleTransactionView
import com.citacomsys.sales.data.repository.ClientSalesRepository
import com.citacomsys.types.Money
import com.design.components.AppListView
import com.citacomsys.core.spi.DashboardPlacement
import org.koin.compose.koinInject

@Composable
fun DailyCashSummaryWidget(
    modifier: Modifier = Modifier,
    salesRepository: ClientSalesRepository = koinInject()
) {
    val navigator = LocalNavigator.current
    val salesList by salesRepository.observeSales().collectAsStateWithLifecycle(initialValue = emptyList())

    val totalSalesMinor = remember(salesList) {
        salesList.sumOf { it.total_amount_minor }
    }
    val totalCurrency = remember(salesList) {
        salesList.firstOrNull()?.total_amount_currency ?: "GHS"
    }
    val formattedTotal = remember(totalSalesMinor, totalCurrency) {
        Money(totalSalesMinor, totalCurrency).formatted
    }

    DashboardWidgetCard(
        modifier = modifier,
        placement = DashboardPlacement.GRID
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Widget Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Payment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Sales & Reconciliation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Shift sales total: $formattedTotal (${salesList.size} Txns)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { navigator.navigateToSingleTop(SalesListNav) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Sales History", maxLines = 1)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Sales List View
            if (salesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales processed during this shift.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AppListView(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(salesList.size, key = { salesList[it].id }) { index ->
                            val sale = salesList[index]
                            DailySaleRowCard(
                                sale = sale,
                                onClick = { navigator.navigateToSingleTop(SalesListNav) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySaleRowCard(
    sale: SaleTransactionView,
    onClick: () -> Unit
) {
    val amount = remember(sale) { Money(sale.total_amount_minor, sale.total_amount_currency).formatted }
    val isCompleted = sale.status.equals("COMPLETED", ignoreCase = true) || sale.status.equals("PAID", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Txn #${sale.id.take(8)} • ${sale.actorName ?: "Operator"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${sale.totalItems} Items • ${sale.createdAt.formatRelativeTime()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SuggestionChip(
                onClick = onClick,
                label = {
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                )
            )
        }
    }
}
