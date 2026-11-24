package com.citacomsys.settings.ui.mesh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.mesh.model.SatelliteStatus
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.resources.AppIcons
import com.design.components.AppListView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MotherHubMonitorScreen(
    hubState: EngineState,
    satellites: Map<String, SatelliteStatus>,
    onBackClick: () -> Unit,
    onPokeNode: (String) -> Unit,
    onStopHub: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mother Hub Management & Node Health") },
                navigationIcon = { BackButton(onBackClick) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            AppListView(
                modifier = Modifier
                    .widthIn(max = 1000.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Server Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Local Gateway Server Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = CircleShape,
                                    color = if (hubState == EngineState.Running) Color(0xFF22C55E).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = if (hubState == EngineState.Running) "RUNNING" else "STOPPED",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hubState == EngineState.Running) Color(0xFF15803D) else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Text("Active Connected Satellites: ${satellites.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (hubState == EngineState.Running) {
                                OutlinedButton(
                                    onClick = onStopHub,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Stop Mother Hub Server")
                                }
                            }
                        }
                    }
                }

                // Bound Satellites Header
                item {
                    Text(
                        "Bound Satellite Terminals (${satellites.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (satellites.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No satellite terminals connected yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(
                        count = satellites.size,
                        key = { satellites.keys.elementAt(it) }
                    ) { index ->
                        val sat = satellites.values.elementAt(index)
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(sat.peerInfo.workstationName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("Binding ID: ${sat.peerInfo.bindingId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = if (sat.isOnline) Color(0xFF22C55E).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (sat.isOnline) "ONLINE" else "OFFLINE",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sat.isOnline) Color(0xFF15803D) else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Text(
                                    "Last Activity: ${sat.lastSyncAt?.toString() ?: "None recorded"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onPokeNode(sat.peerInfo.bindingId) }
                                    ) {
                                        Icon(AppIcons.Refresh, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Poke Node for Sync", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
