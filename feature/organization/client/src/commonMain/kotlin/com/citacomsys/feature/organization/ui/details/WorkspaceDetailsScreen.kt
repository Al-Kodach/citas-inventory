package com.citacomsys.feature.organization.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.navkeys.SubscriptionHubNav
import com.citacomsys.api.organization.nav.BranchManagementNav
import com.citacomsys.api.workspace.model.WorkspaceDetails
import com.citacomsys.types.PackageTier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.util.formatted
import com.citacomsys.types.ProductType
import com.design.components.AppScaffold
import com.design.components.AppVerticalGridView
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkspaceDetailsScreen(
    isCompact: Boolean,
    workspaceId: String,
    onBack: () -> Unit,
    viewModel: WorkspaceDetailsViewModel = koinViewModel()
) {
    val detailsResult by viewModel.workspaceDetails.collectAsStateWithLifecycle()

    LaunchedEffect(workspaceId) {
        viewModel.loadWorkspaceDetails(workspaceId)
    }

    val details = detailsResult.getOrNull()

    AppScaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    ProjectLogos.ProductDashboardLogo(
                        title = details?.name ?: "Business Info",
                        type = details?.type ?: ProductType.INVENTORY,
                    )
                },
                actions = {
                    if (details != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = details.status.name,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 4.dp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    AnimatedVisibility(!isCompact) {
                        IconButton(onClick = { viewModel.loadWorkspaceDetails(workspaceId) }) {
                            Icon(AppIcons.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
//                windowInsets = WindowInsets(0.dp)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(isCompact) {
                FloatingActionButton(onClick = { viewModel.loadWorkspaceDetails(workspaceId) }) {
                    Icon(AppIcons.Refresh, contentDescription = "Refresh")
                }
            }
        }
    ) { padding ->
        UiResultView(
            state = detailsResult,
            onRetry = { viewModel.loadWorkspaceDetails(workspaceId) },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { details ->

            AppVerticalGridView(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Workspace Identity Header Card with Target Stores & LaunchPad (Merged Header)
                item(span = { GridItemSpan(maxLineSpan) }) {
                    WorkspaceHeaderCard(details = details)
                }

                // Resource Usage / Progress Section
                item {
                    WorkspaceResourceUsageSummary(details = details)
                }

                // Subscription & Plan Overview Card (Includes Deployment Mode)
                item {
                    SubscriptionPlanOverviewCard(details = details)
                }


                item {
                    AccountFinancialsCard(details = details)
                }

                // Advanced Business Intelligence Section (Pro & Enterprise Tiers / Trial)
                item {
                    WorkspaceAdvancedBusinessAnalysisSection(details = details)
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceHeaderCard(
    details: WorkspaceDetails
) {
    val navigator = LocalNavigator.current


    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row: Title & Store Management Entry Point
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Branches / Stores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { navigator.navigateToSingleTop(BranchManagementNav(details.workspaceId)) }
            ) {
                Icon(AppIcons.Settings, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Manage Stores")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                details.branches.forEach { branch ->
                    FilterChip(
                        selected = false,
                        onClick = { },
                        label = { Text(branch.name.ifBlank { details.name }) },
                        leadingIcon = {
                            ProjectLogos.getProductIcon(details.type)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPlanOverviewCard(details: WorkspaceDetails) {
    val sub = details.subscription
    val res = details.resource
    val account = details.account

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current Plan & Deployment",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = res?.name ?: "Default Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            ) {
                Text(
                    text = sub?.status?.name ?: "ACTIVE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Billing Cycle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    sub?.billingCycle?.name ?: "MONTHLY",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Recurring Amount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    sub?.recurringAmount?.formatted ?: "GHS 0.00",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(12.dp))

        DetailInfoRow("Deployment Mode", account?.deploymentMode?.name ?: "CLOUD_SYNCED")
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                AppIcons.CalendarToday,
                null,
                Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Expires / Renews: ${sub?.endedAt?.formatted() ?: "Never"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WorkspaceResourceUsageSummary(details: WorkspaceDetails) {
    val res = details.resource
    val usage = details.usage

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Resource Utilization",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
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
            label = "Stores",
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
private fun AccountFinancialsCard(details: WorkspaceDetails) {
    val account = details.account

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Account & Financials",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        DetailInfoRow("Account Balance", account?.balance?.formatted ?: "GHS 0.00")
        DetailInfoRow("SMS Credit Balance", "${account?.smsBalance ?: 0} SMS")
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceAdvancedBusinessAnalysisSection(details: WorkspaceDetails) {
    val navigator = LocalNavigator.current
    val isPremium = details.resource?.tier != null && details.resource?.tier != PackageTier.STARTER

    var selectedBranchId by remember(details.workspaceId) {
        mutableStateOf<String?>(details.branches.firstOrNull()?.id)
    }
    var selectedDateRange by remember { mutableStateOf("This Month") }

    DashboardWidgetCard(
        modifier = Modifier.fillMaxWidth(),
        placement = DashboardPlacement.GRID
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advanced Business Intelligence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                if (!isPremium) {
                    TextButton(onClick = { navigator.navigateToSingleTop(SubscriptionHubNav) }) {
                        Icon(AppIcons.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upgrade Plan")
                    }
                }
            }
            if (isPremium) {
                // Filter Bar: Branch Filter & Date Range Filter
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filter Analysis Scope",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedBranchId == null,
                            onClick = { selectedBranchId = null },
                            label = { Text("All Branches") }
                        )
                        details.branches.forEach { branch ->
                            FilterChip(
                                selected = selectedBranchId == branch.id,
                                onClick = { selectedBranchId = branch.id },
                                label = { Text(branch.name) }
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Today",
                            "This Week",
                            "This Month",
                            "All Time"
                        ).forEach { range ->
                            FilterChip(
                                selected = selectedDateRange == range,
                                onClick = { selectedDateRange = range },
                                label = { Text(range) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // 2x2 Analysis Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Gross Sales Revenue",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = details.account?.balance?.formatted ?: "GHS 0.00",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Stock Valuation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "GHS 0.00",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "0 Orders",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Staff Operator Analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "All Staff Synced",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pro / Enterprise Feature",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Multi-branch performance breakdown, operator sales tracking, and custom date range financial valuation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
