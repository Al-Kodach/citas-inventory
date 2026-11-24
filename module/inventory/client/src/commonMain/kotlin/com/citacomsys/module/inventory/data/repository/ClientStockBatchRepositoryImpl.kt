package com.citacomsys.module.inventory.data.repository

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.core.database.dao.inventory.ProductDao
import com.citacomsys.core.database.dao.inventory.StockBatchDao
import com.citacomsys.core.database.dao.inventory.StockMovementDao
import com.citacomsys.core.database.util.StockWriter
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.remote.isErrorRetriable
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.module.inventory.data.remote.api.StockApiService
import com.citacomsys.module.inventory.domain.repository.ClientStockBatchRepository
import com.citacomsys.module.inventory.mapper.asDomain
import com.citacomsys.module.inventory.mapper.asMovementEntity
import com.citacomsys.module.inventory.mapper.toBatch
import com.citacomsys.module.inventory.mapper.toBatchEntity
import com.citacomsys.module.inventory.request.AdjustStockRequest
import com.citacomsys.module.inventory.request.PriceChangeRequest
import com.citacomsys.module.inventory.request.PriceType
import com.citacomsys.module.inventory.request.RestockRequest
import com.citacomsys.module.inventory.request.UpdateBatchMetadataRequest
import com.citacomsys.shared.event.EntityChangeEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.util.resultOf
import com.citacomsys.types.BatchItem
import com.citacomsys.types.EntityType
import com.citacomsys.types.StockBatch
import com.citacomsys.types.StockMovement
import com.citacomsys.types.generateUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Clock

