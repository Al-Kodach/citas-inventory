package com.citacomsys.feature.telemetry.presentation.screen


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.LazyVerticalGridView
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.feature.api.telemetry.dao.LocalLogEntry
import com.citacomsys.shared.JsonFormatter
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun LocalTelemetryAuditScreen(
    viewModel: LocalTelemetryViewModel = koinInject()
) {
    val logs by viewModel.localLogs.collectAsStateWithLifecycle()
    val stats by viewModel.logStats.collectAsStateWithLifecycle()
    val isFlushing by viewModel.isFlushing.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isMobile = adaptiveInfo.windowSizeClass.minWidthDp <= 600

    var selectedLogToInspect by remember { mutableStateOf<LocalLogEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isMobile) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMobile) 12.dp else 20.dp)
        ) {
            // -- TITLE CONTROLS HEADER ---
            Header(
                isCompact = isMobile,
                isFlushing = isFlushing,
                pendingCount = stats.pendingCount,
                onBack = { navigator.goBack() },
                onSync = { viewModel.forceSyncLogsNow() }
            )

            if (isFlushing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Main Content Area
            LazyVerticalGridView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                columns = GridCells.Adaptive(if (isMobile) 180.dp else 280.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 2. Metrics Grid
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DashboardMetricBox(
                                "Unsynced Logs",
                                stats.pendingCount.toString(),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                Modifier.weight(1f)
                            )
                            DashboardMetricBox(
                                "In-Transit Outbox",
                                stats.syncingCount.toString(),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DashboardMetricBox(
                                "Identity Events",
                                stats.identityCount.toString(),
                                MaterialTheme.colorScheme.surfaceVariant,
                                Modifier.weight(1f)
                            )
                            DashboardMetricBox(
                                "Application Actions",
                                stats.appCount.toString(),
                                MaterialTheme.colorScheme.surfaceVariant,
                                Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Header for Logs
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Latest Local Execution Tracks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (logs.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No telemetry records exist on disk.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(items = logs, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { logItem ->
                        LocalLogItemCard(
                            log = logItem,
                            onInspectClick = { selectedLogToInspect = logItem }
                        )
                    }
                }
            }
        }

        // --- 4. VERTICAL CLOSE ACTION (Side Tab) ---
        if (isMobile) {
            VerticalCloseTab(
                modifier = Modifier.align(Alignment.CenterEnd),
                enabled = !isFlushing,
                onClick = { navigator.goBack() }
            )
        }
    }

    selectedLogToInspect?.let { log ->
        LocalPayloadInspectorModal(
            log = log,
            onDismiss = { selectedLogToInspect = null }
        )
    }
}

@Composable
private fun Header(
    isCompact: Boolean,
    isFlushing: Boolean,
    pendingCount: Long,
    onBack: () -> Unit,
    onSync: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Device Telemetry Audit Log",
                style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (!isCompact) {
                Text(
                    "Inspect cryptographically verified security events stored safely on this hardware device's disk.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isCompact) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, enabled = !isFlushing) {
                    Text("Close Panel")
                }
                Button(
                    onClick = onSync,
                    enabled = !isFlushing && pendingCount > 0
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFlushing) "Flushing Queue..." else "Sync Telemetry Now")
                }
            }
        } else {
            IconButton(
                onClick = onSync,
                enabled = !isFlushing && pendingCount > 0,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun VerticalCloseTab(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .width(40.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Close Panel".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.rotate(-90f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LocalLogItemCard(
    log: LocalLogEntry,
    onInspectClick: () -> Unit
) {
    com.citacomsys.core.components.AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onInspectClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = when (log.level.uppercase()) {
                            "CRITICAL" -> MaterialTheme.colorScheme.error
                            "WARNING" -> Color(0xFFFFA000)
                            else -> if (log.logSource == "IDENTITY") Color(0xFF9C27B0) else Color(0xFF009688)
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = log.logSource,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = log.actionOrEvent,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = if (log.isSyncing) "SYNCING..." else "LOCAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = if (log.isSyncing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                )
            }

            // Technical Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = log.createdAt.replace("T", " ").substringBefore('.'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                if (log.branchId != null) {
                    Text(
                        text = "Branch: ${log.branchId!!.take(8).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = log.payload,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardMetricBox(
    label: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = color, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LocalPayloadInspectorModal(
    log: LocalLogEntry,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val prettyJson = remember(log.payload) { JsonFormatter.format(log.payload) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Local Token Diagnostic View",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(log.payload))
                    isCopied = true
                }) {
                    Text(if (isCopied) "Copied JSON!" else "Copy Payload")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.widthIn(min = 300.dp, max = 700.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Cryptographic metadata trace saved locally under device storage schemas:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                    color = Color(0xFF1E1E1E),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(
                        Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = prettyJson,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CDCFE),
                            softWrap = false
                        )
                    }
                }

                // Track details bar info row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Transaction Guid key:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        log.id.take(12) + "...",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Dismiss View") }
        }
    )

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000.milliseconds)
            isCopied = false
        }
    }
}
