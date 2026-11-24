package com.citacomsys.core.printer

import kotlinx.serialization.Serializable

@Serializable
data class ReceiptPrintJob(
    val storeName: String,
    val branchName: String? = null,
    val storeAddress: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val receiptNumber: String,
    val dateTime: String,
    val cashierName: String,
    val customerName: String? = null,
    val items: List<ReceiptItem>,
    val subtotal: String,
    val tax: String,
    val discount: String? = null,
    val total: String,
    val paymentMethod: String,
    val amountPaid: String? = null,
    val changeDue: String? = null,
    val currencySymbol: String = "$",
    val barcodeOrQr: String? = null,
    val footerMessage: String = "Thank you for shopping with us!"
)

@Serializable
data class ReceiptItem(
    val name: String,
    val quantity: String,
    val unitPrice: String,
    val totalPrice: String
)

@Serializable
data class ReportPrintJob(
    val title: String,
    val subtitle: String? = null,
    val generatedAt: String,
    val generatedBy: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val summaryFields: Map<String, String> = emptyMap()
)

@Serializable
data class BarcodeLabelPrintJob(
    val title: String,
    val barcodeValue: String,
    val priceText: String? = null,
    val subtitleText: String? = null
)
