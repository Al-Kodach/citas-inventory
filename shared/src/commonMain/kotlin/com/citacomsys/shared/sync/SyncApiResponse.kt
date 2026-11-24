package com.citacomsys.shared.sync

import kotlinx.serialization.Serializable

/**
 * A generic response payload for any batch synchronization endpoint.
 * It provides a clearAll, detailed report of which items succeeded and which failed.
 */
@Serializable
data class SyncApiResponse(
    val syncedIds: List<String>,
    val failures: List<SyncFailure>
)


/**
 * Represents a single failed item during a batch sync operation.
 *
 * @param id The unique client-generated ID of the request that failed.
 * @param reason A user-friendly (or developer-friendly) message explaining why it failed.
 *               e.g., "Insufficient stock for product XYZ", "Quote has expired".
 */
@Serializable
data class SyncFailure(
    val id: String,
    val reason: String
)