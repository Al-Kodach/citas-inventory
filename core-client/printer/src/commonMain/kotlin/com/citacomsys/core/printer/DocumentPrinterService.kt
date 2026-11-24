package com.citacomsys.core.printer

interface DocumentPrinterService {
    /**
     * Prints a Sales / POS Receipt onto thermal paper (e.g. Receipt80mm, Receipt58mm) or standard page.
     */
    suspend fun printReceipt(
        job: ReceiptPrintJob,
        format: PrintPageFormat = PrintPageFormat.Receipt80mm(),
        silent: Boolean = false
    ): Result<Unit>

    /**
     * Prints a business / audit Report onto full page paper (e.g. A4, Letter).
     */
    suspend fun printReport(
        job: ReportPrintJob,
        format: PrintPageFormat = PrintPageFormat.A4
    ): Result<Unit>

    /**
     * Prints a barcode price tag or product sticker label.
     */
    suspend fun printLabel(
        job: BarcodeLabelPrintJob,
        format: PrintPageFormat = PrintPageFormat.CustomLabel()
    ): Result<Unit>

    /**
     * Sends pre-generated PDF bytes directly to the platform print spooler or device hardware printer.
     */
    suspend fun printPdfBytes(
        pdfData: ByteArray,
        documentName: String,
        format: PrintPageFormat = PrintPageFormat.A4
    ): Result<Unit>

    /**
     * Generates a formatted PDF document byte array for a POS Sales Receipt.
     */
    suspend fun generateReceiptPdf(
        job: ReceiptPrintJob,
        format: PrintPageFormat = PrintPageFormat.Receipt80mm()
    ): Result<ByteArray>

    /**
     * Generates a formatted PDF document byte array for a business Report.
     */
    suspend fun generateReportPdf(
        job: ReportPrintJob,
        format: PrintPageFormat = PrintPageFormat.A4
    ): Result<ByteArray>
}
