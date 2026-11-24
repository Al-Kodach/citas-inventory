package com.citacomsys.module.inventory.ui.screen.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.database.ProductListItemView
import com.citacomsys.types.ProductType
import com.design.components.AppScaffold
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProductListScreen(
    onProductClick: (String) -> Unit,
    onNewProduct: () -> Unit,
    onMetadataClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ProductListViewModel = koinViewModel(),
    sessionManager: AppSessionManager = koinInject()
) {
    val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val expandedId by viewModel.expandedProductId.collectAsStateWithLifecycle()
    val isCompact = LocalBackButtonVisibility.current
    val productType = sessionManager.activeProductType
    val icon = ProjectLogos.getProductIcon(productType)

    val isExpanded = currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp > 600

    AppScaffold(
        title = "Products",
        onBack = if (isExpanded) null else onBack,
        actions = {
            AnimatedVisibility(isExpanded) {
                Button(
                    onClick = onNewProduct,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        AppIcons.Add,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("New Product")
                }
            }
        },
        supportTitle = null,
        supportContent = {
            DetailsPlaceholder(
                icon = icon,
                title = "Product Catalog",
                description = "Products are Business scoped, all stores will see the same catalog",
            )
        },
        floatingActionButton = {
            AnimatedVisibility(isExpanded.not()) {
                FloatingActionButton(onClick = onNewProduct) {
                    Icon(AppIcons.Add, null)
                }
            }
        }
    ) { padding ->
        UiResultGridView(
            state = itemsState,
            columns = GridCells.Adaptive(380.dp),
            emptyPlaceholder = {
                DetailsPlaceholder(
                    icon = icon,
                    title = "No products found",
                    description = "Create a new product to get started.",
                    actionContent = {
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onNewProduct) {
                            Text("Create New Product")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(padding)
                )
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { products ->
            items(products, key = { it.id }) { product ->
                val isSelected = product.id == selectedItem?.id
                ProductListItemCard(
                    item = product,
                    type = sessionManager.activeProductType,
                    isSelected = isSelected && !isCompact,
                    isExpanded = expandedId == product.id,
                    onClick = {
                        viewModel.onSelect(product)
                        onProductClick(product.id)
                    },
                    onExpandClick = { viewModel.toggleExpand(product.id) },
                    onInfoClick = { onMetadataClick(product.id) }
                )
            }
        }
    }
}

@Composable
private fun ProductListItemCard(
    item: ProductListItemView,
    type: ProductType,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onExpandClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        isSelected = isSelected
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact Product Visual Icon
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ProjectLogos.ProductIcon(
                            type = type.name,
                            size = 24.dp,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Primary Column: Product Name + Stock Badge, then SKU + Brand + Unit
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        StockStatusBadge(
                            count = item.totalAvailable.toInt(),
                            reorderLevel = item.reorderLevel
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!item.sku.isNullOrBlank()) {
                            Text(
                                text = "SKU: ${item.sku}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (item.brandName.isNotBlank() && !item.brandName.equals(
                                "Generic",
                                ignoreCase = true
                            )
                        ) {
                            Text(
                                text = "• Brand: ${item.brandName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (item.unitOfMeasure.isNotBlank()) {
                            Text(
                                text = "• ${item.unitOfMeasure}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                // Action Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Info,
                            contentDescription = "Metadata",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onExpandClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoTag(label = "SKU", value = item.sku)
                        InfoTag(label = "Barcode", value = item.barcode)
                        InfoTag(label = "Unit", value = item.unitOfMeasure)
                        InfoTag(label = "Reorder At", value = item.reorderLevel.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTag(
    label: String,
    value: String?,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    if (value.isNullOrBlank()) return

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StockStatusBadge(
    count: Int,
    reorderLevel: Int,
) {
    val (color, label) = when {
        count <= 0 -> MaterialTheme.colorScheme.error to "Out of Stock"
        count <= reorderLevel -> Color(0xFFF44336) to "Low Stock: $count"
        else -> MaterialTheme.colorScheme.primary to "$count Available"
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
