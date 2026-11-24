package com.citacomsys.feature.organization.ui.hub

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.spi.NavigationCoordinator
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.core.ui.result.isLoading
import com.design.components.AppScaffold
import com.design.components.AppVerticalGridView
import com.design.components.ScrollableRowWithButtons
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.types.ProductType
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationDashboardScreen(
    onNavigateToProfile: () -> Unit,
    viewModel: OrganizationHubViewModel = koinViewModel(),
    coordinator: NavigationCoordinator = koinInject(),
    sessionManager: AppSessionManager = koinInject(),
) {
    val configResult by viewModel.config.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
    val dashboardWidgets by coordinator.activeDashboardWidgets.collectAsStateWithLifecycle()

    val workStationInfo by sessionManager.workStationInfo.collectAsStateWithLifecycle()

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

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val minWidth = windowSizeClass.minWidthDp

    val gridColumns = remember(minWidth) {
        when {
            minWidth >= 1600 -> GridCells.Fixed(3)
            minWidth >= 840 -> GridCells.Fixed(2)
            else -> GridCells.Fixed(1)
        }
    }

    UiResultView(
        state = configResult,
        modifier = Modifier.fillMaxSize()
    ) { _ ->
        AppVerticalGridView(
            modifier = Modifier.fillMaxSize(),
            columns = gridColumns,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            // Grid & List Feature Overview Cards (Displayed Directly on Screen Grid)
            gridWidgets.forEach { widget ->
                item(key = widget.id) {
                    widget.content()
                }
            }

            // 3. System Activity & Audit Stream Section
            if (activityWidgets.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "System Activity & Audit Stream",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                activityWidgets.forEach { widget ->
                    item(key = widget.id, span = { GridItemSpan(maxLineSpan) }) {
                        widget.content()
                    }
                }
            }
        }
    }
}
