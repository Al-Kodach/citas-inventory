package com.citacomsys.module.inventory.ui.screen.batch

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.core.ui.result.getOrEmpty
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.module.inventory.nav.EditBatchNav
import com.citacomsys.module.inventory.nav.StockTransferNavKey
import com.citacomsys.module.inventory.request.PriceType
import com.citacomsys.types.BatchItem
import com.citacomsys.types.Money
import com.citacomsys.types.StockBatch
import com.citacomsys.types.StockMovement
import com.citacomsys.types.StockMovementType
import com.design.components.AppListView
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

enum class DetailsViewMode {
    DETAILS,
    EDIT_PRICE,
    UPDATE_QUANTITY
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatchDetailsScreen(
    productId: String,
    batchId: String,
    onBack: () -> Unit,
    viewModel: BatchDetailsViewModel = koinViewModel()
) {
    val batchDetailState by viewModel.batchDetails.collectAsStateWithLifecycle()
    val movementsState by viewModel.movements.collectAsStateWithLifecycle()
    val isSyncingCloud by viewModel.isSyncingCloudMovements.collectAsStateWithLifecycle()
    val canViewCostPrice = viewModel.canViewCostPrice

    val navigator = LocalNavigator.current
    var currentView by remember { mutableStateOf(DetailsViewMode.DETAILS) }
    var batchToAdjust by remember { mutableStateOf<StockBatch?>(null) }
    var priceType by remember { mutableStateOf(PriceType.SELLING) }
    LaunchedEffect(productId, batchId) {
        viewModel.load(productId, batchId)
    }

    val currentOnBack: () -> Unit = {
        if (currentView != DetailsViewMode.DETAILS) {
            currentView = DetailsViewMode.DETAILS
        } else {
            onBack()
        }
    }

    val title = when (currentView) {
        DetailsViewMode.DETAILS -> "Batch Details"
        DetailsViewMode.EDIT_PRICE -> if (priceType == PriceType.COST) "Update Cost Price" else "Update Selling Price"
        DetailsViewMode.UPDATE_QUANTITY -> "Stock Adjustment"
    }

    AppScaffold(
        title = title,
        onBack = currentOnBack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentView,
                modifier = Modifier.fillMaxSize(),
                label = "DetailsViewModeTransition"
            ) { mode ->
                when (mode) {
                    DetailsViewMode.DETAILS -> {
                        UiResultView(
                            state = batchDetailState,
                            onRetry = { viewModel.load(productId, batchId) },
                            modifier = Modifier.fillMaxSize()
                        ) { batch ->
                            AppListView(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                item {
                                    // Full Batch Details Card
                                    StockBatchCard(
                                        batch = batch,
                                        showActions = true,
                                        canViewCostPrice = viewModel.canViewCostPrice,
                                        canAdjustStock = viewModel.canAdjustStock,
                                        canEditPrice = viewModel.canEditPrice,
                                        onAdjust = {
                                            batchToAdjust = batch
                                            currentView = DetailsViewMode.UPDATE_QUANTITY
                                        },
                                        onEdit = {
                                            navigator.navigateToSingleTop(
                                                EditBatchNav(
                                                    productId,
                                                    batch.id
                                                )
                                            )
                                        },
                                        onDelete = {
                                            viewModel.deleteBatch(batch.id)
                                            onBack()
                                        },
                                        onUpdateCostPrice = {
                                            batchToAdjust = batch
                                            priceType = PriceType.COST
                                            currentView = DetailsViewMode.EDIT_PRICE
                                        },
                                        onUpdateSellingPrice = {
                                            batchToAdjust = batch
                                            priceType = PriceType.SELLING
                                            currentView = DetailsViewMode.EDIT_PRICE
                                        },
                                        onTransfer = {
                                            navigator.navigateToSingleTop(
                                                StockTransferNavKey(
                                                    productId,
                                                    batch.id
                                                )
                                            )
                                        }
                                    )
                                }

                                item {
                                    // Movement History Section Header with "Load All from Cloud" button
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        itemVerticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Movement History",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Button(
                                            onClick = { viewModel.syncCloudMovements() },
                                            enabled = !isSyncingCloud,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            if (isSyncingCloud) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = AppIcons.CloudUpload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (isSyncingCloud) "Loading..." else "Load All",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }

                                if (movementsState.isSuccess.not()) {
                                    item {
                                        UiResultView(
                                            state = movementsState,
                                            onRetry = viewModel::syncCloudMovements,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Movements List for Selected Batch
                                if (movementsState.isEmpty) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No local stock movements for this batch.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(movementsState.getOrEmpty()) { movement ->
                                        StockMovementHistoryRow(movement)
                                        HorizontalDivider(
                                            modifier = Modifier.padding(
                                                top = 12.dp
                                            )
                                        )
                                    }
                                }
                            }

                        }
                    }

                    DetailsViewMode.EDIT_PRICE -> {
                        val activeBatch = batchToAdjust
                        if (activeBatch != null) {
                            PriceUpdateFormView(
                                batchId = activeBatch.id,
                                priceType = priceType,
                                onCancel = { currentView = DetailsViewMode.DETAILS }
                            )
                        } else {
                            currentView = DetailsViewMode.DETAILS
                        }
                    }

                    DetailsViewMode.UPDATE_QUANTITY -> {
                        val activeBatch = batchToAdjust
                        if (activeBatch != null) {
                            AdjustQuantityStock(
                                currentQuantity = activeBatch.currentQuantity,
                                onDismiss = { currentView = DetailsViewMode.DETAILS },
                                onConfirm = { quantity, reason ->
                                    viewModel.adjust(activeBatch.id, quantity, reason)
                                    currentView = DetailsViewMode.DETAILS
                                }
                            )
                        } else {
                            currentView = DetailsViewMode.DETAILS
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockBatchCard(
    batch: StockBatch,
    showActions: Boolean = true,
    canViewCostPrice: Boolean = true,
    canAdjustStock: Boolean = true,
    canEditPrice: Boolean = true,
    onAdjust: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onUpdateCostPrice: () -> Unit = {},
    onUpdateSellingPrice: () -> Unit = {},
    onTransfer: () -> Unit = {}
) {
    val isLow = batch.currentQuantity <= 0
//    val exp = batch.daysUntilExpiry?.toInt()
//    val isNearExpiry = (exp ?: 999) <= 30
    val isStub = batch.id == batch.productId

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isStub) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = if (isStub) "NO ACTIVE BATCH (STUB)" else "Batch: ${batch.batchNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isStub) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    if (!isStub) {
//                        Row {
//                            Text(
//                                text = "Expires: ${
//                                    batch.expiryDate?.formatted() ?: "Perpetual"
//                                }",
//                                style = MaterialTheme.typography.bodySmall,
//                                color = if (isNearExpiry) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            Text(
//                                text = exp?.let { " | $it days left" } ?: "",
//                                style = MaterialTheme.typography.bodySmall,
//                                color = if (isNearExpiry) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
                    } else {
                        DetailsPlaceholder(
                            icon = AppIcons.Inventory,
                            title = "No Active Batch",
                            description = "Add a new batch to get started."
                        )
                    }
                }

                if (!isStub && showActions && canAdjustStock) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTransfer,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(AppIcons.History, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("TRANSFER", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = onAdjust,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(AppIcons.Sync, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ADJUST", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Metrics Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showActions) {
                    MetricItem(
                        label = "Initial Stock",
                        value = "${batch.initialQuantity}",
                        valueColor = if (isStub) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                MetricItem(
                    label = "Current stock",
                    value = "${batch.currentQuantity}",
                    valueColor = if (isLow && !isStub) Color.Red else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (!showActions) {
                    MetricItem(
                        label = "Selling Price",
                        value = batch.sellingPrice.formatted,
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!isStub && showActions) {
                Spacer(Modifier.height(16.dp))

                // Interactive Price Section
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cost Price interactive card (Guarded by canViewCostPrice)
                    if (canViewCostPrice) {
                        Surface(
                            onClick = onUpdateCostPrice,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Cost Price",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        batch.costPrice.formatted,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Icon(AppIcons.Edit, null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Selling Price interactive card
                    Surface(
                        onClick = if (canEditPrice) onUpdateSellingPrice else {
                            {}
                        },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Selling Price",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    batch.sellingPrice.formatted,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (canEditPrice) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        AppIcons.Edit,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Management Row (Metadata & Lifecycle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(AppIcons.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("EDIT METADATA", style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(AppIcons.Delete, "Delete Batch")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockBatchCard(
    batch: BatchItem,
    showActions: Boolean = true,
    canViewCostPrice: Boolean = true,
    canAdjustStock: Boolean = true,
    canEditPrice: Boolean = true,
    onAdjust: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onUpdateCostPrice: () -> Unit = {},
    onUpdateSellingPrice: () -> Unit = {},
    onTransfer: () -> Unit = {}
) {
    val isLow = batch.availableQuantity <= 0
//    val exp = batch.daysUntilExpiry?.toInt()
//    val isNearExpiry = (exp ?: 999) <= 30
    val isStub = batch.batchId == batch.productId

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isStub) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = if (isStub) "NO ACTIVE BATCH (STUB)" else "Batch: ${batch.batchNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isStub) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                    if (!isStub) {
//                        Row {
//                            Text(
//                                text = "Expires: ${
//                                    batch.expiryDate?.formatted() ?: "Perpetual"
//                                }",
//                                style = MaterialTheme.typography.bodySmall,
//                                color = if (isNearExpiry) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            Text(
//                                text = exp?.let { " | $it days left" } ?: "",
//                                style = MaterialTheme.typography.bodySmall,
//                                color = if (isNearExpiry) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
                    } else {
                        DetailsPlaceholder(
                            icon = AppIcons.Inventory,
                            title = "No Active Batch",
                            description = "Add a new batch to get started."
                        )
                    }
                }

                if (!isStub && showActions && canAdjustStock) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTransfer,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(AppIcons.History, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("TRANSFER", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = onAdjust,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(AppIcons.Sync, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ADJUST", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Metrics Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricItem(
                    label = "Current stock",
                    value = "${batch.availableQuantity}",
                    valueColor = if (isLow && !isStub) Color.Red else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (!showActions) {
                    MetricItem(
                        label = "Selling Price",
                        value = Money(batch.sellingPriceAmountMinor).formatted,
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!isStub && showActions) {
                Spacer(Modifier.height(16.dp))

                // Interactive Price Section
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Selling Price interactive card
                    Surface(
                        onClick = if (canEditPrice) onUpdateSellingPrice else {
                            {}
                        },
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Selling Price",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Money(batch.sellingPriceAmountMinor).formatted,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (canEditPrice) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        AppIcons.Edit,
                                        null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Management Row (Metadata & Lifecycle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(AppIcons.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("EDIT METADATA", style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(AppIcons.Delete, "Delete Batch")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockMovementHistoryRow(movement: StockMovement) {
    val color = when (movement.type) {
        StockMovementType.IN -> Color(0xFF4CAF50)
        StockMovementType.OUT -> Color(0xFFF44336)
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.History,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = movement.referenceType?.name?.replace("_", " ")
                            ?: "Manual Adjustment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Batch: ${movement.batchNumber ?: "N/A"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    movement.reason?.let { reasonText ->
                        if (reasonText.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = reasonText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (movement.type == StockMovementType.IN) "+" else "-"}${movement.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    text = movement.createdAt.toString().split("T").first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
