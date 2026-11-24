package com.citacomsys.inventory.admin.security.siem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.feature.api.telemetry.ComponentFailureSummary
import com.citacomsys.feature.api.telemetry.SecurityThreatSummary
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiemDashboardScreen(
    onBack: () -> Unit,
    viewModel: SiemDashboardViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIEM Analytics Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAnalyticsData() }) {
                        Icon(AppIcons.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is SiemDashboardState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is SiemDashboardState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                is SiemDashboardState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ThreatSummarySection(s.threats)
                        FailureSummarySection(s.failures)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreatSummarySection(threats: List<SecurityThreatSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Top Security Threat Vectors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            if (threats.isEmpty()) {
                Text("No critical threats detected.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(threats) { threat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(AppIcons.Security, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(threat.clientIp, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Device: ${threat.deviceId ?: "Unknown"}", style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${threat.totalViolations} violations", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FailureSummarySection(failures: List<ComponentFailureSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Operational Health - Top Component Failures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            if (failures.isEmpty()) {
                Text("All systems operational.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(failures) { failure ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.Error, null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(12.dp))
                                Text(failure.componentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("${failure.totalFailures} errors", style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                failure.exampleErrorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                modifier = Modifier.padding(start = 32.dp)
                            )
                            HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
