package com.citacomsys.inventory.admin.app.dashboard.presentation

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.admin.api.dashboard.AdminDashboardOverview
import com.citacomsys.admin.api.dashboard.RecentOnboarding
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.components.KpiCard
import com.citacomsys.core.modifiers.shimmerEffect
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.getOrDefault
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.types.Money
import com.citacomsys.types.ProductType
import com.design.components.AppListView
import com.design.components.AppVerticalGridView
import com.design.components.DASHBOARD_LARGE_SCREEN_WIDTH
import com.design.components.LocalApplyDashboardPadding
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.PluginDashboardWidget
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel = koinViewModel(),
    coordinator: NavigationCoordinator = koinInject()
) {
    val dashboardWidgets by coordinator.activeDashboardWidgets.collectAsStateWithLifecycle()
    val summaryState by viewModel.summary.collectAsStateWithLifecycle()

    val carouselWidgets = remember(dashboardWidgets) {
        dashboardWidgets.filter { it.placement == DashboardPlacement.ANALYTICS_CAROUSEL }
            .sortedBy { it.order }
    }

    val activityWidgets = remember(dashboardWidgets) {
        dashboardWidgets.filter { it.placement == DashboardPlacement.ACTIVITY_STREAM || it.placement == DashboardPlacement.SIDEBAR }
            .sortedBy { it.order }
    }

    if (summaryState.isSuccess.not()) {
        UiResultView(summaryState)
        return
    }


    UiResultView(
        state = summaryState,
        onRetry = viewModel::loadSummary
    ) { summary ->
        AppVerticalGridView(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            if (carouselWidgets.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DashboardAnalyticsCarousel(
                        widgets = carouselWidgets,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                PlatformKpiSection(summary)
            }

            item {
                SystemHealthSection(summary.systemHealthStatus)
            }

            item {
                PlatformBillingSection(summary)
            }

            item {
                RecentOnboardingsSection(
                    summary.recentOnboardings,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            activityWidgets.forEach { widget ->
                item(key = widget.id) {
                    widget.content()
                }
            }
        }
    }
}

@Composable
private fun PlatformKpiSection(summary: AdminDashboardOverview) {
    val kpis = listOf(
        Triple(
            "Organizations",
            "${summary.activeOrganizations} / ${summary.totalOrganizations}",
            "Legal entities"
        ),
        Triple(
            "Workspaces / Hubs",
            summary.totalWorkspaces.toString(),
            "Active enterprise hubs"
        ),
        Triple(
            "Total Branches",
            summary.totalBranches.toString(),
            "Retail & store locations"
        ),
        Triple(
            "Platform MRR",
            Money(summary.monthlyRecurringRevenueMinor).formatted,
            "Monthly Recurring Revenue"
        ),
        Triple("Total Users", summary.totalUsers.toString(), "Active staff accounts"),
        Triple("Connected Nodes", summary.totalDevices.toString(), "Trusted hardware")
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(kpis) { (title, value, subtitle) ->
            KpiCard(
                modifier = Modifier.width(220.dp).height(140.dp),
                title = title,
                value = value,
                subtitle = subtitle
            )
        }
    }
}

@Composable
private fun PlatformBillingSection(
    summary: AdminDashboardOverview,
    modifier: Modifier = Modifier
) {
    DashboardWidgetCard(modifier.heightIn(max = 400.dp).fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Financial Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BillingMetricCard(
                        modifier = Modifier.weight(1f).height(90.dp),
                        label = "Active Subs",
                        value = summary.activeSubscriptions.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    BillingMetricCard(
                        modifier = Modifier.weight(1f).height(90.dp),
                        label = "Trialing",
                        value = summary.trialSubscriptions.toString(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                BillingMetricCard(
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    label = "Total Revenue",
                    value = Money(summary.totalRevenueMinor).formatted,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun BillingMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentOnboardingsSection(
    onboardings: List<RecentOnboarding>,
    modifier: Modifier = Modifier
) {
    DashboardWidgetCard(modifier.fillMaxSize().heightIn(max = 400.dp)) {
        Column(Modifier.padding(16.dp).fillMaxHeight()) {
            Text(
                "Recent Onboardings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            if (onboardings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No new onboardings yet.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                AppListView(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(onboardings) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        AppIcons.Business,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.tenantName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${item.businessType} • ${item.onboardedAt.take(10)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(true)
                        }
                        HorizontalDivider(
                            Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemHealthSection(
    status: String,
    modifier: Modifier = Modifier
) {
    DashboardWidgetCard(modifier.fillMaxSize().heightIn(max = 400.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "System Operations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val color = when (status) {
                    "OPTIMAL" -> Color(0xFF4CAF50)
                    "DEGRADED" -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }

                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Public, null, tint = color, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    status,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    "Global Perimeter Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
            MetricRow("API Latency", "42ms", true)
            MetricRow("Database Load", "12%", true)
            MetricRow("Sync Success", "99.9%", true)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, isGood: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun StatusBadge(isActive: Boolean) {
    Surface(
        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
        contentColor = Color(0xFF2E7D32),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            "ACTIVE",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DashboardShimmer() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.large).shimmerEffect())
        Box(Modifier.fillMaxWidth().height(400.dp).clip(MaterialTheme.shapes.large).shimmerEffect())
    }
}

@Composable
private fun DashboardAnalyticsCarousel(
    widgets: List<PluginDashboardWidget>,
    modifier: Modifier = Modifier
) {
    if (widgets.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { widgets.size })
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val minWidth = windowSizeClass.minWidthDp

    val horizontalDp = if (minWidth < DASHBOARD_LARGE_SCREEN_WIDTH) 0.dp else 16.dp

    LaunchedEffect(widgets.size) {
        while (widgets.size > 1) {
            delay(8.seconds)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextPage, animationSpec = tween(1000))
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalApplyDashboardPadding provides false) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentPadding = PaddingValues(horizontal = horizontalDp),
                pageSpacing = 16.dp
            ) { page ->
                val pageOffset =
                    ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val scale = lerp(0.95f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(0.7f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        }
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    DashboardWidgetCard(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        widgets[page].content()
                    }
                }
            }
        }

        // Pager Indicators
        if (widgets.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}