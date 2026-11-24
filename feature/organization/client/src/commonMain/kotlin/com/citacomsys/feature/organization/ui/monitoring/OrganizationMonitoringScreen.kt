package com.citacomsys.feature.organization.ui.monitoring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationMonitoringScreen(
    viewModel: OrganizationMonitoringViewModel = koinViewModel()
) {
    val auditState by viewModel.auditFeed.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organization Monitoring") },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAuditFeed() }) {
                        Icon(AppIcons.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            UiResultGridView(
                state = auditState,
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(360.dp),
                contentPadding = PaddingValues(16.dp),
                emptyPlaceholder = {
                    Text("No recent security events recorded for the organization.")
                }
            ) { feed ->
                items(feed.size, key = { feed[it].requestId }) { index ->
                    AuditEntryCard(feed[index])
                }
            }
        }
    }
}

@Composable
private fun AuditEntryCard(entry: AuditTimelineEntry) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (entry.level == "CRITICAL") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (entry.level == "CRITICAL") AppIcons.Alert else AppIcons.Info,
                        contentDescription = null,
                        tint = if (entry.level == "CRITICAL") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(entry.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(entry.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Workspace: ${entry.workspaceId?.take(8) ?: "N/A"} • Operator: ${entry.userId ?: "System"} • ${entry.createdAt.toString().substringBefore('T')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
