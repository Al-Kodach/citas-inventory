package com.citacomsys.module.inventory.service

import com.citacomsys.feature.api.telemetry.TelemetryLevel
import com.citacomsys.feature.api.telemetry.TelemetryAuditor
import com.citacomsys.core.logging.logApplicationEvent
import com.citacomsys.core.persistence.transaction.dbQuery
import com.citacomsys.core.domain.result.toUuid
import com.citacomsys.core.persistence.table.inventory.StockBatchesTable
import com.citacomsys.core.security.resource_quota_quard.WorkspaceQuotaGuard
import com.citacomsys.module.inventory.model.StockTransferPayload
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.module.inventory.repository.StockBatchRepository
import com.citacomsys.module.inventory.repository.StockMovementRepository
import com.citacomsys.module.inventory.request.AdjustStockRequest
import com.citacomsys.module.inventory.request.PriceChangeRequest
import com.citacomsys.module.inventory.request.PriceType
import com.citacomsys.module.inventory.request.RestockRequest
import com.citacomsys.module.inventory.request.UpdateBatchMetadataRequest
import com.citacomsys.module.inventory.validation.StockBatchValidator
import com.citacomsys.types.*
import mu.KotlinLogging
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.math.abs
import kotlin.time.Clock.System.now
import kotlin.time.Instant

