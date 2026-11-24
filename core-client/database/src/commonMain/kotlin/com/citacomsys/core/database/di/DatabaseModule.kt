package com.citacomsys.core.database.di

import com.citacomsys.core.database.CustomerDatabaseManager
import com.citacomsys.core.database.CustomerDatabaseManagerImpl
import com.citacomsys.core.database.DatabaseKeyProvider
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.InventoryDatabaseManagerImpl
import com.citacomsys.core.database.backup.DatabaseBackupDriver
import com.citacomsys.core.database.backup.SqlDelightBackupDriver
import com.citacomsys.core.database.dao.AppUserDao
import com.citacomsys.core.database.dao.DashboardDao
import com.citacomsys.core.database.dao.PendingRequestDao
import com.citacomsys.core.database.dao.SqlAppUserDao
import com.citacomsys.core.database.dao.SqlDelightDashboardDao
import com.citacomsys.core.database.dao.SqlDelightPendingRequestDao
import com.citacomsys.core.database.dao.SqlUserDao
import com.citacomsys.core.database.dao.UserDao
import com.citacomsys.core.database.dao.UserWorkspaceBindingDao
import com.citacomsys.core.database.dao.SqlDelightUserWorkspaceBindingDao
import com.citacomsys.core.database.dao.inventory.ProductDao
import com.citacomsys.core.database.dao.inventory.SqlDelightProductDao
import com.citacomsys.core.database.dao.inventory.SqlDelightStockBatchDao
import com.citacomsys.core.database.dao.inventory.SqlDelightStockMovementDao
import com.citacomsys.core.database.dao.inventory.StockBatchDao
import com.citacomsys.core.database.dao.inventory.StockMovementDao
import com.citacomsys.core.database.dao.inventory.WorkstationDao
import com.citacomsys.core.database.dao.inventory.SqlDelightWorkstationDao
import com.citacomsys.core.database.dao.sale.PosCartDao
import com.citacomsys.core.database.dao.sale.SalesDao
import com.citacomsys.core.database.dao.sale.SqlDelightPosCartDao
import com.citacomsys.core.database.dao.sale.SqlDelightSalesDao
import com.citacomsys.core.database.util.StockWriter
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

expect fun platformModule(): Module

/**
 * Base module for providing the platform-specific database drivers.
 */
val databaseDriverModule = module {
    includes(platformModule())
}

val databaseModule = module {
    includes(databaseDriverModule)

    single { DatabaseKeyProvider(get()) }


    single<DatabaseBackupDriver> {
        SqlDelightBackupDriver(get(), get())
    }

    single<InventoryDatabaseManager> {
        InventoryDatabaseManagerImpl(
            driverFactory = get(),
            koin = get(),
            dispatcher = get(named("IO"))
        )
    }

    single<CustomerDatabaseManager> {
        CustomerDatabaseManagerImpl(
            driverFactory = get(),
            vault = get(),
            dispatcher = get(named("IO"))
        )
    }

    // Dao
    single<StockBatchDao> { SqlDelightStockBatchDao(get(), get()) }
    single<ProductDao> { SqlDelightProductDao(get(), get()) }
    single<StockMovementDao> { SqlDelightStockMovementDao(get(), get()) }
    single<DashboardDao> { SqlDelightDashboardDao(get(), get()) }
    single<PendingRequestDao> { SqlDelightPendingRequestDao(get()) }
    single<SalesDao> { SqlDelightSalesDao(get(), get(), get()) }
    single<PosCartDao> { SqlDelightPosCartDao(get()) }
    single<UserDao> { SqlUserDao(get()) }
    single<AppUserDao> { SqlAppUserDao(get()) }
    single<UserWorkspaceBindingDao> { SqlDelightUserWorkspaceBindingDao(get()) }
    single<WorkstationDao> { SqlDelightWorkstationDao(get()) }
    single { StockWriter(get()) }
}
