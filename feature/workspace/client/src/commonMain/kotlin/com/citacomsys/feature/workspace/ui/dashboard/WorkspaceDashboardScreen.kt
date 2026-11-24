package com.citacomsys.feature.workspace.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.billing.navkeys.SubscriptionHubNav
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.modifiers.shimmerEffect
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.spi.InventoryNav
import com.citacomsys.core.spi.LocalNavigator
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.ui.UiResultGridView
import com.design.components.AppVerticalGridView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.home.data.repository.DashboardActivityData
import com.citacomsys.home.data.repository.DashboardSummaryData
import com.citacomsys.types.Money
import com.citacomsys.types.TrendPoint
import com.design.components.ScrollableRowWithButtons
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.PluginDashboardWidget
import com.design.components.AppScaffold
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkspaceDashboardScreen(
    viewModel: WorkspaceDashboardViewModel = koinViewModel()
) {
    val summaryState by viewModel.summary.collectAsStateWithLifecycle()
    val activityState by viewModel.activityFeed.collectAsStateWithLifecycle()
    val dashboardWidgets by viewModel.activeWidgets.collectAsStateWithLifecycle()
    val isAdvancedReportingEnabled by viewModel.isAdvancedReportingEnabled.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()

    val sessionManager: AppSessionManager = koinInject()
    val navigator = LocalNavigator.current
    val workStationInfo by sessionManager.workStationInfo.collectAsStateWithLifecycle()


    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isCompact = windowSizeClass.minWidthDp < 840

    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val startOfWeek = remember(today) {
        today.minus(today.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)
    }
    val startOfMonth = remember(today) {
        LocalDate(today.year, today.month, 1)
    }


    val carouselWidgets = remember(dashboardWidgets) {
        dashboardWidgets.filter {
            it.placement == DashboardPlacement.ANALYTICS_CAROUSEL || it.placement == DashboardPlacement.HERO
        }.sortedBy { it.order }
    }

    val gridWidgets = remember(dashboardWidgets) {
        dashboardWidgets.filter {
            it.placement == DashboardPlacement.GRID ||
                    it.placement == DashboardPlacement.LIST ||
                    it.placement == DashboardPlacement.SIDEBAR
        }.sortedBy { it.order }
    }

    val activityWidgets = remember(dashboardWidgets) {
        dashboardWidgets.filter {
            it.placement == DashboardPlacement.ACTIVITY || it.placement == DashboardPlacement.ACTIVITY_STREAM
        }.sortedBy { it.order }
    }

    val minWidth = windowSizeClass.minWidthDp

    val gridColumns = remember(minWidth) {
        when {
            minWidth >= 1600 -> GridCells.Fixed(3)
            minWidth >= 840 -> GridCells.Fixed(2)
            else -> GridCells.Fixed(1)
        }
    }

    val saleSubText = remember(startDate, endDate, today, startOfWeek, startOfMonth) {
        when {
            startDate == today && endDate == null -> "today"
            startDate == startOfWeek && endDate == today -> "this week"
            startDate == startOfMonth && endDate == today -> "this month"
            startDate == null && endDate == null -> "all time"
            else -> "selected period"
        }
    }

    AppScaffold(
        topBar = {
            TopAppBar(
                title = {
                    ProjectLogos.ProductDashboardLogo(
                        title = workStationInfo.activeWorkspaceName,
                        type = workStationInfo.activeProductType,
                        size = 40.dp
                    )
                }
            )
        }
    ) { padding ->
        AppVerticalGridView(
            modifier = Modifier.fillMaxSize().padding(padding),
            columns = gridColumns,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Workspace Header, Switcher & Date Range Retention Bar (Full Width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                ActiveWorkspaceBanner(
                    startDate = startDate,
                    endDate = endDate,
                    today = today,
                    startOfWeek = startOfWeek,
                    startOfMonth = startOfMonth,
                    onDateRangeSelected = viewModel::setDateRange
                )
            }

            // Top Carousel / Hero Analytics Widgets (Span Full Width)
            if (carouselWidgets.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ScrollableRowWithButtons(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = carouselWidgets,
                            key = { it.id }
                        ) { widget ->
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 320.dp, max = 480.dp)
                                    .fillMaxWidth() // full width for single item
                            ) {
                                widget.content()
                            }
                        }
                    }

                }
            }

            // KPI Summary Bar Card (Full Width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                when (val s = summaryState) {
                    is UiResult.Loading -> KPIShimmerBar()
                    is UiResult.Success -> KpiSummaryBarCard(
                        summary = s.data,
                        saleSubText = saleSubText,
                        isCompact = isCompact
                    )

                    else -> {}
                }
            }

            // Quick Actions Section (Shown in Compact Mode, Full Width)
            if (isCompact) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    QuickActionsSection(
                        onAddProduct = { navigator.navigateToSingleTop(InventoryNav) },
                        onNewSalesOrder = { navigator.navigateToSingleTop(InventoryNav) },
                        onViewInventory = { navigator.navigateToSingleTop(InventoryNav) },
                        onGenerateReport = { navigator.navigateToSingleTop(InventoryNav) }
                    )
                }
            }

            // Grid & List Feature Overview Cards (Displayed Directly on Screen Grid)
            gridWidgets.forEach { widget ->
                item(key = widget.id) {
                    widget.content()
                }
            }

            // Advanced Analytics (Full Width)
            if (isAdvancedReportingEnabled) {
                when (val s = summaryState) {
                    is UiResult.Success -> {
                        item {
                            RevenueTrendSection(s.data.revenueTrend)
                        }
                    }

                    else -> {}
                }
            }

            //  Workspace Activity Section with Stream Widgets (Full Width)
            item(span = { GridItemSpan(maxLineSpan) }) {
                WorkspaceActivitySection(
                    activityState = activityState,
                    streamWidgets = activityWidgets
                )
            }

            if (isAdvancedReportingEnabled.not()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PremiumFeatureNudge(
                        title = "Unlock Advanced Intelligence",
                        description = "Gain access to daily revenue trends, top moving products, and predictive inventory forecasting by upgrading to a Pro or Enterprise plan.",
                        onUpgrade = { navigator.navigateToSingleTop(SubscriptionHubNav) }
                    )
                }
            }

        }
    }
}


