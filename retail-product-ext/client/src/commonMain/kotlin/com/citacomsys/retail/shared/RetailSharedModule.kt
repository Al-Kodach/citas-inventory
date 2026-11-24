package com.citacomsys.retail.shared

import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.module.inventory.data.repository.BaseClientProductRepository
import com.citacomsys.module.inventory.di.inventoryClientUiDI
import com.citacomsys.module.inventory.ui.screen.product.BaseProductFormViewModel
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.retail.shared.data.remote.RetailApiService
import com.citacomsys.retail.shared.data.repository.ClientRetailRepository
import com.citacomsys.retail.shared.mesh.RetailHubGateway
import com.citacomsys.retail.shared.presentation.screen.product.form.RetailProductFormViewModel
import com.citacomsys.retail.shared.sync.RetailProductSyncHandler
import com.citacomsys.types.BaseProduct
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

const val RETAIL_ENDPOINT: String = "api/v1/inventory/products/retail"

val retailSharedUiModule = module {
    includes(
        inventoryClientUiDI,
    )

    factory<BaseProductFormViewModel<out BaseProduct>> {
        RetailProductFormViewModel(
            notification = get(),
            productRepo = get<ClientRetailRepository>(),
            sessionManager = get(),
            telemetryClient = get(),
            quotaGuard = get()
        )
    }
}

val retailModule = module {
    single<RetailApiService> {
        RetailApiService(
            client = get(),
            endpoint = RETAIL_ENDPOINT,
            json = get()
        )
    }

    singleOf(::ClientRetailRepository) {
        bind<BaseClientProductRepository<RetailProduct>>()
        bind<ClientRetailRepository>()
    }

    singleOf(::RetailHubGateway) {
        bind<HubServiceGateway>()
    }

    singleOf(::RetailProductSyncHandler) {
        bind<EntitySyncHandler>()
    }
}
