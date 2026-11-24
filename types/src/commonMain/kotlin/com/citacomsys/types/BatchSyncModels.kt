package com.citacomsys.types

import kotlinx.serialization.Serializable

@Serializable
data class BatchSyncEnvelope(
    val items: List<BatchSyncItem>
)

@Serializable
data class BatchSyncItem(
    val queueId: String,                 // Local SQLDelight row ID
    val idempotencyKey: String,        // Unique UUID or "${nodeId}_${queueId}"
    val entityType: String,
    val entityId: String,
    val dependantId: String? = null,
    val method: String,                // "POST", "PUT", "DELETE"
    val endpoint: String,              // e.g. "/api/v1/sales"
    val metadataBase64: String,        // Your existing X-Sync-Metadata payload
    val payloadJson: String? = null    // Raw JSON string of the entity
)

@Serializable
data class BatchSyncResponse(
    val results: List<BatchItemResult>
)

@Serializable
data class BatchItemResult(
    val queueId: String,
    val idempotencyKey: String,
    val status: Int,                   // 200, 201, 409, 422, 500
    val body: String? = null,
    val error: String? = null
)

data class DispatchOutcome(
    val status: Int,
    val body: String? = null,
    val error: String? = null
)
