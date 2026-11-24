package com.citacomsys.pharmacy.shared.data.repository

import com.citacomsys.core.database.InventoryDatabaseManager
import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.core.database.dao.inventory.ProductDao
import com.citacomsys.core.database.util.StockWriter
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.mesh.security.ActiveRoutingChainHolder
import com.citacomsys.core.remote.isErrorRetriable
import com.citacomsys.database.ProductEntity
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.module.inventory.data.repository.BaseClientProductRepository
import com.citacomsys.pharmacy.api.dto.PharmacyMetadataUpdateRequest
import com.citacomsys.pharmacy.api.model.PharmacyProduct
import com.citacomsys.pharmacy.api.model.PharmacyProductExt
import com.citacomsys.pharmacy.shared.PHARMACY_ENDPOINT
import com.citacomsys.pharmacy.shared.data.remote.PharmacyApiService
import com.citacomsys.types.EntityType
import com.citacomsys.types.ProductType
import com.citacomsys.types.generateUUID
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.firstOrNull
import kotlin.time.Clock
import kotlinx.serialization.json.Json

class ClientPharmacyRepository(
    dbManager: InventoryDatabaseManager,
    stockWriter: StockWriter,
    apiService: PharmacyApiService,
    sessionManager: AppSessionManager,
    productDao: ProductDao,
    telemetryClient: TelemetryLogClient,
    json: Json = Json { encodeDefaults = true }
) : BaseClientProductRepository<PharmacyProduct>(
    dbManager = dbManager,
    stockWriter = stockWriter,
    apiService = apiService,
    sessionManager = sessionManager,
    productEndpoint = PHARMACY_ENDPOINT,
    productDao = productDao,
    telemetryClient = telemetryClient,
    json = json
) {

    override val entityType: EntityType = EntityType.PHARMACY_PRODUCT

    override fun encodeExtension(product: PharmacyProduct): String {
        return json.encodeToString(PharmacyProductExt.serializer(), product.ext)
    }

    override suspend fun fetchRemoteProduct(productId: String): Result<PharmacyProduct> =
        runCatching {
            (apiService as PharmacyApiService).getById<PharmacyProduct>(productId)
        }

    override suspend fun performRemoteCreate(product: PharmacyProduct): PharmacyProduct {
        return (apiService as PharmacyApiService).create(product)
    }

    suspend fun updatePharmacyMetadata(
        productId: String,
        request: PharmacyMetadataUpdateRequest,
        actorId: String,
        actorName: String,
    ): Result<Unit> = runCatching {
        val isSubscribed = sessionManager.isSubscribed(request.workspaceId)
        val existing = productDao.findById(productId).firstOrNull()

        if (isSubscribed) {
            try {
                (apiService as PharmacyApiService).updateProductMetadataRaw(productId, request)
            } catch (e: Exception) {
                if (isErrorRetriable(e).not()) throw e
                Napier.e(e) { "Online product metadata update failed. Falling back to offline." }

                stockWriter.performUpdate("") { db ->
                    db.pendingRequestQueries.insertRequest(
                        id = generateUUID(),
                        payload = json.encodeToString(
                            PharmacyMetadataUpdateRequest.serializer(),
                            request
                        ),
                        workspace_id = request.workspaceId,
                        endpoint = "$productEndpoint/$productId/metadata",
                        method = RequestMethod.PUT,
                        entityType = EntityType.PHARMACY_PRODUCT,
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
                sku = request.sku ?: existing.sku,
                genericName = request.genericName,
                fdaNumber = request.fdaNumber,
                dosageForm = request.dosageForm,
                strength = request.strength,
                requiresPrescription = request.requiresPrescription,
                therapeuticClass = request.therapeuticClass
            )
            productDao.save(mapToEntity(updated))
        }
    }

    override fun mapToDomain(product: ProductEntity): PharmacyProduct {
        val ex = json.decodeFromString(
            PharmacyProductExt.serializer(),
            product.extension_data_json
        )

        return PharmacyProduct(
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
            genericName = ex.genericName,
            dosageForm = ex.dosageForm,
            strength = ex.strength,
            requiresPrescription = ex.requiresPrescription,
            therapeuticClass = ex.therapeuticClass,
            fdaNumber = ex.fdaNumber,
        )
    }
}
