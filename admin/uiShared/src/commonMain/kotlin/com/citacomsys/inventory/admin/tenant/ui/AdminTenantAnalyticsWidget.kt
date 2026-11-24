package com.citacomsys.inventory.admin.tenant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.inventory.admin.app.dashboard.presentation.AdminDashboardViewModel
import com.design.components.AppDonutChart
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminTenantAnalyticsWidget(
    viewModel: AdminDashboardViewModel = koinViewModel()
) {
    val summaryResult by viewModel.summary.collectAsStateWithLifecycle()

    DashboardWidgetCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tenant Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active vs Trial distribution",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

            UiResultView(
                state = summaryResult,
                onRetry = viewModel::loadSummary,
                modifier = Modifier.fillMaxWidth()
            ) { overview ->
                val active = overview.activeTenants.toFloat()
                val total = overview.totalTenants.toFloat().coerceAtLeast(1f)
                val activeRatio = (active / total).coerceIn(0f, 1f)
                val trialRatio = (1f - activeRatio).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${overview.activeTenants} / ${overview.totalTenants} Active Tenants",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${overview.activeSubscriptions} Subscriptions (${overview.trialSubscriptions} Trials)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppDonutChart(
                            values = listOf(activeRatio, trialRatio),
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.surfaceVariant
                            ),
                            label = "${(activeRatio * 100).toInt()}%"
                        )
                    }
                }
            }
        }
    }
}
