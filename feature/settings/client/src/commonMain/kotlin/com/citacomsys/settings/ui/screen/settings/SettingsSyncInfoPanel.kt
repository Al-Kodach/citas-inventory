package com.citacomsys.settings.ui.screen.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.util.formatted
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.spi.SyncManagerNav
import org.koin.compose.koinInject

@Composable
fun SettingsSyncInfoPanel(
    viewModel: SettingsViewModel = koinInject()
) {
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle(true)
    val metadata by viewModel.syncMetadata.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isMobile = adaptiveInfo.windowSizeClass.minWidthDp <= 400

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isSyncing) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Refresh,
                            contentDescription = null,
                            tint = if (isSyncing) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Cloud Sync Health",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isOnline) "Live" else "Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { viewModel.triggerManualSync() },
                    enabled = !isSyncing && isOnline,
                    modifier = Modifier.clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            AppIcons.Sync,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Metrics Grid
            if (isMobile) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SyncMetricBox(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Last Pull",
                        value = metadata?.lastSuccessfulPullTimestamp?.formatted() ?: "Never",
                        icon = AppIcons.Download
                    )
                    SyncMetricBox(
                        modifier = Modifier.fillMaxWidth(),
                        label = "Last Push",
                        value = metadata?.lastSuccessfulPushTimestamp?.formatted() ?: "Never",
                        icon = AppIcons.CloudUpload
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SyncMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "Last Pull",
                        value = metadata?.lastSuccessfulPullTimestamp?.formatted() ?: "Never",
                        icon = AppIcons.Download
                    )
                    SyncMetricBox(
                        modifier = Modifier.weight(1f),
                        label = "Last Push",
                        value = metadata?.lastSuccessfulPushTimestamp?.formatted() ?: "Never",
                        icon = AppIcons.CloudUpload
                    )
                }
            }

            // Error Message
            metadata?.lastSystemErrorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            AppIcons.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            TextButton(
                onClick = { navigator.navigateToSingleTop(SyncManagerNav) },
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Sync Hub", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(4.dp))
                Icon(AppIcons.ChevronRight, null, Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun SyncMetricBox(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                MaterialTheme.shapes.small
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
