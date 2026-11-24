package com.citacomsys.client.customer.ui.widget

import androidx.compose.foundation.BorderStroke
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiResult
import com.citacomsys.module.api.customer.CustomerLedgerEntry
import com.citacomsys.module.api.customer.CustomerRepository
import com.citacomsys.module.api.customer.CustomerTransactionType
import com.citacomsys.module.api.customer.nav.CustomerDetailsNav
import com.citacomsys.module.api.customer.nav.CustomerListNav
import com.design.components.AppListView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel

class CustomerLedgerWidgetViewModel(
    sessionManager: AppSessionManager,
    repository: CustomerRepository,
    viewModelScope: CoroutineScope
) : ViewModel() {

    val wsId = sessionManager.activeWorkspaceId

    val ledgerEntriesState = combine(
        sessionManager.activeWorkspaceIdFlow,
        sessionManager.activeViewBranchId
    ) { _, _ ->
        repository.observeLedger(20L).first()
    }.asUiResult()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiResult.Loading
        )
}

typealias CustomerGrowthWidgetViewModel = CustomerLedgerWidgetViewModel

@Composable
fun CustomerLedgerWidget(
    modifier: Modifier = Modifier,
    viewModel: CustomerLedgerWidgetViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val state by viewModel.ledgerEntriesState.collectAsStateWithLifecycle()
    val wsId = viewModel.wsId ?: ""

    CustomerLedgerWidgetContent(
        state = state,
        onViewAll = { navigator.navigateToSingleTop(CustomerListNav) },
        onEntryClick = { customerId ->
            navigator.navigateToSingleTop(
                CustomerDetailsNav(customerId, wsId)
            )
        },
        modifier = modifier
    )
}

@Composable
fun CustomerGrowthWidget(
    modifier: Modifier = Modifier,
    viewModel: CustomerLedgerWidgetViewModel = koinViewModel(),
) = CustomerLedgerWidget(modifier, viewModel)

@Composable
fun CustomerLedgerWidgetContent(
    state: UiResult<List<CustomerLedgerEntry>>,
    onViewAll: () -> Unit,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardWidgetCard(
        modifier = modifier,
        placement = DashboardPlacement.LIST
    ) {
        UiResultView(state) { entries ->
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
                                imageVector = AppIcons.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Customer Ledger Activity (${entries.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Latest credit sales, payments & balance adjustments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = onViewAll,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("View All", maxLines = 1)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Ledger Activity List
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No customerState ledger activity recorded.",
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
                            items(entries.size, key = { entries[it].id }) { index ->
                                val item = entries[index]
                                LedgerRowCard(
                                    item = item,
                                    onClick = { onEntryClick(item.customerId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRowCard(
    item: CustomerLedgerEntry,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Customer Account",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (item.type == CustomerTransactionType.CREDIT_PURCHASE) MaterialTheme.colorScheme.errorContainer.copy(
                            alpha = 0.5f
                        )
                        else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = item.type.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (item.type == CustomerTransactionType.CREDIT_PURCHASE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                item.note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = item.amount.formatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.type == CustomerTransactionType.CREDIT_PURCHASE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
