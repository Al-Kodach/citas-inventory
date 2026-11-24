package com.citacomsys.module.inventory.sync

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.util.StockWriter
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.database.ProductEntity
import com.citacomsys.module.inventory.data.remote.api.ktor.KtorProductApiService
import com.citacomsys.shared.sync.SyncPriority
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductItem
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

interface ProductItemSyncHandler : EntitySyncHandler

internal class ProductItemSyncHandlerImpl(
    private val productApiService: KtorProductApiService<ProductItem>,
    private val writer: StockWriter,
    private val dbManager: InventoryDatabaseManager,
) : ProductItemSyncHandler {
    override val entityType: EntityType = EntityType.PRODUCT
    override val dependsOn: Set<EntityType> = emptySet()
    override val priority: SyncPriority = SyncPriority.HIGHEST

    override suspend fun getPayload(request: PendingRequestEntity, json: Json): Any? = null

    /**
     * 2-STAGE PRODUCT MASTER SYNC:
     * Stage 1: Request ProductItem change-list response from cloud endpoint.
     * Stage 2: Atomically delete inactive products & insert/update minimal ProductItems in SQLite.
     */
    override suspend fun onSync(
        targets: List<SyncTarget>
    ): Result<Unit> = runCatching {
        Napier.i { "ProductItem master sync triggered for targets: $targets" }

        val db = dbManager.getDatabase()

        targets.forEach { target ->
            val lastSyncedTimestamp = db.productQueries
                .getProductMaxUpdateTimestampForWorkspace(target.workspaceId)
                .executeAsOneOrNull()?.MAX

            val response = productApiService.getProductChanges(
                workspaceId = target.workspaceId,
                sinceTimestamp = lastSyncedTimestamp
            )

            // Stage 2: Apply Deletions & Insert/Update Master Product Items
            writer.performUpdate("") { dbContext ->
                // Delete inactive / removed products from local DB
                response.deleted.forEach { deletedId ->
                    dbContext.productQueries.deleteById(deletedId)
                    dbContext.batchSnapshotQueries.deleteStubsForProducts(listOf(deletedId))
                }

                // Insert or replace updated minimal ProductItems
                response.updated.forEach { item ->
                    dbContext.productQueries.insertOrReplace(
                        ProductEntity(
                            id = item.id,
                            workspace_id = item.workspaceId,
                            name = item.name,
                            brand_name = item.brandName,
                            sku = item.sku,
                            barcode = item.barcode,
                            unit_of_measure = item.unitOfMeasure,
                            category = item.category,
                            description = item.description,
                            reorder_level = item.reorderLevel,
                            product_type = item.type.name,
                            extension_data_json = "{}",
                            created_at = item.versionTimestamp,
                            updated_at = item.versionTimestamp,
                            is_active = item.isActive,
                            actor_id = "SYSTEM"
                        )
                    )
                }
            }
        }
    }
}
