package com.citacomsys.pharmacy.shared.sync

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.util.StockWriter
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.core.sync.handler.SingleReplayResult
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.pharmacy.shared.data.remote.PharmacyApiService
import com.citacomsys.pharmacy.shared.data.repository.ClientPharmacyRepository
import com.citacomsys.types.EntityType
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json

internal class PharmacyProductSyncHandler(
    private val pharmacyApiService: PharmacyApiService,
    private val pharmacyRepository: ClientPharmacyRepository,
    private val writer: StockWriter,
    private val dbManager: InventoryDatabaseManager
) : EntitySyncHandler {

    override val entityType: EntityType = EntityType.PHARMACY_PRODUCT
    override val dependsOn: Set<EntityType> = emptySet()

    override suspend fun getPayload(request: PendingRequestEntity, json: Json): Any? {
        val productId = request.entity_id
        return when (request.eventAction) {
            "CREATE" -> {
                pharmacyRepository.getById(productId).getOrNull()
                    ?: throw SingleReplayResult.Exception(SingleReplayResult.PermanentFailure("Pharmacy product data lost for $productId"))
            }

            "UPDATE_METADATA" -> request.payload
            else -> null
        }
    }

    /**
     * PULL PHASE: Purges deleted products from local metadata storage across target scopes.
     * POS and management UI derive live product availability directly from ProductListItemView.sq snapshots.
     */
    override suspend fun onSync(
        targets: List<SyncTarget>
    ): Result<Unit> = runCatching {
        Napier.d { "Pharmacy product sync triggered with target: $targets" }

        targets.forEach { target ->
            val lastSyncedTimestamp = dbManager.getDatabase().productQueries
                .getProductMaxUpdateTimestampForWorkspace(target.workspaceId)
                .executeAsOneOrNull()?.MAX

            val response = pharmacyApiService.getProductChanges(
                target.workspaceId, lastSyncedTimestamp
            )

            writer.performUpdate("") { dbContext ->
                response.deleted.forEach { deletedId ->
                    dbContext.stockBatchQueries.deleteById(deletedId)
                }
                response.updated.forEach { product ->
                    dbContext.productQueries.insertOrReplace(
                        pharmacyRepository.mapToEntity(product)
                    )
                }
            }
        }
    }
}
