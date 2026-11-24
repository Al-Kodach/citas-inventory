package com.citacomsys.core.ui.productPackage

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citacomsys.core.components.AppCard
import com.citacomsys.types.PriceDiscount
import com.citacomsys.types.PriceTax
import com.citacomsys.types.ResourcePriceBreakdown

/**
 * An upgraded component for displaying a detailed breakdown of costs.
 * It now explicitly handles Line Items, Discounts, and Taxes.
 */
@Composable
fun ResourcePriceBreakdownCard(
    modifier: Modifier = Modifier,
    resourcePriceBreakdown: ResourcePriceBreakdown,
    discounts: List<PriceDiscount> = emptyList(),
    taxes: List<PriceTax> = emptyList(),
    title: String? = "Invoice Summary",
    isStandalone: Boolean = true
) {
    val content = @Composable {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (title != null) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(4.dp))
            }

            // 1. Individual Line Items (Base Plan + Extras)
            resourcePriceBreakdown.lineItems.forEach { lineItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lineItem.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (lineItem.quantity > 0) {
                            Text(
                                text = "${lineItem.quantity} × ${lineItem.unitPriceAmount.formatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = lineItem.totalAmount.formatted,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // 2. Subtotal
            PriceSummaryRow(
                label = "Subtotal",
                amount = resourcePriceBreakdown.subtotal.formatted,
                isBold = true
            )

            // 3. Discounts (Marketing value highlight)
            discounts.forEach { discount ->
                PriceSummaryRow(
                    label = discount.description,
                    amount = "- ${discount.amount.formatted}",
                    color = MaterialTheme.colorScheme.error,
                    isBold = true
                )
            }

            // 4. Taxes (The "Tasks" - VAT, NHIL, etc)
            taxes.forEach { tax ->
                PriceSummaryRow(
                    label = tax.name,
                    amount = tax.amount.formatted
                )
            }

            Spacer(Modifier.height(8.dp))

            // 5. Grand Total Highlight
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Billing Amount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = resourcePriceBreakdown.total.formatted,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (isStandalone) {
        AppCard(modifier = modifier) { content() }
    } else {
        Column(modifier = modifier) { content() }
    }
}

@Composable
private fun PriceSummaryRow(
    label: String,
    amount: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}