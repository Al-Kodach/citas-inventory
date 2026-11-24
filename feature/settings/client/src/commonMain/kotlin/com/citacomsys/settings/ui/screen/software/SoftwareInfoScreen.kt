package com.citacomsys.settings.ui.screen.software

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.components.ResponsiveScreenWrapper
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import com.citacomsys.feature.api.telemetry.PrivateBinaryAssetDto
import com.citacomsys.feature.api.telemetry.PrivateUpdateResponse
import com.design.AppTheme
import com.design.components.AppScaffold
import com.design.components.AppVerticalScrollbar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SoftwareInfoScreen(
    onBackClick: () -> Unit,
    viewModel: SoftwareInfoViewModel = koinViewModel()
) {
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    SoftwareInfoContent(
        currentVersion = viewModel.currentVersion,
        schemaVersion = viewModel.schemaVersion,
        updateState = updateState,
        onBackClick = onBackClick,
        onCheckForUpdates = viewModel::checkForUpdates,
        onStartDownloading = viewModel::startDownloading,
        onTriggerInstallation = viewModel::triggerInstallation
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftwareInfoContent(
    currentVersion: String,
    schemaVersion: Long,
    updateState: CommonUpdateState,
    onBackClick: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onStartDownloading: () -> Unit,
    onTriggerInstallation: () -> Unit
) {
    AppScaffold(
        title = "Software & Platform Information",
        onBack = onBackClick,
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            ResponsiveScreenWrapper {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Application & Database Specs Card
                    item {
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Application Specifications",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Installed App Version",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "v$currentVersion",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = 0.4f
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Database Schema",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Schema v$schemaVersion",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Cloud Update Management Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    itemVerticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Cloud Software Update Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    UpdateStatusBadge(updateState)
                                }

                                when (updateState) {
                                    is CommonUpdateState.Idle -> {
                                        Text(
                                            "Check the central release server for available updates.",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    is CommonUpdateState.Checking -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Checking release server...",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    is CommonUpdateState.UpToDate -> {
                                        Text(
                                            "Your system is running the latest available release (v$currentVersion).",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF15803D)
                                        )
                                    }

                                    is CommonUpdateState.UpdateAvailable -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "New Version Available: v${updateState.release.version}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                updateState.release.changelog,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    is CommonUpdateState.Downloading -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                "Downloading Update... ${(updateState.progress * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            LinearProgressIndicator(
                                                progress = { updateState.progress },
                                                modifier = Modifier.fillMaxWidth().height(6.dp)
                                            )
                                        }
                                    }

                                    is CommonUpdateState.ReadyToInstall -> {
                                        Text(
                                            "Update package verified and downloaded. Ready to install.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    is CommonUpdateState.Failed -> {
                                        Text(
                                            "Update Error: ${updateState.reason}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                val isExpanded =
                                    currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp >= 600

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        10.dp,
                                        if (isExpanded) Alignment.End else Alignment.CenterHorizontally
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    itemVerticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = onCheckForUpdates,
                                        enabled = updateState !is CommonUpdateState.Checking && updateState !is CommonUpdateState.Downloading
                                    ) {
                                        Icon(
                                            AppIcons.Refresh,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Check for Updates")
                                    }

                                    if (updateState is CommonUpdateState.UpdateAvailable) {
                                        Button(
                                            onClick = onStartDownloading,
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Download Update")
                                        }
                                    }

                                    if (updateState is CommonUpdateState.ReadyToInstall) {
                                        Button(
                                            onClick = onTriggerInstallation,
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                        ) {
                                            Text("Install & Restart")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            AppVerticalScrollbar()
        }
    }
}

@Composable
private fun UpdateStatusBadge(state: CommonUpdateState) {
    val (label, containerColor, contentColor) = when (state) {
        is CommonUpdateState.Idle -> Triple(
            "IDLE",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )

        is CommonUpdateState.Checking -> Triple(
            "CHECKING",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        is CommonUpdateState.UpToDate -> Triple(
            "UP TO DATE",
            Color(0xFF22C55E).copy(alpha = 0.2f),
            Color(0xFF15803D)
        )

        is CommonUpdateState.UpdateAvailable -> Triple(
            "UPDATE AVAILABLE",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )

        is CommonUpdateState.Downloading -> Triple(
            "DOWNLOADING",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )

        is CommonUpdateState.ReadyToInstall -> Triple(
            "READY TO INSTALL",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )

        is CommonUpdateState.Failed -> Triple(
            "FAILED",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Surface(
        shape = CircleShape,
        color = containerColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@AllAdaptivePreviews
@Composable
fun SoftwareInfoScreenPreview() {
    AppTheme {
        SoftwareInfoContent(
            currentVersion = "1.0.0",
            schemaVersion = 1L,
            updateState = CommonUpdateState.UpdateAvailable(
                release = PrivateUpdateResponse(
                    version = "1.1.0",
                    changelog = "Critical security enhancements and inventory sync fixes.",
                    publishedAtString = "2025-03-30",
                    isMandatoryForceUpdate = false,
                    binaries = listOf(
                        PrivateBinaryAssetDto(
                            "app-release.apk",
                            "https://example.com/app.apk",
                            15000000
                        )
                    )
                )
            ),
            onBackClick = {},
            onCheckForUpdates = {},
            onStartDownloading = {},
            onTriggerInstallation = {}
        )
    }
}
