package com.citacomsys.core.printer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.citacomsys.core.printer.BarcodeLabelPrintJob
import com.citacomsys.core.printer.PrintPageFormat

@Composable
fun BarcodeLabelPrintDialog(
    productName: String,
    barcodeValue: String,
    priceText: String? = null,
    onDismiss: () -> Unit,
    onPrint: (BarcodeLabelPrintJob, PrintPageFormat, Int) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }
    var showPrice by remember { mutableStateOf(true) }
    var selectedSizeIndex by remember { mutableStateOf(0) } // 0 = 50x25mm, 1 = 40x30mm

    val selectedFormat = if (selectedSizeIndex == 0) {
        PrintPageFormat.CustomLabel(widthMm = 50f, heightMm = 25f)
    } else {
        PrintPageFormat.CustomLabel(widthMm = 40f, heightMm = 30f)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(420.dp)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Print Barcode Price Tag",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Label Sticker Live Preview Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = productName.take(32),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Text(
                            text = "||| |||| || ||||| |||| |||",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            ),
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = barcodeValue,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = Color.Black
                        )

                        if (showPrice && !priceText.isNullOrBlank()) {
                            Text(
                                text = priceText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                ),
                                color = Color.Black
                            )
                        }
                    }
                }

                // Size Selector Tabs
                TabRow(selectedTabIndex = selectedSizeIndex) {
                    Tab(
                        selected = selectedSizeIndex == 0,
                        onClick = { selectedSizeIndex = 0 },
                        text = { Text("50x25 mm") }
                    )
                    Tab(
                        selected = selectedSizeIndex == 1,
                        onClick = { selectedSizeIndex = 1 },
                        text = { Text("40x30 mm") }
                    )
                }

                // Quantity & Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) quantity = input
                        },
                        label = { Text("Label Copies") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Checkbox(
                            checked = showPrice,
                            onCheckedChange = { showPrice = it }
                        )
                        Text(
                            text = "Show Price",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Actions
                Button(
                    onClick = {
                        val job = BarcodeLabelPrintJob(
                            title = productName,
                            barcodeValue = barcodeValue,
                            priceText = if (showPrice) priceText else null
                        )
                        val qty = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        onPrint(job, selectedFormat, qty)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Print Barcode Tags")
                }
            }
        }
    }
}
