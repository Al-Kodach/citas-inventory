package com.citacomsys.feature.workspace.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.feature.workspace.ui.components.WorkspaceLaunchPad
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.shared.event.AppLifecycleEvent
import com.citacomsys.shared.event.EventBus
import com.citacomsys.types.ProductType
import com.citacomsys.types.WorkspaceStatus
import com.design.components.AppListView
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun BusinessManagementContent(
    workspaces: List<WorkspaceSummary>,
    activeWorkspaceId: String?,
    onViewDetails: (String) -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
    sessionManager: AppSessionManager = koinInject()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<ProductType?>(null) }
    val scope = rememberCoroutineScope()

    val filteredWorkspaces = workspaces.filter { ws ->
        val matchesSearch = ws.name.contains(searchQuery, ignoreCase = true) ||
                ws.type.name.contains(searchQuery, ignoreCase = true)
        val matchesType = selectedTypeFilter == null || ws.type == selectedTypeFilter
        matchesSearch && matchesType
    }

    AppListView(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Bar & Header Item
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search businesses & stores...") },
                    leadingIcon = { Icon(AppIcons.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Text(
                    text = "Manage business, view stores, and lock devices to specific operational business entity or branches.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredWorkspaces.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No businesses found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(filteredWorkspaces, key = { it.id }) { ws ->
                WorkspaceManagementCard(
                    workspace = ws,
                    isActive = ws.id == activeWorkspaceId,
                    onViewDetails = { onViewDetails(ws.id) },
                    onLaunch = { branchId, lockToWs, lockToBranch ->
                        scope.launch {
                            if (ws.id == activeWorkspaceId) {
                                onResume()
                            } else {
                                val authError = sessionManager.isAuthorized(ws.id, branchId)
                                if (authError != null) {
                                    return@launch
                                }
                                EventBus.publish(
                                    AppLifecycleEvent.WorkspaceSelected(
                                        workspaceId = ws.id,
                                        branchId = branchId,
                                        isWsLocked = lockToWs,
                                        isBranchLocked = lockToBranch,
                                        productType = ws.type,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceManagementCard(
    workspace: WorkspaceSummary,
    isActive: Boolean,
    onViewDetails: () -> Unit,
    onLaunch: (
        branchId: String,
        lockToWs: Boolean,
        lockToBranch: Boolean,
    ) -> Unit,
) {
    val (icon, typeLabel, accentColor) = when (workspace.type) {
        ProductType.PHARMACY -> Triple(
            AppIcons.Medication,
            "Pharmacy Platform",
            MaterialTheme.colorScheme.primary,
        )

        ProductType.RETAIL -> Triple(
            AppIcons.Storefront,
            "Retail & POS Platform",
            MaterialTheme.colorScheme.secondary,
        )

        else -> Triple(
            AppIcons.Business,
            "Commercial Platform",
            MaterialTheme.colorScheme.tertiary,
        )
    }

    var selectedBranchId by remember(workspace.id) {
        mutableStateOf(workspace.branches.firstOrNull()?.id)
    }
    val selectedBranch = workspace.branches.find { it.id == selectedBranchId }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header Row: Icon + Workspace Name + Type + Status Badge + Details Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewDetails),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = workspace.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = workspace.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }

                Spacer(Modifier.width(8.dp))

                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            if (workspace.branches.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    workspace.branches.forEach { branch ->
                        val isSelected = branch.id == selectedBranchId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBranchId = branch.id },
                            label = { Text("${branch.name} (${branch.locationCode})") },
                            leadingIcon = if (isSelected) {
                                { Icon(AppIcons.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                        )
                    }
                }
            }

            if (workspace.status == WorkspaceStatus.ACTIVE.name && selectedBranchId != null) {
                WorkspaceLaunchPad(
                    branchName = selectedBranch?.name ?: "Main",
                    isActive = isActive,
                    isLaunching = false,
                    onLaunch = { isWsLocked, isBranchLocked ->
                        onLaunch(
                            selectedBranchId!!,
                            isWsLocked,
                            isBranchLocked,
                        )
                    },
                )
            }
        }
    }
}
