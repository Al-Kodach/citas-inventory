package com.citacomsys.sales.di

import com.citacomsys.api.sales.service.DigitalReceiptService
import com.citacomsys.sales.data.remote.api.InventoryQueryApi
import com.citacomsys.sales.data.remote.api.KtorInventoryQueryApi
import com.citacomsys.sales.data.remote.api.KtorSalesApi
import com.citacomsys.sales.data.remote.api.SalesApi
import com.citacomsys.sales.data.repository.ClientSalesRepository
import com.citacomsys.sales.engine.PosEngine
import com.citacomsys.sales.service.KtorDigitalReceiptService
import com.citacomsys.sales.ui.screen.details.SaleDetailsViewModel
import com.citacomsys.sales.ui.screen.pos.PosViewModel
import com.citacomsys.sales.ui.screen.sales.SalesHistoryViewModel
import com.citacomsys.sales.ui.screen.share.SaleReceiptHandler
import com.citacomsys.sales.ui.screen.share.SaleReceiptUtil
import com.citacomsys.sales.mesh.SalesHubGateway
import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.sales.sync.SalesSyncHandler
import com.citacomsys.core.sync.handler.EntitySyncHandler
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val salesClientInfraModule = module {
    // Repository
    single {
        ClientSalesRepository(
            salesApi = get(),
            salesDao = get(),
            productDao = get(),
            pendingRequestDao = get(),
            sessionManager = get(),
            scope = get(),
            ioDispatcher = get(named("IO")),
            stockWriter = get(),
            cartDao = get(),
            telemetryClient = get()
        )
    }

    single<InventoryQueryApi> { KtorInventoryQueryApi(get()) }
    single<SalesApi> { KtorSalesApi(get()) }

    singleOf(::SalesSyncHandler) {
        bind<EntitySyncHandler>()
    }

    // Mesh Gateway
    singleOf(::SalesHubGateway) bind HubServiceGateway::class
}

internal val salesClientUiModuleDI = module {
    // Pos engine
    single { PosEngine(get(), get(), get(), get(), get()) }
    single<DigitalReceiptService> { KtorDigitalReceiptService(get()) }
    factory {
        SaleReceiptHandler(
            saleUtil = SaleReceiptUtil(get(), get(), get(), get(), get(), get()),
            saleRepository = get(),
            scope = get()
        )
    }
    viewModelOf(::SalesHistoryViewModel)
    viewModel {
        PosViewModel(
            engine = get(),
            salesRepository = get(),
            customerRepository = get(),
            sessionManager = get(),
            printerService = get(),
            scannerService = get(),
            prefs = get()
        )
    }

    viewModel { (saleId: String, wsId: String) ->
        SaleDetailsViewModel(
            saleId = saleId,
            notificationService = get(),
            receiptHandler = get(),
            sessionManager = get(),
            customerRepository = get()
        )
    }
    viewModelOf(::SaleDetailsViewModel)
}
