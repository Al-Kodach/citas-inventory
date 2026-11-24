package com.citacomsys.sales.data.repository

import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.api.sales.SaleStatus
import com.citacomsys.api.sales.dto.CreateSaleRequest
import com.citacomsys.api.sales.dto.PreviewResponse
import com.citacomsys.api.sales.dto.PreviewSaleRequest
import com.citacomsys.api.sales.dto.SalePreview
import com.citacomsys.api.sales.dto.SaleResult
import com.citacomsys.core.database.dao.PendingRequestDao
import com.citacomsys.core.database.dao.inventory.ProductDao
import com.citacomsys.core.database.dao.sale.PosCartDao
import com.citacomsys.core.database.dao.sale.SalesDao
import com.citacomsys.core.database.util.SaleItemInput
import com.citacomsys.core.database.util.StockWriter
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.remote.isErrorRetriable
import com.citacomsys.database.PaymentEntity
import com.citacomsys.database.PosCartEntity
import com.citacomsys.database.PosCartItemEntity
import com.citacomsys.database.ProductListItemView
import com.citacomsys.database.SaleEntity
import com.citacomsys.database.SaleTransactionView
import com.citacomsys.feature.api.telemetry.TelemetryLogClient
import com.citacomsys.sales.data.mapper.asEntity
import com.citacomsys.sales.data.mapper.asItemsEntity
import com.citacomsys.sales.data.mapper.asPaymentsEntity
import com.citacomsys.sales.data.mapper.toDomain
import com.citacomsys.sales.data.mapper.toPayment
import com.citacomsys.sales.data.remote.api.SalesApi
import com.citacomsys.shared.util.resultOf
import com.citacomsys.shared.util.tryOrRecover
import com.citacomsys.types.EntityType
import com.citacomsys.types.Money
import com.citacomsys.types.generateUUID
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock.System.now
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Unified repository for managing Sales data.
 * Orchestrates between Cloud and Local storage with full offline auditing.
 * Merges local inventory and server-computed sellable products for maximum accuracy.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientSalesRepository(
    private val salesApi: SalesApi,
    private val salesDao: SalesDao,
    private val productDao: ProductDao,
    private val pendingRequestDao: PendingRequestDao,
    private val cartDao: PosCartDao,
    private val sessionManager: AppSessionManager,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val stockWriter: StockWriter,
    private val telemetryClient: TelemetryLogClient
) {
    private suspend fun isFeatureOutboxDirty(workspaceId: String): Boolean {
        return runCatching {
            val pending = pendingRequestDao.getByType(EntityType.PRODUCT) +
                    pendingRequestDao.getByType(EntityType.STOCK_BATCH) +
                    pendingRequestDao.getByType(EntityType.RETAIL_PRODUCT) +
                    pendingRequestDao.getByType(EntityType.PHARMACY_PRODUCT)
            pending.any { it.workspace_id == workspaceId }
        }.getOrDefault(false)
    }

    fun observeSales(): Flow<List<SaleTransactionView>> = salesDao.observeAll()

    fun searchSales(query: String): Flow<List<SaleTransactionView>> = salesDao.search(query)

    suspend fun getSaleDetails(
        saleId: String,
        workspaceId: String
    ): Result<SaleDetails> = runCatching {
        val isSubscribed = sessionManager.isSubscribed(workspaceId)
        val localSale = salesDao.findById(saleId).first()

        if (localSale != null) {
            val items = salesDao.getItemsBySale(saleId).map { it.toDomain() }
            val payments = salesDao.getPaymentsBySale(saleId).map { it.toPayment() }
            SaleDetails(localSale.toDomain(), items, payments)
        } else {
            if (isSubscribed) {
                val details = salesApi.getSaleDetails(saleId)
                salesDao.saveWithItems(
                    sale = details.sale.asEntity(),
                    items = details.items.asItemsEntity(details.sale.id),
                    payments = details.payments.asPaymentsEntity()
                )
                details
            } else error("Sales with batchId: $saleId not found")
        }
    }

    fun observeSellableProducts(): Flow<List<ProductListItemView>> = productDao.observePosProducts()

    suspend fun finalizeSale(request: CreateSaleRequest): SaleResult {
        val isSubscribed = sessionManager.isSubscribed(request.workspaceId)
        val status = if (isSubscribed) SaleStatus.PENDING else SaleStatus.COMPLETED

        Napier.i { "POS Local-First Engine: Finalizing sale ${request.id} locally (status=$status, subscribed=$isSubscribed) & enqueuing for background cloud sync..." }

        processOfflineSale(
            request = request,
            status = status,
            enqueueOnFailure = isSubscribed
        )

        return SaleResult.Success(request.id)
    }

    private suspend fun processOfflineSale(
        request: CreateSaleRequest,
        status: SaleStatus,
        enqueueOnFailure: Boolean
    ) {
        val actorId = request.actorId
        val actorName = request.actorName

        Napier.i { "Processing local/offline sale ${request.id} with status $status for actor $actorId ($actorName)" }

        val now = now()

        val saleEntity = SaleEntity(
            id = request.id,
            workspace_id = request.workspaceId,
            branch_id = request.branchId,
            customer_id = request.customerId,
            total_amount_minor = request.payments.sumOf { it.amount.amountMinor },
            total_amount_currency = request.payments.first().amount.currencyCode,
            total_items = request.items.sumOf { it.quantity }.toLong(),
            price_variance = 0L,
            status = status.name,
            created_at = now,
            actor_id = actorId
        )

        val paymentEntities = request.payments.map {
            PaymentEntity(
                id = generateUUID(),
                sale_id = request.id,
                method = it.method.name,
                amount_minor = it.amount.amountMinor,
                currency_code = it.amount.currencyCode,
                reference = it.reference,
                created_at = now
            )
        }

        // Delegate to StockWriter. It will handle tracking dependencies, creating items, and queueing.
        stockWriter.performSale(
            sale = saleEntity,
            payments = paymentEntities,
            items = request.items.map { SaleItemInput(it.productId, it.quantity) },
            actorId = actorId,
            actorName = actorName,
            enqueueOnFailure = enqueueOnFailure,
            originalRequestPayload = request
        )

        scope.launch(ioDispatcher) {
            try {
                telemetryClient.trackApplicationEvent(
                    actorId = actorId,
                    entityType = "SALE",
                    entityId = request.id,
                    action = if (status == SaleStatus.COMPLETED) "SALE_COMPLETED_LOCAL" else "SALE_PENDING_OFFLINE",
                    summary = "Processed sale locally with status $status for actor $actorName",
                    workspaceId = request.workspaceId
                )
            } catch (e: Exception) {
                Napier.e(e) { "Failed to track telemetry for local sale ${request.id}" }
            }
        }
    }

    suspend fun refreshCatalog(): Result<Unit> = runCatching {
    }

    suspend fun getProduct(productId: String): ProductListItemView? {
        return observeSellableProducts().first().find { it.id == productId }
    }

    // --- POS Cart Persistence (Isolated by Workspace + Branch) ---

    fun observeCarts(workspaceId: String, branchId: String): Flow<List<PosCartEntity>> =
        cartDao.observeAllCarts(workspaceId, branchId)

    fun observeActiveCart(workspaceId: String, branchId: String): Flow<PosCartEntity?> =
        cartDao.observeActiveCart(workspaceId, branchId)

    suspend fun getActiveCartItems(workspaceId: String, branchId: String): List<PosCartItemEntity> {
        val activeCart = cartDao.getActiveCart(workspaceId, branchId) ?: return emptyList()
        return cartDao.getItemsForCart(activeCart.id)
    }

    suspend fun saveActiveCart(
        workspaceId: String,
        branchId: String,
        items: List<PosCartItemEntity>,
        cartId: String? = null,
        name: String? = null,
        customerId: String? = null,
        customerName: String? = null,
        customerPhone: String? = null
    ) {
        val currentActive = cartDao.getActiveCart(workspaceId, branchId)
        val targetId = cartId ?: currentActive?.id ?: generateUUID()

        val cart = PosCartEntity(
            id = targetId,
            workspace_id = workspaceId,
            branch_id = branchId,
            name = name ?: currentActive?.name ?: "Draft Sale",
            last_updated_at = now(),
            is_active = true,
            customer_id = customerId ?: currentActive?.customer_id,
            customer_name = customerName ?: currentActive?.customer_name,
            customer_phone = customerPhone ?: currentActive?.customer_phone
        )
        cartDao.saveCart(cart, items)
    }

    suspend fun createNewCart(workspaceId: String, branchId: String) {
        val id = generateUUID()
        val cart = PosCartEntity(
            id = id,
            workspace_id = workspaceId,
            branch_id = branchId,
            name = "Draft Sale",
            last_updated_at = now(),
            is_active = true,
            customer_id = null,
            customer_name = null,
            customer_phone = null
        )
        cartDao.saveCart(cart, emptyList())
        cartDao.setActiveCart(id, workspaceId, branchId)
    }

    suspend fun switchCart(cartId: String, workspaceId: String, branchId: String) {
        cartDao.setActiveCart(cartId, workspaceId, branchId)
    }

    suspend fun deleteCart(cartId: String, workspaceId: String, branchId: String) {
        cartDao.deleteCart(cartId, workspaceId, branchId)
    }

    suspend fun clearActiveCart(workspaceId: String, branchId: String) {
        cartDao.getActiveCart(workspaceId, branchId)?.let {
            cartDao.deleteCart(it.id, workspaceId, branchId)
        }
    }

    suspend fun previewSale(request: PreviewSaleRequest): Result<PreviewResponse> {
        Napier.i { "POS Local-First Engine: Generating local invoice preview for workspace ${request.workspaceId}" }
        return Result.success(generateLocalPreview(request))
    }

    private suspend fun generateLocalPreview(request: PreviewSaleRequest): PreviewResponse {
        val previewItems = stockWriter.generatePreview(
            branchId = request.branchId,
            items = request.items.map { SaleItemInput(it.productId, it.quantity) }
        )

        return PreviewResponse.Success(
            SalePreview(
                quoteId = "offline_${generateUUID()}",
                items = previewItems,
                totalAmount = previewItems.fold(Money.zero()) { acc, i -> acc + i.totalAmount },
                expiresAt = now().plus(1.toDuration(DurationUnit.HOURS))
            )
        )
    }
}
