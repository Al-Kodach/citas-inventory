package com.citacomsys.module.inventory

import com.citacomsys.module.inventory.repository.ExposedProductRepository
import com.citacomsys.module.inventory.repository.ExposedStockBatchRepository
import com.citacomsys.module.inventory.repository.ExposedStockMovementRepository
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.module.inventory.repository.StockBatchRepository
import com.citacomsys.module.inventory.repository.StockMovementRepository
import com.citacomsys.module.inventory.service.BaseProductService
import com.citacomsys.module.inventory.service.StockBatchService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val inventoryServiceModule = module {
    single<StockBatchRepository> { ExposedStockBatchRepository() }
    single<ProductRepository> { ExposedProductRepository() }
    single<StockMovementRepository> { ExposedStockMovementRepository(get()) }
    singleOf(::StockBatchService)
    singleOf(::BaseProductService)

    single {
        InventoryExpiryAlertProcessor(
            inventoryRepo = get(),
            notificationService = get(),
            userRepository = get(),
            workspaceRepository = get()
        )
    } bind com.citacomsys.core.notification.AlertProcessor::class
}
