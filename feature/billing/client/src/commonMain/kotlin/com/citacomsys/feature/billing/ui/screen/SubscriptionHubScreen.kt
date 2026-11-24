package com.citacomsys.feature.billing.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.TransactionHistory
import com.citacomsys.api.billing.navkeys.*
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.kernel.model.WorkspaceResource
import com.citacomsys.kernel.model.WorkspaceResourcesUsage
import com.design.ThemeManager
import com.design.components.AppListView
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubscriptionHubScreen(
    onNavigate: (TenantBillingNav) -> Unit,
    viewModel: WorkspaceSubscriptionViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val configState by viewModel.configState.collectAsStateWithLifecycle()

    AppScaffold(
        title = "Subscription",
        actions = {
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(AppIcons.Refresh, "Refresh")
            }
        }
    ) { padding ->
        AppListView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Plan Overview Card
            item {
                CurrentPlanCard(
                    subscription = state.planInfo,
                    onManageSubscription = { onNavigate(SubscriptionManagementNav) }
                )
            }

            // Resource Usage Quick View
            item {
                UiResultView(
                    configState,
                    onRetry = viewModel::refreshConfig
                ) { config ->
                    WorkspaceResourceUsageSummary(
                        config?.resource,
                        config?.usage,
                        viewModel::refreshConfig
                    )
                }
            }

            // Action Grid
            item {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubActionCard(
                        modifier = Modifier.weight(1f),
                        icon = AppIcons.History,
                        label = "Manage Plan",
                        onClick = { onNavigate(SubscriptionManagementNav) }
                    )
                    HubActionCard(
                        modifier = Modifier.weight(1f),
                        icon = AppIcons.Sms,
                        label = "SMS Top-up",
                        onClick = { onNavigate(SmsTopupNav) }
                    )
                }
            }

            // Recent Transaction History (Integrated)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Invoices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigate(BillingHistoryNav) }) {
                        Text("See All")
                    }
                }
            }
            item {
                UiResultView(
                    state.recentTransactions,
                    onRetry = { viewModel.refresh() }
                ) { history ->
                    if (history.isEmpty()) {
                        Text(
                            "No recent transactions found.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp).alpha(0.6f)
                        )
                    } else {
                        history.take(3).forEach { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                onClick = { onNavigate(InvoiceDetailsNav(transaction.id)) }
                            )
                        }

                        if (history.size > 3) {
                            TextButton(onClick = { onNavigate(BillingHistoryNav) }) {
                                Text("See All")
                            }
                        }
                    }
                }
            }

            // Footer / Secondary Actions
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(Modifier.alpha(0.3f))
                ListItem(
                    headlineContent = { Text("Payment Methods") },
                    supportingContent = { Text("Manage saved cards or MoMo accounts") },
                    leadingContent = {
                        Icon(
                            AppIcons.CreditCard,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = { Icon(AppIcons.ChevronRight, null) },
                    modifier = Modifier.clickable { /* Future: Navigate to Payment Methods */ }
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionHistory,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        headlineContent = { Text(transaction.summary) },
        supportingContent = { Text("${transaction.date} • ${transaction.referenceCode}") },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = transaction.amount.formatted,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (transaction.isPaid) "Paid" else "Pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (transaction.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        },
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = if (transaction.isPaid) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (transaction.isPaid) AppIcons.CheckCircle else AppIcons.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (transaction.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
fun CurrentPlanCard(
    subscription: TenantSubscriptionUiState?,
    onManageSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = subscription?.packageName ?: "Loading...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Status Badge
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Text(
                        text = subscription?.status ?: "ACTIVE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Renews on ${subscription?.expiryDate ?: "---"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onManageSubscription,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Manage Plan", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview
@Composable
fun CurrentPlanCardBrandPreviews() {
    val sampleSubscription = TenantSubscriptionUiState(
        packageName = "Enterprise Plan",
        status = "TRIAL",
        expiryDate = "Nov 4, 2026"
    )

    val colorSchemes = listOf(
        "Deep Red (Citas Pharmacy)" to ThemeManager.fromColors(Color(0xFFB71C1C)),
        "Clinical Green" to ThemeManager.fromColors(Color(0xFF2E7D32)),
        "Medical Navy Blue" to ThemeManager.fromColors(Color(0xFF1565C0)),
        "Deep Charcoal" to ThemeManager.fromColors(Color(0xFF212121)),
        "Warm Amber" to ThemeManager.fromColors(Color(0xFFE65100))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        colorSchemes.forEach { (label, scheme) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MaterialTheme(colorScheme = scheme) {
                    CurrentPlanCard(
                        subscription = sampleSubscription,
                        onManageSubscription = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceResourceUsageSummary(
    res: WorkspaceResource?,
    usage: WorkspaceResourcesUsage?,
    onRefresh: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FlowRow(Modifier.fillMaxWidth()) {
            Text(
                text = "Resource Utilization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onRefresh) {
                Icon(AppIcons.Refresh, null)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ResourceProgressBar(
                    label = "Users",
                    current = usage?.userCount ?: 0,
                    limit = res?.maxUsers ?: 0,
                    icon = AppIcons.People
                )
                ResourceProgressBar(
                    label = "Devices & Workstations",
                    current = usage?.deviceCount ?: 0,
                    limit = res?.maxDevice ?: 0,
                    icon = AppIcons.Smartphone
                )
                ResourceProgressBar(
                    label = "Products Catalog",
                    current = usage?.productCountUsed ?: 0,
                    limit = res?.maxProductsLimit ?: 0,
                    icon = AppIcons.Storefront
                )
                ResourceProgressBar(
                    label = "Branches",
                    current = usage?.branchCount ?: 0,
                    limit = res?.maxBranches ?: 0,
                    icon = AppIcons.Business
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History Data Retention",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${res?.historyRetentionMonths ?: 12} Months",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceProgressBar(
    label: String,
    current: Int,
    limit: Int,
    icon: ImageVector
) {
    val progress = if (limit > 0) current.toFloat() / limit.toFloat() else 0f
    val color = when {
        progress > 0.9f -> MaterialTheme.colorScheme.error
        progress > 0.7f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                "$current / $limit",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}


@Composable
fun HubActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(110.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}