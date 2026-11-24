package com.citacomsys.core.printer.di

import com.citacomsys.core.printer.DefaultDocumentPrinterService
import com.citacomsys.core.printer.DocumentPrinterService
import org.koin.dsl.module

val printerModule = module {
    single<DocumentPrinterService> {
        DefaultDocumentPrinterService(
            deviceService = get()
        )
    }
}
