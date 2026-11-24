package com.citacomsys.api.sales.dto

import com.citacomsys.types.generateUUID
import com.citacomsys.api.sales.SaleItem
import com.citacomsys.types.Money
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Instant

// The DETAILS of which batch was used (The "Breakdown")
@Serializable
data class BatchAllocation(
    val batchId: String,
    val batchNumber: String,
    val quantity: Int,
    val unitPrice: Money,
    val expectedCostPrice: Money,
    val expiryDate: LocalDate?
)

// The DEFINITIVE SNAPSHOT of an item (Used for Response, Cache, and Persistence)
@Serializable
data class SaleItemSnapshot(
    val productId: String,
    val productName: String,
    val requestedQuantity: Int,
    val fulfilledQuantity: Int,
    val unitPrice: Money,      // The average price shown to user
    val totalAmount: Money,    // price * fulfilled
    val priceVariance: Money,      // calculated during FIFO
    val batches: List<BatchAllocation> // The breakdown
) {
    fun toRecord(saleId: String): SaleItem {
        return SaleItem(
            id = generateUUID(),
            saleId = saleId,
            productId = productId,
            productName = productName,
            batchId = batches.first().batchId,
            batchNumber = batches.first().batchNumber,
            expiryDate = batches.first().expiryDate,
            quantity = fulfilledQuantity,
            soldUnitPrice = unitPrice,
            priceVariance = priceVariance
        )
    }
}

@Serializable
data class SaleSnapshot(
    val id: String,
    val workspaceId: String,
    val items: List<SaleItemSnapshot>,
    val totalAmount: Money,
    val totalQuantity: Int,
    val expiresAt: Instant,
    val requestHash: String,
    val inventoryHash: String,
)