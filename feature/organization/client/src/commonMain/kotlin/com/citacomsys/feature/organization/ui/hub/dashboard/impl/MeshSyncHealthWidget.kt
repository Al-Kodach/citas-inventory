package com.citacomsys.feature.organization.ui.hub.dashboard.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.citacomsys.core.components.DashboardWidgetCard
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.mesh.model.MeshPeerInfo
import com.citacomsys.core.mesh.model.SatelliteStatus
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.AppManifest
import com.citacomsys.core.spi.AppOperatingMode
import com.citacomsys.core.spi.DashboardPlacement
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.MeshDiagnosticsNav
import com.citacomsys.core.util.formatRelativeTime
import com.citacomsys.kernel.service.PlatformRuntimeStore
import com.citacomsys.types.NodeMeshMode
import com.citacomsys.types.ProductType
import com.design.AppTheme
import kotlin.time.Clock
import org.koin.compose.koinInject

@Composable
fun MeshSyncHealthWidget(
    modifier: Modifier = Modifier,
    manifest: AppManifest = koinInject(),
    meshManager: MeshManager = koinInject(),
    syncPrefs: SyncPreferencesRepository = koinInject()
) {
    val operatingMode by manifest.operatingMode.collectAsStateWithLifecycle()
    val isMotherHubEnabled by syncPrefs.isMotherHubEnabled.collectAsStateWithLifecycle()
    val isMotherHub = isMotherHubEnabled || operatingMode == AppOperatingMode.ORG

    val hubEngineState by meshManager.state.collectAsStateWithLifecycle()
    val activeHub by meshManager.activeHub.collectAsStateWithLifecycle()
    val activeSatellites by meshManager.activeSatellites.collectAsStateWithLifecycle()
    val cachedHubIp by syncPrefs.activeHubIp.collectAsStateWithLifecycle()
    val cachedHubPort by syncPrefs.activeHubPort.collectAsStateWithLifecycle()
    val meshSyncMode by syncPrefs.meshSyncMode.collectAsStateWithLifecycle()

    val navigator = LocalNavigator.current

    DashboardWidgetCard(
        modifier = modifier,
        placement = DashboardPlacement.LIST
    ) {
        if (isMotherHub) {
            MotherHubHealthView(
                hubEngineState = hubEngineState,
                activeSatellites = activeSatellites,
                onClick = {
                    navigator.navigateTo(MeshDiagnosticsNav)
                }
            )
        } else {
            SatelliteNodeHealthView(
                activeHub = activeHub,
                cachedHubIp = cachedHubIp,
                cachedHubPort = cachedHubPort,
                meshSyncMode = meshSyncMode,
                onClick = {
                    navigator.navigateTo(MeshDiagnosticsNav)
                }
            )
        }
    }
}

