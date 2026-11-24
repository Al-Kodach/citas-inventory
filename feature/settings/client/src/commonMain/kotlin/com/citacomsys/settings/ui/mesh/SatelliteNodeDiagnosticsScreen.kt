package com.citacomsys.settings.ui.mesh

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.mesh.model.MeshConnectionState
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.theme.isDesktopPlatform
import com.design.components.AppListView
import com.design.components.AppScaffold
import io.github.oikvpqya.compose.fastscroller.ScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.ThumbStyle
import io.github.oikvpqya.compose.fastscroller.TrackStyle
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteNodeDiagnosticsScreen(
    activeHub: MeshPeerInfo?,
    discoveredPeers: List<MeshPeerInfo>,
    connectionState: MeshConnectionState,
    boundPeer: MeshPeerInfo?,
    onBackClick: () -> Unit,
    onStartDiscovery: () -> Unit,
    onBindPeer: (MeshPeerInfo) -> Unit,
    onStartEngine: () -> Unit,
    onTestConnection: () -> Unit,
    onPromoteToHub: () -> Unit
) {
    var showManualIpDialog by remember { mutableStateOf(false) }
    var manualIpText by remember { mutableStateOf("") }
    var manualPortText by remember { mutableStateOf("8443") }

    val currentBound = boundPeer ?: activeHub
    val state: LazyListState = rememberLazyListState()


    AppScaffold(
        title = "Mesh Node Connection & Setup",
        onBack = onBackClick,
        actions = {
            IconButton(onClick = { showManualIpDialog = true }) {
                Icon(AppIcons.Add, contentDescription = "Manual IP Connect")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {

            LazyColumn(
                state = state,
                modifier = Modifier
                    .widthIn(max = 1000.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stepper Header Card: 1. DISCOVER -> 2. BIND -> 3. START ENGINE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.4f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                itemVerticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Mesh Node Setup Process",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isDesktopPlatform) "JVM Background Sync Engine Active" else "Android Foreground Sync Service Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                val (statusLabel, statusBg, statusFg) = when (connectionState) {
                                    MeshConnectionState.ENGINE_RUNNING -> Triple(
                                        "ENGINE RUNNING",
                                        Color(0xFF22C55E).copy(alpha = 0.2f),
                                        Color(0xFF15803D)
                                    )

                                    MeshConnectionState.ENGINE_READY -> Triple(
                                        "ENGINE READY",
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.primary
                                    )

                                    MeshConnectionState.BOUND -> Triple(
                                        "NODE BOUND",
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    )

                                    MeshConnectionState.DISCOVERING -> Triple(
                                        "DISCOVERING...",
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    MeshConnectionState.DISCONNECTED -> Triple(
                                        "DISCONNECTED",
                                        MaterialTheme.colorScheme.errorContainer,
                                        MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = statusBg
                                ) {
                                    Text(
                                        text = statusLabel,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 3.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = statusFg
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.4f
                                )
                            )

                            // Visual 3-step Stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StepIndicatorItem(
                                    stepNumber = "1",
                                    label = "1. Discover",
                                    isCurrent = connectionState == MeshConnectionState.DISCOVERING,
                                    isCompleted = connectionState != MeshConnectionState.DISCOVERING && connectionState != MeshConnectionState.DISCONNECTED
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                                )
                                StepIndicatorItem(
                                    stepNumber = "2",
                                    label = "2. Bind",
                                    isCurrent = connectionState == MeshConnectionState.BOUND,
                                    isCompleted = connectionState == MeshConnectionState.ENGINE_READY || connectionState == MeshConnectionState.ENGINE_RUNNING
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                                )
                                StepIndicatorItem(
                                    stepNumber = "3",
                                    label = "3. Start Engine",
                                    isCurrent = connectionState == MeshConnectionState.ENGINE_READY,
                                    isCompleted = connectionState == MeshConnectionState.ENGINE_RUNNING
                                )
                            }
                        }
                    }
                }

                // STEP 2: BINDING INFORMATION CARD (Shown when a node/hub is bound or active)
                if (currentBound != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    itemVerticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Node Binding Information (Step 2)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = if (currentBound.isMotherHub) "MOTHER HUB" else "WORKSTATION NODE",
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 2.dp
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BindingRow("Target Workstation", currentBound.workstationName)
                                    BindingRow(
                                        "Network LAN Address",
                                        "${currentBound.ipAddress}:${currentBound.port}"
                                    )
                                    BindingRow("Hardware Identifier", currentBound.hardwareId)
                                    BindingRow(
                                        "Workspace Binding ID",
                                        currentBound.bindingId.ifBlank { "Bound to Organization" })
                                    BindingRow(
                                        "Verification Status",
                                        if (currentBound.orgVerificationToken != null) "Verified Fleet DNA Token" else "Standard Local Peer"
                                    )
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = 0.4f
                                    )
                                )

                                // STEP 3: START ENGINE BUTTON
                                if (connectionState != MeshConnectionState.ENGINE_RUNNING) {
                                    Button(
                                        onClick = onStartEngine,
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            AppIcons.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Start Sync Engine (Step 3)",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF22C55E).copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                AppIcons.Check,
                                                null,
                                                tint = Color(0xFF15803D),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Engine Running: Real-time telemetry & background sync stream active.",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 1: DISCOVER PEERS & WORKSTATIONS SECTION
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discovered LAN Workstations & Hubs (${discoveredPeers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(onClick = onStartDiscovery) {
                            Icon(
                                AppIcons.Refresh,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Scan LAN")
                        }
                    }
                }

                if (discoveredPeers.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No local workstations or hubs discovered on LAN yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = { showManualIpDialog = true }) {
                                        Text("Manual IP Connect")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        count = discoveredPeers.size,
                        key = { discoveredPeers[it].hardwareId }
                    ) { index ->
                        val peer = discoveredPeers[index]
                        val isBound = currentBound?.hardwareId == peer.hardwareId

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isBound) BorderStroke(
                                2.dp,
                                MaterialTheme.colorScheme.primary
                            ) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            peer.workstationName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = if (peer.isMotherHub) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                if (peer.isMotherHub) "MOTHER HUB" else "WORKSTATION",
                                                modifier = Modifier.padding(
                                                    horizontal = 8.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (peer.isMotherHub) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "IP Address: ${peer.ipAddress}:${peer.port}  •  Hardware ID: ${
                                            peer.hardwareId.take(
                                                8
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { onBindPeer(peer) },
                                    enabled = !isBound,
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Text(if (isBound) "Bound" else "Select & Bind")
                                }
                            }
                        }
                    }
                }

                // Promote Workstation Option
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.3f
                            )
                        )
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Promote this Workstation to Mother Hub",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enable local server engine to act as LAN gateway for other workstations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            OutlinedButton(onClick = onPromoteToHub) {
                                Text("Promote to Hub")
                            }
                        }
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight()
                    .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                adapter = rememberScrollbarAdapter(scrollState = state),
                style = ScrollbarStyle(
                    minimalHeight = 52.dp,
                    thickness = 4.dp,
                    hoverDurationMillis = 300,
                    thumbStyle = ThumbStyle(
                        shape = RoundedCornerShape(2.dp),
                        unhoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    ),
                    trackStyle = TrackStyle(
                        shape = RoundedCornerShape(2.dp),
                        unhoverColor = Color.Transparent,
                        hoverColor = Color.Transparent,
                    ),
                ),
                enablePressToScroll = true,
            )
        }
    }

    if (showManualIpDialog) {
        AlertDialog(
            onDismissRequest = { showManualIpDialog = false },
            title = { Text("Manual Workstation / Hub Connection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter LAN IP address and port of target workstation or Mother Hub.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = manualIpText,
                        onValueChange = { manualIpText = it },
                        label = { Text("LAN IP Address") },
                        placeholder = { Text("192.168.1.100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualPortText,
                        onValueChange = { manualPortText = it },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val port = manualPortText.toIntOrNull() ?: 8443
                        if (manualIpText.isNotBlank()) {
                            val manualPeer = MeshPeerInfo(
                                hardwareId = "manual-ip-${manualIpText.trim()}",
                                bindingId = "manual-ip-${manualIpText.trim()}",
                                organizationId = "",
                                workspaceId = null,
                                appType = null,
                                workstationName = "Manual Peer (${manualIpText.trim()})",
                                ipAddress = manualIpText.trim(),
                                port = port,
                                isMotherHub = true,
                                orgVerificationToken = null,
                                tlsFingerprint = null,
                                lastSeenAt = Clock.System.now()
                            )
                            onBindPeer(manualPeer)
                            showManualIpDialog = false
                        }
                    }
                ) {
                    Text("Bind Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualIpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BindingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepIndicatorItem(
    stepNumber: String,
    label: String,
    isCurrent: Boolean,
    isCompleted: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = when {
                isCompleted -> Color(0xFF22C55E)
                isCurrent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(AppIcons.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                } else {
                    Text(
                        stepNumber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent || isCompleted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}
