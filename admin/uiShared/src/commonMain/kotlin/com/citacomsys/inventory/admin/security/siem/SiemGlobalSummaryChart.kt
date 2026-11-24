package com.citacomsys.inventory.admin.security.siem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.feature.api.telemetry.ComponentFailureSummary
import com.citacomsys.feature.api.telemetry.SecurityThreatSummary
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.formatted
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LazyItemScope.SiemGlobalSummaryChart(
    viewModel: SiemDashboardViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        when (val current = state) {
            is SiemDashboardState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(
                    Alignment.Center
                ), color = Color.Cyan
            )

            is SiemDashboardState.Error -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Telemetry Error: ${current.message}",
                    color = Color.Red,
                    style = MaterialTheme.typography.headlineSmall
                )
                Button(
                    onClick = { viewModel.loadAnalyticsData() },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Retry Connecting") }
            }

            is SiemDashboardState.Success -> DashboardContentLayout(
                current.threats,
                current.failures
            )
        }
    }
}

@Composable
private fun LazyItemScope.DashboardContentLayout(
    threats: List<SecurityThreatSummary>,
    failures: List<ComponentFailureSummary>
) {
    // Splits automatically into two columns for cross-platform layout support
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Left Column: Threat Vector Metrics
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "PROACTIVE SECURITY RISK INTELLIGENCE",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
//            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                items(threats) { threat -> ThreatItemCard(threat) }
//            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                threats.forEach { threat -> ThreatItemCard(threat) }
            }
        }

        // Right Column: System Component Reliability
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "INFRASTRUCTURE CRASH & STABILITY METRICS",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
//            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                items(failures) { failure -> FailureItemCard(failure) }
//            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                failures.forEach { failure -> FailureItemCard(failure) }
            }
        }
    }
}

@Composable
private fun ThreatItemCard(threat: SecurityThreatSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                AppIcons.Warning,
                contentDescription = "Threat Alert",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "IP Target: ${threat.clientIp}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Device Scope: ${threat.deviceId ?: "Unknown Device Fingerprint"}",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "Client Engine Group: ${threat.appFlavor.uppercase()}",
                    color = Color.Cyan,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Badge(containerColor = Color(0xFF7F1D1D)) {
                Text(
                    "${threat.totalViolations} Blocks",
                    color = Color(0xFFFCA5A5),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun FailureItemCard(failure: ComponentFailureSummary) {
    Card(
        colors = CardDefaults.cardColors(Color(0xFF1E293B)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    failure.componentName,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.weight(1f))
                Badge(containerColor = Color(0xFF78350F)) {
                    Text(
                        "${failure.totalFailures} Crashes",
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = failure.exampleErrorMessage,
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Last Occurred: ${failure.lastOccurredAt.formatted()}",
                color = Color.DarkGray,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
