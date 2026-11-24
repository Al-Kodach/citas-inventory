package com.citacomsys.module.inventory.request

import com.citacomsys.types.Money
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class AdjustStockRequest(
    val batchId: String, // The batch to adjust
    val workspaceId: String,
    val branchId: String,
    val productId: String,
    val quantity: Int, // positive or negative
    val reason: String,
    val createdAt: Instant = Clock.System.now()
)

@Serializable
data class UpdateBatchMetadataRequest(
    val batchId: String,
    val workspaceId: String,
    val branchId: String,
    val batchNumber: String,
    val productId: String,
    val expiryDate: LocalDate?,
    val updatedAt: Instant = Clock.System.now(),
)

@Serializable
data class PriceChangeRequest(
    val batchId: String,
    val workspaceId: String,
    val branchId: String,
    val newPrice: Money,
    val type: PriceType = PriceType.SELLING,
    val reason: String? = null,
    val createdAt: Instant = Clock.System.now()
)

enum class PriceType {
    COST,
    SELLING;

    companion object {
        fun from(s: String): PriceType {
            return when (s.lowercase()) {
                "cost" -> COST
                "selling" -> SELLING
                else -> throw IllegalArgumentException("Invalid price type: $s")
            }
        }
    }
}