@Composable
private fun MotherHubHealthView(
    hubEngineState: EngineState,
    activeSatellites: Map<String, SatelliteStatus>,
    onClick: () -> Unit,
    meshManager: MeshManager = koinInject(),
    platformStore: PlatformRuntimeStore = koinInject(),
    sessionManager: AppSessionManager = koinInject()
) {
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    val isRunning = hubEngineState == EngineState.Running
    val onlineSatellitesCount = remember(activeSatellites) {
        activeSatellites.values.count { it.isOnline }
    }

    val orgId = platformStore.organizationId
    val activeWsId = sessionManager.activeWorkspaceId
        ?: platformStore.organizationConfig.value?.workspaces?.firstOrNull()?.id
    val isBound = !orgId.isNullOrBlank() && !activeWsId.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mother Hub Gateway",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "LAN listener & cross-branch relay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            SuggestionChip(
                onClick = onClick,
                label = {
                    Text(
                        text = "MOTHER HUB",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Engine Status Banner with Quick Action Switch
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isRunning) Color(0xFF2E7D32).copy(alpha = 0.12f)
            else if (!isBound) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isRunning) Color(0xFF2E7D32)
                                       else if (!isBound) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                    Column {
                        Text(
                            text = when {
                                isRunning -> "Mother Hub Active • LAN Listening"
                                !isBound -> "Unbound Workstation • Click to Setup"
                                else -> "Mother Hub Inactive • Engine Stopped"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isRunning -> Color(0xFF1B5E20)
                                !isBound -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = when {
                                isRunning -> "Listening for satellite syncs on local network"
                                !isBound -> "Workstation binding required for mesh gateway"
                                else -> "Toggle engine on to start Mother Hub server"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isRunning,
                    onCheckedChange = { _ ->
                        if (!isBound) {
                            navigator.navigateTo(MeshDiagnosticsNav)
                        } else {
                            scope.launch {
                                if (isRunning) {
                                    meshManager.stopMesh()
                                } else {
                                    meshManager.startMesh()
                                }
                            }
                        }
                    }
                )
            }
        }

        // Satellites Summary Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bound Satellites (${activeSatellites.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = "$onlineSatellitesCount Online • ${activeSatellites.size - onlineSatellitesCount} Offline",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Satellites Summary Items
        if (activeSatellites.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "No satellite terminals connected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                activeSatellites.values.forEach { sat ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (sat.isOnline) Color(0xFF2E7D32) else Color(
                                                0xFFD32F2F
                                            ),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = sat.peerInfo.workstationName.ifBlank { sat.peerInfo.bindingId },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "IP: ${sat.peerInfo.ipAddress} • User: ${sat.activeUser ?: "None"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (sat.isOnline) "Sync OK" else sat.lastSyncAt?.formatRelativeTime()
                                    ?: "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sat.isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SatelliteNodeHealthView(
    activeHub: MeshPeerInfo?,
    cachedHubIp: String?,
    cachedHubPort: Int,
    meshSyncMode: NodeMeshMode,
    onClick: () -> Unit = {}
) {
    val isConnected = activeHub != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Satellite Terminal Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Terminal relay & Mother Hub sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            SuggestionChip(
                onClick = onClick,
                label = {
                    Text(
                        text = "SATELLITE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Connection Status Banner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isConnected) Color(0xFF2E7D32).copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(
                alpha = 0.4f
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        )
                )
                Column {
                    Text(
                        text = if (isConnected) "Connected to Mother Hub" else "Offline • Mother Hub Unreachable",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = if (isConnected) "Real-time LAN telemetry & sync active" else "Operating offline. Requests queuing locally.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Known / Cached Hub Information
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Known Mother Hub Info",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hub Name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = activeHub?.workstationName
                                ?: if (cachedHubIp != null) "Cached Hub ($cachedHubIp)" else "None Discovered",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LAN Address",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val displayIp = activeHub?.ipAddress ?: cachedHubIp
                        val displayPort = activeHub?.port ?: cachedHubPort
                        val addressText = when {
                            displayIp != null && displayPort > 0 -> "$displayIp:$displayPort"
                            displayIp != null -> displayIp
                            else -> "Unassigned"
                        }
                        Text(
                            text = addressText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mesh Sync Mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = meshSyncMode.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@AllAdaptivePreviews
@Composable
private fun MeshSyncHealthWidgetMotherHubPreview() {
    val sampleSatellites = remember {
        mapOf(
            "sat_01" to SatelliteStatus(
                peerInfo = MeshPeerInfo(
                    hardwareId = "hw_accra_pos1",
                    bindingId = "binding_accra_01",
                    organizationId = "org_citacomsys",
                    workspaceId = "ws_accra",
                    appType = ProductType.RETAIL,
                    workstationName = "Accra POS Terminal 1",
                    ipAddress = "192.168.1.105",
                    port = 8080,
                    isMotherHub = false,
                    lastSeenAt = Clock.System.now()
                ),
                isOnline = true,
                lastSyncAt = Clock.System.now(),
                activeUser = "Kofi Mensah"
            ),
            "sat_02" to SatelliteStatus(
                peerInfo = MeshPeerInfo(
                    hardwareId = "hw_kumasi_pos2",
                    bindingId = "binding_kumasi_02",
                    organizationId = "org_citacomsys",
                    workspaceId = "ws_kumasi",
                    appType = ProductType.PHARMACY,
                    workstationName = "Kumasi Counter POS",
                    ipAddress = "192.168.1.112",
                    port = 8080,
                    isMotherHub = false,
                    lastSeenAt = Clock.System.now()
                ),
                isOnline = false,
                lastSyncAt = null,
                activeUser = null
            )
        )
    }

    AppTheme {
        DashboardWidgetCard(
            modifier = Modifier.padding(16.dp),
            placement = DashboardPlacement.LIST
        ) {
            MotherHubHealthView(
                hubEngineState = EngineState.Running,
                activeSatellites = sampleSatellites,
                onClick = {}
            )
        }
    }
}

@AllAdaptivePreviews
@Composable
private fun MeshSyncHealthWidgetSatellitePreview() {
    val sampleHub = remember {
        MeshPeerInfo(
            hardwareId = "hw_mother_hub_01",
            bindingId = "binding_hub_main",
            organizationId = "org_citacomsys",
            workspaceId = "ws_main",
            appType = ProductType.INVENTORY,
            workstationName = "HQ Mother Hub Server",
            ipAddress = "192.168.1.100",
            port = 8443,
            isMotherHub = true,
            lastSeenAt = Clock.System.now()
        )
    }

    AppTheme {
        DashboardWidgetCard(
            modifier = Modifier.padding(16.dp),
            placement = DashboardPlacement.LIST
        ) {
            SatelliteNodeHealthView(
                activeHub = sampleHub,
                cachedHubIp = "192.168.1.100",
                cachedHubPort = 8443,
                meshSyncMode = NodeMeshMode.NODE
            )
        }
    }
}

/**
 * Standalone Quick Action Widget to toggle the Mother Hub server engine.
 * If the workstation is NOT bound to an active workspace/org, clicking navigates the user to Mesh Setup/Diagnostics.
 */
@Composable
fun MeshHubQuickActionWidget(
    modifier: Modifier = Modifier,
    meshManager: MeshManager = koinInject(),
    platformStore: PlatformRuntimeStore = koinInject(),
    sessionManager: AppSessionManager = koinInject(),
    syncPrefs: SyncPreferencesRepository = koinInject()
) {
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    val hubEngineState by meshManager.state.collectAsStateWithLifecycle()
    val isMotherHubEnabled by syncPrefs.isMotherHubEnabled.collectAsStateWithLifecycle()
    val isRunning = hubEngineState == EngineState.Running && isMotherHubEnabled

    val orgId = platformStore.organizationId
    val activeWsId = sessionManager.activeWorkspaceId
        ?: platformStore.organizationConfig.value?.workspaces?.firstOrNull()?.id
    val isBound = !orgId.isNullOrBlank() && !activeWsId.isNullOrBlank()

    DashboardWidgetCard(
        modifier = modifier,
        placement = DashboardPlacement.GRID
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isRunning) Color(0xFF2E7D32).copy(alpha = 0.15f)
                            else if (!isBound) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRunning) AppIcons.Public else if (!isBound) AppIcons.Warning else AppIcons.Devices,
                            contentDescription = null,
                            tint = if (isRunning) Color(0xFF2E7D32)
                                   else if (!isBound) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "Mother Hub Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            !isBound -> "Unbound Workstation • Click to Setup"
                            isRunning -> "Active • Serving LAN Terminals"
                            else -> "Inactive • Ready to enable"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            !isBound -> MaterialTheme.colorScheme.error
                            isRunning -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Switch(
                checked = isRunning,
                onCheckedChange = { _ ->
                    if (!isBound) {
                        navigator.navigateTo(MeshDiagnosticsNav)
                    } else {
                        scope.launch {
                            if (isRunning) {
                                meshManager.stopMesh()
                            } else {
                                meshManager.startMesh()
                            }
                        }
                    }
                }
            )
        }
    }
}
