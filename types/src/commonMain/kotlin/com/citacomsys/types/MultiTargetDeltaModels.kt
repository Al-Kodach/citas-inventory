package com.citacomsys.types

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class MultiTargetDeltaRequest(
    val workspaces: Map<String, List<BranchCursor>>
)

@Serializable
data class BranchCursor(
    val branchId: String,
    val lastBatchSyncAt: Instant? = null,
    val lastProductSyncAt: Instant? = null
)

@Serializable
data class MultiTargetDeltaResponse(
    val workspaceDeltas: Map<String, WorkspaceDeltaResponse>
)

@Serializable
data class WorkspaceDeltaResponse(
    val products: ChangeListResponse<ProductItem, String> = ChangeListResponse(emptyList(), emptyList()),
    val branchBatches: Map<String, ChangeListResponse<BatchItem, String>> = emptyMap()
)
