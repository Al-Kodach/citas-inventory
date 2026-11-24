package com.citacomsys.core.barcode

interface BarcodePrinterService {
    suspend fun printBarcodeLabel(
        title: String,
        code: String,
        price: String?,
        expiryDate: String?
    ): Result<Unit>
}