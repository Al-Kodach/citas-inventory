package com.citacomsys.settings.ui.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.domain.model.SyncConfig
import com.citacomsys.core.domain.model.SyncTarget
import com.citacomsys.core.domain.model.SyncTimingStrategy
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.kernel.model.BranchSummary
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.ProductType
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.types.PackageTier
import com.design.AppTheme
import com.design.components.AppScaffold
import io.github.oikvpqya.compose.fastscroller.ScrollbarStyle
import io.github.oikvpqya.compose.fastscroller.ThumbStyle
import io.github.oikvpqya.compose.fastscroller.TrackStyle
import io.github.oikvpqya.compose.fastscroller.VerticalScrollbar
import io.github.oikvpqya.compose.fastscroller.rememberScrollbarAdapter
import org.koin.compose.viewmodel.koinViewModel

private enum class IntervalUnit {
    MINUTES, HOURS
}

@Composable
fun SyncPolicyScreen(
    onBackClick: () -> Unit,
    viewModel: SyncPolicyViewModel = koinViewModel()
) {
    val syncConfig by viewModel.syncConfig.collectAsStateWithLifecycle()
    val isMotherHubEnabled by viewModel.isMotherHubEnabled.collectAsStateWithLifecycle()
    val organizationWorkspaces by viewModel.organizationWorkspaces.collectAsStateWithLifecycle()
    val activeWsName = viewModel.activeWorkspaceName
    val isSubscribed = viewModel.isSubscribed

    SyncPolicyContent(
        syncConfig = syncConfig,
        isMotherHubEnabled = isMotherHubEnabled,
        organizationWorkspaces = organizationWorkspaces,
        activeWorkspaceName = activeWsName,
        isSubscribed = isSubscribed,
        onBackClick = onBackClick,
        onToggleSyncTarget = viewModel::toggleSyncTarget,
        onSetSyncTimingStrategy = viewModel::setSyncTimingStrategy,
        onSetSyncIntervalMinutes = viewModel::setSyncIntervalMinutes,
        onSetAllowCellularSync = viewModel::setAllowCellularSync,
        onSetRequiresCharging = viewModel::setRequiresCharging,
        onSetRequiresDeviceIdle = viewModel::setRequiresDeviceIdle
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SyncPolicyContent(
    syncConfig: SyncConfig,
    isMotherHubEnabled: Boolean = false,
    organizationWorkspaces: List<WorkspaceSummary>,
    activeWorkspaceName: String?,
    isSubscribed: Boolean,
    onBackClick: () -> Unit,
    onToggleSyncTarget: (workspaceId: String, branchId: String, isSelected: Boolean) -> Unit,
    onSetSyncTimingStrategy: (SyncTimingStrategy) -> Unit,
    onSetSyncIntervalMinutes: (Int) -> Unit = {},
    onSetAllowCellularSync: (Boolean) -> Unit = {},
    onSetRequiresCharging: (Boolean) -> Unit = {},
    onSetRequiresDeviceIdle: (Boolean) -> Unit = {}
) {
    val totalBranchesCount = remember(organizationWorkspaces) {
        organizationWorkspaces.sumOf { it.branches.size }
    }
    val isSingleStorePlan = totalBranchesCount <= 1
    val state: LazyListState = rememberLazyListState()

    AppScaffold(
        title = "Sync & Backup Settings",
        onBack = onBackClick,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Info Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.5f
                            )
                        )
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Cloud Sync & Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isSubscribed)
                                    "Manage how frequently your store data syncs with the cloud and set data usage options."
                                else
                                    "Cloud sync is paused (Offline Mode). Data is saved safely on your local device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Store Locations / Query Targets (Starter vs Multi-Branch)
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSingleStorePlan) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "Store Location",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Single store location — automatically configured for sync.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                activeWorkspaceName ?: "Main Store",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Active store location",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                "ACTIVE",
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
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        "Workspace Stores & Sync Targets",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Select store locations to query and synchronize with this device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                organizationWorkspaces.forEach { ws ->
                                    WorkspaceConfig(
                                        ws = ws,
                                        syncConfig = syncConfig,
                                        isMotherHubEnabled = isMotherHubEnabled,
                                        onToggleSyncTarget = onToggleSyncTarget
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Sync Frequency Configuration
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Sync Frequency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Choose how frequently data is backed up and synchronized.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            SyncTimingStrategy.entries.forEach { strategy ->
                                val isSelected = syncConfig.syncTimingStrategy == strategy
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    },
                                    border = if (isSelected) {
                                        BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        )
                                    } else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSetSyncTimingStrategy(strategy) }
                                ) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onSetSyncTimingStrategy(strategy) }
                                            )
                                            Column {
                                                FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
                                                    if (strategy == SyncTimingStrategy.Auto) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.secondaryContainer
                                                        ) {
                                                            Text(
                                                                "Recommended for Satellite/Pos",
                                                                modifier = Modifier
                                                                    .padding(
                                                                        horizontal = 6.dp,
                                                                        vertical = 2.dp
                                                                    )
                                                                    .horizontalScroll(
                                                                        rememberScrollState()
                                                                    ),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = when (strategy) {
                                                            SyncTimingStrategy.Auto -> "Automatic"
                                                            SyncTimingStrategy.NIGHT -> "Nightly Backup"
                                                            SyncTimingStrategy.TIMEBASE -> "Scheduled Interval"
                                                        },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                                Text(
                                                    text = when (strategy) {
                                                        SyncTimingStrategy.Auto -> "Syncs changes immediately after sales/transactions and when connected or idle"
                                                        SyncTimingStrategy.NIGHT -> "Syncs once a day during off-peak night hours (~2 AM)"
                                                        SyncTimingStrategy.TIMEBASE -> "Syncs automatically at set time intervals"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Expandable Interval Configurator when Scheduled Interval is selected
                                        if (strategy == SyncTimingStrategy.TIMEBASE && isSelected) {
                                            Spacer(Modifier.height(8.dp))
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.5f
                                                )
                                            )
                                            Spacer(Modifier.height(8.dp))

                                            TimebaseIntervalConfigurator(
                                                currentMinutes = syncConfig.syncIntervalMinutes,
                                                onSetSyncIntervalMinutes = onSetSyncIntervalMinutes
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Data Usage & Battery Options
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Data Usage & Battery Options",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Set network and power conditions for background sync.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Sync on Wi-Fi Only Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Sync on Wi-Fi Only",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (!syncConfig.allowCellularSync) "Syncs on Wi-Fi or unmetered connections only" else "Syncs over both mobile data and Wi-Fi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = !syncConfig.allowCellularSync,
                                    onCheckedChange = { wifiOnly -> onSetAllowCellularSync(!wifiOnly) }
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                )
                            )

                            // Require Charging Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Sync only while charging",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Runs background sync only when plugged into power",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = syncConfig.requiresCharging,
                                    onCheckedChange = onSetRequiresCharging
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                )
                            )

                            // Require Device Idle Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Sync when device is idle",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Runs sync in the background when the phone/device is not actively being used",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = syncConfig.requiresDeviceIdle,
                                    onCheckedChange = onSetRequiresDeviceIdle
                                )
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceConfig(
    ws: WorkspaceSummary,
    syncConfig: SyncConfig,
    isMotherHubEnabled: Boolean,
    onToggleSyncTarget: (workspaceId: String, branchId: String, isSelected: Boolean) -> Unit
) {
    val isCloudDeployed = ws.isCloudDeployed

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isCloudDeployed) AppIcons.CloudDone else AppIcons.CloudOff,
                contentDescription = if (isCloudDeployed) "Cloud Deployed" else "Offline Local Only",
                tint = if (isCloudDeployed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = ws.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                shape = CircleShape,
                color = if (isCloudDeployed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (isCloudDeployed) "CLOUD" else "LOCAL",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCloudDeployed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ws.branches.forEach { br ->
                val isBrSelected = syncConfig.targets.any {
                    it.workspaceId == ws.id && it.branchId == br.id
                }

                FilterChip(
                    selected = isBrSelected,
                    onClick = { onToggleSyncTarget(ws.id, br.id, !isBrSelected) },
                    label = {
                        Text(
                            text = "${br.name} (${br.locationCode})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isBrSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        if (isBrSelected) {
                            Icon(
                                imageVector = AppIcons.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (isCloudDeployed) AppIcons.Cloud else AppIcons.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimebaseIntervalConfigurator(
    currentMinutes: Int,
    onSetSyncIntervalMinutes: (Int) -> Unit
) {
    var selectedUnit by remember(currentMinutes) {
        mutableStateOf(
            if (currentMinutes >= 60 && currentMinutes % 60 == 0) IntervalUnit.HOURS else IntervalUnit.MINUTES
        )
    }

    val displayValue = remember(currentMinutes, selectedUnit) {
        if (selectedUnit == IntervalUnit.HOURS) {
            (currentMinutes / 60).coerceAtLeast(1).toString()
        } else {
            currentMinutes.toString()
        }
    }

    var textInput by remember(displayValue) { mutableStateOf(displayValue) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Configure Sync Interval",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Preset Options
        Text(
            "Quick Presets",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val presetOptions = listOf(
            "15m" to 15,
            "30m" to 30,
            "45m" to 45,
            "1h" to 60,
            "2h" to 120,
            "4h" to 240,
            "6h" to 360,
            "12h" to 720,
            "24h" to 1440
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetOptions.forEach { (label, minutes) ->
                val isSelectedPreset = currentMinutes == minutes
                FilterChip(
                    selected = isSelectedPreset,
                    onClick = {
                        onSetSyncIntervalMinutes(minutes)
                    },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    shape = MaterialTheme.shapes.small
                )
            }
        }

        // Custom Value & Unit Picker Row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    textInput = filtered
                    val num = filtered.toIntOrNull()
                    if (num != null && num > 0) {
                        onSetSyncIntervalMinutes(num * if (selectedUnit == IntervalUnit.HOURS) 60 else 1)
                    }
                },
                label = { Text("Custom Interval Value") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedUnit == IntervalUnit.MINUTES,
                    onClick = {
                        selectedUnit = IntervalUnit.MINUTES
                        val num = textInput.toIntOrNull() ?: 15
                        onSetSyncIntervalMinutes(num)
                    },
                    label = { Text("Minutes") }
                )
                FilterChip(
                    selected = selectedUnit == IntervalUnit.HOURS,
                    onClick = {
                        selectedUnit = IntervalUnit.HOURS
                        val num = textInput.toIntOrNull() ?: 1
                        onSetSyncIntervalMinutes(num * 60)
                    },
                    label = { Text("Hours") }
                )
            }
        }

        val summaryText = if (currentMinutes >= 60 && currentMinutes % 60 == 0) {
            val hrs = currentMinutes / 60
            "Effective Schedule: Automatically syncs every $hrs ${if (hrs == 1) "hour" else "hours"} ($currentMinutes mins)"
        } else {
            "Effective Schedule: Automatically syncs every $currentMinutes minutes"
        }

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}


@AllAdaptivePreviews
@Composable
fun SyncPolicyScreenPreview() {
    val sampleWorkspaces = listOf(
        WorkspaceSummary(
            id = "ws-1",
            name = "Accra Central Retail",
            type = ProductType.RETAIL,
            deploymentMode = DeploymentMode.CLOUD_SYNCED,
            status = "ACTIVE",
            packageTier = PackageTier.ENTERPRISE,
            branches = listOf(
                BranchSummary(
                    id = "br-1",
                    name = "Main Branch",
                    locationCode = "ACC-01",
                    isActive = true
                ),
                BranchSummary(
                    id = "br-2",
                    name = "Osu Branch",
                    locationCode = "ACC-02",
                    isActive = true
                )
            )
        ),
        WorkspaceSummary(
            id = "ws-2",
            name = "Kumasi Pharmacy",
            type = ProductType.PHARMACY,
            deploymentMode = DeploymentMode.OFFLINE_LOCAL,
            packageTier = PackageTier.STARTER,
            status = "ACTIVE",
            branches = listOf(
                BranchSummary(
                    id = "br-3",
                    name = "Adum Store",
                    locationCode = "KSI-01",
                    isActive = true
                )
            )
        )
    )

    val sampleSyncConfig = SyncConfig(
        isMotherHubEnabled = true,
        syncTimingStrategy = SyncTimingStrategy.TIMEBASE,
        syncIntervalMinutes = 120,
        targets = listOf(
            SyncTarget(workspaceId = "ws-1", branchId = "br-1"),
            SyncTarget(workspaceId = "ws-1", branchId = "br-2")
        )
    )

    AppTheme {
        SyncPolicyContent(
            syncConfig = sampleSyncConfig,
            organizationWorkspaces = sampleWorkspaces,
            activeWorkspaceName = "Accra Central Retail",
            isSubscribed = true,
            onBackClick = {},
            onToggleSyncTarget = { _, _, _ -> },
            onSetSyncTimingStrategy = {},
            onSetSyncIntervalMinutes = {}
        )
    }
}
