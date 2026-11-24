package com.citacomsys.feature.billing.ui.screen.sms

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.platform.uiModel.SmsPackageUiState
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.ui.layout.SectionWrapper
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SmsPurchaseReviewScreen(
    packageId: String,
    onBack: () -> Unit,
    onConfirmed: (billId: String) -> Unit,
    viewModel: SmsPurchaseViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Trigger the previewInvoice quote generation on entry
    LaunchedEffect(packageId) {
        viewModel.loadInvoicePreview(packageId)
    }

    SectionWrapper(
        title = "Confirm Purchase",
        onBack = onBack,
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grand Total", style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = state.previewState.getOrNull()?.total ?: "---",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.confirmSmsPurchase(packageId) { billId ->
                                onConfirmed(billId)
                            }
                        },
                        modifier = Modifier.height(56.dp).width(200.dp),
                        enabled = state.previewState is UiResult.Success && !state.isLoading,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Pay with MoMo")
                        }
                    }
                }
            }
        }
    ) {
        UiResultView(
            state = state.previewState,
            onRetry = { viewModel.loadInvoicePreview(packageId) }
        ) { preview ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "BILLING SUMMARY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Package Line Item
                        preview.lineItems.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.description, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${item.quantity} units @ ${item.unitPrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(item.totalAmount, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(Modifier.alpha(0.5f))

                        // Subtotal
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text(preview.subtotal, style = MaterialTheme.typography.bodyMedium)
                        }

                        // Discounts/Promos
                        preview.discounts.forEach { discount ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(discount.description, color = MaterialTheme.colorScheme.error)
                                Text(
                                    "- ${discount.amount}",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Taxes
                        preview.taxes.forEach { tax ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tax.description, style = MaterialTheme.typography.bodySmall)
                                Text(tax.amount, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Important Information Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    // Payment Notice
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Info, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "After confirmation, you will receive payment instructions for Mobile Money.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            AppIcons.Info,
                            null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Instant Credit",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "SMS units are credited to your account as soon as payment verification is complete. Units do not expire.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun SmsPackageSelectionScreen(
    onBack: () -> Unit,
    onNavigateToReview: (packageId: String) -> Unit,
    viewModel: SmsPurchaseViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SectionWrapper(
        title = "SMS Top-up",
        onBack = onBack
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Info
            Column {
                Text(
                    "Select SMS Bundle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "SMS credits are used for customer alerts and notifications. Credits do not expire.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            UiResultView(state.packagesResult) { packages ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(packages) { pkg ->
                        SmsBundleCard(
                            pkg = pkg,
                            isSelected = state.selectedPackageId == pkg.id,
                            onClick = { viewModel.selectPackage(pkg.id) }
                        )
                    }
                }
            }

            // Action Button
            Button(
                onClick = { state.selectedPackageId?.let { onNavigateToReview(it) } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.selectedPackageId != null && !state.isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Review Purchase")
            }
        }
    }

}

@Composable
private fun SmsBundleCard(
    pkg: SmsPackageUiState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Promo Badge
            if (pkg.hasPromo) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        pkg.savings ?: "PROMO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }

            Text(
                "${pkg.units}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Text("SMS Units", style = MaterialTheme.typography.labelMedium)

            Spacer(Modifier.height(8.dp))

            Text(
                pkg.promoPrice ?: pkg.originalPrice,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (pkg.hasPromo) {
                Text(
                    pkg.originalPrice,
                    style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

