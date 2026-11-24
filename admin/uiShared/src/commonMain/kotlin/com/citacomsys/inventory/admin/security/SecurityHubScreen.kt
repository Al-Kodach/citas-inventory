package com.citacomsys.inventory.admin.security

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.inventory.admin.security.components.LiveActivityStreamWidget
import com.citacomsys.inventory.admin.security.components.SocToolCard

@Composable
fun SecurityHubScreen(
    onNavigateToDevices: () -> Unit,
    onNavigateToAuditLogs: () -> Unit,
    onNavigateToSiemDashboard: () -> Unit
) {
    val isCompact = currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp <= 600

    LazyListView(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        item {
            Text("Security Operations Center", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(32.dp))
        }

        item {
            // SOC Tools Grid - Responsive Layout
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                maxItemsInEachRow = if (isCompact) 1 else 3
            ) {
                val cardModifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.weight(1f)
                
                SocToolCard(
                    title = "Device Registry",
                    description = "Monitor and manage trusted endpoints",
                    icon = AppIcons.Devices,
                    onClick = onNavigateToDevices,
                    modifier = cardModifier
                )
                SocToolCard(
                    title = "Audit Logs",
                    description = "Global system activity stream",
                    icon = AppIcons.History,
                    onClick = onNavigateToAuditLogs,
                    modifier = cardModifier
                )
                SocToolCard(
                    title = "SIEM Insights",
                    description = "Threat analytics and health trends",
                    icon = AppIcons.Analytics,
                    onClick = onNavigateToSiemDashboard,
                    modifier = cardModifier
                )
            }
            Spacer(Modifier.height(40.dp))
        }

        item {
            LiveActivityStreamWidget()
        }
    }
}

