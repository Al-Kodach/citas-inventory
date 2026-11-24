package com.citacomsys.settings.ui.mesh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.MeshDiscoveryNav
import com.citacomsys.core.spi.MeshNodePairingCenterNav
import com.citacomsys.core.util.formatRelativeTime
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshDiagnosticsScreen(
    onBackClick: () -> Unit,
    viewModel: MeshDiagnosticsViewModel = koinViewModel()
) {
    val navigator = LocalNavigator.current

    val hubState by viewModel.hubState.collectAsStateWithLifecycle()
    val activeHub by viewModel.activeHub.collectAsStateWithLifecycle()
    val activeSatellites by viewModel.activeSatellites.collectAsStateWithLifecycle()
    val boundPeer by viewModel.boundPeer.collectAsStateWithLifecycle()
    val isHub by viewModel.isHub.collectAsStateWithLifecycle()

    val isEngineRunning = hubState == EngineState.Running
    val isSatelliteConnected = activeHub != null || boundPeer != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isHub) "Mesh Server Diagnostics" else "Mesh Node Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startDiscovery() }) {
                        Icon(AppIcons.Refresh, contentDescription = "Refresh LAN Scan")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Banner
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isEngineRunning || isSatelliteConnected -> Color(0xFF2E7D32).copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            color = when {
                                                isEngineRunning || isSatelliteConnected -> Color(
                                                    0xFF2E7D32
                                                )

                                                else -> MaterialTheme.colorScheme.outline
                                            },
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = when {
                                        isHub && isEngineRunning -> "Mesh Service Active • Broadcasting"
                                        isHub -> "Mesh Service Inactive • Stopped"
                                        isSatelliteConnected -> "Connected to Mesh Server"
                                        else -> "Mesh Node Ready"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isEngineRunning || isSatelliteConnected -> Color(0xFF1B5E20)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isHub) "MESH SERVER" else "MESH NODE",
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Text(
                            text = when {
                                isHub && isEngineRunning -> "Serving ${activeSatellites.size} connected terminals on local network"
                                isHub -> "Mesh service is stopped. Click 'Start Service' to listen for terminals."
                                isSatelliteConnected -> "Connected to Mesh Server at ${(boundPeer ?: activeHub)?.ipAddress}"
                                else -> "Select a role action below to configure mesh network."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ROLE 1: MESH SERVER ROLE VIEW
            if (isHub) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected Terminals (${activeSatellites.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { if (isEngineRunning) viewModel.stopHubEngine() else viewModel.startHubEngine() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEngineRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isEngineRunning) AppIcons.Close else AppIcons.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isEngineRunning) "Stop Service" else "Start Service")
                        }
                    }
                }

                if (activeSatellites.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No connected terminals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(
                        activeSatellites.values.toList(),
                        key = { it.peerInfo.hardwareId }) { sat ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                color = if (sat.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = sat.peerInfo.workstationName.ifBlank { sat.peerInfo.hardwareId },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "ID: ${sat.peerInfo.hardwareId.take(8)} • Last Sync: ${sat.lastSyncAt?.formatRelativeTime() ?: "Offline"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.pokeNode(sat.peerInfo.hardwareId) },
                                        contentPadding = PaddingValues(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    ) {
                                        Text("Poke")
                                    }
                                    IconButton(
                                        onClick = { viewModel.unregisterNode(sat.peerInfo.hardwareId) }
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Delete,
                                            contentDescription = "Remove Node",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (isEngineRunning.not()) {
                    // Role Navigation Affordance
                    item {
                        OutlinedButton(
                            onClick = { navigator.navigateToSingleTop(MeshDiscoveryNav) },
                        ) {
                            Icon(
                                AppIcons.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Switch to Node Discovery Mode")
                        }
                    }
                }


            } else {
                // ROLE 2: MESH NODE ROLE VIEW
                item {
                    Text(
                        text = "Connected Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    val hub = boundPeer ?: activeHub
                    if (hub != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hub.workstationName.ifBlank { "Mesh Server" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "IP: ${hub.ipAddress}:${hub.port} • ID: ${
                                            hub.hardwareId.take(
                                                8
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = { viewModel.rediscoverHub() },
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Text("Re-Sync")
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No Server connected. Click 'Find Servers' to scan local network.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { navigator.navigateToSingleTop(MeshDiscoveryNav) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                AppIcons.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Find Servers")
                        }

                        OutlinedButton(
                            onClick = { navigator.navigateToSingleTop(MeshNodePairingCenterNav) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                AppIcons.Devices,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Pairing Center")
                        }
                    }
                }
            }
        }
    }
}
