package com.citacomsys.types

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class StockMovement(
    val id: String,
    val branchId: String,
    val workspaceId: String,
    val productId: String,
    val batchId: String,
    val batchNumber: String,
    val productName: String? = null,

    val type: StockMovementType,
    val reason: String? = null,
    val quantity: Int,

    val referenceId: String?, // saleId, purchaseId, adjustmentId
    val referenceType: ReferenceType?,

    val actorType: ActorType,
    val createdAt: Instant
)

enum class StockMovementType {
    IN,        // Purchase / restock
    OUT,       // Sale
}

enum class ReferenceType {
    SALE,
    PURCHASE,
    ADJUSTMENT_IN,
    ADJUSTMENT_OUT,
    RETURN,
    TRANSFER_OUT,  // Deducted from source location
    TRANSFER_IN // Injected into destination location
}

enum class ActorType {
    USER,
    SYSTEM,
    DEVICE
}



