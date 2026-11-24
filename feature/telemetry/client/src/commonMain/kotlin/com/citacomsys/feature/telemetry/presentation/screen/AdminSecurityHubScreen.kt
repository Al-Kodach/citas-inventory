package com.citacomsys.feature.telemetry.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
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
import com.citacomsys.core.spi.TelemetryNav
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.feature.api.telemetry.dto.AuditTimelineEntry
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSecurityHubScreen(
    viewModel: WorkspaceSecurityHubViewModel = koinViewModel()
) {
    val auditState by viewModel.auditFeed.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cloud Audit Feed", "Security Actions")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Hub") },
                navigationIcon = {
                    IconButton(onClick = { navigator.goBack() }) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAuditFeed() }) {
                        Icon(AppIcons.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { navigator.navigateToSingleTop(TelemetryNav) }) {
                        Icon(AppIcons.Smartphone, contentDescription = "Local Logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> CloudAuditFeed(auditState)
                    1 -> SecurityActionsContent()
                }
            }
        }
    }
}

@Composable
private fun CloudAuditFeed(state: com.citacomsys.core.ui.result.UiResult<List<AuditTimelineEntry>>) {
    UiResultGridView(
        state,
        columns = GridCells.Adaptive(360.dp),
        emptyPlaceholder = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recent security events recorded in the cloud.")
            }
        }
    ) { feed ->
        items(feed) { entry ->
            AuditEntryCard(entry)
        }
    }
}

private fun auditFeedIsEmpty(feed: List<AuditTimelineEntry>?): Boolean = feed.isNullOrEmpty()

@Composable
private fun AuditEntryCard(entry: AuditTimelineEntry) {
    AppCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
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
                    "Operator: ${entry.userId ?: "System"} • ${entry.createdAt.toString().substringBefore('T')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun SecurityActionsContent() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Active Security Policies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        AppCard(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Multi-Factor Authentication") },
                supportingContent = { Text("Require SMS verification for all administrative logins.") },
                leadingContent = { Icon(AppIcons.Shield, null) },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )
        }
        
        AppCard(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Session Expiry") },
                supportingContent = { Text("Automatically log out users after 30 minutes of inactivity.") },
                leadingContent = { Icon(AppIcons.Timer, null) },
                trailingContent = { Switch(checked = true, onCheckedChange = {}) }
            )
        }
    }
}
