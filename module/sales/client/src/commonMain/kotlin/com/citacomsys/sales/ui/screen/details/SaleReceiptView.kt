package com.citacomsys.sales.ui.screen.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.api.sales.SaleDetails
import kotlin.text.takeLast
import kotlin.text.uppercase

@Composable
fun SaleReceiptView(
    details: SaleDetails,
    modifier: Modifier = Modifier
) {
    // Thermal printers usually have specific widths (58mm ~ 384px, 80mm ~ 576px)
    Column(
        modifier = modifier
            .width(384.dp)
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header
        Text(
            text = "PHARMACY RECEIPT",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(details.sale.id.takeLast(8).uppercase(), style = MaterialTheme.typography.labelSmall)

        Spacer(Modifier.height(8.dp))
        Text(
            details.sale.createdAt.toString().replace("T", " ").split(".").first(),
            style = MaterialTheme.typography.bodySmall
        )

        DashedDivider()

        // 2. Items
        details.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${item.quantity} x ${item.unitPrice.amount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    item.totalPrice.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        DashedDivider()

        // 3. Totals
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "TOTAL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                details.sale.totalAmount.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("Thank you for your business!", style = MaterialTheme.typography.bodySmall)
        Text("Items sold are not returnable.", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DashedDivider() {
    // Simple implementation of a dashed line for thermal aesthetics
    Text(
        "-".repeat(40),
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}