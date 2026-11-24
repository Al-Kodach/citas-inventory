package com.citacomsys.sales.ui.screen.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.asUiError
import com.citacomsys.database.ProductListItemView
import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.notification.AlertService
import com.citacomsys.module.api.customer.CustomerRepository
import com.citacomsys.types.PageQuery
import com.citacomsys.sales.data.repository.ClientSalesRepository
import com.citacomsys.sales.engine.PosEngine
import com.citacomsys.sales.engine.PosIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.core.barcode.BarcodeScannerService
import com.citacomsys.core.printer.DocumentPrinterService
import com.citacomsys.core.printer.PrintPageFormat
import com.citacomsys.core.printer.ReceiptItem
import com.citacomsys.core.printer.ReceiptPrintJob
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.module.api.customer.CustomerRegistrationRequest
import com.citacomsys.types.PhoneNumber
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PosViewModel(
    private val engine: PosEngine,
    private val salesRepository: ClientSalesRepository,
    private val customerRepository: CustomerRepository,
    private val sessionManager: AppSessionManager,
    scannerService: BarcodeScannerService,
    private val printerService: DocumentPrinterService,
    private val prefs: KSafe
) : ViewModel() {

    val scanner = scannerService

    private val _autoPrintEnabled = MutableStateFlow(
        prefs.getDirect("pos_auto_print_enabled", false)
    )

    val autoPrintEnabled = _autoPrintEnabled.asStateFlow()

    private val _selectedPaperFormat = MutableStateFlow(
        prefs.getDirect("pos_receipt_paper_format", "80mm")
    )

    val selectedPaperFormat = _selectedPaperFormat.asStateFlow()

    fun toggleAutoPrint(enabled: Boolean) {
        _autoPrintEnabled.value = enabled
        prefs.putDirect("pos_auto_print_enabled", enabled)
    }

    fun setReceiptPaperFormat(formatKey: String) {
        _selectedPaperFormat.value = formatKey
        prefs.putDirect("pos_receipt_paper_format", formatKey)
    }

    /**
     * Direct observation of the engine's state.
     */
    val uiState = engine.state

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    /**
     * Reactive pipeline for sellable products.
     * Merges the repository observation with local search filtering for zero-latency.
     */
    val sellableProductsState: StateFlow<UiResult<List<ProductListItemView>>> by lazy {
        combine(
            salesRepository.observeSellableProducts(),
            _searchQuery.debounce(300.milliseconds)
        ) { products, query ->
            if (query.isBlank()) {
                UiResult.Success(products)
            } else {
                val filtered = products.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.brandName.contains(query, ignoreCase = true) ||
                            it.id.contains(query, ignoreCase = true) ||
                            (!it.sku.isNullOrBlank() && it.sku!!.contains(
                                query,
                                ignoreCase = true
                            )) ||
                            (!it.barcode.isNullOrBlank() && it.barcode!!.contains(
                                query,
                                ignoreCase = true
                            ))
                }
                UiResult.Success(filtered)
            }
        }.onStart<UiResult<List<ProductListItemView>>> { emit(UiResult.Loading) }
            .catch { emit(UiResult.Error(it.asUiError().message)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UiResult.Loading
            )
    }

    init {
        // Observe engine errors and propagate to global notifications
        uiState.map { it.error }
            .distinctUntilChanged()
            .onEach { err ->
                if (!err.isNullOrBlank()) AlertService.showError(err)
            }.launchIn(viewModelScope)

        // Observe completed sales
        uiState.map { it.completedSaleId }
            .distinctUntilChanged()
            .onEach { saleId ->
                if (!saleId.isNullOrBlank()) {
                    loadCompletedSaleDetails(saleId)
                }
            }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onBarcodeScanned(scannedCode: String) {
        val cleanCode = scannedCode.trim()
        if (cleanCode.isBlank()) return

        val products = (sellableProductsState.value as? UiResult.Success)?.data ?: emptyList()
        val exactMatch = products.find {
            it.id.equals(cleanCode, ignoreCase = true) ||
                    (!it.barcode.isNullOrBlank() && it.barcode!!.equals(
                        cleanCode,
                        ignoreCase = true
                    )) ||
                    (!it.sku.isNullOrBlank() && it.sku!!.equals(cleanCode, ignoreCase = true))
        }

        if (exactMatch != null) {
            addProduct(exactMatch.id)
            AlertService.showSuccess("Added ${exactMatch.name} to cart")
            _searchQuery.value = ""
        } else {
            _searchQuery.value = cleanCode
            AlertService.showError("No product found matching barcode: $cleanCode")
        }
    }

    fun clearState() {
        viewModelScope.launch {
            engine.dispatch(PosIntent.ClearState)
        }
    }

    fun addProduct(productId: String) {
        viewModelScope.launch {
            engine.dispatch(PosIntent.AddProduct(productId))
        }
    }

    fun updateQuantity(productId: String, delta: Int) {
        viewModelScope.launch {
            if (delta > 0) engine.dispatch(PosIntent.IncreaseQty(productId))
            else engine.dispatch(PosIntent.DecreaseQty(productId))
        }
    }

    fun removeItem(productId: String) {
        viewModelScope.launch {
            engine.dispatch(PosIntent.RemoveProduct(productId))
        }
    }

    /**
     * Initializes the checkout process by requesting a salePreview (quote).
     * This must be called before [confirmPayment].
     */
    fun checkout() {
        viewModelScope.launch {
            engine.dispatch(PosIntent.Checkout)
        }
    }

    /**
     * Resets the checkout process (quote) but keeps items in the cart.
     */
    fun cancelCheckout() {
        viewModelScope.launch {
            engine.dispatch(PosIntent.CancelCheckout)
        }
    }

    /**
     * Finalizes the transaction with payment.
     * The UI should observe [uiState.completedSaleId] to handle navigation.
     */
    fun confirmPayment(method: PaymentMethod, reference: String? = null) {
        viewModelScope.launch {
            engine.dispatch(PosIntent.ConfirmPayment(method, reference))
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            engine.dispatch(PosIntent.ClearCart)
        }
    }

    fun dismissError() {
        viewModelScope.launch {
            engine.dispatch(PosIntent.DismissError)
        }
    }

    // --- Customer Integration ---

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery = _customerSearchQuery.asStateFlow()

    val customerSearchResults: StateFlow<UiResult<List<CustomerItem>>> = _customerSearchQuery
        .debounce(300.milliseconds)
        .flatMapLatest { query ->
            flow {
                if (query.trim().length < 2) {
                    emit(UiResult.Success(emptyList()))
                    return@flow
                }
                emit(UiResult.Loading)
                try {
                    val workspaceId =
                        sessionManager.activeWorkspaceId ?: error("Workspace not configured")
                    val paged = customerRepository.fetchPaged(
                        "",
                        PageQuery(page = 1, size = 20, search = query, workspaceId = workspaceId)
                    )
                    emit(UiResult.Success(paged.items))
                } catch (e: Exception) {
                    emit(UiResult.Error(e.asUiError().message))
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiResult.Success(emptyList())
        )

    fun onCustomerSearchQueryChange(query: String) {
        _customerSearchQuery.value = query
    }

    fun selectCustomer(customer: CustomerItem?) {
        viewModelScope.launch {
            engine.dispatch(PosIntent.SelectCustomer(customer))
            _customerSearchQuery.value = ""

            val name = customer?.fullName ?: "Draft Sale"
            val phone = customer?.phone ?: customer?.email ?: "No Contact"
            val draftName = "$name ($phone)"
            updateDraftName(draftName)
        }
    }

    // --- Draft Management ---

    fun createNewDraft() {
        viewModelScope.launch {
            engine.dispatch(PosIntent.CreateNewDraft)
        }
    }

    fun switchDraft(cartId: String) {
        viewModelScope.launch {
            engine.dispatch(PosIntent.SwitchDraft(cartId))
        }
    }

    fun deleteDraft(cartId: String) {
        viewModelScope.launch {
            engine.dispatch(PosIntent.DeleteDraft(cartId))
        }
    }

    fun updateDraftName(name: String) {
        viewModelScope.launch {
            engine.dispatch(PosIntent.UpdateDraftName(name))
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            salesRepository.refreshCatalog()
        }
    }

    // --- Receipt & Print Management ---

    private val _completedSaleDetails = MutableStateFlow<UiResult<SaleDetails>>(UiResult.Loading)
    val completedSaleDetails = _completedSaleDetails.asStateFlow()

    private val _printState = MutableStateFlow<UiResult<Unit>>(UiResult.Success(Unit))
    val printState = _printState.asStateFlow()

    fun loadCompletedSaleDetails(saleId: String) {
        viewModelScope.launch {
            _completedSaleDetails.value = UiResult.Loading
            try {
                val wsId = sessionManager.activeWorkspaceId ?: error("No active workspace")
                val details = salesRepository.getSaleDetails(saleId, wsId).getOrThrow()
                _completedSaleDetails.value = UiResult.Success(details)
            } catch (e: Exception) {
                _completedSaleDetails.value = UiResult.Error(e.asUiError().message)
            }
        }
    }

    fun printReceipt(
        saleDetails: SaleDetails,
        format: PrintPageFormat = PrintPageFormat.Receipt80mm(),
        silent: Boolean = false
    ) {
        viewModelScope.launch {
            _printState.value = UiResult.Loading
            runCatching {
                val wsInfo = sessionManager.workStationInfo.value
                val cashier = sessionManager.appUser.value?.name ?: wsInfo.username.ifBlank { "Cashier" }
                val details = saleDetails
                val job = ReceiptPrintJob(
                    storeName = wsInfo.activeWorkspaceName,
                    branchName = wsInfo.activeBranchName.takeIf {
                        it.isNotBlank() && !it.startsWith("BR-")
                    },
                    storeAddress = wsInfo.address,
                    phone = wsInfo.phone,
                    email = wsInfo.email,
                    website = wsInfo.website,
                    receiptNumber = details.sale.id.takeLast(8).uppercase(),
                    dateTime = details.sale.createdAt.toString(),
                    cashierName = cashier,
                    customerName = details.sale.customerId,
                    items = details.items.map { item ->
                        ReceiptItem(
                            name = item.productName,
                            quantity = item.quantity.toString(),
                            unitPrice = item.unitPrice.formatted,
                            totalPrice = item.totalPrice.formatted
                        )
                    },
                    subtotal = details.sale.totalAmount.formatted,
                    tax = "0.00",
                    total = details.sale.totalAmount.formatted,
                    paymentMethod = details.payments.firstOrNull()?.method?.name ?: "CASH",
                    amountPaid = details.payments.firstOrNull()?.amount?.formatted,
                    changeDue = null,
                    barcodeOrQr = details.sale.id
                )
                printerService.printReceipt(job, format, silent = silent).getOrThrow()
            }.onSuccess {
                _printState.value = UiResult.Success(Unit)
                AlertService.showSuccess("Receipt sent to printer")
            }.onFailure { e ->
                _printState.value = UiResult.Error(e.asUiError().message)
                AlertService.showError("Failed to print receipt: ${e.message}")
            }
        }
    }
}
