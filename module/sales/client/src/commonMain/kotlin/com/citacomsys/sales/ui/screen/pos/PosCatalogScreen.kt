package com.citacomsys.sales.ui.screen.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.api.sales.nav.PosCartNavKey
import com.citacomsys.api.sales.nav.PosNavKey
import com.citacomsys.api.sales.nav.SalesDetailsNavKey
import com.citacomsys.core.barcode.BarcodeScannerService
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.components.RefreshButton
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.printer.PrintPageFormat
import com.citacomsys.core.printer.ReceiptItem
import com.citacomsys.core.printer.ReceiptPrintJob
import com.citacomsys.core.printer.ui.ReceiptView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.database.PosCartEntity
import com.citacomsys.database.ProductListItemView
import com.citacomsys.sales.engine.PosUiState
import com.citacomsys.types.ProductType
import com.design.AppTheme
import com.design.components.AppScaffold
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random
import kotlin.random.nextInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosCatalogRoute(
    viewModel: PosViewModel = koinViewModel(),
    onShowCart: () -> Unit
) {
    val navState = LocalNavigator.current
    val productsState by viewModel.sellableProductsState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }

    val isExpanded = currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp > 840

    var showDraftsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded, uiState.items) {
        if (isExpanded && navState.state.currentTop != PosCartNavKey && uiState.items.isNotEmpty() && uiState.completedSaleId == null) {
            onShowCart()
        }
    }

    if (uiState.completedSaleId != null) {
        val completedSaleId = uiState.completedSaleId!!
        val completedSaleDetails by viewModel.completedSaleDetails.collectAsStateWithLifecycle()
        val printState by viewModel.printState.collectAsStateWithLifecycle()
        val autoPrintEnabled by viewModel.autoPrintEnabled.collectAsStateWithLifecycle()
        val selectedPaperFormat by viewModel.selectedPaperFormat.collectAsStateWithLifecycle()

        LaunchedEffect(completedSaleDetails, autoPrintEnabled) {
            val details = (completedSaleDetails as? UiResult.Success)?.data
            if (details != null && autoPrintEnabled && printState !is UiResult.Success) {
                val format =
                    if (selectedPaperFormat == "58mm") PrintPageFormat.Receipt58mm() else PrintPageFormat.Receipt80mm()
                viewModel.printReceipt(details, format, silent = true)
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                viewModel.clearState()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ReceiptSheetContent(
                saleDetailsResult = completedSaleDetails,
                printState = printState,
                autoPrintEnabled = autoPrintEnabled,
                selectedPaperFormat = selectedPaperFormat,
                onToggleAutoPrint = viewModel::toggleAutoPrint,
                onSelectFormat = viewModel::setReceiptPaperFormat,
                onPrintManual = { details ->
                    val format =
                        if (selectedPaperFormat == "58mm") PrintPageFormat.Receipt58mm() else PrintPageFormat.Receipt80mm()
                    viewModel.printReceipt(details, format, silent = false)
                },
                onDone = {
                    viewModel.clearState()
                }
            )
        }
    }

    CatalogScreen(
        productsState = productsState,
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        refreshCatalog = viewModel::refreshCatalog,
        onBarcodeScanned = viewModel::onBarcodeScanned,
        addProduct = viewModel::addProduct,
        updateQuantity = viewModel::updateQuantity,
        onShowCart = onShowCart,
        toggleScanner = { showScanner = !showScanner },
        toggleDraftsSheet = { showDraftsSheet = !showDraftsSheet },
    )

    if (showScanner) {
        viewModel.scanner.Scan(
            onResult = { scanned ->
                viewModel.onBarcodeScanned(scanned.rawValue)
                showScanner = false
            },
            onClose = { showScanner = false }
        )
    }

    if (showDraftsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDraftsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            DraftsBottomSheetContent(
                drafts = uiState.drafts,
                activeCartId = uiState.activeCartId,
                onSelectDraft = { cartId ->
                    viewModel.switchDraft(cartId)
                    showDraftsSheet = false
                },
                onNewDraft = {
                    viewModel.createNewDraft()
                    showDraftsSheet = false
                },
                onDeleteDraft = viewModel::deleteDraft
            )
        }
    }
}


