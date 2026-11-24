package com.citacomsys.feature.telemetry.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AnimatedProgressBar
import com.citacomsys.feature.api.telemetry.CommonUpdateState
import eu.anifantakis.lib.ksafe.biometrics.KSafeBiometrics
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import com.citacomsys.core.splash.SplashScreen

@Composable
fun ForceUpdateGateScreen(
    onBypassAllowed: () -> Unit,
    onNoUpdate: () -> Unit,
    viewModel: ForceUpdateViewModel = koinViewModel()
) {
    val syncState by viewModel.updateState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates(onNoUpdate = onNoUpdate)
    }

    // Determine if we are specifically in a mandatory update state
    val isMandatory = remember(syncState) {
        (syncState as? CommonUpdateState.UpdateAvailable)?.release?.isMandatoryForceUpdate == true
    }

    // Early return with SplashScreen while checking or if no mandatory update is detected yet
    if (!isMandatory && syncState !is CommonUpdateState.Downloading &&
        syncState !is CommonUpdateState.ReadyToInstall &&
        syncState !is CommonUpdateState.Failed
    ) {
        SplashScreen()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(min = 450.dp, max = 650.dp),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = when (syncState) {
                        is CommonUpdateState.Failed -> MaterialTheme.colorScheme.error
                        is CommonUpdateState.ReadyToInstall -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )

                Text(
                    text = "Mandatory Platform Update Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Your local terminal version (v${viewModel.currentVersion}) has been deprecated by the server. To maintain ledger compliance and security token signatures, you must download the critical upgrade package before accessing your workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                when (val current = syncState) {
                    is CommonUpdateState.UpdateAvailable -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Discovered Release Version: v${current.release.version}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Changelog: ${current.release.changelog}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }

                    is CommonUpdateState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Downloading Upgrade Package...",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    "${(current.progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedProgressBar(progress = current.progress)
                        }
                    }

                    is CommonUpdateState.ReadyToInstall -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Binary verification successful! Ready to apply patch update.",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )

                            Button(
                                onClick = { viewModel.triggerInstallation() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFF4CAF50
                                    )
                                )
                            ) {
                                Text("Apply Installation & Auto-Restart")
                            }
                        }
                    }

                    is CommonUpdateState.Failed -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text(
                                    text = "Handshake Error Interruption: ${current.reason}",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val authorized =
                                                KSafeBiometrics.verifyBiometric("Authorize administrative manager terminal manual resetWorkspaceSession overrides")
                                            if (authorized) {
                                                viewModel.startDownloading()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry Connection")
                                }

                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            val authorized =
                                                KSafeBiometrics.verifyBiometric("Verify administrator biometric privileges to bypass download blocks")
                                            if (authorized) {
                                                onBypassAllowed()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Text("Admin Local Bypass")
                                }
                            }
                        }
                    }

                    else -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}
