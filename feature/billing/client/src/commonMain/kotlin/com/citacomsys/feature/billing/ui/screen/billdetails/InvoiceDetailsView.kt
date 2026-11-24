package com.citacomsys.feature.billing.ui.screen.billdetails

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.BillingRecord
import com.citacomsys.api.billing.SubscriptionBillingPayload
import com.citacomsys.api.billing.SmsTopupBillingPayload
import com.citacomsys.core.components.CitacomsysIcon
import com.citacomsys.core.components.TextField
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.productPackage.ResourcePriceBreakdownCard
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.ui.layout.SectionWrapper
import com.citacomsys.shared.displayName
import com.citacomsys.types.BillingStatus
import com.citacomsys.types.ResourcePriceBreakdown
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun InvoiceDetailsView(
    invoiceId: String,
    onBack: () -> Unit,
    viewModel: BillingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val record = state.billRecord.getOrNull()

    // UI state for manual payment form
    var showPaymentForm by androidx.compose.runtime.mutableStateOf(false)
    val isLoading = state.actionState is UiResult.Loading


    LaunchedEffect(invoiceId) {
        viewModel.loadBillDetails(invoiceId)
    }

    SectionWrapper(
        title = "Invoice Details",
        onBack = onBack,
        bottomBar = {
            if (record?.status == BillingStatus.PENDING && !showPaymentForm) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (record.canCancelInvoice) {
                        OutlinedButton(
                            onClick = { viewModel.cancelBill(invoiceId, "", onBack) },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cancel Bill")
                        }
                    }
                    Button(
                        onClick = { showPaymentForm = true },
                        modifier = Modifier.weight(1.5f),
                        enabled = !isLoading
                    ) {
                        Text("I've Paid")
                    }
                }
            }
        }
    ) {
        UiResultView(state.billRecord) { invoice ->
            InvoiceDetailsContent(
                invoice = invoice,
                isLoading = state.actionState is UiResult.Loading,
                showPaymentForm = showPaymentForm,
                error = state.actionState.getErrorOrNull(),
                markAsPaid = { invoiceId, senderName, transactionId ->
                    viewModel.markAsPaid(invoiceId, senderName, transactionId) {
                        showPaymentForm = false
                    }
                },
                closePaymentForm = { showPaymentForm = false }
            )
        }
    }
}

@Composable
fun InvoiceDetailsContent(
    invoice: BillingRecord,
    showPaymentForm: Boolean,
    isLoading: Boolean,
    error: String?,
    markAsPaid: (
        invoiceId: String,
        senderName: String,
        transactionId: String,
    ) -> Unit,
    closePaymentForm: () -> Unit
) {
    var senderName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var transactionId by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            ""
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Status Header
        InvoiceStatusHeader(invoice)

        // 2. Resource Pricing Breakdown (Invoice Level)
        val breakdown = remember(invoice.quote) {
            when (val p = invoice.quote.payload) {
                is SubscriptionBillingPayload -> ResourcePriceBreakdown(
                    lineItems = p.provisionedPackage.lineItems,
                    subtotal = invoice.quote.subtotal,
                    total = invoice.quote.total
                )

                is SmsTopupBillingPayload -> p.resourcePriceBreakdown
            }
        }

        ResourcePriceBreakdownCard(
            resourcePriceBreakdown = breakdown,
            discounts = invoice.quote.discounts,
            taxes = invoice.quote.taxes,
            title = "Bill Summary",
            isStandalone = false
        )

        // 3. Totals
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Text("Total Amount Due", style = MaterialTheme.typography.labelMedium)
            Text(
                invoice.amount.formatted,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // 4. Payment Instruction Card (Manual MoMo Flow)
        if (invoice.status == BillingStatus.PENDING || invoice.status == BillingStatus.PAYMENT_SUBMITTED) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    )
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Payment Instructions (Mobile Money)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("Please send the exact amount to the merchant below using your reference code.")

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp).alpha(0.5f)
                    )

                    PaymentInfoRow("Merchant Number", invoice.paymentTarget)
                    PaymentInfoRow("Recipient", invoice.paymentRecipientName)
                    PaymentInfoRow(
                        "Reference Code",
                        invoice.referenceCode,
                        isHighlight = true
                    )
                }
            }
        }

        // 5. Manual Payment Details Submission Form
        if (showPaymentForm && invoice.status == BillingStatus.PENDING) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = 0.2f
                    )
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Confirm Payment Details", fontWeight = FontWeight.Bold)

                    TextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = "Sender Name / Registered MoMo Name",
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextField(
                        value = transactionId,
                        onValueChange = { transactionId = it },
                        label = "Transaction ID / Reference ID",
                        modifier = Modifier.fillMaxWidth()
                    )

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = closePaymentForm,
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }

                        Button(
                            onClick = {
                                markAsPaid(
                                    invoice.id,
                                    senderName,
                                    transactionId
                                )
                            },
                            modifier = Modifier.weight(1.5f),
                            enabled = senderName.isNotBlank() && transactionId.isNotBlank() && !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(
                                Modifier.size(
                                    20.dp
                                ), strokeWidth = 2.dp
                            )
                            else Text("Submit for Verification")
                        }
                    }
                }
            }
        }

        if (invoice.status == BillingStatus.PAYMENT_SUBMITTED) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Verification in progress. Usually takes 1-2 hours.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun PaymentInfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (isHighlight) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InvoiceStatusHeader(record: com.citacomsys.api.billing.BillingRecord) {
    val (containerColor, contentColor, icon, label) = when (record.status) {
        BillingStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            AppIcons.Schedule
        ).add("Payment Pending")

        BillingStatus.PAID, BillingStatus.FULFILLED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            AppIcons.CheckCircle
        ).add("Paid & Active")

        BillingStatus.PAYMENT_SUBMITTED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            AppIcons.History
        ).add("Verification in Progress")

        else -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            AppIcons.Cancel
        ).add("Invoice ${record.status.displayName()}")
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CitacomsysIcon(
                imageVector = icon,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = "Ref: ${record.referenceCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Helper for Triple
private fun <A, B, C> Triple<A, B, C>.add(d: String) = Quad(first, second, third, d)
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)