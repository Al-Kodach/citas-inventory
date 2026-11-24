package com.citacomsys.module.inventory.ui.screen.stocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.module.inventory.nav.BatchDetailsNavKey
import com.citacomsys.module.inventory.nav.CreateBatchNavKey
import com.citacomsys.module.inventory.ui.screen.batch.StockBatchCard
import com.design.components.AppScaffold
import com.design.components.ScrollableRowWithButtons
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Stock Management screen.
 * Displays stocks (batches) for a product with search and status filtering.
 * Clicking a stock batch navigates to the full screen (BatchDetailsScreen).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductStocksScreen(
    productId: String,
    goBack: () -> Unit,
    viewModel: ProductStocksViewModel = koinViewModel()
) {
    val batchesState by viewModel.batches.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val stockStatusFilter by viewModel.stockStatusFilter.collectAsStateWithLifecycle()
    val icon = ProjectLogos.getProductIcon(viewModel.productType)
    val navigator = LocalNavigator.current

    LaunchedEffect(productId) {
        viewModel.loadStocks(productId)
    }

    AppScaffold(
        title = "Stocks",
        onBack = goBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navigator.navigateToSingleTop(CreateBatchNavKey(productId))
                },
                icon = {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = "Restock"
                    )
                },
                text = { Text("RESTOCK", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        supportTitle = null,
        supportContent = {
            DetailsPlaceholder(
                icon = icon,
                title = "Product Stocks & Batches",
                description = "Create stocks for the current selected store",
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by batch number...") },
                leadingIcon = { Icon(AppIcons.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(AppIcons.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Status Filter Chips
            ScrollableRowWithButtons(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = stockStatusFilter == StockStatusFilter.ALL,
                        onClick = { viewModel.onStatusFilterChange(StockStatusFilter.ALL) },
                        label = { Text("All Batches") }
                    )
                }
                item {
                    FilterChip(
                        selected = stockStatusFilter == StockStatusFilter.LOW_STOCK,
                        onClick = { viewModel.onStatusFilterChange(StockStatusFilter.LOW_STOCK) },
                        label = { Text("Low Stock") }
                    )
                }
                item {
                    FilterChip(
                        selected = stockStatusFilter == StockStatusFilter.EXPIRING_SOON,
                        onClick = { viewModel.onStatusFilterChange(StockStatusFilter.EXPIRING_SOON) },
                        label = { Text("Near Expiry") }
                    )
                }
            }

            // Batches List
            UiResultGridView(
                state = batchesState,
                onRetry = { viewModel.loadStocks(productId) },
                modifier = Modifier.fillMaxSize(),
                emptyPlaceholder = {
                    DetailsPlaceholder(
                        icon = AppIcons.Inventory2,
                        title = if (searchQuery.isNotBlank()) "No Matching Batches" else "No Active Stocks",
                        description = if (searchQuery.isNotBlank()) "Try adjusting your search or status filter."
                            else "Tap Restock below to save a new stock batch for this product.",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            ) { batches ->

                items(batches, key = { it.batchId }) { batch ->
                    Surface(
                        onClick = {
                            navigator.navigateToSingleTop(
                                BatchDetailsNavKey(
                                    productId = productId,
                                    batchId = batch.batchId
                                )
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        StockBatchCard(
                            batch = batch,
                            showActions = false
                        )
                    }
                }
            }
        }
    }
}
