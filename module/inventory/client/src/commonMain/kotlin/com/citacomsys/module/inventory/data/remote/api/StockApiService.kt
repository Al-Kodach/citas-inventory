package com.citacomsys.module.inventory.data.remote.api

import com.citacomsys.module.inventory.model.StockTransferPayload
import com.citacomsys.module.inventory.request.AdjustStockRequest
import com.citacomsys.module.inventory.request.PriceChangeRequest
import com.citacomsys.module.inventory.request.RestockRequest
import com.citacomsys.module.inventory.request.UpdateBatchMetadataRequest
import com.citacomsys.types.BatchItem
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.MultiTargetDeltaRequest
import com.citacomsys.types.MultiTargetDeltaResponse
import com.citacomsys.types.StockBatch
import com.citacomsys.types.StockMovement
import kotlin.time.Instant

interface StockApiService {
    suspend fun restock(request: RestockRequest)

    suspend fun adjust(request: AdjustStockRequest)

    suspend fun updateMetadata(request: UpdateBatchMetadataRequest)

    suspend fun getBatchSnapshotChanges(
        workspaceId: String,
        branchId: String?,
        sinceTimestamp: Instant? = null
    ): ChangeListResponse<BatchItem, String>

    suspend fun getMultiTargetDelta(request: MultiTargetDeltaRequest): MultiTargetDeltaResponse

    suspend fun updatePrice(
        batchId: String,
        request: PriceChangeRequest
    )

    suspend fun getById(id: String): StockBatch

    suspend fun delete(id: String)

    suspend fun getMovementsForProduct(productId: String, since: Instant?): List<StockMovement>
    suspend fun getMovementsForBatch(productId: String, batchId: String, since: Instant?): List<StockMovement>
    suspend fun submitTransfer(payload: StockTransferPayload)
}
