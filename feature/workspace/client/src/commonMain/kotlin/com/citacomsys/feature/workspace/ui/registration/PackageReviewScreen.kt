package com.citacomsys.feature.workspace.ui.registration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.citacomsys.api.platform.uiModel.BillTermUiState
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.ResourcePriceBreakdown
import com.citacomsys.types.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageReviewScreen(
    handler: SubscriptionPackageReviewHandler,
    flavor: String,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val state by handler.state.collectAsStateWithLifecycle()
    val breakdownState = state.resourcePriceBreakdown
    val pkg = state.selectedPackage
    val selectedTerm = state.selectedBillingTerm

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isWide = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Plan & Pricing") },
                navigationIcon = { BackButton(onClick = onBack) }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    Button(
                        onClick = onNext,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "Confirm & Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        if (isWide) {
            Row(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left Column: Plan Info & Resources
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    PlanHeaderCard(pkg, selectedTerm, breakdownState, flavor)
                    IncludedResourcesSection(pkg)
                }

                // Right Column: Modules & Billing
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ModulesSection(pkg)
                    BillingSummarySection(pkg,  breakdownState)
                }
            }
        } else {
            LazyListView(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { PlanHeaderCard(pkg, selectedTerm, breakdownState, flavor) }
                item { IncludedResourcesSection(pkg) }
                item { ModulesSection(pkg) }
                item { BillingSummarySection(pkg, breakdownState) }
            }
        }
    }
}

@Composable
private fun PlanHeaderCard(
    pkg: ProductPackageUiState?,
    selectedTerm: BillTermUiState?,
    breakdownState: UiResult<ResourcePriceBreakdown>,
    flavor: String
) {
    pkg?.let {
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
            Column(Modifier.padding(24.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            AppIcons.Inventory,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                it.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black
                            )
                            selectedTerm?.let { term ->
                                Text(
                                    term.billingCycleName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    UiResultView(
                        state = breakdownState,
                        onRetry = {}
                    ) { breakdown ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = breakdown.total.formatted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                val modeText =
                    if (selectedTerm?.deploymentMode == DeploymentMode.OFFLINE_LOCAL) "Operating Mode: Offline Mode (Local Device Storage)" else "Operating Mode: Cloud Storage & Backup"
                Text(
                    text = modeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    it.description
                        ?: "Professional management suite for $flavor.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IncludedResourcesSection(pkg: ProductPackageUiState?) {
    pkg?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Plan Features & Limits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ResourceRow(AppIcons.Person, "Staff Accounts", "${it.maxUsers}")
                    ResourceRow(
                        AppIcons.Smartphone,
                        "POS Terminals / Registers",
                        "${it.maxDevices}"
                    )
                    ResourceRow(AppIcons.Business, "Branch Locations", "${it.maxBranches}")
                    ResourceRow(AppIcons.Category, "Product Items Limit", "${it.maxProductsLimit}")
                    ResourceRow(
                        AppIcons.Inventory,
                        "Active Stock Batches",
                        "${it.maxActiveBatchesLimit}"
                    )
                    ResourceRow(AppIcons.Sms, "SMS Alert Quota", "${it.smsQuotaPerCycle}")
                    ResourceRow(
                        AppIcons.History,
                        "Sales & Record History",
                        "${it.historyRetentionMonths} Months"
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ModulesSection(pkg: ProductPackageUiState?) {
    pkg?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Included Add-on Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    it.supportedModules.forEach { module ->
                        Surface(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = module.displayName(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillingSummarySection(
    pkg: ProductPackageUiState?,
    breakdownState: UiResult<ResourcePriceBreakdown>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Pricing & Cost Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Spacer(Modifier.weight(1f))
                pkg?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            UiResultValue(breakdownState) { breakdown ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    breakdown.lineItems.forEach { item ->
                        PriceLine(
                            label = "${item.description} (${item.quantity})",
                            value = item.totalAmount.formatted
                        )
                    }

                    if (breakdown.lineItems.isNotEmpty()) {
                        HorizontalDivider()
                    }

                    PriceLine("Subtotal", breakdown.subtotal.formatted)
                    breakdown.discounts.forEach { discount ->
                        PriceLine(
                            discount.description,
                            "- ${discount.amount.formatted}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    HorizontalDivider()
                    PriceLine("Total", breakdown.total.formatted, isBold = true)
                }
            }
        }
    }
}

@Composable
private fun <T> UiResultValue(state: UiResult<T>, content: @Composable (T) -> Unit) {
    UiResultView(state = state, onRetry = {}) { data ->
        content(data)
    }
}

@Composable
private fun PriceLine(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color,
            maxLines = 1,
            textAlign = TextAlign.End
        )
    }
}
