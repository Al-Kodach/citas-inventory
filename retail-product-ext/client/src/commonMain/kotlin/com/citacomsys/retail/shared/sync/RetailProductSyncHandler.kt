package com.citacomsys.retail.shared.sync

import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.core.sync.handler.SingleReplayResult
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.retail.shared.data.remote.RetailApiService
import com.citacomsys.retail.shared.data.repository.ClientRetailRepository
import com.citacomsys.types.EntityType
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.util.StockWriter
import com.citacomsys.core.domain.model.SyncTarget
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json


internal class RetailProductSyncHandler(
    private val retailApiService: RetailApiService,
    private val retailRepository: ClientRetailRepository,
    private val writer: StockWriter,
    private val dbManager: InventoryDatabaseManager
) : EntitySyncHandler {

    override val entityType: EntityType = EntityType.RETAIL_PRODUCT
    override val dependsOn: Set<EntityType> = emptySet()

    override suspend fun getPayload(request: PendingRequestEntity, json: Json): Any? {
        Napier.d(
            tag = "RetailProductSyncHandler",
            message = "Requesting for payload for ${request.eventAction}"
        )
        val productId = request.entity_id
        return when (request.eventAction) {
            "CREATE" -> {
                val payload = retailRepository.getById(productId).getOrNull()
                    ?: throw SingleReplayResult.Exception(
                        SingleReplayResult.PermanentFailure("Retail product data lost for $productId")
                    )
                Napier.d(tag = "RetailProductSyncHandler", message = "Payload: $payload")
                payload
            }

            "UPDATE_METADATA" -> request.payload
            else -> null
        }
    }

    override suspend fun onSync(
        targets: List<SyncTarget>
    ): Result<Unit> = runCatching {
        Napier.d { "Retail product sync triggered with target: $targets" }
        targets.forEach { target ->
            val lastSyncedTimestamp = dbManager.getDatabase().productQueries
                .getProductMaxUpdateTimestampForWorkspace(target.workspaceId)
                .executeAsOneOrNull()?.MAX

            val response = retailApiService.getProductChanges(
                target.workspaceId, lastSyncedTimestamp
            )

            // Clean up or update the offline downloaded product metadata
            writer.performUpdate("") { dbContext ->
                response.deleted.forEach { deletedId ->
                    dbContext.stockBatchQueries.deleteById(deletedId)
                }

                response.updated.forEach { product ->
                    dbContext.productQueries.insertOrReplace(
                        retailRepository.mapToEntity(product)
                    )
                }
            }
        }
    }
}
