package com.citacomsys.feature.billing.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.productPackage.InvoicePreviewView
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.feature.billing.ui.screen.manage.SubscriptionManagementViewModel
import com.citacomsys.core.ui.layout.ResourceSpecItem
import com.citacomsys.core.ui.layout.SectionWrapper

@Composable
fun UpgradeReviewScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionManagementViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Fetch the live pro-rated invoice previewInvoice from the server on entry
    LaunchedEffect(state.selectedPackage) {
        viewModel.loadInvoicePreview()
    }

    SectionWrapper(
        title = "Review Upgrade",
        onBack = onBack,
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total to Pay Now", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = state.previewState.getOrNull()?.total ?: "---",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.confirmUpgrade()
                        },
                        enabled = state.previewState.isLoading,
                        modifier = Modifier.height(56.dp).width(200.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (state.previewState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Confirm & Pay")
                        }
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Info - Explains pro-rating
            Text(
                text = "Your new resources will be applied immediately. Any remaining credit from your current plan has been automatically deducted as a discount on this invoice.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Target Plan Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "UPGRADING TO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        state.selectedPackage!!.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Billing Cycle: ${state.selectedTerm!!.billingCycleName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val selectedPackage = state.selectedPackage ?: return@Column

            // Resource Specs Grid (Visualizing the value)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "NEW LIMITS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResourceSpecItem(
                        Modifier.weight(1f),
                        AppIcons.Person,
                        "${selectedPackage.maxUsers}",
                        "Users"
                    )
                    ResourceSpecItem(
                        Modifier.weight(1f),
                        AppIcons.Smartphone,
                        "${selectedPackage.maxDevices}",
                        "Devices"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResourceSpecItem(
                        Modifier.weight(1f),
                        AppIcons.Category,
                        "${selectedPackage.maxProductsLimit}",
                        "Products"
                    )
                    ResourceSpecItem(
                        Modifier.weight(1f),
                        AppIcons.Layers,
                        "${selectedPackage.maxActiveBatchesLimit}",
                        "Active Batches"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResourceSpecItem(
                        Modifier.weight(1f),
                        AppIcons.Sms,
                        "${selectedPackage.smsQuotaPerCycle}",
                        "SMS/Cycle"
                    )
                    ResourceSpecItem(
                        Modifier.weight(1f),
                        AppIcons.History,
                        "${selectedPackage.historyRetentionMonths}m",
                        "Retention"
                    )
                }
            }

            // The Bill Preview (The Math)
            InvoicePreviewView(
                invoicePreviewState = state.previewState,
                onRefresh = {
                    viewModel.loadInvoicePreview()
                }
            )

            Spacer(Modifier.height(100.dp))
        }
    }
}