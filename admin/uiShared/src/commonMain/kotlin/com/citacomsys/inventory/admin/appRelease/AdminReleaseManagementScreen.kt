package com.citacomsys.inventory.admin.appRelease

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.ActionButton
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.feature.api.telemetry.PrivateUpdateResponse
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminReleaseManagementScreen(
    onBack: () -> Unit,
    viewModel: AdminReleaseManagementViewModel = koinInject(),
    uploadViewModel: AdminReleaseUploadViewModel = koinInject()
) {
    val history by viewModel.releasesHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    val windowSize = currentWindowAdaptiveInfoV2().windowSizeClass
    val isCompact = windowSize.minWidthDp <= 800 // Adjusted threshold for console layout

    Column(
        modifier = Modifier.fillMaxSize().padding(if (isCompact) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        "Release Engine",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    "Monitor and deploy critical system packages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        onClick = { viewModel.refreshReleaseHistory() },
                        isLoading = isLoading,
                        icon = Icons.Default.Refresh,
                        label = "Refresh"
                    )
                    ActionButton(
                        onClick = { viewModel.showUploadModal = true },
                        isLoading = isLoading,
                        icon = Icons.Default.CloudUpload,
                        label = "Stage Build",
                        isPrimary = true
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(AppIcons.ArrowBack, contentDescription = "Back")
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Platform Release Engine Workspace",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Monitor live pipeline distribution histories and deploy critical system packages.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.refreshReleaseHistory() },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload tracks")
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh Feed")
                    }
                    Button(onClick = { viewModel.showUploadModal = true }, enabled = !isLoading) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stage New Build")
                    }
                }
            }
        }

        HorizontalDivider()

        // ---  HISTORICAL LEDGER CARD ROW STREAM ---
        if (isLoading && history.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (history.isEmpty()) {
            DetailsPlaceholder(
                icon = AppIcons.Backup,
                title = "No Platform Builds Found",
                description = "No platform builds have been committed to the deployment registries yet."
            )
        } else {
            LazyListView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { release ->
                    ReleaseHistoryCard(release)
                }
            }
        }
    }

    // --- MOUNT FILEKIT MULTIPART UPLOAD SHEETS MODAL OVERLAYS ---
    if (viewModel.showUploadModal) {
        Dialog(onDismissRequest = { viewModel.showUploadModal = false }) {
            Surface(
                modifier = Modifier.width(650.dp).wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                AdminReleaseUploadScreen(
                    onDismiss = {
                        viewModel.showUploadModal = false
                        viewModel.refreshReleaseHistory()
                    },
                    viewModel = uploadViewModel
                )
            }
        }
    }
}



@Composable
private fun ReleaseHistoryCard(release: PrivateUpdateResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Build Version: v${release.version}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (release.isMandatoryForceUpdate) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                "MANDATORY PATCH",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    "Published At: ${release.publishedAtString}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text("Changelog: ${release.changelog}", style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Attached package binaries list loops rows
            Text(
                "Packaged Installers:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            release.binaries.forEach { binary ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            binary.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        "${(binary.sizeInBytes / 1024 / 1024)} MB",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
