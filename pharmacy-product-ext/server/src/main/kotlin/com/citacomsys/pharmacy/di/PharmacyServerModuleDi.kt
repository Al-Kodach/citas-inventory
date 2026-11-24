package com.citacomsys.pharmacy.di

import com.citacomsys.pharmacy.api.PharmacyProductRepository
import com.citacomsys.pharmacy.repository.ExposedPharmacyProductRepository
import com.citacomsys.pharmacy.service.PharmacyProductService
import org.koin.dsl.bind
import org.koin.dsl.module

val pharmacyAppModuleDi = module {
    single<PharmacyProductRepository> {
        ExposedPharmacyProductRepository(get())
    }
    single<PharmacyProductService> {
        PharmacyProductService(get(), get(), get())
    }
    
    single {
        com.citacomsys.pharmacy.service.PatientRefillAlertProcessor(
            smsService = get(),
            resourceManager = get(),
            workspaceRepository = get(),
            clock = get()
        )
    } bind com.citacomsys.core.notification.AlertProcessor::class
}
