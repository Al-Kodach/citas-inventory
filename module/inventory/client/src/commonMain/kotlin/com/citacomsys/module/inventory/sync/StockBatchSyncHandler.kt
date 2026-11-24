package com.citacomsys.module.inventory.sync

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.util.StockWriter
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.sync.handler.EntitySyncHandler
import com.citacomsys.core.sync.handler.SingleReplayResult
import com.citacomsys.database.BatchSnapshotEntity
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.module.inventory.data.remote.api.StockApiService
import com.citacomsys.module.inventory.mapper.toBatch
import com.citacomsys.module.inventory.request.RestockRequest
import com.citacomsys.shared.sync.SyncPriority
import com.citacomsys.types.BatchItem
import com.citacomsys.types.BranchCursor
import com.citacomsys.types.EntityType
import com.citacomsys.types.MultiTargetDeltaRequest
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlin.time.Clock

interface StockBatchSyncHandler : EntitySyncHandler

internal class StockBatchSyncHandlerImpl(
    private val api: StockApiService,
    private val writer: StockWriter,
    private val dbManager: InventoryDatabaseManager
) : StockBatchSyncHandler {
    override val entityType: EntityType = EntityType.STOCK_BATCH
    override val dependsOn: Set<EntityType> = setOf()
    override val priority: SyncPriority = SyncPriority.HIGHEST

    /**
     * PULL PHASE: Fetches server snapshots across all configured targets in 1 multi-target round-trip,
     * keeping network handshakes safely isolated from the database transaction scope.
     */
    override suspend fun onSync(
        targets: List<SyncTarget>
    ): Result<Unit> = runCatching {
        if (targets.isEmpty()) return@runCatching

        Napier.i { "Multi-target stock sync triggered with targets $targets" }

        val localSyncCutoffTime = Clock.System.now()
        val db = dbManager.getDatabase()

        val workspacesMap = mutableMapOf<String, MutableList<BranchCursor>>()
        targets.forEach { target ->
            val lastSyncedTimestampStr = db.batchSnapshotQueries
                .getMaxSnapshotVersionTimestampForWorkspace(target.workspaceId)
                .executeAsOneOrNull()?.MAX

            workspacesMap.getOrPut(target.workspaceId) { mutableListOf() }.add(
                BranchCursor(
                    branchId = target.branchId,
                    lastBatchSyncAt = lastSyncedTimestampStr,
                    lastProductSyncAt = lastSyncedTimestampStr
                )
            )
        }

        val request = MultiTargetDeltaRequest(workspaces = workspacesMap)
        val deltaResponse = api.getMultiTargetDelta(request)

        deltaResponse.workspaceDeltas.forEach { (_, delta) ->
            val allBatchItems = mutableListOf<BatchItem>()
            val deletedBatchIds = mutableListOf<String>()

            delta.branchBatches.forEach { (_, branchChangeList) ->
                allBatchItems.addAll(branchChangeList.updated)
                deletedBatchIds.addAll(branchChangeList.deleted)
            }

            val incomingProductIds = allBatchItems.map { it.productId }.distinct()

            val updatedEntities = allBatchItems.map {
                BatchSnapshotEntity(
                    batch_id = it.batchId,
                    workspace_id = it.workspaceId,
                    product_id = it.productId,
                    branch_id = it.branchId,
                    batch_number = it.batchNumber,
                    available_quantity = it.availableQuantity.toLong(),
                    selling_price_amount_minor = it.sellingPriceAmountMinor,
                    selling_price_currency = it.sellingPriceCurrency,
                    expiry_date = it.expiryDate,
                    version_timestamp = it.versionTimestamp,
                )
            }

            // Enter the atomic transaction context ONLY to rapidly flash data rows to SQLite
            writer.performUpdate("") { dbContext ->
                // Apply explicitly tracked deletions from the cloud view engine
                deletedBatchIds.forEach { deletedBatchId ->
                    dbContext.batchSnapshotQueries.deleteSnapshotForBatch(deletedBatchId)
                    dbContext.stockBatchQueries.deleteById(deletedBatchId)
                }

                // Clean out placeholders for incoming active items in bulk
                if (incomingProductIds.isNotEmpty()) {
                    dbContext.batchSnapshotQueries.deleteStubsForProducts(incomingProductIds)
                }
            }

            // Hand over the transformed entity mappings and the true cutoff anchor to stock writer
            writer.applySyncSnapshots(updatedEntities, localSyncCutoffTime)
        }
    }

    override suspend fun getPayload(request: PendingRequestEntity, json: Json): Any? {
        val entityId = request.entity_id
        return when (request.eventAction) {
            "RESTOCK" -> {
                val batch = dbManager.getDatabase().stockBatchQueries.selectById(entityId)
                    .executeAsOneOrNull()?.toBatch()
                    ?: throw SingleReplayResult.Exception(SingleReplayResult.PermanentFailure("Restock data lost for $entityId"))

                RestockRequest(
                    id = entityId,
                    workspaceId = batch.workspaceId,
                    branchId = batch.branchId,
                    productId = batch.productId,
                    batchNumber = batch.batchNumber,
                    expiryDate = batch.expiryDate,
                    quantity = batch.initialQuantity,
                    costPrice = batch.costPrice,
                    sellingPrice = batch.sellingPrice,
                    createdAt = batch.createdAt
                )
            }
            // STOCK_ADJUSTMENT, UPDATE_PRICE are handler by defaultWorker
            else -> null
        }
    }
}
