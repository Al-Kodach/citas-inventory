package com.citacomsys.feature.organization.ui.hub.dashboard.impl

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.organization.nav.WorkspaceDetailsNav
import com.citacomsys.api.organization.nav.WorkspaceListNav
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.design.components.AppListView
import com.citacomsys.core.spi.DashboardPlacement
import org.koin.mp.KoinPlatform

@Composable
fun ActiveWorkspacesWidget() {
    val koin = KoinPlatform.getKoin()
    val store = koin.getOrNull<PlatformRuntimeStore>()
    val navigator = LocalNavigator.current
    val config by (store?.organizationConfig?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) })
    val workspaces = remember(config) { config?.workspaces ?: emptyList() }

    DashboardWidgetCard(placement = DashboardPlacement.LIST) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Widget Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stores & Businesses (${workspaces.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Registered operational business units",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = { navigator.navigateToSingleTop(WorkspaceListNav) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("View All", maxLines = 1)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Workspaces List
            if (workspaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No workspaces created yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AppListView(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(workspaces.size, key = { workspaces[it].id }) { index ->
                            val ws = workspaces[index]
                            val isActive = ws.status.equals("ACTIVE", ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                onClick = { navigator.navigateToSingleTop(WorkspaceDetailsNav(ws.id)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ws.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Type: ${ws.type.name} • Branches: ${ws.branches.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val cc = if (isActive) Color.Unspecified else Color.DarkGray

                                    SuggestionChip(
                                        onClick = { navigator.navigateToSingleTop(WorkspaceDetailsNav(ws.id)) },
                                        label = {
                                            Text(
                                                text = if (isActive) "ACTIVE" else "INACTIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else
                                                MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = cc,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareNodesWidget() {
    val koin = KoinPlatform.getKoin()
    val store = koin.getOrNull<PlatformRuntimeStore>()
    val config by (store?.organizationConfig?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) })
    val totalNodes = config?.metrics?.totalWorkstations ?: 0L

    DashboardWidgetCard(placement = DashboardPlacement.ANALYTICS_CAROUSEL) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hardware Nodes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bound POS & terminal units",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = AppIcons.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            Text(
                text = totalNodes.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