@Composable
private fun ActiveWorkspaceBanner(
    startDate: LocalDate?,
    endDate: LocalDate?,
    today: LocalDate,
    startOfWeek: LocalDate,
    startOfMonth: LocalDate,
    onDateRangeSelected: (LocalDate?, LocalDate?) -> Unit,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Date Range Retention Filter Bar (All options selectable)
        ScrollableRowWithButtons(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = startDate == today && endDate == null,
                    onClick = { onDateRangeSelected(today, null) },
                    label = { Text("Today") }
                )
            }
            item {
                FilterChip(
                    selected = startDate == startOfWeek && endDate == today,
                    onClick = { onDateRangeSelected(startOfWeek, today) },
                    label = { Text("This Week") }
                )
            }
            item {
                FilterChip(
                    selected = startDate == startOfMonth && endDate == today,
                    onClick = { onDateRangeSelected(startOfMonth, today) },
                    label = { Text("This Month") }
                )
            }
            item {
                FilterChip(
                    selected = startDate == null && endDate == null,
                    onClick = { onDateRangeSelected(null, null) },
                    label = { Text("All Time") }
                )
            }
        }
    }
}

@Composable
private fun KpiSummaryBarCard(
    summary: DashboardSummaryData,
    saleSubText: String = "today",
    isCompact: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isCompact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KpiSummaryItem(
                    label = "Total Sales",
                    value = Money(summary.totalRevenueMinor).formatted,
                    subtext = saleSubText,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier.height(48.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                KpiSummaryItem(
                    label = "Low Stock",
                    value = summary.lowStockCount.toString(),
                    subtext = "items",
                    valueColor = if (summary.lowStockCount > 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier.height(48.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                KpiSummaryItem(
                    label = "Pending Orders",
                    value = (summary.expiryRiskCount + summary.stockoutRiskCount).toString(),
                    subtext = "orders",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    KpiSummaryItem(
                        label = "Total Sales",
                        value = Money(summary.totalRevenueMinor).formatted,
                        subtext = saleSubText,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    KpiSummaryItem(
                        label = "Low Stock",
                        value = summary.lowStockCount.toString(),
                        subtext = "items",
                        valueColor = if (summary.lowStockCount > 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                        modifier = Modifier.weight(1f)
                    )
                    KpiSummaryItem(
                        label = "Pending Orders",
                        value = (summary.expiryRiskCount + summary.stockoutRiskCount).toString(),
                        subtext = "orders",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiSummaryItem(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (valueColor != Color.Unspecified) valueColor else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun KPIShimmerBar() {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(90.dp).shimmerEffect()
    ) {}
}

@Composable
private fun QuickActionsSection(
    onAddProduct: () -> Unit,
    onNewSalesOrder: () -> Unit,
    onViewInventory: () -> Unit,
    onGenerateReport: () -> Unit
) {
    DashboardWidgetCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ScrollableRowWithButtons(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    QuickActionTile(
                        icon = AppIcons.Add,
                        title = "Add Product",
                        onClick = onAddProduct
                    )
                }
                item {
                    QuickActionTile(
                        icon = AppIcons.Receipt,
                        title = "New Sales Order",
                        onClick = onNewSalesOrder
                    )
                }
                item {
                    QuickActionTile(
                        icon = AppIcons.Inventory2,
                        title = "View Inventory",
                        onClick = onViewInventory
                    )
                }
                item {
                    QuickActionTile(
                        icon = AppIcons.Analytics,
                        title = "Generate Report",
                        onClick = onGenerateReport
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(90.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WorkspaceActivitySection(
    activityState: UiResult<List<DashboardActivityData>>,
    streamWidgets: List<PluginDashboardWidget> = emptyList()
) {
    DashboardWidgetCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Workspace Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (streamWidgets.isNotEmpty()) {
                streamWidgets.forEach { widget ->
                    Box(modifier = Modifier.padding(vertical = 4.dp)) {
                        widget.content()
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }

            UiResultGridView(
                state = activityState,
                modifier = Modifier.fillMaxWidth().height(320.dp),
                columns = GridCells.Adaptive(320.dp),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                emptyPlaceholder = {
                    Text(
                        text = "No recent workspace activity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            ) { movements ->
                items(movements.size, key = { movements[it].id }) { index ->
                    val movement = movements[index]
                    val isSale = movement.type == "IN" || movement.activityType?.contains(
                        "SALE",
                        ignoreCase = true
                    ) == true
                    val isOut = movement.type == "OUT"
                    val icon: ImageVector =
                        if (isSale) AppIcons.PointOfSale else if (isOut) AppIcons.Inventory2 else AppIcons.Warning
                    val iconColor: Color =
                        if (isSale) Color(0xFF2E7D32) else if (isOut) Color(0xFF1565C0) else Color(
                            0xFFE53935
                        )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = iconColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${movement.activityType ?: "ACTIVITY"}: ${movement.productName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Batch #${movement.batchNumber ?: "N/A"} • ${if (movement.type == "IN") "+" else "-"}${movement.quantity} Units • By: ${movement.actorName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevenueTrendSection(
    trend: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    DashboardWidgetCard(
        modifier = modifier,
        placement = DashboardPlacement.GRID
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Revenue Performance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Daily sales trend for the selected period",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            if (trend.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales data available for this period.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.Bottom
                ) {
                    val maxVal = trend.maxOf { it.value }.coerceAtLeast(1.0)
                    trend.forEach { point ->
                        val barHeight = (point.value / maxVal * 140).dp
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(
                                            topStart = 4.dp,
                                            topEnd = 4.dp
                                        )
                                    )
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = point.label.takeLast(5),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureNudge(
    title: String,
    description: String,
    onUpgrade: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onUpgrade,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Unlock with Pro")
            }
        }
    }
}
