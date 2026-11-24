package com.citacomsys.sales.common

import com.citacomsys.core.security.hashing.HashUtils
import com.citacomsys.shared.error.InsufficientStockException
import com.citacomsys.types.ActorType
import com.citacomsys.types.ReferenceType
import com.citacomsys.types.StockMovement
import com.citacomsys.types.StockMovementType
import com.citacomsys.module.inventory.repository.StockBatchRepository
import com.citacomsys.module.inventory.repository.ProductRepository
import com.citacomsys.module.inventory.repository.StockMovementRepository
import com.citacomsys.types.generateUUID
import com.citacomsys.api.sales.dto.BatchAllocation
import com.citacomsys.api.sales.dto.SaleItemSnapshot
import io.ktor.server.plugins.NotFoundException
import com.citacomsys.types.Money
import kotlinx.serialization.json.Json
import kotlin.time.Clock.System.now

/**
 * Shared logic for calculating sale totals, generating stock movements,
 * and auditing pricing discrepancies for loss analysis.
 */
internal class SalesComputer(
    private val inventoryRepo: StockBatchRepository,
    private val stockRepo: StockMovementRepository,
    private val productRepo: ProductRepository
) {

    val hashingJson = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = true
    }

    /**
     * Generates a high-performance hash of the current physical state.
     */
    suspend fun getInventoryHash(workspaceId: String, productIds: List<String>): String {
        val states = stockRepo.getProductsState(workspaceId, productIds)

        val canonicalStates = states.sortedBy { it.productId }

        val canonicalString = hashingJson.encodeToString(canonicalStates)

        return HashUtils.sha256(canonicalString)
    }

    /**
     * Unified fulfillment engine.
     */
    suspend fun computeItemFulfillment(
        workspaceId: String,
        productId: String,
        quantity: Int,
        quoteUnitPrice: Money?,
        shouldLock: Boolean
    ): SaleItemSnapshot {
        val product = productRepo.getById(productId, workspaceId)
            ?: throw NotFoundException("Product not found")

        val batchSnapshots = inventoryRepo.getBatchSnapshotsFIFO(
            workspaceId = workspaceId, productId = productId,
            shouldLock = shouldLock
        )

        var remaining = quantity
        val allocations = mutableListOf<BatchAllocation>()
        var totalAmount = Money.zero()
        var totalPriceVariance = Money.zero()
        var actualPrice: Money = quoteUnitPrice ?: Money.zero()

        for (snapshot in batchSnapshots) {
            if (remaining <= 0) break
            if (snapshot.availableQuantity <= 0) continue

            val taken = minOf(snapshot.availableQuantity, remaining)

            val expectedPrice = snapshot.unitPrice
            actualPrice = quoteUnitPrice ?: expectedPrice

            val lineVariance = (actualPrice - expectedPrice) * taken

            allocations.add(
                BatchAllocation(
                    batchId = snapshot.batchId,
                    batchNumber = snapshot.batchNumber,
                    quantity = taken,
                    unitPrice = actualPrice,
                    expectedCostPrice = expectedPrice,
                    expiryDate = snapshot.expiryDate
                )
            )

            totalAmount += (actualPrice * taken)
            totalPriceVariance += lineVariance
            remaining -= taken
        }

        if (remaining > 0) {
            val totalAvailable = batchSnapshots.sumOf { it.availableQuantity }
            throw InsufficientStockException(
                "Insufficient stock for ${product.name}",
                mapOf(productId to totalAvailable.toString())
            )
        }

        return SaleItemSnapshot(
            productId = productId,
            productName = product.name,
            requestedQuantity = quantity,
            fulfilledQuantity = quantity,
            unitPrice = actualPrice,
            totalAmount = totalAmount,
            priceVariance = totalPriceVariance,
            batches = allocations
        )
    }

    /**
     * Generates StockMovement records from a list of computations.
     */
    fun buildMovements(
        workspaceId: String,
        saleId: String,
        branchId: String,
        items: List<SaleItemSnapshot>
    ): List<StockMovement> {
        return items.flatMap { comp ->
            comp.batches.map { sold ->
                StockMovement(
                    id = generateUUID(),
                    workspaceId = workspaceId,
                    productId = comp.productId,
                    batchId = sold.batchId,
                    branchId = branchId,
                    batchNumber = sold.batchNumber,
                    productName = comp.productName,
                    type = StockMovementType.OUT,
                    quantity = sold.quantity,
                    referenceType = ReferenceType.SALE,
                    referenceId = saleId,
                    actorType = ActorType.USER,
                    createdAt = now()
                )
            }
        }
    }
}
