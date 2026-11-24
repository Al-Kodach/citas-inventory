package com.citacomsys.module.inventory.data.repository

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
import com.citacomsys.module.inventory.data.remote.api.ktor.KtorProductApiService
import com.citacomsys.module.inventory.model.ProductSnapshot
import com.citacomsys.types.BaseProduct
import com.citacomsys.types.EntityType
import com.citacomsys.types.PageQuery
import com.citacomsys.types.PageResult
import com.citacomsys.types.generateUUID
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseClientProductRepository<T : BaseProduct>(
    protected val dbManager: InventoryDatabaseManager,
    protected val stockWriter: StockWriter,
    val apiService: KtorProductApiService<T>,
    val sessionManager: AppSessionManager,
    protected val productEndpoint: String,
    protected val productDao: ProductDao,
    protected val telemetryClient: TelemetryLogClient,
    protected val json: Json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
) {
    abstract val entityType: EntityType

    // ==============================================================================
    //  CORE OPERATIONS (Shared List Views / Sales App Context)
    // ==============================================================================

    fun observeProducts(): Flow<List<ProductListItemView>> = productDao.observeAll()
    fun observeViewById(id: String): Flow<ProductListItemView?> = productDao.find(id)
    fun observeProducts(query: PageQuery): Flow<List<ProductListItemView>> = productDao.observeAll(query)
    fun search(query: String): Flow<List<ProductListItemView>> =
        productDao.search(query, sessionManager.activeViewBranchId.value)

    fun observeProductEntity(id: String): Flow<ProductEntity?> = productDao.findById(id)
    fun observeProduct(id: String): Flow<T?> =
        productDao.findById(id).map { p -> p?.let { mapToDomain(it) } }

    suspend fun delete(
        productId: String,
        actorId: String,
        workspaceId: String,
        auditBranchId: String
    ): Result<Unit> =
        runCatching {
            val isSubscribed = sessionManager.isSubscribed(workspaceId)

            var shouldEnqueue = false
            if (isSubscribed) {
                try {
                    apiService.delete(productId)
                } catch (e: Exception) {
                    if (isErrorRetriable(e).not()) throw e
                    shouldEnqueue = true
                }
            }

            val product = getProductEntity(productId) ?: return@runCatching

            dbManager.transaction {
                if (shouldEnqueue) {
                    dbManager.getDatabase().pendingRequestQueries.insertRequest(
                        id = generateUUID(),
                        payload = null,
                        workspace_id = product.workspace_id,
                        endpoint = "$productEndpoint/$productId",
                        method = RequestMethod.DELETE,
                        entityType = entityType,
                        entity_id = productId,
                        dependant_id = productId,
                        eventAction = "DELETE",
                        actor_id = actorId,
                        actor_name = "",
                        branch_id = auditBranchId,
                        routing_chain_json = ActiveRoutingChainHolder.get(),
                        createdAt = Clock.System.now().toString()
                    )
                }

                stockWriter.performProductDeletion(
                    productId = productId,
                    auditBranchId = auditBranchId,
                    actorId = actorId,
                    wsId = workspaceId
                )
            }

            telemetryClient.trackApplicationEvent(
                actorId = actorId,
                entityType = "PRODUCT",
                entityId = productId,
                action = "DELETE",
                summary = "Deleted product: ${product.name} (Global Action)"
            )
        }

    /**
     * SHARED CREATION LOGIC: Handles quota, network, and atomic local write.
     */
    suspend fun createProduct(
        product: T,
        actorId: String,
        actorName: String
    ): Result<Unit> = runCatching {
        val now = Clock.System.now()

        // Prepare the immutable fully formed local domain record
        val workspaceId = product.workspaceId
        val isSubscribed = sessionManager.isSubscribed(workspaceId)

        var shouldEnqueue = false
        if (isSubscribed) {
            try {
                performRemoteCreate(product)
            } catch (e: Exception) {
                if (isErrorRetriable(e)) {
                    Napier.e(e) { "Online product creation failed. Falling back to offline queue." }
                    shouldEnqueue = true
                } else {
                    throw e
                }
            }
        }

        // DATABASE WRITE LOCK PHASE (Atomic, instant execution context)
        dbManager.transaction {
            if (shouldEnqueue) {
                dbManager.getDatabase().pendingRequestQueries.insertRequest(
                    id = generateUUID(),
                    entityType = entityType,
                    dependant_id = null,
                    entity_id = product.id,
                    endpoint = productEndpoint,
                    payload = null,
                    workspace_id = workspaceId,
                    eventAction = "CREATE",
                    method = RequestMethod.POST,
                    actor_id = actorId,
                    actor_name = actorName,
                    routing_chain_json = ActiveRoutingChainHolder.get(),
                    branch_id = "",
                    createdAt = now.toString()
                )
            }

            // Commit structural landing lines directly down to local hardware storage
            stockWriter.performOfflineProductInitialization(mapToEntity(product))
        }
    }

    protected abstract suspend fun performRemoteCreate(product: T): T

    @PublishedApi
    internal suspend fun saveProductLocally(product: T) {
        productDao.save(mapToEntity(product))
    }

    @PublishedApi
    internal suspend fun getProductEntity(productId: String): ProductEntity? =
        productDao.findById(productId).firstOrNull()

    open suspend fun getById(productId: String): Result<T> = runCatching {
        getProductEntity(productId)?.let {
            mapToDomain(it)
        } ?: fetchRemoteProduct(productId).getOrThrow().also {
            saveProductLocally(it)
        }
    }

    protected abstract suspend fun fetchRemoteProduct(productId: String): Result<T>

    fun mapToEntity(
        product: T
    ): ProductEntity {
        return ProductEntity(
            id = product.id,
            workspace_id = product.workspaceId,
            name = product.name,
            brand_name = product.brandName,
            barcode = product.barcode,
            sku = product.sku,
            unit_of_measure = product.unitOfMeasure,
            category = product.category,
            description = product.description,
            reorder_level = product.reorderLevel,
            product_type = product.type.name,
            extension_data_json = encodeExtension(product),
            created_at = product.createdAt,
            updated_at = product.updatedAt,
            is_active = true,
            actor_id = "",
        )
    }

    abstract fun encodeExtension(product: T): String
    abstract fun mapToDomain(product: ProductEntity): T

    suspend fun fetchPaged(query: PageQuery): PageResult<ProductSnapshot> {
        return apiService.fetchPaged(query)
    }

    fun globalSearch(query: PageQuery): Flow<PageResult<ProductSnapshot>> =
        flow {
            emit(apiService.fetchPaged(query))
        }
}
