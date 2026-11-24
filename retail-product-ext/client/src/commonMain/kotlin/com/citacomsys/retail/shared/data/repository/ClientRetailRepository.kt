package com.citacomsys.retail.shared.data.repository

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.core.database.dao.inventory.ProductDao
import com.citacomsys.core.database.util.StockWriter
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.remote.isErrorRetriable
import com.citacomsys.database.ProductEntity
import com.citacomsys.database.ProductListItemView
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.module.inventory.data.repository.BaseClientProductRepository
import com.citacomsys.retail.api.dto.RetailMetadataUpdateRequest
import com.citacomsys.retail.api.model.RetailProduct
import com.citacomsys.retail.api.model.RetailProductExt
import com.citacomsys.retail.shared.RETAIL_ENDPOINT
import com.citacomsys.retail.shared.data.remote.RetailApiService
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductType
import com.citacomsys.types.generateUUID
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

class ClientRetailRepository(
    dbManager: InventoryDatabaseManager,
    stockWriter: StockWriter,
    apiService: RetailApiService,
    sessionManager: AppSessionManager,
    productDao: ProductDao,
    telemetryClient: TelemetryLogClient,
    json: Json = Json { encodeDefaults = true }
) : BaseClientProductRepository<RetailProduct>(
    dbManager = dbManager,
    stockWriter = stockWriter,
    apiService = apiService,
    sessionManager = sessionManager,
    productEndpoint = RETAIL_ENDPOINT,
    productDao = productDao,
    telemetryClient = telemetryClient,
    json = json
) {
    override val entityType: EntityType = EntityType.RETAIL_PRODUCT


    override fun encodeExtension(product: RetailProduct): String {
        return json.encodeToString(RetailProductExt.serializer(), product.ext)
    }

    override suspend fun fetchRemoteProduct(productId: String): Result<RetailProduct> =
        runCatching {
            (apiService as RetailApiService).getById<RetailProduct>(productId)
        }

    override suspend fun performRemoteCreate(product: RetailProduct): RetailProduct {
        return (apiService as RetailApiService).create(product)
    }

    suspend fun updateRetailMetadata(
        productId: String,
        request: RetailMetadataUpdateRequest,
        actorId: String,
        actorName: String,
    ): Result<Unit> = runCatching {
        val isSubscribed = sessionManager.isSubscribed(request.workspaceId)
        val existing = productDao.findById(productId).firstOrNull()

        if (isSubscribed) {
            try {
                (apiService as RetailApiService).updateProductMetadata(productId, request)
            } catch (e: Exception) {
                if (isErrorRetriable(e).not()) throw e
                Napier.e(e) { "Online retail product metadata update failed. Falling back to offline." }

                stockWriter.performUpdate("") { db ->
                    db.pendingRequestQueries.insertRequest(
                        id = generateUUID(),
                        payload = json.encodeToString(
                            RetailMetadataUpdateRequest.serializer(),
                            request
                        ),
                        workspace_id = request.workspaceId,
                        endpoint = "$productEndpoint/$productId/metadata",
                        method = RequestMethod.PUT,
                        entityType = EntityType.RETAIL_PRODUCT,
                        entity_id = productId,
                        dependant_id = productId,
                        eventAction = "UPDATE_METADATA",
                        actor_id = actorId,
                        actor_name = actorName,
                        branch_id = "",
                        routing_chain_json = ActiveRoutingChainHolder.get(),
                        createdAt = Clock.System.now().toString()
                    )
                }
            }
        }

        // Update the local state
        if (existing != null)  {
            val product = mapToDomain(existing)

            val updated = product.copy(
                name = request.name,
                brandName = request.brandName,
                unitOfMeasure = request.unitOfMeasure ?: existing.unit_of_measure,
                reorderLevel = request.reorderLevel ?: existing.reorder_level,
                barcode = request.barcode ?: existing.barcode,
                description = request.description ?: existing.description,
                category = request.category ?: existing.category,
                updatedAt = request.updatedAt,
                sku = request.sku ?: existing.sku,
                tags = request.tags,
                attributes = request.attributes
            )

            productDao.save(mapToEntity(updated))
        }
    }

    override fun mapToDomain(product: ProductEntity): RetailProduct {
        val ex = json.decodeFromString(
            RetailProductExt.serializer(),
            product.extension_data_json
        )
        return RetailProduct(
            id = product.id,
            workspaceId = product.workspace_id,
            name = product.name,
            brandName = product.brand_name,
            sku = product.sku,
            barcode = product.barcode,
            category = product.category,
            unitOfMeasure = product.unit_of_measure,
            description = product.description,
            reorderLevel = product.reorder_level,
            type = ProductType.safeValueOf(product.product_type),
            isActive = product.is_active,
            createdAt = product.created_at,
            updatedAt = product.updated_at,
            tags = ex.tags,
            attributes = ex.attributes
        )
    }
}
