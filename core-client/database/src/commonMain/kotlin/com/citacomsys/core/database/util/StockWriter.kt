package com.citacomsys.core.database.util

import com.citacomsys.api.sales.dto.BatchAllocation
import com.citacomsys.api.sales.dto.CreateSaleRequest
import com.citacomsys.api.sales.dto.SaleItemSnapshot
import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.database.*
import com.citacomsys.types.EntityType
import com.citacomsys.types.Money
import com.citacomsys.types.ReferenceType
import com.citacomsys.types.StockMovementType
import com.citacomsys.types.generateUUID
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class StockWriter(
    private val manager: InventoryDatabaseManager,
    private val clock: Clock = Clock.System
) {
    private val defaultTimestamp = Instant.parse("1970-01-01T00:00:00Z")


    /**
     * ATOMIC SALE WITH INLINE DEPENDENCY TRACKING & QUEUE DELEGATION
     */
    suspend fun performSale(
        sale: SaleEntity,
        items: List<SaleItemInput>,
        payments: List<PaymentEntity>,
        actorId: String,
        actorName: String,
        enqueueOnFailure: Boolean,
        originalRequestPayload: CreateSaleRequest? = null
    ) = internalPerformSale(
        sale,
        items,
        payments,
        actorId,
        actorName,
        enqueueOnFailure,
        originalRequestPayload
    )

    private suspend fun internalPerformSale(
        sale: SaleEntity,
        items: List<SaleItemInput>,
        payments: List<PaymentEntity>,
        actorId: String,
        actorName: String? = null,
        enqueueOnFailure: Boolean = false,
        originalRequestPayload: CreateSaleRequest? = null
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            db.saleQueries.insert(sale)
            payments.forEach { db.saleQueries.insertOrUpdatePayment(it) }

            val consumedBatchIds = mutableSetOf<String>()
            val branchId = sale.branch_id

            items.forEach { input ->
                var remainingToDeduct = input.quantity

                //  Read the live operational view which includes drift calculations
                val availableBatches = db.batchSnapshotQueries
                    .selectLiveAvailableForProduct(input.productId, branchId)
                    .executeAsList()

                for (batch in availableBatches) {
                    if (remainingToDeduct <= 0) break

                    val liveAvailable = batch.live_available.toInt()
                    if (liveAvailable <= 0) continue

                    val take = minOf(liveAvailable, remainingToDeduct)
                    consumedBatchIds.add(batch.batch_id)

                    val pName = db.productListItemViewQueries.findById(input.productId).executeAsOneOrNull()?.name ?: ""
                    // 2. Persist the Sale Line Item
                    db.saleQueries.insertOrUpdateSaleItem(
                        SaleItemEntity(
                            id = generateUUID(),
                            sale_id = sale.id,
                            branch_id = branchId,
                            product_id = input.productId,
                            product_name = pName,
                            batch_id = batch.batch_id,
                            batch_number = batch.batch_number,
                            quantity = take.toLong(),
                            sold_price_minor = batch.selling_price_amount_minor,
                            sold_price_currency = batch.selling_price_currency,
                            price_variance = 0L
                        )
                    )

                    // Persist the Local Movement Log
                    createMovement(
                        db = db,
                        productId = input.productId,
                        batchId = batch.batch_id,
                        wsId = batch.workspace_id,
                        branchId = branchId,
                        qty = take,
                        type = StockMovementType.OUT,
                        refType = ReferenceType.SALE,
                        refId = sale.id,
                        actorId = actorId,
                        batchNumber = batch.batch_number
                    )

                    remainingToDeduct -= take
                }

                // Safe Business Validation Check
                if (remainingToDeduct > 0) {
                    throw InsufficientStockException(
                        productId = input.productId,
                        missingQuantity = remainingToDeduct
                    )
                }
            }

            // ─── INLINE QUEUE INJECTION PHASE ───
            if (enqueueOnFailure && originalRequestPayload != null && actorName != null) {

                // Scan the consumed batch IDs to see which ones are waiting in the queue to be created on the cloud
                val unSyncedDependencies = consumedBatchIds.filter { bId ->
                    db.pendingRequestQueries.findPendingRestockByEntityId(bId)
                        .executeAsOneOrNull() != null
                }

                // Flatten them down to a simple comma-separated search target string
                val dependencyListString = if (unSyncedDependencies.isNotEmpty()) {
                    unSyncedDependencies.joinToString(separator = ",")
                } else null

                db.pendingRequestQueries.insertRequest(
                    id = generateUUID(),
                    payload = null,
                    workspace_id = sale.workspace_id,
                    branch_id = sale.branch_id,
                    endpoint = "api/v1/sales/sync",
                    method = RequestMethod.POST,
                    entityType = EntityType.SALE,
                    entity_id = sale.id,
                    routing_chain_json = null,
                    dependant_id = dependencyListString, // Flawless lineage tracking across multiple batch splits!
                    eventAction = "CREATE",
                    actor_id = actorId,
                    actor_name = actorName,
                    createdAt = clock.now().toString()
                )
            }
        }
    }

    /**
     * Handover Sync Engine: Marks local logs as accounted for up to the sync cutoff point.
     */
    suspend fun applySyncSnapshots(
        batchSnapshots: List<BatchSnapshotEntity>,
        syncCutoffTime: Instant
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            val productsIds = batchSnapshots
                .map { it.product_id }
                .distinct()

            // Clear out their local stubs in a single call before writing the new rows
            if (productsIds.isNotEmpty()) {
                db.batchSnapshotQueries.deleteStubsForProducts(productsIds)
            }

            db.stockBatchQueries.updateHasMovement(batchSnapshots.map { it.batch_id })

            // Process the updates and movements ledger smoothly
            batchSnapshots.forEach { snapshot ->
                db.batchSnapshotQueries.insertOrReplace(snapshot)

                db.stockMovementQueries.markAsAccountedForBatch(
                    batch_id = snapshot.batch_id,
                    cutoffTimestamp = syncCutoffTime
                )
            }
        }
    }


    /**
     * ATOMIC RESTOCK: Registers heavy metadata, handles stub cleanup, and logs the IN movement.
     */
    suspend fun performRestock(
        productId: String,
        batch: StockBatchEntity,
        branchId: String,
        quantity: Int,
        actorId: String
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            // Write the heavy storage management metadata record
            db.stockBatchQueries.insertOrReplace(batch)
            db.stockBatchQueries.updateHasMovement(listOf(batch.id))

            val product =
                db.productListItemViewQueries.findById(productId_ = productId).executeAsOne()

            // STUB CLEANUP: Evict the placeholder row if this is the first batch arriving for this product
            db.batchSnapshotQueries.deleteProductStub(productId)

            // SEED OPERATIONAL SNAPSHOT: Insert the real operational line for this specific batch
            db.batchSnapshotQueries.insertOrReplace(
                BatchSnapshotEntity(
                    batch_id = batch.id,
                    workspace_id = batch.workspace_id,
                    product_id = product.id,
                    branch_id = branchId,
                    batch_number = batch.batch_number,
                    available_quantity = quantity.toLong(),
                    selling_price_amount_minor = batch.selling_price_amount_minor,
                    selling_price_currency = batch.selling_price_currency,
                    expiry_date = batch.expiry_date,
                    version_timestamp = batch.updated_at
                )
            )

            // Record the inflow movement to calculate live_available instantly on the views
            createMovement(
                db = db,
                productId = batch.product_id,
                batchId = batch.id,
                wsId = batch.workspace_id,
                branchId = branchId,
                qty = quantity,
                type = StockMovementType.IN,
                refType = ReferenceType.PURCHASE,
                actorId = actorId,
                batchNumber = batch.batch_number
            )
        }
    }

    /**
     * ATOMIC ADJUSTMENT: Relative inventory level modifications (+/-)
     */
    suspend fun performAdjustment(
        productId: String,
        batchId: String,
        branchId: String,
        wsId: String,
        batchNumber: String,
        quantity: Int,
        reason: String?,
        actorId: String
    ) {
        val db = manager.getDatabase()
        val type = if (quantity > 0) StockMovementType.IN else StockMovementType.OUT
        val refType =
            if (quantity > 0) ReferenceType.ADJUSTMENT_IN else ReferenceType.ADJUSTMENT_OUT

        manager.transaction {
            createMovement(
                db = db,
                productId = productId,
                batchId = batchId,
                wsId = wsId,
                branchId = branchId,
                qty = abs(quantity),
                type = type,
                refType = refType,
                actorId = actorId,
                batchNumber = batchNumber,
                reason = reason
            )
        }
    }

    private suspend fun createMovement(
        db: InventoryDatabase,
        productId: String,
        batchId: String,
        branchId: String,
        qty: Int,
        type: StockMovementType,
        refType: ReferenceType,
        wsId: String,
        actorId: String,
        refId: String? = null,
        batchNumber: String,
        reason: String? = null
    ) {
        db.stockMovementQueries.insert(
            StockMovementEntity(
                id = generateUUID(),
                workspace_id = wsId,
                branch_id = branchId,
                product_id = productId,
                batch_id = batchId,
                type = type.name,
                quantity = qty.toLong(),
                reference_id = refId,
                reference_type = refType.name,
                batch_number = batchNumber,
                reason = reason,
                actor_id = actorId,
                actor_type = "USER",
                created_at = clock.now(), // Uses steady local clock sequence
                is_accounted_for = false
            )
        )
    }

    /**
     * PURE OFFLINE CATALOG INITIALIZATION
     * Registers a new master product entry into the catalog and binds a single,
     * clean zero-stock placeholder stub. This enables the item to show up on the
     * Sales App list screen immediately without inventing fake batch records.
     */
    suspend fun performOfflineProductInitialization(
        product: ProductEntity
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            // Persist the primary master catalog entry
            db.productQueries.insertOrReplace(product)

            db.productItemQueries.insertOrReplace(
                ProductItemEntity(
                    id = product.id,
                    name = product.name,
                    brand_name = product.brand_name,
                    unit_of_measure = product.unit_of_measure,
                    reorder_level = product.reorder_level,
                    product_type = product.product_type,
                    total_available = 0,
                    current_price_amount_minor = 0L,
                    current_price_currency = "GHS",
                    workspace_id = product.workspace_id,
                    version_timestamp = product.created_at,
                    is_active = true,
                    sku = product.sku,
                    barcode = product.barcode
                )
            )

            // IDEMPOTENT STUB SEEDING:
            // Visibility across all branches is handled by the ProductListItemView when branchId = ''
            db.batchSnapshotQueries.insertPlaceholderStub(
                batch_id = product.id,
                workspace_id = product.workspace_id,
                product_id = product.id,
                branch_id = "", // Workspace-global stub
                batch_number = "",
                available_quantity = 0L,
                selling_price_amount_minor = 0L,
                selling_price_currency = "GHS",
                expiry_date = null,
                version_timestamp = defaultTimestamp
            )
        }
    }

    /**
     * ATOMIC PRODUCT DELETION
     * Soft-deletes the product metadata and clears all active physical stock paths
     * across ALL branches. This is a global workspace-wide action.
     */
    suspend fun performProductDeletion(
        productId: String,
        actorId: String,
        wsId: String,
        auditBranchId: String,
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            // Scramble and soft-delete the heavy core catalog record
            db.productQueries.softDeleteWithScramble(
                id = productId,
                updatedAt = clock.now()
            )

            // Global Wipe: Erase active operational snapshot lines across all branches
            db.batchSnapshotQueries.deleteSnapshotsForProduct(productId = productId)

            // Global Wipe: Mark all local physical batches across all branches as deleted
            db.stockBatchQueries.markAllBatchesAsDeletedForProduct(productId = productId)

            db.stockMovementQueries.insert(
                StockMovementEntity(
                    id = generateUUID(),
                    workspace_id = wsId,
                    branch_id = auditBranchId,
                    product_id = productId,
                    batch_id = productId, // Fallback to product identity
                    type = StockMovementType.OUT.name,
                    quantity = 0L, // Terminal marker
                    reference_id = productId,
                    reference_type = "PRODUCT_DELETED",
                    batch_number = "",
                    reason = "Product catalog terminal purge requested offline (Global Action).",
                    actor_id = actorId,
                    actor_type = "USER",
                    created_at = clock.now(),
                    is_accounted_for = false
                )
            )
        }
    }

    /**
     * ATOMIC BATCH DELETION / ARCHIVAL
     * Soft-deletes an individual batch, drops its visible snapshot row,
     * and balances the remaining stock out of circulation.
     */
    suspend fun performBatchDeletion(
        productId: String,
        batchId: String,
        branchId: String,
        batchNumber: String,
        actorId: String,
        wsId: String
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            // Soft-delete or scramble the physical batch metadata record
            db.stockBatchQueries.softDeleteWithScrumble(
                id = batchId,
                actorId = actorId,
                updatedAt = clock.now()
            )

            // Evict this specific batch entry line from the active operational snapshot table
            db.batchSnapshotQueries.deleteSnapshotForBatch(batchId = batchId)

            // Optional Handover Guard:
            // If this was the last batch for the product, inject an empty catalog product-stub row
            // so the product stays visible on the management panels with 0 stock.
            val totalRemainingActiveLines = db.batchSnapshotQueries
                .countActiveLinesForProduct(productId = productId)
                .executeAsOne()

            if (totalRemainingActiveLines == 0L) {
                val product = db.productQueries.selectById(productId).executeAsOne()
                db.batchSnapshotQueries.insertPlaceholderStub(
                    batch_id = product.id,
                    workspace_id = product.workspace_id,
                    product_id = product.id,
                    branch_id = "", // Workspace-global stub
                    batch_number = "",
                    available_quantity = 0L,
                    selling_price_amount_minor = 0L,
                    selling_price_currency = "GHS",
                    expiry_date = null,
                    version_timestamp = defaultTimestamp
                )
            }

            // Log a terminal outflow audit adjustment to inform the server of the localized removal
            createMovement(
                db = db,
                productId = productId,
                wsId = wsId,
                batchId = batchId,
                branchId = branchId,
                qty = 0, // Terminal tracking item marker
                type = StockMovementType.OUT,
                refType = ReferenceType.ADJUSTMENT_OUT,
                actorId = actorId,
                batchNumber = batchNumber,
                reason = "Batch terminal archival purge executed offline."
            )
        }
    }

    /**
     * ATOMIC EXTENSION UPDATE: Updates heavy metadata blobs and enqueues sync events.
     */
    suspend fun performProductUpdate(
        product: ProductEntity
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            db.productQueries.insertOrReplace(product)
        }
    }

    /**
     * ATOMIC OFFLINE ENQUEUE: Adds a request to the sync queue.
     */
    suspend fun enqueuePendingRequest(
        request: PendingRequestEntity
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            db.pendingRequestQueries.insert(request)
        }
    }

    /**
     * ATOMIC GENERIC UPDATE: Wrapper for safe transactional batch/product modifications.
     */
    suspend fun performUpdate(
        id: String,
        block: suspend (InventoryDatabase) -> Unit
    ) {
        val db = manager.getDatabase()
        manager.transaction {
            block(db)
        }
    }

    /**
     * GENERATE PREVIEW: Dry-run of the sale logic to determine fulfillment and pricing.
     */
    suspend fun generatePreview(
        branchId: String,
        items: List<SaleItemInput>
    ): List<SaleItemSnapshot> = manager.dbQuery {
        val db = manager.getDatabase()

        items.map { input ->
            var remainingToDeduct = input.quantity
            val allocations = mutableListOf<BatchAllocation>()

            val availableBatches = db.batchSnapshotQueries
                .selectLiveAvailableForProduct(input.productId, branchId)
                .executeAsList()

            var productName = db.productListItemViewQueries.findById(input.productId).executeAsOneOrNull()?.name ?: "Unknown Item"
            var totalFulfilled = 0
            var totalPriceMinor = 0L
            var currency = "GHS"

            for (batch in availableBatches) {
                if (remainingToDeduct <= 0) break
                currency = batch.selling_price_currency

                val liveAvailable = batch.live_available.toInt()
                if (liveAvailable <= 0) continue

                val take = minOf(liveAvailable, remainingToDeduct)
                allocations.add(
                    BatchAllocation(
                        batchId = batch.batch_id,
                        batchNumber = batch.batch_number,
                        quantity = take,
                        unitPrice = Money(
                            batch.selling_price_amount_minor,
                            batch.selling_price_currency
                        ),
                        expectedCostPrice = Money.zero(), // Placeholder
                        expiryDate = batch.expiry_date?.let { LocalDate.parse(it) }
                    )
                )

                totalFulfilled += take
                totalPriceMinor += (batch.selling_price_amount_minor * take)
                remainingToDeduct -= take
            }

            val averagePrice = if (totalFulfilled > 0) totalPriceMinor / totalFulfilled else 0L

            SaleItemSnapshot(
                productId = input.productId,
                productName = productName,
                requestedQuantity = input.quantity,
                fulfilledQuantity = totalFulfilled,
                unitPrice = Money(averagePrice, currency),
                totalAmount = Money(totalPriceMinor, currency),
                priceVariance = Money.zero(),
                batches = allocations
            )
        }
    }
}

data class SaleItemInput(val productId: String, val quantity: Int)

class InsufficientStockException(val productId: String, val missingQuantity: Int) :
    Exception("Insufficient local inventory space to fulfill product: $productId")
