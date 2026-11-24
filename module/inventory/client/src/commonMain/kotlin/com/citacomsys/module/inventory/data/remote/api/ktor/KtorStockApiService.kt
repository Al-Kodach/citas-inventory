package com.citacomsys.module.inventory.data.remote.api.ktor

import com.citacomsys.core.remote.BaseApi
import com.citacomsys.module.inventory.data.remote.api.StockApiService
import com.citacomsys.module.inventory.model.StockTransferPayload
import com.citacomsys.types.StockBatch
import com.citacomsys.types.StockMovement
import com.citacomsys.module.inventory.request.AdjustStockRequest
import com.citacomsys.module.inventory.request.PriceChangeRequest
import com.citacomsys.module.inventory.request.RestockRequest
import com.citacomsys.module.inventory.request.UpdateBatchMetadataRequest
import com.citacomsys.types.BatchItem
import com.citacomsys.types.ChangeListResponse
import com.citacomsys.types.MultiTargetDeltaRequest
import com.citacomsys.types.MultiTargetDeltaResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlin.time.Instant

internal class KtorStockApiServiceService(
    client: HttpClient,
    private val endpoint: String = "api/v1/stock-batches"
) : StockApiService, BaseApi(client) {
    override suspend fun restock(request: RestockRequest) {
        return executeRequest {
            client.post(endpoint) {
                setBody(request)
            }
        }
    }

    override suspend fun adjust(request: AdjustStockRequest) {
        return executeRequest {
            client.put("$endpoint/${request.batchId}") {
                setBody(request)
            }
        }
    }

    override suspend fun updateMetadata(request: UpdateBatchMetadataRequest) {
        return executeRequest {
            client.put("$endpoint/${request.batchId}/meta") {
                setBody(request)
            }
        }
    }

    override suspend fun updatePrice(
        batchId: String,
        request: PriceChangeRequest,
    ) {
        return executeRequest {
            client.put("$endpoint/$batchId/price") {
                setBody(request)
            }
        }
    }

    override suspend fun getMovementsForProduct(
        productId: String,
        since: Instant?
    ): List<StockMovement> = executeRequest {
        client.get("${endpoint}/$productId/movement") {
            since?.let {
                url {
                    parameters.append("since", since.toString())
                }
            }
        }
    }

    override suspend fun getMovementsForBatch(
        productId: String,
        batchId: String,
        since: Instant?
    ): List<StockMovement> = executeRequest {
        client.get("${endpoint}/$productId/$batchId/movement") {
            since?.let {
                url {
                    parameters.append("since", since.toString())
                }
            }
        }
    }

    override suspend fun submitTransfer(payload: StockTransferPayload) {
        return executeRequest {
            client.post("api/v1/stock-transfers") {
                setBody(payload)
            }
        }
    }

    override suspend fun getBatchSnapshotChanges(
        workspaceId: String,
        branchId: String?,
        sinceTimestamp: Instant?
    ): ChangeListResponse<BatchItem, String> {
        return executeRequest {
            client.get(endpoint) {
                url {
                    parameters.append("activeWorkspaceId", workspaceId)
                    branchId?.let {
                        parameters.append("branchId", it)
                    }
                    sinceTimestamp?.let {
                        parameters.append("since", sinceTimestamp.toString())
                    }
                }
            }
        }
    }

    override suspend fun getMultiTargetDelta(request: MultiTargetDeltaRequest): MultiTargetDeltaResponse {
        return executeRequest {
            client.post("api/v1/products/deltas") {
                setBody(request)
            }
        }
    }

    override suspend fun delete(id: String) {
        return executeRequest { client.delete("${endpoint}/$id") }
    }

    override suspend fun getById(id: String): StockBatch {
        return executeRequest {
            client.get("${endpoint}/$id")
        }
    }
}