@Composable
private fun CatalogScreen(
    productsState: UiResult<List<ProductListItemView>>,
    uiState: PosUiState,
    searchQuery: String,
    toggleScanner: () -> Unit,
    toggleDraftsSheet: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    refreshCatalog: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    addProduct: (String) -> Unit,
    updateQuantity: (String, Int) -> Unit,
    onShowCart: () -> Unit
) {
    val isExpanded = currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp > 840
    val cartItemCount = uiState.items.sumOf { it.quantity }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val heightModifier = remember {
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = 1f - scrollBehavior.state.collapsedFraction
                scaleY = 1f - scrollBehavior.state.collapsedFraction
            }
            .padding(end = 16.dp, bottom = 12.dp)
    }

    AppScaffold(
        scrollBehavior = scrollBehavior,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text("Products Catalog", fontWeight = FontWeight.Bold)
                    },
                    actions = {
                        IconButton(onClick = toggleDraftsSheet) {
                            Icon(AppIcons.History, contentDescription = "Drafts")
                        }
                        RefreshButton(
                            isRefreshing = productsState.isLoading,
                            onClick = refreshCatalog
                        )
                    }
                )
                TopAppBar(
                    title = {
                        if (scrollBehavior.state.collapsedFraction < 1f) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = heightModifier
                                    .onPreviewKeyEvent { keyEvent ->
                                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                            if (searchQuery.isNotBlank()) {
                                                onBarcodeScanned(searchQuery)
                                                true
                                            } else false
                                        } else false
                                    },
                                placeholder = {
                                    Text(
                                        "Search or scan barcode...",
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = { Icon(AppIcons.Search, null) },
                                trailingIcon = {
                                    IconButton(onClick = toggleScanner) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan Barcode"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        onBarcodeScanned(searchQuery)
                                    }
                                }),
                                shape = MaterialTheme.shapes.medium,
                                singleLine = true
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }

        },
        floatingActionButton = {
            if (!isExpanded && cartItemCount > 0) {
                BadgedBox(badge = { Badge { Text("$cartItemCount") } }) {
                    ExtendedFloatingActionButton(
                        onClick = onShowCart,
                        icon = { Icon(AppIcons.PointOfSale, null) },
                        text = { Text("Review Cart") }
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        UiResultGridView(
            state = productsState,
            columns = GridCells.Adaptive(380.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            emptyPlaceholder = {
                DetailsPlaceholder(
                    icon = AppIcons.Inventory2,
                    title = "No Products Found",
                    description = "Add a new product to get started.",
                    modifier = Modifier.fillMaxSize()
                )
            }
        ) { items ->
            items(items = items) { product ->
                val inCartQty = uiState.items.find { it.productId == product.id }?.quantity ?: 0
                ProductCard(
                    product = product,
                    inCartQuantity = inCartQty,
                    onAdd = {
                        addProduct(product.id)
                    },
                    onRemove = { updateQuantity(product.id, -1) }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftsBottomSheetContent(
    drafts: List<PosCartEntity>,
    activeCartId: String?,
    onSelectDraft: (String) -> Unit,
    onNewDraft: () -> Unit,
    onDeleteDraft: (String) -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Saved Drafts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onNewDraft) {
                Icon(AppIcons.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("New Sale")
            }
        }

        if (drafts.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No saved drafts", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyListView(modifier = Modifier.heightIn(max = 400.dp)) {
                items(count = drafts.size, key = { drafts[it].id }) { index ->
                    val draft = drafts[index]
                    val isActive = draft.id == activeCartId
                    Card(
                        onClick = { onSelectDraft(draft.id) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isActive) {
                            BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary
                            )
                        } else {
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    ) {
                        Row(
                            Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    draft.name ?: "Untitled Draft",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Last updated: ${
                                        draft.last_updated_at.toString().split("T").first()
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isActive) {
                                IconButton(onClick = { onDeleteDraft(draft.id) }) {
                                    Icon(
                                        AppIcons.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@AllAdaptivePreviews
@Composable
fun CatalogScreenPreview() {
    val state = PosUiState()
    val products = (1..30).map {
        ProductListItemView(
            id = it.toString(),
            workspaceId = it.toString(),
            branchId = it.toString(),
            name = "Product $it",
            brandName = "Brand $it",
            unitOfMeasure = "Each",
            totalAvailable = Random.nextLong(0, 25),
            currentPriceAmount = 100,
            currentPriceCurrency = "USD",
            nextExpiry = null,
            sku = null,
            barcode = null,
            productType = ProductType.INVENTORY.name,
            reorderLevel = Random.nextInt(0, 5)
        )
    }

    val productState = UiResult.Success(products)

    AppTheme {
        CatalogScreen(
            productsState = productState,
            uiState = state,
            searchQuery = "",
            onSearchQueryChange = {},
            refreshCatalog = {},
            onBarcodeScanned = {},
            addProduct = {},
            updateQuantity = { _, _ -> },
            onShowCart = {},
            toggleScanner = {},
            toggleDraftsSheet = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptSheetContent(
    saleDetailsResult: UiResult<SaleDetails>,
    printState: UiResult<Unit>,
    autoPrintEnabled: Boolean,
    selectedPaperFormat: String,
    onToggleAutoPrint: (Boolean) -> Unit,
    onSelectFormat: (String) -> Unit,
    onPrintManual: (SaleDetails) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = AppIcons.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Text(
            text = "Sale Finalized",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )

        if (printState is UiResult.Error) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = printState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Print,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Auto-Print Receipt",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Switch(
                        checked = autoPrintEnabled,
                        onCheckedChange = onToggleAutoPrint
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPaperFormat == "80mm",
                        onClick = { onSelectFormat("80mm") },
                        label = { Text("80mm Thermal") }
                    )
                    FilterChip(
                        selected = selectedPaperFormat == "58mm",
                        onClick = { onSelectFormat("58mm") },
                        label = { Text("58mm Thermal") }
                    )
                }
            }
        }

        when (saleDetailsResult) {
            is UiResult.Loading, UiResult.Idle -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

            is UiResult.Error -> {
                Text(
                    text = saleDetailsResult.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is UiResult.Success -> {
                val details = saleDetailsResult.data
                val receiptJob = remember(details) {
                    ReceiptPrintJob(
                        storeName = "BUSINESS RECEIPT",
                        receiptNumber = details.sale.id.takeLast(8).uppercase(),
                        dateTime = details.sale.createdAt.toString().split("T").firstOrNull() ?: "",
                        cashierName = "Cashier",
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
                        amountPaid = details.payments.firstOrNull()?.amount?.formatted
                    )
                }
                val pageFormat =
                    if (selectedPaperFormat == "58mm") PrintPageFormat.Receipt58mm() else PrintPageFormat.Receipt80mm()

                ReceiptView(
                    job = receiptJob,
                    format = pageFormat,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPrintManual(details) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = printState !is UiResult.Loading
                    ) {
                        if (printState is UiResult.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                AppIcons.Print,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Print Receipt")
                        }
                    }

                    Button(
                        onClick = onDone,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("New Sale", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

