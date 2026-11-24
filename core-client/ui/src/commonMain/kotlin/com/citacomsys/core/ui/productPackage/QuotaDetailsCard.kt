package com.citacomsys.core.ui.productPackage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.AppCard
import com.citacomsys.types.PriceDiscount
import com.citacomsys.types.PriceTax
import com.citacomsys.types.ResourcePriceBreakdown

@Composable
fun QuotaDetailsCard(
    title: String,
    subtitle: String,
    resourcePriceBreakdown: ResourcePriceBreakdown,
    modifier: Modifier = Modifier,
    discounts: List<PriceDiscount> = emptyList(),
    taxes: List<PriceTax> = emptyList(),
    quotas: List<QuotaItem> = emptyList(),
    showPriceBreakdown: Boolean = true
) {
    AppCard(
        modifier = modifier,
        isSelected = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (quotas.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.1f))

                // Quota Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    quotas.forEach { quota ->
                        QuotaView(quota)
                    }
                }
            }

            if (showPriceBreakdown) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.1f))

                ResourcePriceBreakdownCard(
                    resourcePriceBreakdown = resourcePriceBreakdown,
                    discounts = discounts,
                    taxes = taxes,
                    title = "Price Breakdown",
                    isStandalone = false
                )
            }
        }
    }
}

data class QuotaItem(
    val label: String,
    val value: String,
    val icon: ImageVector? = null
)

@Composable
private fun QuotaView(item: QuotaItem) {
    Column {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            item.icon?.let {
                Icon(
                    it,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
