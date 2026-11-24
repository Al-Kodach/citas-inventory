package com.citacomsys.inventory.admin.tenant.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.admin.api.tenant.AdminWorkspaceSummary
import com.citacomsys.admin.api.tenant.BranchSummary
import com.citacomsys.admin.api.tenant.OrganizationDetails
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.onSuccess
import com.citacomsys.api.organization.model.OrgStatus
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.components.navigationsuite.TabletDarkPreview
import com.citacomsys.types.WorkspaceStatus
import com.design.components.AppScaffold
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.types.Money
import com.design.AppTheme
import org.koin.compose.koinInject
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTenantDetailsScreen(
    organizationId: String,
    onBack: () -> Unit = {},
    viewModel: AdminTenantDetailsViewModel = koinInject(),
) {
    val detailsState by viewModel.selectedTenantDetails.collectAsStateWithLifecycle()

    LaunchedEffect(organizationId) {
        viewModel.selectTenant(organizationId)
    }

    AdminTenantDetailsContent(
        detailsState = detailsState,
        onBack = onBack,
        onRetry = { viewModel.selectTenant(organizationId) },
        onActivateTenant = { viewModel.activateTenant(organizationId) },
        onDeleteTenant = { viewModel.deleteTenant(organizationId) { onBack() } },
        onSuspendTenant = { reason -> viewModel.suspendTenant(organizationId, reason) },
        onActivateWorkspace = { wsId -> viewModel.activateWorkspace(organizationId, wsId) },
        onDeleteWorkspace = { wsId -> viewModel.deleteWorkspace(organizationId, wsId) },
        onSuspendWorkspace = { wsId, reason ->
            viewModel.suspendWorkspace(
                organizationId,
                wsId,
                reason
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTenantDetailsContent(
    detailsState: UiResult<OrganizationDetails>,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onActivateTenant: () -> Unit = {},
    onDeleteTenant: () -> Unit = {},
    onSuspendTenant: (reason: String) -> Unit = {},
    onActivateWorkspace: (workspaceId: String) -> Unit = {},
    onDeleteWorkspace: (workspaceId: String) -> Unit = {},
    onSuspendWorkspace: (workspaceId: String, reason: String) -> Unit = { _, _ -> },
) {
    var showOrgSuspendDialog by remember { mutableStateOf(false) }
    var wsSuspendTarget by remember { mutableStateOf<String?>(null) }

    AppScaffold(
        title = "Organization Management",
        onBack = if (LocalBackButtonVisibility.current) onBack else null,
        actions = {
            detailsState.onSuccess { details ->
                if (details.status != OrgStatus.SUSPENDED) {
                    IconButton(onClick = { showOrgSuspendDialog = true }) {
                        Icon(
                            AppIcons.Block,
                            contentDescription = "Suspend Organization",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(onClick = { onActivateTenant() }) {
                        Icon(
                            AppIcons.Done,
                            contentDescription = "Activate Organization",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = { onDeleteTenant() }) {
                    Icon(
                        AppIcons.Delete,
                        contentDescription = "Archive Organization",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) { padding ->
        UiResultView(
            modifier = Modifier.fillMaxSize().padding(padding),
            state = detailsState,
            onRetry = onRetry
        ) { details ->
            OrganizationDetailsContent(
                details = details,
                onSuspendWorkspace = { wsId -> wsSuspendTarget = wsId },
                onActivateWorkspace = { wsId -> onActivateWorkspace(wsId) },
                onDeleteWorkspace = { wsId -> onDeleteWorkspace(wsId) }
            )
        }
    }

    if (showOrgSuspendDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showOrgSuspendDialog = false },
            title = { Text("Suspend Organization") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Suspending this organization will also suspend all child workspaces.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason for suspension") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOrgSuspendDialog = false
                        onSuspendTenant(reason)
                    },
                    enabled = reason.isNotBlank()
                ) {
                    Text("Suspend Organization", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrgSuspendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    wsSuspendTarget?.let { _ ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { wsSuspendTarget = null },
            title = { Text("Suspend Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Suspending this workspace will block terminal sync and user access.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason for suspension") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = wsSuspendTarget
                        wsSuspendTarget = null
                        if (targetId != null) {
                            onSuspendWorkspace(targetId, reason)
                        }
                    },
                    enabled = reason.isNotBlank()
                ) {
                    Text("Suspend Workspace", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { wsSuspendTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrganizationDetailsContent(
    details: OrganizationDetails,
    onSuspendWorkspace: (String) -> Unit,
    onActivateWorkspace: (String) -> Unit,
    onDeleteWorkspace: (String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth > 840.dp
        val contentMaxWidth = if (isWide) 960.dp else Dp.Infinity

        LazyListView(
            modifier = Modifier.widthIn(max = contentMaxWidth).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 2.dp, end = 8.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        // Title & Status Badge in Adaptive FlowRow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                details.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when (details.status) {
                                    OrgStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                    OrgStatus.SUSPENDED -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = details.status.name,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when (details.status) {
                                        OrgStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                                        OrgStatus.SUSPENDED -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                        Spacer(Modifier.height(14.dp))

                        // Organization Metadata Items in Adaptive FlowRow
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoItem(label = "Business Code", value = details.businessCode)
                            InfoItem(label = "Email Address", value = details.email ?: "N/A")
                            InfoItem(label = "Contact Phone", value = details.phone ?: "N/A")
                            InfoItem(
                                label = "Created Date",
                                value = details.createdAt.toString().take(10)
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            FlowRow(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Total Recurring Subscription",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    details.totalRecurringAmount.formatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Workspaces & Branches (${details.workspaces.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(details.workspaces.size) { index ->
                val ws = details.workspaces[index]
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    ws.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Type: ${ws.businessType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = when (ws.status) {
                                    WorkspaceStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                    WorkspaceStatus.SUSPENDED -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = ws.status.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (ws.status) {
                                        WorkspaceStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
                                        WorkspaceStatus.SUSPENDED -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Recurring: ${ws.recurringAmount?.formatted ?: "N/A"} / cycle",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Workspace Management Actions
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (ws.status != WorkspaceStatus.SUSPENDED) {
                                    TextButton(onClick = { onSuspendWorkspace(ws.id) }) {
                                        Text(
                                            "Suspend",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    TextButton(onClick = { onActivateWorkspace(ws.id) }) {
                                        Text(
                                            "Activate",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                TextButton(onClick = { onDeleteWorkspace(ws.id) }) {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Assigned Branches (${ws.branches.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        ws.branches.forEach { branch ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "• ${branch.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Code: ${branch.locationCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.widthIn(min = 140.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@AllAdaptivePreviews
@Composable
private fun AdminTenantDetailsScreenPreview() {
    AppTheme {
        AdminTenantDetailsContent(
            detailsState = UiResult.Success(
                OrganizationDetails(
                    id = "org-1",
                    name = "MediPlus Pharmacy & Healthcare Logistics Ltd",
                    businessCode = "MEDI_PHARM_8A9F",
                    email = "admin@mediplus.com",
                    phone = "+233208169630",
                    status = OrgStatus.ACTIVE,
                    createdAt = Clock.System.now(),
                    totalRecurringAmount = Money(14900, "USD"),
                    workspaces = listOf(
                        AdminWorkspaceSummary(
                            id = "ws-1",
                            name = "MediPlus Central Retail & Dispensing",
                            businessType = "PHARMACY",
                            status = WorkspaceStatus.ACTIVE,
                            recurringAmount = Money(9900, "USD"),
                            branches = listOf(
                                BranchSummary("b-1", "Osu Main Branch", "OSU_01", isActive = true),
                                BranchSummary("b-2", "East Legon Express", "LEG_02", isActive = true)
                            ),
                            createdAt = Clock.System.now()
                        )
                    )
                )
            )
        )
    }
}
