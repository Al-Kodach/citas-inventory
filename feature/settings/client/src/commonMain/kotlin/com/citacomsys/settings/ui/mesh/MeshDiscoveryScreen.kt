package com.citacomsys.settings.ui.mesh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.citacomsys.core.mesh.model.MeshConnectionState
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.resources.AppIcons
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshDiscoveryScreen(
    onBackClick: () -> Unit,
    onNavigateToPairingCenter: () -> Unit = {},
    viewModel: MeshDiscoveryViewModel = koinViewModel()
) {
    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val boundPeer by viewModel.boundPeer.collectAsStateWithLifecycle()
    val enabled = connectionState != MeshConnectionState.BOUND || connectionState != MeshConnectionState.DISCOVERING

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service Discovery") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (connectionState) {
                    MeshConnectionState.BOUND -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                    MeshConnectionState.DISCOVERING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = when (connectionState) {
                                    MeshConnectionState.BOUND -> Color(0xFF2E7D32)
                                    MeshConnectionState.DISCOVERING -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                shape = CircleShape
                            )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (connectionState) {
                                MeshConnectionState.BOUND -> "Connected & Bound to Server"
                                MeshConnectionState.DISCOVERING -> "Scanning local network for Servers..."
                                else -> "Service Ready"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = boundPeer?.let { "Bound to ${it.workstationName} (${it.ipAddress})" }
                                ?: "Click 'Scan LAN' to discover available mesh servers on local network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discovered Servers (${discoveredPeers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { viewModel.startDiscovery() },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    enabled = enabled
                ) {
                    Icon(AppIcons.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (enabled) "Scan LAN" else "Scanning...")
                }
            }

            if (discoveredPeers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No servers discovered on LAN",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Click 'Scan LAN' above to search for servers on the local network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(discoveredPeers, key = { it.hardwareId }) { peer ->
                        val isCurrentBound = boundPeer?.hardwareId == peer.hardwareId
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentBound) Color(0xFF2E7D32).copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = peer.workstationName.ifBlank { "Mesh Server" },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "VERIFIED",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Address: ${peer.ipAddress}:${peer.port} • ID: ${peer.hardwareId.take(8)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = { viewModel.connectAndJoinHub(peer) },
                                    enabled = !isCurrentBound,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCurrentBound) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(if (isCurrentBound) "Connected" else "Connect & Join")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