internal class ClientStockBatchRepositoryImpl(
    private val stockBatchDao: StockBatchDao,
    private val stockMovementDao: StockMovementDao,
    private val productDao: ProductDao,
    private val api: StockApiService,
    private val sessionManager: AppSessionManager,
    private val scope: CoroutineScope,
    private val stockWriter: StockWriter,
    private val dbManager: InventoryDatabaseManager,
    private val telemetryClient: TelemetryLogClient,
    private val syncEngine: EntitySyncEngine
) : ClientStockBatchRepository {
    private val activeFilterBranchId = sessionManager.activeViewBranchId.value

    override fun search(query: String): Flow<List<BatchItem>> =
        stockBatchDao.search(query).map { it.map { StockBatchDao.mapToItem(it) } }

    override fun observeBatchesForProduct(productId: String): Flow<List<BatchItem>> =
        stockBatchDao.getAll(productId).map { it.map { StockBatchDao.mapToItem(it) } }

    override fun observeBatch(batchId: String): Flow<BatchItem?> {
        return stockBatchDao.observeItem(batchId).map { entity ->
            entity?.let { StockBatchDao.mapToItem(it) }
                .also { if (it == null) fetchBatchDetailsCloud(batchId)  }
        }
    }

    override fun observeBatchView(batchId: String): Flow<StockBatch?> =
        stockBatchDao.observeBatch(batchId).map { entity -> entity?.toBatch() }

    override fun observeStockMoventByProduct(productId: String): Flow<List<StockMovement>> {
        return stockMovementDao.observeByProduct(productId, activeFilterBranchId)
            .map { it.map { entity -> entity.asDomain() } }
    }

    override fun observeMovementsForBatch(batchId: String): Flow<List<StockMovement>> {
        return stockMovementDao.observeByBatch(batchId)
            .map { it.map { entity -> entity.asDomain() } }
    }

    override suspend fun findById(id: String): StockBatch? {
        return stockBatchDao.getBatchMeta(id)?.toBatch() ?: run {
            if (sessionManager.isSubscribed()) {
                fetchBatchDetailsCloud(id).getOrNull()
            } else {
                null
            }
        }
    }

    override suspend fun fetchBatchDetailsCloud(batchId: String): Result<StockBatch> = runCatching {
        val cloudBatch = api.getById(batchId)
        stockBatchDao.save(cloudBatch.toBatchEntity())
        cloudBatch
    }

    override suspend fun syncMovementsFromCloud(productId: String, batchId: String?): Result<Unit> =
        runCatching {
            if (sessionManager.isSubscribed()) {
                val db = dbManager.getDatabase()
                val lastSyncedTimestampStr =
                    db.stockMovementQueries.getMaxCreatedAtForProduct(
                        productId,
                        activeFilterBranchId
                    ).executeAsOneOrNull()?.MAX

                val movements = if (batchId != null) {
                    api.getMovementsForBatch(productId, batchId, lastSyncedTimestampStr)
                } else {
                    api.getMovementsForProduct(productId, lastSyncedTimestampStr)
                }

                if (movements.isNotEmpty()) {
                    stockWriter.performUpdate(productId) { dbContext ->
                        movements.forEach { movement ->
                            dbContext.stockMovementQueries.insert(
                                movement.asMovementEntity()
                                    .copy(is_accounted_for = true)
                            )
                        }
                    }
                }
            }
        }

    override suspend fun updatePrice(
        request: PriceChangeRequest,
        actorId: String,
        actorName: String
    ): Result<Unit> {
        if (request.type == PriceType.COST && request.reason == null) {
            return Result.failure(IllegalArgumentException("Reason requires to change cost Price"))
        }

        if (sessionManager.isSubscribed(request.workspaceId)) {
            return resultOf {
                api.updatePrice(request.batchId, request)
            }.onSuccess {
                recordPriceChangeLocally(request.batchId, request, actorId)
            }.recover { e ->
                if (isErrorRetriable(e)) {
                    recordPriceChangeLocally(request.batchId, request, actorId)
                    enqueueRequest(
                        endpoint = "/api/v1/stock-batches/${request.batchId}/price",
                        payload = null,
                        workspaceId = request.workspaceId,
                        eventAction = "UPDATE_PRICE",
                        entityId = request.batchId,
                        dependantId = request.batchId,
                        method = RequestMethod.PUT,
                        actorId = actorId,
                        actorName = actorName,
                        branchId = request.branchId,
                        routingChainJson = ActiveRoutingChainHolder.get()
                    )
                } else throw e
            }
        }

        return resultOf { recordPriceChangeLocally(request.batchId, request, actorId) }
    }

    private suspend fun recordPriceChangeLocally(
        batchId: String,
        request: PriceChangeRequest,
        actorId: String
    ) {
        if (request.type == PriceType.SELLING) {
            stockWriter.performUpdate(batchId) { db ->
                db.stockBatchQueries.updatePrice(
                    selling_price_amount_minor = request.newPrice.amountMinor,
                    selling_price_currency = request.newPrice.currencyCode,
                    id = batchId,
                    actor_id = actorId,
                    updated_at = request.createdAt
                )
                val snapshot =
                    db.batchSnapshotQueries.selectById(batchId).executeAsOneOrNull()?.copy(
                        selling_price_amount_minor = request.newPrice.amountMinor,
                        selling_price_currency = request.newPrice.currencyCode,
                    )
                if (snapshot != null) {
                    db.batchSnapshotQueries.insertOrReplace(snapshot)
                }
            }
        } else {
            stockWriter.performUpdate(batchId) { db ->
                db.stockBatchQueries.updateCostPrice(
                    cost_price_amount_minor = request.newPrice.amountMinor,
                    cost_price_currency = request.newPrice.currencyCode,
                    price_update_reason = request.reason!!,
                    id = batchId,
                    updated_at = request.createdAt,
                    actor_id = actorId
                )
            }
        }
        val batch = stockBatchDao.getItem(batchId)

        if (batch != null) {
            EventBus.publish(
                EntityChangeEvent(
                    seq = 0L,
                    entityType = EntityType.STOCK_BATCH.name,
                    entityId = batchId,
                    organizationId = "",
                    workspaceId = batch.workspace_id,
                    branchId = batch.branch_id,
                    action = "UPDATED",
                    timestamp = Clock.System.now()
                )
            )
        }
    }

    override suspend fun updateBatchMetadata(
        batchId: String,
        batchNumber: String,
        productId: String,
        expiryDate: LocalDate?,
        actorId: String,
        actorName: String
    ): Result<Unit> {
        val updatedAt = Clock.System.now()
        val existing = stockBatchDao.getItem(batchId) ?: error("Batch not found")

        val batchCheck = batchNumber.let {
            stockBatchDao.findByBatchNumber(batchNumber = it, productId = productId)
        }

        if (batchCheck != null && batchCheck.batch_id != batchId) {
            error("Batch number already in use by another record.")
        }

        val request = UpdateBatchMetadataRequest(
            batchId = batchId,
            workspaceId = existing.workspace_id,
            branchId = existing.branch_id,
            batchNumber = batchNumber,
            productId = productId,
            expiryDate = expiryDate,
            updatedAt = updatedAt
        )

        val action: suspend () -> Unit = {
            // Snapshots cache the expiry date, so we use stockWriter
            stockWriter.performUpdate(batchId) { db ->
                db.stockBatchQueries.updateBatchMetadata(
                    batch_number = batchNumber,
                    expiry_date = expiryDate?.toString(),
                    expiry_date_ms = expiryDate?.toEpochDays(),
                    updated_at = updatedAt,
                    id = batchId
                )
            }
        }

        if (sessionManager.isSubscribed(existing.workspace_id)) {
            return resultOf { api.updateMetadata(request) }
                .onSuccess { action() }
                .recover {
                    if (isErrorRetriable(it)) {
                        action()
                        enqueueRequest(
                            endpoint = "/api/v1/stock-batches/${request.batchId}/metadata",
                            payload = Json.encodeToString(
                                UpdateBatchMetadataRequest.serializer(),
                                request
                            ),
                            workspaceId = existing.workspace_id,
                            eventAction = "UPDATE_BATCH_METADATA",
                            entityId = batchId,
                            dependantId = batchId,
                            method = RequestMethod.PUT,
                            actorId = actorId,
                            actorName = actorName,
                            branchId = existing.branch_id,
                            routingChainJson = ActiveRoutingChainHolder.get()
                        )
                    }
                }
        }

        action()
        return Result.success(Unit)
    }


    override suspend fun restock(
        request: RestockRequest,
        actorId: String,
        actorName: String
    ): Result<Unit> = runCatching {
        request.validate()

        request.batchNumber.let {
            stockBatchDao.findByBatchNumber(request.batchNumber, request.productId)?.let {
                throw IllegalArgumentException("Batch ${request.batchNumber} already exists.")
            }
        }


        dbManager.getDatabase().productListItemViewQueries
            .findById(request.productId)
            .executeAsOneOrNull()
            ?: productDao.findById(request.productId).firstOrNull()
            ?: error("Product not found")

        val batch = StockBatch(
            id = request.id,
            workspaceId = request.workspaceId,
            branchId = request.branchId,
            productId = request.productId,
            batchNumber = request.batchNumber,
            expiryDate = request.expiryDate,
            initialQuantity = request.quantity,
            currentQuantity = request.quantity,
            costPrice = request.costPrice,
            sellingPrice = request.sellingPrice,
            createdAt = request.createdAt,
            updatedAt = request.createdAt,
            isDeleted = false,
            hasMovements = false
        )

        val action: suspend () -> Unit = {
            stockWriter.performRestock(
                productId = request.productId,
                batch = batch.toBatchEntity(),
                branchId = batch.branchId,
                quantity = batch.initialQuantity,
                actorId = actorName
            )
        }

        // Logic: Satellite or cloud deploy
        if (sessionManager.isSubscribed(request.workspaceId)) {
            try {
                api.restock(request)
            } catch (e: Exception) {
                if (isErrorRetriable(e)) {
                    enqueueRequest(
                        endpoint = "/api/v1/stock-batches/push/restock",
                        payload = null,
                        workspaceId = request.workspaceId,
                        entityId = request.id,
                        branchId = request.branchId,
                        dependantId = request.productId,
                        method = RequestMethod.POST,
                        eventAction = "RESTOCK",
                        actorId = actorId,
                        actorName = actorName,
                        routingChainJson = ActiveRoutingChainHolder.get()
                    )
                } else {
                    // Hard Logic Error (400 Bad Request).
                    throw e
                }
            }
        }

        // save stock and movement
        action()

        EventBus.publish(
            EntityChangeEvent(
                seq = 0L,
                entityType = EntityType.STOCK_BATCH.name,
                entityId = request.id,
                organizationId = "",
                workspaceId = request.workspaceId,
                branchId = request.branchId,
                action = "CREATED",
                timestamp = Clock.System.now()
            )
        )

        scope.launch {
            telemetryClient.trackApplicationEvent(
                actorId = actorId,
                entityType = "STOCK_BATCH",
                entityId = request.id,
                action = "RESTOCK",
                summary = "Restocked product: ${request.productId} with ${request.quantity} units (Batch: ${request.batchNumber})"
            )
        }
    }

    override suspend fun adjustStock(
        request: AdjustStockRequest,
        actorId: String,
        actorName: String
    ): Result<Unit> = runCatching {
        val existing = stockBatchDao.getItem(request.batchId)
            ?: throw IllegalStateException("Batch not found")

        val action: suspend () -> Unit = {
            stockWriter.performAdjustment(
                productId = existing.product_id,
                batchId = request.batchId,
                branchId = existing.branch_id,
                wsId = existing.workspace_id,
                quantity = request.quantity,
                batchNumber = existing.batch_number,
                reason = request.reason,
                actorId = actorId
            )
        }

        if (sessionManager.isSubscribed(request.workspaceId)) {
            try {
                api.adjust(request)
                // Wait for network
            } catch (e: Exception) {
                if (isErrorRetriable(e)) {
                    enqueueRequest(
                        endpoint = "/api/v1/stock-batches/push/${request.batchId}/adjust",
                        payload = Json.encodeToString(AdjustStockRequest.serializer(), request),
                        workspaceId = existing.workspace_id,
                        entityId = request.batchId,
                        dependantId = request.batchId,
                        eventAction = "STOCK_ADJUSTMENT",
                        method = RequestMethod.PUT,
                        actorId = actorId,
                        actorName = actorName,
                        routingChainJson = ActiveRoutingChainHolder.get(),
                        branchId = existing.branch_id
                    )
                } else {
                    throw e
                }
            }
        }

        action()

        EventBus.publish(
            EntityChangeEvent(
                seq = 0L,
                entityType = EntityType.STOCK_BATCH.name,
                entityId = request.batchId,
                organizationId = "",
                workspaceId = existing.workspace_id,
                branchId = existing.branch_id,
                action = "UPDATED",
                timestamp = Clock.System.now()
            )
        )

        scope.launch {
            telemetryClient.trackApplicationEvent(
                actorId = actorId,
                entityType = "STOCK_BATCH",
                entityId = request.batchId,
                action = "ADJUST",
                summary = "Adjusted stock for batch ${existing.batch_number} by ${request.quantity} units. Reason: ${request.reason}"
            )
        }
    }

    override suspend fun onRefresh(): Result<Unit> = runCatching {
        if (sessionManager.isSubscribed()) {
            syncEngine.requestSync(EntityType.STOCK_BATCH)
        }
    }

    override suspend fun delete(
        id: String,
        actorId: String,
        actorName: String
    ) {
        val existing = stockBatchDao.getItem(id) ?: error("Batch not found")

        val action: suspend () -> Unit = {
            stockWriter.performBatchDeletion(
                productId = existing.product_id,
                batchId = id,
                branchId = existing.branch_id,
                batchNumber = existing.batch_number,
                wsId = existing.workspace_id,
                actorId = actorId
            )
        }

        if (sessionManager.isSubscribed(existing.workspace_id)) {
            try {
                api.delete(id)
            } catch (e: Exception) {
                if (isErrorRetriable(e).not()) throw e
                enqueueRequest(
                    endpoint = "api/v1/stock-batches/$id",
                    method = RequestMethod.DELETE,
                    payload = null,
                    workspaceId = existing.workspace_id,
                    entityId = id,
                    dependantId = id,
                    eventAction = "DELETE",
                    actorId = actorId,
                    actorName = actorName,
                    branchId = existing.branch_id,
                    routingChainJson = ActiveRoutingChainHolder.get()
                )
            }
        }

        action()

        scope.launch {
            telemetryClient.trackApplicationEvent(
                actorId = actorId,
                entityType = "STOCK_BATCH",
                entityId = id,
                action = "DELETE",
                summary = "Deleted stock batch: ${existing.batch_number} (Product ID: ${existing.product_id})"
            )
        }
    }


    private suspend fun enqueueRequest(
        endpoint: String,
        payload: String?,
        workspaceId: String,
        branchId: String,
        entityId: String,
        dependantId: String?,
        eventAction: String,
        method: RequestMethod,
        actorId: String,
        actorName: String,
        routingChainJson: String? = null
    ) {
        stockWriter.performUpdate("") { db ->
            db.pendingRequestQueries.insertRequest(
                id = generateUUID(),
                payload = payload,
                workspace_id = workspaceId,
                endpoint = endpoint,
                method = method,
                entityType = EntityType.STOCK_BATCH,
                entity_id = entityId,
                branch_id = branchId,
                dependant_id = dependantId,
                eventAction = eventAction,
                routing_chain_json = routingChainJson,
                actor_id = actorId,
                actor_name = actorName,
                createdAt = Clock.System.now().toString()
            )
        }
    }
}
