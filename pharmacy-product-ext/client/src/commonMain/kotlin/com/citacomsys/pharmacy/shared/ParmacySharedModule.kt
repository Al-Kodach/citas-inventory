package com.citacomsys.pharmacy.shared

import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.module.inventory.data.repository.BaseClientProductRepository
import com.citacomsys.module.inventory.di.inventoryClientUiDI
import com.citacomsys.module.inventory.ui.screen.product.BaseProductFormViewModel
import com.citacomsys.pharmacy.api.model.PharmacyProduct
import com.citacomsys.pharmacy.shared.data.remote.PharmacyApiService
import com.citacomsys.pharmacy.shared.data.repository.ClientPharmacyRepository
import com.citacomsys.pharmacy.shared.mesh.PharmacyHubGateway
import com.citacomsys.pharmacy.shared.presentation.screen.product.form.PharmacyProductFormExt
import com.citacomsys.pharmacy.shared.sync.PharmacyProductSyncHandler
import com.citacomsys.types.BaseProduct
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

const val PHARMACY_ENDPOINT: String = "api/v1/inventory/products/pharmacy"

val pharmacySharedUiModule = module {
    includes(
        inventoryClientUiDI,
    )
    factory<BaseProductFormViewModel<out BaseProduct>> {
        PharmacyProductFormExt(
            notification = get(),
            productRepo = get<ClientPharmacyRepository>(),
            sessionManager = get(),
            telemetryClient = get(),
            quotaGuard = get()
        )
    }
}

val pharmacyModule = module {
    single<PharmacyApiService> {
        PharmacyApiService(
            client = get(),
            endpoint = PHARMACY_ENDPOINT,
            json = get()
        )
    }

    singleOf(::ClientPharmacyRepository) {
        bind<BaseClientProductRepository<PharmacyProduct>>()
        bind<ClientPharmacyRepository>()
    }

    singleOf(::PharmacyHubGateway) {
        bind<HubServiceGateway>()
    }

    singleOf(::PharmacyProductSyncHandler) {
        bind<EntitySyncHandler>()
    }
}
