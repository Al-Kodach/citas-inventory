package com.citacomsys.sales.engine

import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.api.sales.dto.CreateSaleRequest
import com.citacomsys.api.sales.dto.PaymentRequest
import com.citacomsys.api.sales.dto.PreviewResponse
import com.citacomsys.api.sales.dto.PreviewSaleRequest
import com.citacomsys.api.sales.dto.SaleItemRequest
import com.citacomsys.api.sales.dto.SaleResult
import com.citacomsys.core.domain.security.UserCapabilityManager
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.database.PosCartItemEntity
import com.citacomsys.kernel.service.PlatformClientService
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.sales.data.repository.ClientSalesRepository
import com.citacomsys.shared.event.EventBus
import com.citacomsys.shared.event.SecurityBridgeEvent
import com.citacomsys.types.Money
import com.citacomsys.types.ProductModule
import com.citacomsys.types.User
import com.citacomsys.types.UserCapability
import com.citacomsys.types.generateUUID
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Core engine responsible for managing Point of Sale (POS) logic.
 * Optimized for high performance via local-first state management, lazy checkout quoting,
 * thread-safe cart persistence, and reactive write-capability observers.
 */
class PosEngine(
    private val salesRepository: ClientSalesRepository,
    private val scope: CoroutineScope,
    private val runtimeService: PlatformClientService,
    private val sessionManager: AppSessionManager,
    private val capsManager: UserCapabilityManager
) {
    private val _state = MutableStateFlow(PosUiState())
    val state = _state.asStateFlow()

    private val cartMutex = Mutex()

    private val activeBranchId: String get() = sessionManager.resolveWriteBranchId()
    private val workspaceId: String
        get() = sessionManager.activeWorkspaceId
            ?: error("Your session has expired. Please log in again.")
    private val actor: User
        get() = sessionManager.appUser.value
            ?: error("Your session has expired. Please log in again.")

    init {
        observeSecurity()
        observeDrafts()
    }

    private fun observeSecurity() {
        scope.launch {
            runtimeService.modulesMap.collect {
                val canWrite = runtimeService.canWrite(ProductModule.SALES)
                val canProcessSales = capsManager.has(UserCapability.PROCESS_SALES)
                _state.update { state ->
                    state.copy(
                        canWriteSale = canWrite,
                        canProcessSales = canProcessSales
                    )
                }
            }
        }
    }

    private fun observeDrafts() {
        scope.launch {
            combine(
                sessionManager.activeWorkspaceIdFlow,
                sessionManager.writeTargetBranchId
            ) { wsId, branchId ->
                val currentWs = wsId ?: sessionManager.activeWorkspaceId
                val currentBranch = branchId ?: sessionManager.resolveWriteBranchId()
                currentWs to currentBranch
            }.collect { (wsId, branchId) ->
                if (wsId.isNullOrBlank() || branchId.isNullOrBlank()) return@collect

                launch {
                    salesRepository.observeCarts(wsId, branchId)
                        .collect { drafts -> _state.update { it.copy(drafts = drafts) } }
                }

                launch {
                    salesRepository.observeActiveCart(wsId, branchId)
                        .distinctUntilChangedBy { it?.id }
                        .collect { activeCart ->
                            if (activeCart != null) {
                                try {
                                    val items = salesRepository.getActiveCartItems(wsId, branchId).map { it.toCartItem() }

                                    _state.update {
                                        it.copy(
                                            activeCartId = activeCart.id,
                                            activeCartName = activeCart.name ?: "",
                                            items = items,
                                            selectedCustomer = if (activeCart.customer_id != null) {
                                                CustomerItem(
                                                    id = activeCart.customer_id!!,
                                                    workspaceId = wsId,
                                                    branchId = branchId,
                                                    fullName = activeCart.customer_name ?: "",
                                                    phone = activeCart.customer_phone,
                                                    email = null,
                                                    timestamp = Clock.System.now()
                                                )
                                            } else null
                                        ).recalculate()
                                    }
                                } catch (e: Exception) {
                                    Napier.w { "Cart hydration skipped: ${e.message}" }
                                }
                            } else {
                                salesRepository.createNewCart(wsId, branchId)
                            }
                        }
                }
            }
        }
    }

    suspend fun dispatch(intent: PosIntent) {
        when (intent) {
            is PosIntent.AddProduct -> addProduct(intent.productId)
            is PosIntent.RemoveProduct -> removeProduct(intent.productId)
            is PosIntent.IncreaseQty -> changeQuantity(intent.productId, 1)
            is PosIntent.DecreaseQty -> changeQuantity(intent.productId, -1)
            PosIntent.Checkout -> prepareCheckout()
            is PosIntent.ConfirmPayment -> confirmPayment(intent.method, intent.reference)
            PosIntent.ClearCart -> clearCart()
            PosIntent.DismissError -> _state.update { it.copy(error = null) }
            PosIntent.CancelCheckout -> _state.update { it.copy(quote = null) }
            PosIntent.ClearState -> clearState()

            // Draft Management
            PosIntent.CreateNewDraft -> salesRepository.createNewCart(workspaceId, activeBranchId)
            is PosIntent.SwitchDraft -> salesRepository.switchCart(intent.cartId, workspaceId, activeBranchId)
            is PosIntent.DeleteDraft -> salesRepository.deleteCart(intent.cartId, workspaceId, activeBranchId)
            is PosIntent.UpdateDraftName -> updateDraftName(intent.name)
            is PosIntent.SelectCustomer -> selectCustomer(intent.customer)
        }
    }

    private fun clearState() {
        _state.value = PosUiState()
        observeSecurity()
    }

    private fun selectCustomer(customer: CustomerItem?) {
        _state.update { it.copy(selectedCustomer = customer) }
        persistActiveCart()
    }

    private fun updateDraftName(name: String) {
        _state.update { it.copy(activeCartName = name) }
        persistActiveCart()
    }

    private suspend fun addProduct(productId: String) {
        try {
            val product = salesRepository.getProduct(productId)
                ?: throw IllegalStateException("Product not found.")

            _state.update { current ->
                val existing = current.items.find { it.productId == productId }
                val updatedItems = if (existing != null) {
                    current.items.map {
                        if (it.productId == productId) it.copy(quantity = it.quantity + 1) else it
                    }
                } else {
                    current.items + CartItem(
                        productId = product.id,
                        name = product.name ?: "",
                        unitPrice = Money(product.currentPriceAmount, product.currentPriceCurrency),
                        quantity = 1
                    )
                }
                current.copy(items = updatedItems).recalculate()
            }
            persistActiveCart()
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message) }
        }
    }

    private fun removeProduct(productId: String) {
        _state.update {
            it.copy(items = it.items.filterNot { item -> item.productId == productId })
                .recalculate()
        }
        persistActiveCart()
    }

    private fun changeQuantity(productId: String, delta: Int) {
        _state.update { current ->
            val updatedItems = current.items.mapNotNull { item ->
                if (item.productId == productId) {
                    val newQty = (item.quantity + delta).coerceAtMost(999)
                    if (newQty <= 0) null else item.copy(quantity = newQty)
                } else item
            }
            current.copy(items = updatedItems).recalculate()
        }
        persistActiveCart()
    }

    private fun persistActiveCart() {
        scope.launch {
            cartMutex.withLock {
                runCatching {
                    val current = _state.value
                    val cartId = current.activeCartId ?: return@runCatching

                    salesRepository.saveActiveCart(
                        workspaceId = workspaceId,
                        branchId = activeBranchId,
                        items = current.items.map { it.toEntity(cartId) },
                        cartId = cartId,
                        name = current.activeCartName,
                        customerId = current.selectedCustomer?.id,
                        customerName = current.selectedCustomer?.fullName,
                        customerPhone = current.selectedCustomer?.phone
                    )
                }.onFailure { e ->
                    Napier.e(tag = "POS_ENGINE", throwable = e) { "Failed to persist active cart" }
                }
            }
        }
    }

    private fun prepareCheckout() {
        scope.launch {
            val current = _state.value
            if (current.items.isEmpty()) return@launch

            _state.update { it.copy(isLoading = true, error = null, errorDetails = emptyMap()) }

            try {
                val currentWsId = workspaceId
                val currentBranchId = activeBranchId

                val previewResult = salesRepository.previewSale(
                    PreviewSaleRequest(
                        workspaceId = currentWsId,
                        branchId = currentBranchId,
                        items = current.items.map { SaleItemRequest(it.productId, it.quantity) }
                    )
                )

                previewResult.onSuccess { result ->
                    if (result is PreviewResponse.Success) {
                        val quote = result.salePreview
                        _state.update { state ->
                            state.copy(
                                quote = quote,
                                total = quote.totalAmount,
                                totalItems = quote.items.sumOf { it.fulfilledQuantity },
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = (result as PreviewResponse.Error).message,
                                errorDetails = result.details
                            )
                        }
                    }
                }.onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Checkout initialization failed: ${e.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Session expired. Please log in again."
                    )
                }
            }
        }
    }

    private fun confirmPayment(method: PaymentMethod, reference: String? = null) {
        scope.launch {
            val current = _state.value
            val quote = current.quote ?: return@launch _state.update {
                it.copy(error = "Please initialize checkout first.")
            }
            _state.update { it.copy(isLoading = true, error = null, errorDetails = emptyMap()) }

            val saleId = generateUUID()

            try {
                val currentActor = actor
                val currentWsId = workspaceId
                val currentBranchId = activeBranchId

                var initialDepositRequest: PaymentRequest? = null
                var cleanRef = reference

                if (method == PaymentMethod.CREDIT && current.selectedCustomer != null) {
                    if (reference != null && reference.contains("DEPOSIT:")) {
                        runCatching {
                            val parts = reference.substringAfter("DEPOSIT:").split("|REF:")
                            val depStr = parts.firstOrNull() ?: "0"
                            val depositMinor = ((depStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                            cleanRef = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: saleId

                            if (depositMinor > 0) {
                                initialDepositRequest = PaymentRequest(
                                    amount = Money(depositMinor, quote.totalAmount.currencyCode),
                                    method = PaymentMethod.CASH,
                                    reference = cleanRef
                                )
                            }
                        }
                    }
                }

                val finalRequest = CreateSaleRequest(
                    id = saleId,
                    customerId = current.selectedCustomer?.id,
                    quoteId = quote.quoteId,
                    actorId = currentActor.id,
                    items = current.items.map {
                        SaleItemRequest(it.productId, it.quantity, it.unitPrice)
                    },
                    branchId = currentBranchId,
                    payments = listOf(
                        PaymentRequest(quote.totalAmount, method, cleanRef)
                    ),
                    initialDeposit = initialDepositRequest,
                    workspaceId = currentWsId,
                    actorName = currentActor.name
                )

                when (val saleResult = salesRepository.finalizeSale(finalRequest)) {
                    is SaleResult.Success -> {
                        salesRepository.clearActiveCart(currentWsId, currentBranchId) // Remove draft on success
                        _state.update {
                            it.copy(
                                completedSaleId = saleId,
                                isLoading = false,
                                items = emptyList(),
                                quote = null
                            )
                        }
                        salesRepository.refreshCatalog()
                    }

                    is SaleResult.QuoteExpired -> {
                        _state.update { it.copy(error = "Prices have expired. Refreshing...") }
                        delay(2000.milliseconds)
                        prepareCheckout()
                    }

                    is SaleResult.QuoteMismatch -> {
                        _state.update { it.copy(error = "Cart items changed. Refreshing...") }
                        delay(2000.milliseconds)
                        salesRepository.refreshCatalog()
                        prepareCheckout()
                    }

                    is SaleResult.InsufficientStock -> {
                        val outOfStockItems = saleResult.details.keys.joinToString(", ")
                        _state.update { it.copy(error = "Items are out of stock: $outOfStockItems") }
                        delay(2000.milliseconds)
                        prepareCheckout()
                    }

                    is SaleResult.InvalidContext -> {
                        _state.update { it.copy(error = "Session error. Please log in again.") }
                        delay(2000.milliseconds)
                        EventBus.publish(
                            SecurityBridgeEvent.fromUserId("current_user")
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Transaction failed: ${e.asUiError().message}"
                    )
                }
            }
        }
    }

    fun clearCart() {
        scope.launch {
            salesRepository.clearActiveCart(workspaceId, activeBranchId)
            _state.update { it.copy(items = emptyList(), quote = null).recalculate() }
        }
    }

    private fun PosUiState.recalculate(): PosUiState {
        val totalAmount =
            items.fold(Money.zero()) { acc, item -> acc + (item.unitPrice * item.quantity) }
        return this.copy(total = totalAmount, totalItems = items.sumOf { it.quantity })
    }
}

private fun PosCartItemEntity.toCartItem() = CartItem(
    productId = product_id,
    name = product_name,
    unitPrice = Money(unit_price_minor, unit_price_currency),
    quantity = quantity.toInt()
)

private fun CartItem.toEntity(cartId: String) = PosCartItemEntity(
    id = generateUUID(),
    cart_id = cartId,
    product_id = productId,
    product_name = name,
    unit_price_minor = unitPrice.amountMinor,
    unit_price_currency = unitPrice.currencyCode,
    quantity = quantity.toLong()
)
