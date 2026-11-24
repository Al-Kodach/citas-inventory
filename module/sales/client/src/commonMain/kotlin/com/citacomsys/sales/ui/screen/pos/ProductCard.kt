package com.citacomsys.sales.ui.screen.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.database.ProductListItemView
import com.citacomsys.types.Money
import io.github.aakira.napier.Napier

@Composable
fun ProductCard(
    product: ProductListItemView,
    inCartQuantity: Int,
    onAdd: () -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.totalAvailable <= 0
    val isLowStock = product.totalAvailable <= 3 && !isOutOfStock
    val enabled = inCartQuantity < product.totalAvailable && product.totalAvailable > 0

    Napier.d { "${product.name} in cart: $inCartQuantity, Available: ${product.totalAvailable}" }

    AppCard(
        onClick = if (enabled) onAdd else null,
        modifier = modifier.fillMaxWidth(),
        isSelected = inCartQuantity > 0,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getUomIcon(product.unitOfMeasure),
                    contentDescription = null,
                    tint = if (isOutOfStock) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOutOfStock) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOfNotNull(
                        product.sku?.takeIf { it.isNotBlank() }?.let { "SKU: $it" },
                        product.brandName.takeIf { it.isNotBlank() },
                        product.unitOfMeasure
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = Money(product.currentPriceAmount, product.currentPriceCurrency).formatted,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isOutOfStock) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (inCartQuantity > 0 && onRemove != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(onClick = onRemove) {
                            Icon(
                                AppIcons.Remove,
                                contentDescription = "Remove item",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "$inCartQuantity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = onAdd,
                            enabled = enabled
                        ) {
                            Icon(
                                AppIcons.Add,
                                contentDescription = "Add item",
                                modifier = Modifier.size(20.dp),
                                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onAdd,
                        enabled = enabled
                    ) {
                        Icon(
                            AppIcons.Add,
                            contentDescription = "Add to Cart",
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (isOutOfStock) "OUT" else "${product.totalAvailable} left",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isOutOfStock -> MaterialTheme.colorScheme.error
                        isLowStock -> Color(0xFFF57C00)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun SaleSuccessDialog(
    saleId: String,
    onNewSale: () -> Unit,
    onViewDetails: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onNewSale,
        icon = {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Sale Completed!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Transaction recorded successfully with Receipt ID #${saleId.takeLast(8).uppercase()}.")
        },
        confirmButton = {
            Button(onClick = onNewSale) {
                Text("New Sale")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onViewDetails) {
                Text("View Details")
            }
        }
    )
}

private fun getUomIcon(uom: String): ImageVector {
    return when (uom.lowercase()) {
        "box", "boxes" -> AppIcons.Inventory2
        "piece", "pcs", "pack" -> AppIcons.Category
        else -> AppIcons.Inventory2
    }
}
