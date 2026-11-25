package com.citacomsys.sales.ui.screen.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.api.sales.SaleDetails
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.printer.PrintPageFormat
import com.citacomsys.core.printer.ReceiptItem
import com.citacomsys.core.printer.ReceiptPrintJob
import com.citacomsys.core.printer.ui.ReceiptView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.types.PhoneNumber
import com.design.AppTheme
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosPaymentScreen(
    viewModel: PosViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCustomer = uiState.selectedCustomer
    val completedSaleId = uiState.completedSaleId

    if (completedSaleId != null) {
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
                onBack()
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
                    onBack()
                }
            )
        }
    }

    AppScaffold(
        title = "Payment",
        onBack = onBack,
        supportTitle = selectedCustomer?.fullName ?: "Ledger",
        supportContent = {
            CustomerLedgerSummaryContent(customer = selectedCustomer)
        },
        actionIcon = AppIcons.Person,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        PaymentContent(
            total = uiState.total.formatted,
            isLoading = uiState.isLoading,
            error = uiState.error,
            selectedCustomer = uiState.selectedCustomer,
            onConfirm = viewModel::confirmPayment,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun PaymentContent(
    modifier: Modifier = Modifier,
    total: String,
    isLoading: Boolean,
    error: String? = null,
    selectedCustomer: CustomerItem? = null,
    onConfirm: (PaymentMethod, String?) -> Unit,
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var reference by remember { mutableStateOf("") }
    var depositText by remember { mutableStateOf("") }
    val isCustomerSelected = selectedCustomer != null

    Column(
        modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text("Select Method", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        PaymentMethodItem(
            PaymentMethod.CASH,
            "Cash",
            AppIcons.PointOfSale,
            selectedMethod == PaymentMethod.CASH
        ) { selectedMethod = it }
        Spacer(Modifier.height(8.dp))

        PaymentMethodItem(
            PaymentMethod.CARD,
            "Bank Card",
            AppIcons.Fingerprint,
            selectedMethod == PaymentMethod.CARD
        ) { selectedMethod = it }
        Spacer(Modifier.height(8.dp))

        PaymentMethodItem(
            PaymentMethod.MOBILE_MONEY,
            "Mobile Money",
            AppIcons.Call,
            selectedMethod == PaymentMethod.MOBILE_MONEY
        ) { selectedMethod = it }

        Spacer(Modifier.height(8.dp))

        PaymentMethodItem(
            PaymentMethod.CREDIT,
            "Buy on Credit",
            AppIcons.History,
            selectedMethod == PaymentMethod.CREDIT,
            enabled = isCustomerSelected
        ) { selectedMethod = it }

        if (selectedMethod != PaymentMethod.CASH && selectedMethod != PaymentMethod.CREDIT) {
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference / TxID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (selectedMethod == PaymentMethod.CREDIT) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Credit Sale & Upfront Cash Deposit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "This transaction will create a CREDIT_PURCHASE ledger entry against ${selectedCustomer?.fullName ?: "the customer"}'s account. If an upfront cash deposit is paid now, enter it below to automatically record a matching PAYMENT ledger entry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = depositText,
                        onValueChange = { depositText = it },
                        label = { Text("Upfront Deposit Paid Now (GHS)") },
                        placeholder = { Text("e.g. 400.00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("Reference / TxID / Receipt Note (Optional)") },
                        placeholder = { Text("e.g. Deposit TxID #MOMO9812") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(Modifier.padding(vertical = 16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Amount", style = MaterialTheme.typography.titleMedium)
            Text(
                total,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        error?.let {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val depositInfo =
                    if (selectedMethod == PaymentMethod.CREDIT && depositText.isNotBlank()) {
                        "DEPOSIT:${depositText.trim()}|REF:${reference.trim()}"
                    } else reference.takeIf { it.isNotBlank() }

                onConfirm(selectedMethod, depositInfo)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("FINALIZE SALE", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun CustomerLedgerSummaryContent(customer: CustomerItem?) {
    if (customer == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = AppIcons.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No Customer Assigned",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Assign a customer in the cart to view financial account balances and credit history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = AppIcons.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = customer.phone?.let { PhoneNumber.tryCreate(it)?.mask ?: it }
                            ?: "No Phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodItem(
    method: PaymentMethod,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean = true,
    onClick: (PaymentMethod) -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick(method) },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (!enabled && method == PaymentMethod.CREDIT) {
                    Text(
                        text = "Select a customer first",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = enabled
            )
        }
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
                            Text("Print Receipt Sheet")
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

@AllAdaptivePreviews
@Composable
private fun PaymentContentPreview() {
    AppTheme {
        PaymentContent(
            total = "GHS 125.00",
            isLoading = false,
            onConfirm = { _, _ -> }
        )
    }
}
