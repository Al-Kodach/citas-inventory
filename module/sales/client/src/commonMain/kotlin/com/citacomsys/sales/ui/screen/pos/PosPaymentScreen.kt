package com.citacomsys.sales.ui.screen.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.sales.PaymentMethod
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.module.api.customer.CustomerItem
import com.citacomsys.types.PhoneNumber
import com.design.AppTheme
import com.design.components.AppScaffold
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PosPaymentScreen(
    viewModel: PosViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCustomer = uiState.selectedCustomer

    LaunchedEffect(uiState) {
        if (uiState.completedSaleId != null) {
            delay(500.milliseconds)
            onBack()
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
        // Customer Identity Summary
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
