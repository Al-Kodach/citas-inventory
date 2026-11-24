package com.citacomsys.app.shared.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.app.shared.boot.AppPlatform
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.kernel.client.WorkspaceConfigurationManager
import com.citacomsys.kernel.model.WsConfigurationState
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.design.AppTheme
import io.github.aakira.napier.Napier

@Composable
fun WorkspaceBootstrapLoader(
    configurationManager: WorkspaceConfigurationManager,
    event: AppLifecycleEvent.WorkspaceSelected,
    onComplete: () -> Unit,
    onFail: () -> Unit,
) {
    val state by configurationManager.wsConfigurationState.collectAsStateWithLifecycle()
    val lifecycleState by AppPlatform.lifecycleState.collectAsStateWithLifecycle()
    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryCount, lifecycleState) {
        if (lifecycleState is AppLifecycleEvent.WorkspaceInfraReady) {
            Napier.d(tag = "WORKSPACE_CONFIG_SCREEN") { "WorkspaceInfraReady, configuring workspace executed" }
            configurationManager.configureWs(event) {
                EventBus.publish(AppLifecycleEvent.WorkspaceConfigCompleted)
            }
        }
    }

    WorkspaceBootstrapLoaderContent(
        state = state,
        lifecycleState = lifecycleState,
        onFail = onFail,
    ) {
        retryCount++
    }
}

@Composable
fun WorkspaceBootstrapLoaderContent(
    state: WsConfigurationState,
    lifecycleState: AppLifecycleEvent,
    onComplete: () -> Unit = {},
    onFail: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is WsConfigurationState.Error -> {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 500.dp)
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.Error,
                                        contentDescription = "Configuration Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Workspace Launch Failed",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(
                                    12.dp,
                                    Alignment.CenterHorizontally
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = onRetry,
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Retry Connection", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onFail,
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text("Return to Hub", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                else -> {
                    val configuringState = state as? WsConfigurationState.Configuring
                    val currentStep = configuringState?.currentStep ?: 1
                    val total = configuringState?.totalSeps() ?: configuringState?.totalSteps ?: 3

                    val message = when {
                        configuringState != null -> configuringState.currentStepMessage
                        lifecycleState is AppLifecycleEvent.WorkspaceInfraConfiguration -> "Configuring workspace infrastructure..."
                        lifecycleState is AppLifecycleEvent.WorkspaceInfraReady -> "Workspace infrastructure ready. Finalizing..."
                        else -> "Initializing workspace environment..."
                    }

                    val targetFraction = if (configuringState != null && total > 0) {
                        (currentStep.toFloat() / total.toFloat()).coerceIn(0.1f, 1f)
                    } else 0.15f

                    val animatedProgress by animateFloatAsState(
                        targetValue = targetFraction,
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        label = "bootstrap_progress"
                    )

                    val percentage = (animatedProgress * 100).toInt().coerceIn(0, 100)

                    Card(
                        modifier = Modifier
                            .widthIn(max = 480.dp)
                            .fillMaxWidth()
                            .padding(24.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            ProjectLogos.CitasProjectLogo(size = 48.dp)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Setting Up Workspace",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                AnimatedContent(
                                    targetState = message,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                                    },
                                    label = "bootstrap_msg_transition"
                                ) { msg ->
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Step $currentStep of $total",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "$percentage%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.4f
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun WsConfigurationState.Configuring.totalSeps(): Int? = totalSteps.takeIf { it > 0 }

@AllAdaptivePreviews
@Composable
fun WorkspaceBootstrapLoaderPreview() {
    AppTheme {
        Surface {
            WorkspaceBootstrapLoaderContent(
                state = WsConfigurationState.Configuring(
                    currentStep = 2,
                    totalSteps = 3,
                    currentStepMessage = "Connecting to workspace server...",
                ),
                lifecycleState = AppLifecycleEvent.WorkspaceInfraConfiguration,
            )
        }
    }
}
