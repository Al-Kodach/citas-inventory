package com.citacomsys.core.barcode.di

import com.citacomsys.core.barcode.BarcodePrinterService
import com.citacomsys.core.barcode.BarcodeScannerService
import com.citacomsys.core.barcode.DefaultBarcodePrinterService
import com.citacomsys.core.barcode.createBarcodeScannerService
import com.citacomsys.device.service.DeviceService
import com.citacomsys.device.service.PdfLineInfo
import com.citacomsys.device.service.ValueType
import org.koin.dsl.module

val barcodeModule = module {
    single<BarcodeScannerService> { createBarcodeScannerService() }
    single<BarcodePrinterService> { DefaultBarcodePrinterService(get()) }
}
