package com.citacomsys.module.inventory.di

import com.citacomsys.core.mesh.gateway.HubServiceGateway
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.module.inventory.data.remote.api.StockApiService
import com.citacomsys.module.inventory.data.remote.api.ktor.KtorProductApiService
import com.citacomsys.module.inventory.data.remote.api.ktor.KtorStockApiServiceService
import com.citacomsys.module.inventory.data.repository.ClientStockBatchRepositoryImpl
import com.citacomsys.module.inventory.domain.repository.ClientStockBatchRepository
import com.citacomsys.module.inventory.mesh.InventoryHubGateway
import com.citacomsys.module.inventory.sync.ProductItemSyncHandler
import com.citacomsys.module.inventory.sync.ProductItemSyncHandlerImpl
import com.citacomsys.module.inventory.sync.StockBatchSyncHandler
import com.citacomsys.module.inventory.sync.StockBatchSyncHandlerImpl
import com.citacomsys.module.inventory.ui.screen.batch.BatchDetailsViewModel
import com.citacomsys.module.inventory.ui.screen.batch.BatchPriceEditViewModel
import com.citacomsys.module.inventory.ui.screen.batch.CreateBatchViewModel
import com.citacomsys.module.inventory.ui.screen.product.ProductListViewModel
import com.citacomsys.module.inventory.ui.screen.product.ProductMetadataViewModel
import com.citacomsys.module.inventory.ui.screen.stocks.ProductStocksViewModel
import com.citacomsys.module.inventory.ui.screen.stocks.StockTransferViewModel
import com.citacomsys.module.inventory.ui.widget.LowStockAlertWidgetViewModel
import com.citacomsys.types.ProductItem
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val inventoryClientUiDI = module {
    viewModelOf(::ProductStocksViewModel)
    viewModelOf(::BatchDetailsViewModel)
    viewModelOf(::ProductListViewModel)
    viewModelOf(::CreateBatchViewModel)
    viewModelOf(::BatchPriceEditViewModel)
    viewModelOf(::ProductMetadataViewModel)
    viewModelOf(::StockTransferViewModel)
    viewModelOf(::LowStockAlertWidgetViewModel)
}

val inventoryClientInfraDI = module {
    // Inventory
    single<StockApiService> { KtorStockApiServiceService(get()) }
    single { KtorProductApiService<ProductItem>(get(), "api/v1/inventory/products") }

    // Mesh Gateway
    singleOf(::InventoryHubGateway) bind HubServiceGateway::class

    // Sync handlers
    singleOf(::ClientStockBatchRepositoryImpl) {
        bind<ClientStockBatchRepository>()
    }

    singleOf(::StockBatchSyncHandlerImpl) {
        bind<StockBatchSyncHandler>()
        bind<EntitySyncHandler>()
    }

    singleOf(::ProductItemSyncHandlerImpl) {
        bind<ProductItemSyncHandler>()
        bind<EntitySyncHandler>()
    }
}
