package com.citacomsys.sales

import com.citacomsys.api.sales.SaleRepository
import com.citacomsys.api.sales.service.DigitalReceiptService
import com.citacomsys.sales.common.SalesComputer
import com.citacomsys.sales.gateway.offlineSalesRoute
import com.citacomsys.sales.gateway.onlineSalesRouting
import com.citacomsys.sales.gateway.sellableProductsRouting
import com.citacomsys.sales.repo.ExposedSalesRepository
import com.citacomsys.sales.service.InventoryQueryService
import com.citacomsys.sales.service.OnlineSaleService
import com.citacomsys.sales.service.ServerDigitalReceiptService
import com.citacomsys.sales.usecase.IngestOfflineSaleUseCase
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import org.koin.core.context.loadKoinModules
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.ktor.ext.inject

private val saleModule = module {
    single {
        OnlineSaleService(
            stockRepo = get(),
            saleRepo = get(),
            userRepo = get(),
            computer = get(),
            notificationService = get(),
            workspaceRepository = get(),
            customerService = get(),
            auditor = get()
        )
    }
    single<SaleRepository> { ExposedSalesRepository(get()) }
    singleOf(::InventoryQueryService)
    singleOf(::IngestOfflineSaleUseCase)
    singleOf(::SalesComputer)

    single<DigitalReceiptService> {
        ServerDigitalReceiptService(
            smsService = get(),
            workspaceRepo = get(),
            saleService = get()
        )
    }
}

fun Application.configureSaleModule() {

    loadKoinModules(saleModule)

    val saleService: OnlineSaleService by inject()
    val queryService: InventoryQueryService by inject()
    val digitalReceiptService: DigitalReceiptService by inject()
    val syncOfflineSalesUseCase by inject<IngestOfflineSaleUseCase>()

    routing {
        onlineSalesRouting(
            saleService,
            digitalReceiptService,
        )
        offlineSalesRoute(syncOfflineSalesUseCase)
        sellableProductsRouting(queryService)
    }
}
