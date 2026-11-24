package com.citacomsys.core.printer

import com.citacomsys.device.service.DeviceService
import com.citacomsys.device.service.PdfLineInfo
import com.citacomsys.device.service.ValueType
import io.github.aakira.napier.Napier

class DefaultDocumentPrinterService(
    private val deviceService: DeviceService
) : DocumentPrinterService {

    override suspend fun printReceipt(
        job: ReceiptPrintJob,
        format: PrintPageFormat,
        silent: Boolean
    ): Result<Unit> = runCatching {
        Napier.d(
            "Printing POS receipt '${job.receiptNumber}' with format ${format.name} (silent=$silent)",
            tag = "DocumentPrinterService"
        )
        val lines = buildReceiptLineInfos(job, format)
        val pdfBytes = deviceService.generateContentPdf(lines)
        deviceService.print(pdfBytes, "receipt-${job.receiptNumber}.pdf", silent = silent)
    }

    override suspend fun printReport(
        job: ReportPrintJob,
        format: PrintPageFormat
    ): Result<Unit> = runCatching {
        Napier.d(
            "Printing report '${job.title}' with format ${format.name}",
            tag = "DocumentPrinterService"
        )
        val lines = buildReportLineInfos(job, format)
        val pdfBytes = deviceService.generateContentPdf(lines)
        deviceService.print(pdfBytes, "report-${job.title.lowercase().replace(" ", "-")}.pdf")
    }

    override suspend fun printLabel(
        job: BarcodeLabelPrintJob,
        format: PrintPageFormat
    ): Result<Unit> = runCatching {
        Napier.d(
            "Printing barcode label '${job.title}' with format ${format.name}",
            tag = "DocumentPrinterService"
        )
        val lines = listOfNotNull(
            PdfLineInfo(job.title, ValueType.FONT_BOLD, 0, 0),
            PdfLineInfo("BARCODE: ${job.barcodeValue}", ValueType.TEXT, 0, 0),
            job.priceText?.let { PdfLineInfo("PRICE: $it", ValueType.FONT_BOLD, 0, 0) },
            job.subtitleText?.let { PdfLineInfo(it, ValueType.TEXT, 0, 0) }
        )
        val pdfBytes = deviceService.generateContentPdf(lines)
        deviceService.print(pdfBytes, "label-${job.barcodeValue}.pdf")
    }

    override suspend fun printPdfBytes(
        pdfData: ByteArray,
        documentName: String,
        format: PrintPageFormat
    ): Result<Unit> = runCatching {
        deviceService.print(pdfData, "$documentName.pdf")
    }

    override suspend fun generateReceiptPdf(
        job: ReceiptPrintJob,
        format: PrintPageFormat
    ): Result<ByteArray> = runCatching {
        val lines = buildReceiptLineInfos(job, format)
        deviceService.generateContentPdf(lines)
    }

    override suspend fun generateReportPdf(
        job: ReportPrintJob,
        format: PrintPageFormat
    ): Result<ByteArray> = runCatching {
        val lines = buildReportLineInfos(job, format)
        deviceService.generateContentPdf(lines)
    }

    private fun buildReceiptLineInfos(
        job: ReceiptPrintJob,
        format: PrintPageFormat
    ): List<PdfLineInfo> {
        val lines = mutableListOf<PdfLineInfo>()

        // Store Header
        lines.add(PdfLineInfo(job.storeName, ValueType.FONT_BOLD, 0, 0))
        job.branchName?.let { lines.add(PdfLineInfo(it, ValueType.TEXT, 0, 0)) }
        job.storeAddress?.let { lines.add(PdfLineInfo(it, ValueType.TEXT, 0, 0)) }
        job.phone?.let { lines.add(PdfLineInfo("Tel: $it", ValueType.TEXT, 0, 0)) }
        lines.add(PdfLineInfo("----------------------------------------", ValueType.TEXT, 0, 0))

        // Receipt Meta
        lines.add(PdfLineInfo("Receipt #: ${job.receiptNumber}", ValueType.TEXT, 0, 0))
        lines.add(PdfLineInfo("Date: ${job.dateTime}", ValueType.TEXT, 0, 0))
        lines.add(PdfLineInfo("Cashier: ${job.cashierName}", ValueType.TEXT, 0, 0))
        job.customerName?.let { lines.add(PdfLineInfo("Customer: $it", ValueType.TEXT, 0, 0)) }
        lines.add(PdfLineInfo("----------------------------------------", ValueType.TEXT, 0, 0))

        // Items Header
        lines.add(PdfLineInfo("ITEM             QTY    PRICE    TOTAL", ValueType.FONT_BOLD, 0, 0))
        lines.add(PdfLineInfo("----------------------------------------", ValueType.TEXT, 0, 0))

        // Items
        for (item in job.items) {
            lines.add(PdfLineInfo(item.name, ValueType.FONT_BOLD, 0, 0))
            lines.add(
                PdfLineInfo(
                    "  x${item.quantity} @ ${item.unitPrice} = ${item.totalPrice}",
                    ValueType.TEXT,
                    0,
                    0
                )
            )
        }
        lines.add(PdfLineInfo("----------------------------------------", ValueType.TEXT, 0, 0))

        // Totals & Payment
        lines.add(PdfLineInfo("Subtotal: ${job.subtotal}", ValueType.TEXT, 0, 0))
        job.discount?.let { lines.add(PdfLineInfo("Discount: $it", ValueType.TEXT, 0, 0)) }
        lines.add(PdfLineInfo("Tax: ${job.tax}", ValueType.TEXT, 0, 0))
        lines.add(PdfLineInfo("TOTAL: ${job.total}", ValueType.FONT_BOLD, 0, 0))
        lines.add(PdfLineInfo("Payment Method: ${job.paymentMethod}", ValueType.TEXT, 0, 0))
        job.amountPaid?.let { lines.add(PdfLineInfo("Amount Paid: $it", ValueType.TEXT, 0, 0)) }
        job.changeDue?.let { lines.add(PdfLineInfo("Change: $it", ValueType.TEXT, 0, 0)) }

        // Footer & QR/Barcode
        lines.add(PdfLineInfo("----------------------------------------", ValueType.TEXT, 0, 0))
        job.barcodeOrQr?.let { lines.add(PdfLineInfo("BARCODE: $it", ValueType.TEXT, 0, 0)) }
        lines.add(PdfLineInfo(job.footerMessage, ValueType.TEXT, 0, 0))

        return lines
    }

    private fun buildReportLineInfos(
        job: ReportPrintJob,
        format: PrintPageFormat
    ): List<PdfLineInfo> {
        val lines = mutableListOf<PdfLineInfo>()

        // Report Header
        lines.add(PdfLineInfo(job.title, ValueType.FONT_BOLD, 0, 0))
        job.subtitle?.let { lines.add(PdfLineInfo(it, ValueType.TEXT, 0, 0)) }
        lines.add(
            PdfLineInfo(
                "Generated: ${job.generatedAt} by ${job.generatedBy}",
                ValueType.TEXT,
                0,
                0
            )
        )
        lines.add(
            PdfLineInfo(
                "Format: ${format.name} (${format.widthMm}x${format.heightMm}mm)",
                ValueType.TEXT,
                0,
                0
            )
        )
        lines.add(
            PdfLineInfo(
                "==================================================",
                ValueType.TEXT,
                0,
                0
            )
        )

        // Table Headers
        if (job.headers.isNotEmpty()) {
            lines.add(PdfLineInfo(job.headers.joinToString(" | "), ValueType.FONT_BOLD, 0, 0))
            lines.add(
                PdfLineInfo(
                    "--------------------------------------------------",
                    ValueType.TEXT,
                    0,
                    0
                )
            )
        }

        // Table Rows
        for (row in job.rows) {
            lines.add(PdfLineInfo(row.joinToString(" | "), ValueType.TEXT, 0, 0))
        }

        // Summary Fields
        if (job.summaryFields.isNotEmpty()) {
            lines.add(
                PdfLineInfo(
                    "==================================================",
                    ValueType.TEXT,
                    0,
                    0
                )
            )
            for ((key, value) in job.summaryFields) {
                lines.add(PdfLineInfo("$key: $value", ValueType.FONT_BOLD, 0, 0))
            }
        }

        return lines
    }
}
