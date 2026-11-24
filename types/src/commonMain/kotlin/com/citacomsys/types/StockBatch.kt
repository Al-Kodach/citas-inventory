package com.citacomsys.types

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

typealias BatchId = String

/**
 * Heavy management domain model for a single stock batch.
 * Contains full cost pricing, audit reasons, and movement flags.
 * Queried when viewing detailed batch metadata or performing adjustments.
 */
@Serializable
data class StockBatch(
    val id: String,
    val workspaceId: String,
    val branchId: String,
    val productId: String,
    val batchNumber: String,
    val expiryDate: LocalDate?,
    val initialQuantity: Int,
    val currentQuantity: Int,
    val hasMovements: Boolean,
    val costPrice: Money,
    val costPriceChangeReason: String? = null,
    val sellingPrice: Money,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val isDeleted: Boolean = false
)

/**
 * Combined snapshot containing both batch inventory levels and product metadata.
 * Used internally for alerts, FEFO expiry tracking, and cross-domain reporting
 * where both batch and product information are required simultaneously.
 */
@Serializable
@SerialName("BatchStockSnapshot")
data class BatchStockSnapshot(
    val batchId: String,
    val workspaceId: String,
    val batchNumber: String,
    val branchId: String,
    // --- Product Identity ---
    val productId: String,
    val productName: String,
    val brandName: String,
    val unitOfMeasure: String,
    // --- Financials & Expiry ---
    val availableQuantity: Int,
    val initialQuantity: Int,
    val unitPrice: Money,
    val expiryDate: LocalDate?,
    val batchCreatedAt: Instant,
    val versionTimestamp: Instant
)

/**
 * Lean operational DTO for stock batch list displays and sync deltas.
 * Excludes product metadata (product name, brand) to avoid redundancy with ProductItem.
 */
@Serializable
data class BatchItem(
    val batchId: String,
    val productId: String,
    val workspaceId: String,
    val branchId: String,
    val batchNumber: String,
    val availableQuantity: Int,
    val sellingPriceAmountMinor: Long,
    val sellingPriceCurrency: String = "GHS",
    val expiryDate: String? = null,
    val versionTimestamp: Instant
)
