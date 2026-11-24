package com.citacomsys.feature.organization.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.api.organization.model.OrganizationReportSummary
import com.citacomsys.core.resources.AppIcons
import com.design.components.AppScaffold

@Composable
fun OrganizationReportsScreen(
    summary: OrganizationReportSummary?,
    onBackClick: () -> Unit
) {
    AppScaffold(
        title = "Business Reports",
        onBack = onBackClick
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Consolidated Commercial Intelligence",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(280.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    ReportMetricCard(
                        title = "Active Workspaces",
                        value = "${summary?.totalWorkspacesCount ?: 0}",
                        subtitle = "Provisioned Branch Locations",
                        icon = AppIcons.Business
                    )
                }
                item {
                    ReportMetricCard(
                        title = "Active Team Members",
                        value = "${summary?.activeUsersCount ?: 0}",
                        subtitle = "Registered Operators & Admins",
                        icon = AppIcons.People
                    )
                }
                item {
                    ReportMetricCard(
                        title = "Total Gross Revenue",
                        value = summary?.totalRevenue?.formatted ?: "GHS 0.00",
                        subtitle = "Combined Multi-Branch Revenue",
                        icon = AppIcons.Payments
                    )
                }
                item {
                    ReportMetricCard(
                        title = "Inventory Stock Valuation",
                        value = summary?.stockValuation?.formatted ?: "GHS 0.00",
                        subtitle = "Total Stock Asset Value",
                        icon = AppIcons.Inventory2
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
