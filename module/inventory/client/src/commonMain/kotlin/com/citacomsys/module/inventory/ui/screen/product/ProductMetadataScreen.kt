package com.citacomsys.module.inventory.ui.screen.product

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.components.ConfirmationDialog
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.ProductExtensionViewRegistry
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.isError
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.core.ui.sudo.ActorSudoAuthDialogs
import com.citacomsys.database.ProductListItemView
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.Money
import com.design.components.AppListView
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProductMetadataScreen(
    productId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNavigateToStocks: () -> Unit,
    viewModel: ProductMetadataViewModel = koinInject(),
    sessionManager: AppSessionManager = koinInject()
) {
    val productResult by viewModel.productState.collectAsStateWithLifecycle()
    val productInfo by viewModel.productLiveView.collectAsStateWithLifecycle()
    val deleteState by viewModel.actionResult.collectAsStateWithLifecycle()
    val showDeleteConfirmDialog = remember { mutableStateOf(false) }

    val activeProductType = sessionManager.activeProductType

    LaunchedEffect(Unit) {
        viewModel.selected(productId)
    }

    LaunchedEffect(deleteState) {
        if (deleteState.isSuccess) {
            onBack()
        }
    }

    UiResultView(productResult) { product ->
        AppListView(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            //  INTEGRATED HERO HEADER
            item {
                ProductHeroHeader(
                    product = product,
                    deleteState = deleteState,
                    liveInfo = productInfo,
                    onBack = onBack,
                    onEdit = onEdit,
                    onDelete = { showDeleteConfirmDialog.value = true }
                )
            }

            // PRIMARY ACTION (Transition to Management)
            item {
                Button(
                    onClick = onNavigateToStocks,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(AppIcons.Layers, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Manage Inventory & Batches", fontWeight = FontWeight.Bold)
                }
            }

            // PRODUCT CATALOG DETAILS
            item {
                CatalogDetailsSection(product)
            }

            // DYNAMIC INJECTION POINT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        ProductExtensionViewRegistry.ExtensionContent(product)
                    }
                }
            }

            if (deleteState.isError) {
                item {
                    UiResultView(deleteState)
                }
            }
        }

        ActorSudoAuthDialogs(sudoManager = viewModel.sudoHandler)
    }

    if (showDeleteConfirmDialog.value) {
        ConfirmationDialog(
            title = "Warning: Product deletion is a global workspace-wide action. All stock batches and history for this item across ALL branches will be permanently erased.",
            showDialog = showDeleteConfirmDialog,
            onDismiss = { showDeleteConfirmDialog.value = false },
            onConfirmationClick = {
                showDeleteConfirmDialog.value = false
                viewModel.delete()
            }
        )
    }
}

@Composable
private fun ProductHeroHeader(
    product: BaseProduct,
    deleteState: UiResult<Unit>,
    liveInfo: ProductListItemView?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(20.dp)) {
            // Top Row: Navigation and Actions
            FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(AppIcons.Edit, null) }

                if (deleteState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            AppIcons.Delete,
                            null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Middle Row: Identity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppIcons.Inventory,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(product.isActive)
                    }
                    if (product.brandName.isNotBlank()) {
                        Text(
                            product.brandName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Bottom Section: Live Metrics "Context"
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroMetricBox(
                    modifier = Modifier.weight(1f),
                    label = "In Stock",
                    value = liveInfo?.totalAvailable?.toString() ?: "...",
                    color = if ((liveInfo?.totalAvailable ?: 0) <= (product.reorderLevel ?: 0))
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                HeroMetricBox(
                    modifier = Modifier.weight(1f),
                    label = "Unit Price",
                    value = liveInfo?.currentPriceAmount?.let {
                        Money(
                            it,
                            liveInfo.currentPriceCurrency
                        ).formatted
                    } ?: "---",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}


@Composable
private fun HeroMetricBox(modifier: Modifier, label: String, value: String, color: Color) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    val color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            if (isActive) "ACTIVE" else "INACTIVE",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun CatalogDetailsSection(product: BaseProduct) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Product Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Core Identification
            DetailRow("SKU", product.sku ?: "No SKU Assigned")
            DetailRow("Barcode", product.barcode ?: "No Barcode")

            HorizontalDivider(modifier = Modifier.alpha(0.3f))

            // Classification and Logic context
            DetailRow("Category", product.category ?: "Uncategorized")

            // Critical for inventory context: "Box", "Tablet", "Bottle"
            DetailRow("Unit of Measure", product.unitOfMeasure)

            // Reorder level for admin visibility
            DetailRow("Reorder Threshold", "${product.reorderLevel} ${product.unitOfMeasure}")

            // Narrative Description
            if (!product.description.isNullOrBlank()) {
                HorizontalDivider(modifier = Modifier.alpha(0.3f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Product Notes / Description",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = product.description!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}


@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
