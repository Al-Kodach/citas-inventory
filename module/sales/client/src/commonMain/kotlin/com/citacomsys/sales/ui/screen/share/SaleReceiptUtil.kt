package com.citacomsys.sales.ui.screen.share

import androidx.compose.ui.graphics.ImageBitmap
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.api.sales.service.DigitalReceiptService
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.printer.DocumentPrinterService
import com.citacomsys.core.printer.PrintPageFormat
import com.citacomsys.core.printer.ReceiptItem
import com.citacomsys.core.printer.ReceiptPrintJob
import com.citacomsys.core.util.toImageBitmap
import com.citacomsys.device.service.DeviceService
import com.citacomsys.device.service.PdfLineInfo
import com.citacomsys.device.service.ValueType
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.sales.data.repository.ClientSalesRepository
import com.citacomsys.shared.validation.Validators
import com.citacomsys.shared.validation.validate
import com.citacomsys.types.PhoneNumber
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class SaleReceiptUtil(
    private val receiptService: DigitalReceiptService,
    private val sessionManager: AppSessionManager,
    private val salesRepository: ClientSalesRepository,
    private val service: PlatformClientService,
    private val printerService: DocumentPrinterService,
    private val deviceService: DeviceService
) {
    private val context get() = service.activeWorkspaceConfig.value!!

    private fun validatePhone(phone: String) {
        if (!validate(phone, Validators.required(), Validators.phone()).isValid) {
            error("Invalid username number")
        }
    }

    suspend fun sendDigitalReceipt(
        phone: String,
        saleId: String,
        wsId: String
    ) {
        validatePhone(phone)
        val phoneNumber = PhoneNumber.create(phone)

        if (sessionManager.isSubscribed(wsId)) {
            // Server handles quota/credits check
            receiptService.sendSmsReceipt(saleId, phoneNumber, context.workspaceId).getOrThrow()
        } else {
            // Local generation for offline/standalone mode
            val details = salesRepository.getSaleDetails(saleId, wsId).getOrThrow()
            val textReceipt = buildTextReceipt(details)
            receiptService.sendReceipt(phone, textReceipt, context.workspaceId).getOrThrow()
        }
    }

    private fun buildTextReceipt(details: SaleDetails): String = buildString {
        append("${context.name.uppercase()}\n")
        append("ID: ${details.sale.id.takeLast(8).uppercase()}\n")
        append("Date: ${details.sale.createdAt.toString().split("T").first()}\n\n")

        details.items.forEach { item ->
            append("${item.productName} x ${item.quantity} @ ${item.unitPrice.amount}\n")
        }

        append("\nTotal: ${details.sale.totalAmount.formatted}\n")
        append("Thank you!")
    }

    fun getReceiptPdfContent(
        details: SaleDetails,
        staffName: String
    ): List<PdfLineInfo> = mutableListOf<PdfLineInfo>().apply {
        // Header
        add(PdfLineInfo("18", ValueType.FONT_BOLD))
        add(PdfLineInfo(context.name, ValueType.TEXT))
        add(PdfLineInfo("", ValueType.NEW_LINE, height = 10))

        // Metadata
        add(PdfLineInfo("10", ValueType.FONT_REGULAR))
        add(PdfLineInfo("ID: ${details.sale.id.takeLast(8).uppercase()}", ValueType.TEXT))
        add(PdfLineInfo("Date: ${details.sale.createdAt.formated()}", ValueType.TEXT))
        add(PdfLineInfo("Staff: $staffName", ValueType.TEXT))
        add(PdfLineInfo("-".repeat(40), ValueType.TEXT))

        // Items
        details.items.forEach { item ->
            add(PdfLineInfo("12", ValueType.FONT_BOLD))
            add(PdfLineInfo(item.productName, ValueType.TEXT))
            add(PdfLineInfo("10", ValueType.FONT_REGULAR))
            add(
                PdfLineInfo(
                    "${item.quantity} x ${item.unitPrice.amount} = ${item.totalPrice.amount}",
                    ValueType.TEXT
                )
            )
            add(PdfLineInfo("", ValueType.NEW_LINE, height = 5))
        }

        // Footer
        add(PdfLineInfo("-".repeat(40), ValueType.TEXT))
        add(PdfLineInfo("14", ValueType.FONT_BOLD))
        add(PdfLineInfo("TOTAL: ${details.sale.totalAmount.formatted}", ValueType.TEXT))
        add(PdfLineInfo("", ValueType.NEW_LINE, height = 20))
        add(PdfLineInfo("10", ValueType.FONT_REGULAR))
        add(PdfLineInfo("Thank you for your business!", ValueType.TEXT))
    }

    suspend fun savePdf(pdfByteArray: ByteArray, saleId: String) {
        val fileName = "Receipt_${saleId.takeLast(8).uppercase()}.pdf"
        deviceService.downloadFile(pdfByteArray, fileName)
    }

    suspend fun sharePdf(pdfByteArray: ByteArray, saleId: String) {
        val fileName = "Receipt_${saleId.takeLast(8).uppercase()}.pdf"
        deviceService.downloadFile(pdfByteArray, fileName)
    }

    suspend fun print(pdfByteArray: ByteArray, saleId: String) {
        deviceService.print(pdfByteArray, "${saleId.takeLast(8).uppercase()}.pdf")
    }

    fun buildReceiptPrintJob(
        details: SaleDetails,
        cashierName: String
    ): ReceiptPrintJob {
        val wsInfo = sessionManager.workStationInfo.value
        return ReceiptPrintJob(
            storeName = wsInfo.activeWorkspaceName.ifBlank { context.name },
            branchName = wsInfo.activeBranchName.takeIf { it.isNotBlank() && !it.startsWith("BR-") },
            storeAddress = wsInfo.address,
            phone = wsInfo.phone,
            email = wsInfo.email,
            website = wsInfo.website,
            receiptNumber = details.sale.id.takeLast(8).uppercase(),
            dateTime = details.sale.createdAt.formated(),
            cashierName = cashierName.ifBlank { wsInfo.username.ifBlank { "Cashier" } },
            items = details.items.map { item ->
                ReceiptItem(
                    name = item.productName,
                    quantity = item.quantity.toString(),
                    unitPrice = item.unitPrice.formatted,
                    totalPrice = item.totalPrice.formatted
                )
            },
            subtotal = details.sale.totalAmount.formatted,
            tax = "0.00",
            total = details.sale.totalAmount.formatted,
            paymentMethod = details.payments.firstOrNull()?.method?.name ?: "CASH",
            barcodeOrQr = details.sale.id
        )
    }

    suspend fun printReceiptWithJob(
        details: SaleDetails,
        cashierName: String,
        format: PrintPageFormat = PrintPageFormat.Receipt80mm()
    ): Result<Unit> {
        val job = buildReceiptPrintJob(details, cashierName)
        return printerService.printReceipt(job, format)
    }

    suspend fun generatePdf(content: List<PdfLineInfo>) = deviceService.generateContentPdf(content)

    suspend fun getReceiptImages(pdfData: ByteArray): List<ImageBitmap> {
        return deviceService.renderPdfToImages(pdfData).mapNotNull { it.toImageBitmap() }
    }
}

fun Instant.formated(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    return this.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
}
