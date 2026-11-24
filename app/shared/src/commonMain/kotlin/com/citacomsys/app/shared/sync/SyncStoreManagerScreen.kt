package com.citacomsys.app.shared.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.database.dao.RequestMethod
import com.citacomsys.core.domain.model.LastSyncMetadata
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.util.formatted
import com.citacomsys.database.PendingRequestEntity
import com.citacomsys.types.EntityType
import com.design.AppTheme
import com.design.components.AppScaffold
import com.design.components.ScrollableRowWithButtons
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
fun SyncStoreManagerScreen(
    onBack: () -> Unit,
    viewModel: SyncManagerViewModel = koinViewModel()
) {
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val activeSyncCount by viewModel.activeSyncCount.collectAsStateWithLifecycle()
    val metrics by viewModel.syncMetrics.collectAsStateWithLifecycle()
    val queue by viewModel.pendingRequests.collectAsStateWithLifecycle()
    val lastSyncMetadata by viewModel.lastSyncMetadata.collectAsStateWithLifecycle()

    SyncStoreManagerContent(
        isSyncing = isSyncing,
        activeSyncCount = activeSyncCount,
        metrics = metrics,
        queue = queue,
        lastSyncMetadata = lastSyncMetadata,
        onBack = onBack,
        onForceSyncAll = viewModel::forceSyncAll,
        onRetryRequest = viewModel::retryRequest,
        onDeleteRequest = viewModel::deleteRequest,
        onRetryAllDeadLetters = viewModel::retryAllDeadLetters,
        onClearAllDeadLetters = viewModel::clearAllDeadLetters,
        onGetDiagnosticsReport = viewModel::generateDiagnosticsReport
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SyncStoreManagerContent(
    isSyncing: Boolean,
    activeSyncCount: Int,
    metrics: SyncMetricsData,
    queue: UiResult<List<PendingRequestEntity>>,
    lastSyncMetadata: LastSyncMetadata?,
    onBack: () -> Unit,
    onForceSyncAll: () -> Unit,
    onRetryRequest: (String) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onRetryAllDeadLetters: () -> Unit = {},
    onClearAllDeadLetters: () -> Unit = {},
    onGetDiagnosticsReport: () -> String = { "" }
) {
    var selectedPayloadToInspect by remember { mutableStateOf<PendingRequestEntity?>(null) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    AppScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "System Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    IconButton(
                        onClick = { showDiagnosticsDialog = true },
                    ) {
                        Icon(AppIcons.Download, contentDescription = "Export Log")
                    }

                    IconButton(
                        onClick = onForceSyncAll,
                        enabled = !isSyncing,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            AppIcons.Refresh,
                            contentDescription = "Force Sync All",
                            tint = if (isSyncing) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        UiResultGridView(
            state = queue,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { requests ->
            item {
                // Progress line indicator
                if (isSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                // Last Sync Status Banner
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        itemVerticalAlignment = Alignment.CenterVertically
                    ) {
                        val lastPush = lastSyncMetadata?.lastSuccessfulPushTimestamp
                        val lastPull = lastSyncMetadata?.lastSuccessfulPullTimestamp
                        val lastPushText = lastPush?.formatted() ?: "None yet"
                        val lastPullText = lastPull?.formatted() ?: "None yet"

                        Text(
                            text = "Last Upload: $lastPushText   •   Last Download: $lastPullText",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (lastSyncMetadata?.lastSystemErrorMessage != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    "SYNC ERROR",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                // Metric Cards Row with Horizontal Scroll & Navigation Buttons
                ScrollableRowWithButtons(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        MetricCard(
                            title = "Pending Uploads",
                            count = metrics.pendingCount.toString(),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                    item {
                        MetricCard(
                            title = "Currently Processing",
                            count = metrics.syncingCount.toString(),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                    item {
                        MetricCard(
                            title = "Dead-Letter Queue",
                            count = metrics.deadLetterCount.toString(),
                            containerColor = if (metrics.deadLetterCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (metrics.deadLetterCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(220.dp)
                        )
                    }
                }
            }
            // Batch Actions Bar for Dead-Letter Recovery
            if (metrics.deadLetterCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🚨 ${metrics.deadLetterCount} Dead-Letter payload(s) require intervention.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onRetryAllDeadLetters,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(
                                        "Retry All Failed",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                OutlinedButton(
                                    onClick = onClearAllDeadLetters
                                ) {
                                    Text(
                                        "Clear Dead Letters",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Offline Request Queue Ledger (${requests.size} entries)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (requests.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No offline transactions pending.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    count = requests.size,
                    key = { requests[it].id }
                ) { index ->
                    val item = requests[index]
                    PendingRequestRowCard(
                        item = item,
                        onRetry = { onRetryRequest(item.id) },
                        onDelete = { onDeleteRequest(item.id) },
                        onInspectPayload = { selectedPayloadToInspect = item }
                    )
                }
            }
        }
    }

    selectedPayloadToInspect?.let { request ->
        PayloadInspectorModal(
            request = request,
            onDismiss = { selectedPayloadToInspect = null }
        )
    }

    if (showDiagnosticsDialog) {
        val reportText = remember { onGetDiagnosticsReport() }
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = { Text("Sync Diagnostics Log") },
            text = {
                OutlinedTextField(
                    value = reportText,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().height(250.dp)
                )
            },
            confirmButton = {
                Button(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@AllAdaptivePreviews
@Composable
fun SyncStoreManagerScreenPreview() {
    val sampleRequests = listOf(
        PendingRequestEntity(
            id = "req-101",
            payload = "{\"saleId\": \"SALE-2025-001\", \"amount\": 150.00}",
            workspace_id = "ws-1",
            branch_id = "br-1",
            endpoint = "/api/v1/sales",
            method = RequestMethod.POST,
            entityType = EntityType.SALE,
            entity_id = "SALE-2025-001",
            dependant_id = null,
            eventAction = "CREATE",
            isSyncing = false,
            attempts = 1,
            actor_id = "user-1",
            actor_name = "Admin",
            routing_chain_json = null,
            createdAt = "2025-03-30T10:00:00Z",
            lastFailureReason = null,
            lastFailureTimestamp = null,
            canRetry = true,
        ),
        PendingRequestEntity(
            id = "req-102",
            payload = "{\"batchId\": \"888\", \"qty\": 50}",
            workspace_id = "ws-1",
            branch_id = "br-1",
            endpoint = "/api/v1/stock/batches",
            method = RequestMethod.PUT,
            entityType = EntityType.STOCK_BATCH,
            entity_id = "888",
            dependant_id = null,
            eventAction = "UPDATE",
            isSyncing = false,
            attempts = 3,
            actor_id = "user-1",
            actor_name = "Admin",
            routing_chain_json = null,
            createdAt = "2025-03-30T09:30:00Z",
            lastFailureReason = "HTTP 409 Conflict: Parent batch mismatch",
            lastFailureTimestamp = null,
            canRetry = false,
        )
    )

    AppTheme {
        SyncStoreManagerContent(
            isSyncing = false,
            activeSyncCount = 0,
            metrics = SyncMetricsData(pendingCount = 1, syncingCount = 0, deadLetterCount = 1),
            queue = UiResult.Success(sampleRequests),
            lastSyncMetadata = LastSyncMetadata(
                lastSuccessfulPushTimestamp = Clock.System.now(),
                lastSuccessfulPullTimestamp = Clock.System.now()
            ),
            onBack = {},
            onForceSyncAll = {},
            onRetryRequest = {},
            onDeleteRequest = {},
            onRetryAllDeadLetters = {},
            onClearAllDeadLetters = {},
            onGetDiagnosticsReport = { "Sample Diagnostics Report Log" }
        )
    }
}
