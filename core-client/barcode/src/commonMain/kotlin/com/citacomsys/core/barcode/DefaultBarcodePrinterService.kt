package com.citacomsys.core.barcode

import com.citacomsys.device.service.DeviceService
import com.citacomsys.device.service.PdfLineInfo
import com.citacomsys.device.service.ValueType

class DefaultBarcodePrinterService(
    private val deviceService: DeviceService
) : BarcodePrinterService {
    override suspend fun printBarcodeLabel(
        title: String,
        code: String,
        price: String?,
        expiryDate: String?
    ): Result<Unit> = runCatching {
        val lines = listOfNotNull(
            PdfLineInfo(title, ValueType.FONT_BOLD, 0, 0),
            PdfLineInfo("Code: $code", ValueType.TEXT, 0, 0),
            price?.let { PdfLineInfo("Price: $it", ValueType.TEXT, 0, 0) },
            expiryDate?.let { PdfLineInfo("Expiry: $it", ValueType.TEXT, 0, 0) }
        )

        val pdfBytes = deviceService.generateContentPdf(lines)
        deviceService.print(pdfBytes, "barcode-label-$code.pdf")
    }
}