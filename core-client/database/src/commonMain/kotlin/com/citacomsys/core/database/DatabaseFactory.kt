package com.citacomsys.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.customer.database.CustomerDatabase
import com.citacomsys.customer.database.CustomerEntity
import com.citacomsys.customer.database.CustomerLedgerEntity
import com.citacomsys.customer.database.CustomerSearchEntity
import com.citacomsys.database.BatchSnapshotEntity
import com.citacomsys.database.BranchEntity
import com.citacomsys.database.DashboardSummaryCache
import com.citacomsys.database.InventoryDatabase
import com.citacomsys.database.PaymentEntity
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.database.PosCartEntity
import com.citacomsys.database.ProductEntity
import com.citacomsys.database.ProductItemEntity
import com.citacomsys.database.ProductPackageEntity
import com.citacomsys.database.SaleEntity
import com.citacomsys.database.AppUserEntity
import com.citacomsys.customer.database.UserEntity
import com.citacomsys.database.UserWorkspaceBindingEntity
import com.citacomsys.database.StockBatchEntity
import com.citacomsys.database.StockMovementEntity
import com.citacomsys.database.WorkstationBindingEntity
import com.citacomsys.telemetry.database.TelemetryDatabase
import com.citacomsys.types.EntityType
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserType
import org.koin.core.Koin
import org.koin.dsl.module

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class DriverFactory {
    suspend fun createNamedDriver(
        name: String,
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
        passphrase: ByteArray? = null
    ): SqlDriver
}

suspend fun createCoreDatabase(factory: DriverFactory, koin: Koin): InventoryDatabase {
    val driver = factory.createNamedDriver("inventory.db", InventoryDatabase.Schema)

    koin.loadModules(listOf(module { single { driver } }))

    return InventoryDatabase(
        driver = driver,
        PendingRequestEntityAdapter = PendingRequestEntity.Adapter(
            entityTypeAdapter = enumerationByName<EntityType>(),
            methodAdapter = enumerationByName<RequestMethod>(),
            lastFailureTimestampAdapter = InstantAsStringAdapter
        ),
        ProductEntityAdapter = ProductEntity.Adapter(
            created_atAdapter = InstantAsStringAdapter,
            updated_atAdapter = InstantAsStringAdapter,
            reorder_levelAdapter = LongAsIntAdapter
        ),
        ProductItemEntityAdapter = ProductItemEntity.Adapter(
            version_timestampAdapter = InstantAsStringAdapter,
            reorder_levelAdapter = LongAsIntAdapter,
            total_availableAdapter = LongAsIntAdapter
        ),
        productPackageEntityAdapter = ProductPackageEntity.Adapter(
            updated_atAdapter = InstantAsLongAdapter,
        ),
        SaleEntityAdapter = SaleEntity.Adapter(
            created_atAdapter = InstantAsStringAdapter,
        ),
        StockMovementEntityAdapter = StockMovementEntity.Adapter(
            created_atAdapter = InstantAsLongAdapter
        ),
        StockBatchEntityAdapter = StockBatchEntity.Adapter(
            created_atAdapter = InstantAsStringAdapter,
            updated_atAdapter = InstantAsStringAdapter
        ),
        PaymentEntityAdapter = PaymentEntity.Adapter(
            created_atAdapter = InstantAsStringAdapter
        ),
        BatchSnapshotEntityAdapter = BatchSnapshotEntity.Adapter(
            version_timestampAdapter = InstantAsStringAdapter,
        ),
        PosCartEntityAdapter = PosCartEntity.Adapter(
            last_updated_atAdapter = InstantAsStringAdapter
        ),
        DashboardSummaryCacheAdapter = DashboardSummaryCache.Adapter(
            cached_atAdapter = InstantAsLongAdapter
        ),
        BranchEntityAdapter = BranchEntity.Adapter(
            created_atAdapter = InstantAsStringAdapter,
            updated_atAdapter = InstantAsStringAdapter
        ),
        WorkstationBindingEntityAdapter = WorkstationBindingEntity.Adapter(
            last_seen_atAdapter = InstantAsStringAdapter,
            created_atAdapter = InstantAsStringAdapter
        ),
        AppUserEntityAdapter = AppUserEntity.Adapter(
            user_typeAdapter = enumerationByName<UserType>(),
            statusAdapter = enumerationByName<UserStatus>(),
            created_atAdapter = InstantAsStringAdapter,
            last_updated_atAdapter = InstantAsStringAdapter
        ),
        UserWorkspaceBindingEntityAdapter = UserWorkspaceBindingEntity.Adapter(
            created_atAdapter = InstantAsStringAdapter,
            updated_atAdapter = InstantAsStringAdapter
        )
    )
}

suspend fun createTelemetryDatabase(factory: DriverFactory): TelemetryDatabase {
    val driver = factory.createNamedDriver("telemetry.db", TelemetryDatabase.Schema)
    return TelemetryDatabase(driver = driver)
}

suspend fun createCustomerDatabase(factory: DriverFactory, secret: ByteArray?): CustomerDatabase {
    val driver = factory.createNamedDriver("customer.db", CustomerDatabase.Schema, secret)
    return CustomerDatabase(
        driver = driver,
        CustomerEntityAdapter = CustomerEntity.Adapter(
            deleted_atAdapter = InstantAsStringAdapter,
            created_atAdapter = InstantAsStringAdapter,
            updated_atAdapter = InstantAsStringAdapter
        ),
        CustomerSearchEntityAdapter = CustomerSearchEntity.Adapter(
            timestampAdapter = InstantAsStringAdapter
        ),
        CustomerLedgerEntityAdapter = CustomerLedgerEntity.Adapter(
            recorded_atAdapter = InstantAsStringAdapter,
        ),
        UserEntityAdapter = UserEntity.Adapter(
            user_typeAdapter = enumerationByName<UserType>(),
            created_atAdapter = InstantAsStringAdapter,
            deleted_atAdapter = InstantAsStringAdapter,
            last_updated_atAdapter = InstantAsStringAdapter,
            statusAdapter = enumerationByName<UserStatus>()
        )
    )
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
}
