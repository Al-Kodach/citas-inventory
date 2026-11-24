package com.citacomsys.app.shared.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.app.shared.screen.AppViewModel
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.mesh.service.MeshManager
import com.citacomsys.core.notification.PlatformNotificationHelper
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.sync.engine.EntitySyncEngine
import com.citacomsys.types.ProductType
import com.design.Dimensions
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppStatusBar(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = koinViewModel(),
    systemControls: @Composable RowScope.() -> Unit = {},
) {
    val workStationInfo by viewModel.workStationInfo.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val fontScale by viewModel.userFontScale.collectAsStateWithLifecycle()

    val koin = getKoin()
    val meshManager = remember { koin.getOrNull<MeshManager>() }
    val syncPrefs = remember { koin.getOrNull<SyncPreferencesRepository>() }
    val meshBadgeText by remember(meshManager, syncPrefs) {
        if (meshManager != null && syncPrefs != null) {
            combine(
                syncPrefs.isMotherHubEnabled,
                meshManager.state,
                meshManager.activeHub,
                meshManager.boundPeer,
                meshManager.activeSatellites
            ) { isHubEnabled, engineState, activeHub, boundPeer, activeSatellites ->
                val isHubRunning = engineState == EngineState.Running
                val isHub = isHubEnabled && isHubRunning
                val isSatelliteConnected = activeHub != null || boundPeer != null

                when {
                    isHub -> {
                        val onlineCount = activeSatellites.values.count { it.isOnline }
                        "Hub: $onlineCount Nodes"
                    }
                    isSatelliteConnected -> {
                        "Mesh: Connected"
                    }
                    else -> {
                        null
                    }
                }
            }
        } else {
            flowOf(null)
        }
    }.collectAsStateWithLifecycle(null)

    val hubIconSize = Dimensions.iconSize(fontScale)
    val barVerticalPadding = if (fontScale > 1.2f) 4.dp else 8.dp
    val notificationCount by PlatformNotificationHelper.notificationCount.collectAsStateWithLifecycle()
    val hasCritical by PlatformNotificationHelper.hasCritical.collectAsStateWithLifecycle()
    val notifications = remember(notificationCount) { PlatformNotificationHelper.getNotifications() }
    var showNotificationDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = barVerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: Branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    ProjectLogos.AppLogo(
                        isWsMode = workStationInfo.isOrgMode.not(),
                        productType = ProductType.INVENTORY,
                        size = 32.dp,
                        showTitle = false
                    )

                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = workStationInfo.orgName.ifBlank { "Citas" },
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = (18f + ((fontScale - 1f) * 2f)).sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(8.dp))

                            val syncEngine: EntitySyncEngine = koinInject()
                            val isSyncing by syncEngine.isSyncing.collectAsStateWithLifecycle()
                            if (isSyncing) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            text = "SYNCING",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }

                            // Mesh Status Badge (Exposed via AppViewModel)
                            if (meshBadgeText != null) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = meshBadgeText!!,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Right: Global Status & Controls (Adaptive: Dropdown menu on mobile/compact screens)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.wrapContentWidth(),
                ) {
                    // Wide Screen (Desktop/Tablet): Inline buttons
                    if (notificationCount > 0) {
                        IconButton(
                            onClick = { showNotificationDialog = true },
                            modifier = Modifier.size(hubIconSize + 14.dp),
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(notificationCount.toString())
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    "Notifications",
                                    tint = if (hasCritical) Color(0xFFFFD700) else MaterialTheme.colorScheme.onPrimary.copy(
                                        alpha = 0.8f,
                                    ),
                                    modifier = Modifier.size(hubIconSize),
                                )
                            }
                        }
                    }

                    // Theme Toggle
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.size(hubIconSize + 14.dp),
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme == true) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            modifier = Modifier.size(hubIconSize),
                        )
                    }

                    // Font Size Toggle
                    IconButton(
                        onClick = { viewModel.nextFontScale() },
                        modifier = Modifier.size(hubIconSize + 14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Toggle Font Size",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(hubIconSize + (fontScale - 1).dp * 1.5f),
                        )
                    }

                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = AppIcons.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        )
                    }

                    systemControls()
                }
            }
        }
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("System Notifications ($notificationCount)", fontWeight = FontWeight.Bold)
                    TextButton(onClick = { PlatformNotificationHelper.clearAll() }) {
                        Text("Clear All")
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(notifications, key = { it.id }) { item ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (item.isCritical) MaterialTheme.colorScheme.errorContainer.copy(
                                alpha = 0.3f,
                            )
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    item.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(item.message, style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(onClick = { PlatformNotificationHelper.dismiss(item.id) }) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showNotificationDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}
