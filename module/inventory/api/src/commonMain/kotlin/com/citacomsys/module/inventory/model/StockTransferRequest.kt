package com.citacomsys.module.inventory.model

import kotlinx.serialization.Serializable

@Serializable
data class StockTransferPayload(
    val id: String,
    val workspaceId: String,
    val productId: String,
    val batchId: String,               // Originating batch ID at source location
    val sourceBranchId: String,
    val destinationBranchId: String,
    val quantity: Int,
    val reason: String,                // Explicitly captured human-readable log notes
    val actorId: String
)