class StockBatchService(
    private val stockBatchRepository: StockBatchRepository,
    private val stockMovementRepo: StockMovementRepository,
    private val productRepo: ProductRepository,
    private val auditor: TelemetryAuditor,
    private val quotaGuard: WorkspaceQuotaGuard,
) {
    private val logger = KotlinLogging.logger { }
    suspend fun restock(
        request: RestockRequest,
        workspaceId: String
    ): Result<Unit> = runCatching {
        quotaGuard.ensureAvailability(workspaceId, ResourceType.BATCH)

        val product = productRepo.getById(request.productId, workspaceId)
            ?: run {
                auditor.logApplicationEvent(
                    "PRODUCT_RESTOCK",
                    entityType = EntityType.STOCK_BATCH.name,
                    entityId = request.productId,
                    summary = "Failed to save stock as product not found"
                )
                error("Product ${request.productId} not found in your inventory")
            }

        val batch = StockBatch(
            id = request.id,
            workspaceId = workspaceId,
            branchId = request.branchId,
            productId = request.productId,
            batchNumber = request.batchNumber,
            expiryDate = request.expiryDate,
            costPrice = request.costPrice,
            sellingPrice = request.sellingPrice,
            initialQuantity = request.quantity,
            currentQuantity = request.quantity,
            hasMovements = false,
            createdAt = request.createdAt,
            updatedAt = request.createdAt
        )

        StockBatchValidator.validateRestock(
            originApp = product.type,
            batchNumber = batch.batchNumber,
            quantity = request.quantity,
            costPriceMinor = batch.costPrice.amountMinor,
            sellingPriceMinor = batch.sellingPrice.amountMinor,
            expiryDate = batch.expiryDate,
            currentTime = batch.createdAt,
        )

        if (stockBatchRepository.isExist(
                request.batchNumber,
                request.productId,
                request.branchId,
                workspaceId
            )
        ) {
            error("Batch already exists")
        }

        val createdBatch = stockBatchRepository.save(batch)

        stockMovementRepo.create(
            StockMovement(
                id = generateUUID(),
                workspaceId = workspaceId,
                branchId = request.branchId,
                productId = request.productId,
                batchId = batch.id,
                type = StockMovementType.IN,
                quantity = request.quantity,
                referenceType = ReferenceType.PURCHASE,
                referenceId = null,
                batchNumber = batch.batchNumber,
                actorType = ActorType.USER,
                createdAt = request.createdAt
            )
        )

        auditor.logApplicationEvent(
            action = "STOCK_RESTOCK",
            entityType = EntityType.STOCK_BATCH.name,
            entityId = batch.id,
            summary = "New stock batch (${batch.batchNumber}) created for product ${product.name}. Quantity: ${request.quantity}",
            level = TelemetryLevel.INFO
        )

        Result.success(Unit)
    }

    suspend fun updateBatchMetadata(
        request: UpdateBatchMetadataRequest,
        workspaceId: String
    ) {
        stockBatchRepository.updateBatchMetadata(
            request.batchId,
            request.batchNumber,
            request.expiryDate,
            workspaceId = workspaceId
        )

        auditor.logApplicationEvent(
            action = "BATCH_METADATA_UPDATE",
            entityType = EntityType.STOCK_BATCH.name,
            entityId = request.batchId,
            summary = "Batch metadata updated (Number: ${request.batchNumber}, Expiry: ${request.expiryDate})",
            level = TelemetryLevel.INFO
        )
    }

    suspend fun adjust(request: AdjustStockRequest): Result<Unit> =
        runCatching {
            require(request.reason.isNotEmpty()) {
                "Reason for stock adjustment is required"
            }

            val targetBatch =
                stockBatchRepository.findSnapshotById(
                    batchId = request.batchId,
                    workspaceId = request.workspaceId
                ) ?: error("Requested batch for adjustment not found")

            val remains = targetBatch.availableQuantity - request.quantity
            if (remains < 0) {
                error("Requested quantity exceeds available stock")
            }

            val type = if (request.quantity > 0)
                StockMovementType.IN else StockMovementType.OUT

            val refType = if (StockMovementType.IN == type)
                ReferenceType.ADJUSTMENT_IN else ReferenceType.ADJUSTMENT_OUT

            stockMovementRepo.create(
                StockMovement(
                    id = generateUUID(),
                    workspaceId = request.workspaceId,
                    branchId = targetBatch.branchId,
                    productId = targetBatch.productId,
                    batchId = targetBatch.batchId,
                    batchNumber = targetBatch.batchNumber,
                    type = type,
                    reason = request.reason,
                    quantity = abs(request.quantity),
                    referenceType = refType,
                    referenceId = null,
                    actorType = ActorType.USER,
                    createdAt = request.createdAt
                )
            )

            auditor.logApplicationEvent(
                action = "STOCK_ADJUSTMENT",
                entityType = EntityType.STOCK_BATCH.name,
                entityId = request.batchId,
                summary = "Stock adjusted by ${request.quantity} for batch ${targetBatch.batchNumber}. Reason: ${request.reason}",
                level = TelemetryLevel.WARNING
            )
        }


    suspend fun executeTransfer(
        request: StockTransferPayload,
        workspaceId: String
    ): Result<Unit> = runCatching {
        val batchMetadata = stockBatchRepository.findById(
            batchId = request.batchId,
            workspaceId = workspaceId
        ) ?: error("Source stock batch not found or unauthorized access.")

        val batchStockSnapshot = stockBatchRepository.findSnapshotById(
            batchId = request.batchId,
            workspaceId = workspaceId
        ) ?: error("Source stock batch not found or unauthorized access.")

        if (batchStockSnapshot.availableQuantity < request.quantity) {
            error("Insufficient available inventory at source branch. Requested: ${request.quantity}, Available: ${batchStockSnapshot.availableQuantity}")
        }

        val destBatchId = stockBatchRepository.findOrCreateMatchingBranchBatch(
            workspaceId = workspaceId,
            branchId = request.destinationBranchId,
            productId = request.productId,
            batchNumber = batchMetadata.batchNumber,
            expiryDate = batchMetadata.expiryDate,
            costPrice = batchMetadata.costPrice,
            sellingPrice = batchMetadata.sellingPrice
        )

        val transactionReferenceId = "TRF-${request.id.take(8)}"
        val now = now()

        stockMovementRepo.create(
            StockMovement(
                id = generateUUID(),
                workspaceId = workspaceId,
                branchId = request.sourceBranchId,
                productId = request.productId,
                batchId = batchMetadata.id,
                batchNumber = batchMetadata.batchNumber,
                type = StockMovementType.OUT,
                quantity = request.quantity,
                reason = "Inter-branch transfer dispatched to branch: ${request.destinationBranchId}. Memo: ${request.reason}",
                referenceType = ReferenceType.TRANSFER_OUT,
                referenceId = transactionReferenceId,
                actorType = ActorType.USER,
                createdAt = now
            )
        )

        stockMovementRepo.create(
            StockMovement(
                id = generateUUID(),
                workspaceId = workspaceId,
                branchId = request.destinationBranchId,
                productId = request.productId,
                batchId = destBatchId,
                batchNumber = batchMetadata.batchNumber,
                type = StockMovementType.IN,
                quantity = request.quantity,
                reason = "Inter-branch transfer received from branch: ${request.sourceBranchId}. Memo: ${request.reason}",
                referenceType = ReferenceType.TRANSFER_IN,
                referenceId = transactionReferenceId,
                actorType = ActorType.USER,
                createdAt = now
            )
        )

        auditor.logApplicationEvent(
            action = "STOCK_TRANSFER",
            entityType = EntityType.STOCK_BATCH.name,
            entityId = batchMetadata.id,
            summary = "Moved ${request.quantity} units of batch ${batchMetadata.batchNumber} from branch ${request.sourceBranchId} to branch ${request.destinationBranchId}. Reason: ${request.reason}",
            level = TelemetryLevel.INFO
        )
    }


    suspend fun updatePrice(
        request: PriceChangeRequest,
        workspaceId: String
    ) {
        dbQuery {
            val batchId = request.batchId
            val newAmount = request.newPrice
            var oldAmount = Money.zero()

            var action = "STOCK_PRICE_UPDATE"

            val batch = StockBatchesTable.selectAll()
                .where { StockBatchesTable.id eq batchId.toUuid() }.single()

            if (request.type == PriceType.SELLING) {
                oldAmount = Money(
                    batch[StockBatchesTable.sellingPriceAmountMinor],
                    batch[StockBatchesTable.sellingPriceCurrency]
                )

                StockBatchesTable.update({
                    StockBatchesTable.id eq batchId.toUuid() and
                            (StockBatchesTable.workspaceId eq workspaceId.toUuid())
                }) {
                    it[sellingPriceAmountMinor] = newAmount.amountMinor
                    it[sellingPriceCurrency] = newAmount.currencyCode
                    it[updatedAt] = now()
                }
            } else {
                require(request.reason != null) {
                    "Please provide reason for cost price change"
                }

                oldAmount =
                    Money(
                        batch[StockBatchesTable.costPriceAmountMinor],
                        batch[StockBatchesTable.costPriceCurrency]
                    )

                StockBatchesTable.update({
                    StockBatchesTable.id eq batchId.toUuid() and
                            (StockBatchesTable.workspaceId eq workspaceId.toUuid())
                }) {
                    it[costPriceAmountMinor] = newAmount.amountMinor
                    it[costPriceCurrency] = newAmount.currencyCode
                    it[updatedAt] = now()
                }

                action = "STOCK_COST_UPDATE"
            }

            auditor.logApplicationEvent(
                action = action,
                entityId = request.batchId,
                entityType = EntityType.STOCK_BATCH.name,
                level = TelemetryLevel.WARNING,
                summary = "Price updated for batch ${request.batchId}"
            )
        }
    }

    suspend fun getById(id: String, workspaceId: String): StockBatch? {
        return stockBatchRepository.findById(id, workspaceId)
    }

    suspend fun getChangeList(
        since: Instant,
        organizationId: String,
        workspaceId: String,
        branchId: String?
    ): ChangeListResponse<BatchItem, String> {
        return stockBatchRepository.getChangeList(since, organizationId, workspaceId, branchId)
    }

    suspend fun getStockMovementsForBatch(
        productId: String,
        batchId: String,
        since: Instant?,
        workspaceId: String,
        branchId: String? = null,
    ): List<StockMovement> {
        return stockMovementRepo.getStockMovementsForProduct(
            productId,
            batchId,
            since,
            workspaceId,
            branchId,
        )
    }

    suspend fun delete(id: String, workspaceId: String) {
        stockBatchRepository.delete(workspaceId, id)

        auditor.logApplicationEvent(
            action = "BATCH_DELETED",
            entityType = EntityType.STOCK_BATCH.name,
            entityId = id,
            summary = "Stock batch $id was deleted",
            level = TelemetryLevel.WARNING
        )
    }
}
