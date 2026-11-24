package com.citacomsys.core.printer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citacomsys.core.printer.PrintPageFormat
import com.citacomsys.core.printer.ReceiptPrintJob

/**
 * A Jetpack Compose Receipt View that renders a receipt on screen or to bitmap for printing.
 * Displays Workspace Header (Name, Phone, Email, Address), Itemized Table, Totals, and Footer.
 */
@Composable
fun ReceiptView(
    job: ReceiptPrintJob,
    format: PrintPageFormat = PrintPageFormat.Receipt80mm(),
    modifier: Modifier = Modifier
) {
    val cardWidth = when (format) {
        is PrintPageFormat.Receipt58mm -> 320.dp
        is PrintPageFormat.Receipt80mm -> 420.dp
        else -> 480.dp
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        tonalElevation = 2.dp,
        modifier = modifier.width(cardWidth)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- HEADER: Workspace Info ---
            Text(
                text = job.storeName.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            job.branchName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }

            job.storeAddress?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }

            val contactInfo = listOfNotNull(
                job.phone?.let { "Tel: $it" },
                job.email
            ).joinToString(" | ")

            if (contactInfo.isNotBlank()) {
                Text(
                    text = contactInfo,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }

            DashedDivider()

            // --- RECEIPT METADATA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Receipt #: ${job.receiptNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Black
                )
                Text(
                    text = job.dateTime,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.DarkGray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cashier: ${job.cashierName}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.DarkGray
                )
                job.customerName?.let {
                    Text(
                        text = "Customer: $it",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.DarkGray
                    )
                }
            }

            DashedDivider()

            // --- ITEMS TABLE ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ITEM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.weight(2f),
                    color = Color.Black
                )
                Text(
                    text = "QTY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
                Text(
                    text = "PRICE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    color = Color.Black
                )
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    color = Color.Black
                )
            }

            DashedDivider()

            for (item in job.items) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.weight(2f),
                        color = Color.Black
                    )
                    Text(
                        text = item.quantity,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                    Text(
                        text = item.unitPrice,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = Color.Black
                    )
                    Text(
                        text = item.totalPrice,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = Color.Black
                    )
                }
            }

            DashedDivider()

            // --- SUMMARY TOTALS ---
            SummaryRow(label = "Subtotal", value = job.subtotal)
            job.discount?.let { SummaryRow(label = "Discount", value = "-$it") }
            SummaryRow(label = "Tax / VAT", value = job.tax)

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Black
                )
                Text(
                    text = "${job.currencySymbol}${job.total}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.Black
                )
            }

            DashedDivider()

            // --- PAYMENT DETAILS ---
            SummaryRow(label = "Payment Method", value = job.paymentMethod)
            job.amountPaid?.let { SummaryRow(label = "Amount Paid", value = it) }
            job.changeDue?.let { SummaryRow(label = "Change Due", value = it) }

            DashedDivider()

            // --- FOOTER: Contact & Thank You ---
            Text(
                text = job.footerMessage,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            job.website?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = Color.DarkGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.Black
        )
    }
}

@Composable
private fun DashedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - -",
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        ),
        color = Color.Gray,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
