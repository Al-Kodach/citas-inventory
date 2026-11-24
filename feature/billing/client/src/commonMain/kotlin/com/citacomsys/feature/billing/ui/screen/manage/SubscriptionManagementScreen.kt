package com.citacomsys.feature.billing.ui.screen.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.api.workspace.model.SubscriptionSummary
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.components.LazyVerticalGridView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.formatted
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.productPackage.SelectableBillingTermCard
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getOrDefault
import com.citacomsys.core.ui.result.getOrNull
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubscriptionManagementScreen(
    onBack: () -> Unit,
    onNavigateToPreview: () -> Unit,
    viewModel: SubscriptionManagementViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AppScaffold(
        onBack = onBack,
        title = "Manage Subscription",
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyVerticalGridView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                UiResultView(state.currentSubscription) { current ->
                    CurrentPlanStatusCard(current)
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Available Plans",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state == UiResult.Error || state == UiResult.Loading) {
                item {
                    UiResultView(state.availablePackages)
                }
            }

            items(state.availablePackages.getOrDefault(emptyList())) { pkg ->
                val isCurrent =
                    state.currentSubscription.getOrNull()?.packageName == pkg.name

                UpgradePackageCard(
                    pkg = pkg,
                    isSelected = state.selectedPackage?.id == pkg.id,
                    isCurrentPlan = isCurrent,
                    onClick = { viewModel.selectPackage(pkg) }
                )
            }


            // Billing Term Selection (Appears once a package is selected)
            state.selectedPackage?.let {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Billing Cycle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            state.selectedPackage?.let {
                items(it.billingTerms) { term ->
                    SelectableBillingTermCard(
                        modifier = Modifier.fillMaxSize(),
                        billingTerm = term,
                        isSelected = state.selectedTerm?.id == term.id,
                        onSelect = { viewModel.selectTerm(term) },
                        canSelect = true
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {

                // Action Button
                Button(
                    onClick = {
                        state.selectedPackage?.let { pkg ->
                            state.selectedTerm?.let { term ->
                                onNavigateToPreview()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = state.selectedPackage != null && state.selectedTerm != null
                ) {
                    val isUpgrade =
                        state.selectedPackage?.name != state.currentSubscription.getOrNull()?.packageName
                    Text(if (isUpgrade) "Review Upgrade" else "Review Subscription")
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun UpgradePackageCard(
    pkg: ProductPackageUiState,
    isSelected: Boolean,
    isCurrentPlan: Boolean,
    onClick: () -> Unit
) {
    AppCard(
        isSelected = isSelected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        pkg.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    if (isCurrentPlan) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("YOUR CURRENT PLAN", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(
                    text = pkg.billingTerms.firstOrNull()?.originalPrice ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Small horizontal grid of resources
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallResourceChip(AppIcons.Person, "${pkg.maxUsers} Users")
                SmallResourceChip(AppIcons.Smartphone, "${pkg.maxDevices} Devices")
            }
        }
    }
}

@Composable
fun CurrentPlanStatusCard(summary: SubscriptionSummary) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Current Subscription", style = MaterialTheme.typography.labelSmall)
                Text(
                    summary.packageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Renews: ${summary.expiresAt?.formatted() ?: "Never"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Badge(
                containerColor = if (summary.status.isRestricted) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(summary.status.name, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
fun SmallResourceChip(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}