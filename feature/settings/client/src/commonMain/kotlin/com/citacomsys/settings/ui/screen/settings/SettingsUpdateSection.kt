package com.citacomsys.settings.ui.screen.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.feature.api.telemetry.CommonUpdateState

@Composable
fun SettingsUpdateSection(viewModel: SettingsViewModel) {
    val updateState by viewModel.updateState.collectAsStateWithLifecycle(CommonUpdateState.Idle)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = AppIcons.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(18.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "Application Version",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v${viewModel.currentVersion}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        AnimatedContent(targetState = updateState) { state ->
            when (state) {
                is CommonUpdateState.Idle, is CommonUpdateState.UpToDate -> {
                    TextButton(onClick = { viewModel.checkForUpdates() }) {
                        Text("Check for Updates")
                    }
                }

                is CommonUpdateState.Checking -> {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                is CommonUpdateState.UpdateAvailable -> {
                    Button(onClick = { viewModel.startUpdateDownload() }) {
                        Text("Download v${state.release.version}")
                    }
                }

                is CommonUpdateState.Downloading -> {
                    CircularProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.size(24.dp)
                    )
                }

                is CommonUpdateState.ReadyToInstall -> {
                    Button(
                        onClick = { viewModel.applyUpdate() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Install Now")
                    }
                }

                is CommonUpdateState.Failed -> {
                    IconButton(onClick = { viewModel.checkForUpdates() }) {
                        Icon(AppIcons.Error, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
