package com.citacomsys.feature.organization.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.resources.ProjectLogos
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.feature.workspace.ui.components.WorkspaceLaunchDialog
import com.citacomsys.kernel.model.BranchSummary
import com.citacomsys.kernel.model.WorkspaceSummary
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.PackageTier
import com.citacomsys.types.ProductType
import com.citacomsys.types.WorkspaceStatus
import com.design.AppTheme
import com.design.components.AppScaffold
import com.design.components.ScrollableRowWithButtons
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceListScreen(
    onWorkspaceClick: (String) -> Unit,
    onSetupNewWorkspace: () -> Unit,
    viewModel: WorkspaceListViewModel = koinViewModel()
) {
    val workspacesState by viewModel.workspacesState.collectAsStateWithLifecycle(UiResult.Loading)
    val activeWorkspaceId by viewModel.activeWorkspaceId.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()

    var launchTarget by remember { mutableStateOf<Pair<WorkspaceSummary, String>?>(null) }
    var selectedWsId by remember { mutableStateOf("") }

    val filteredWorkspaces by viewModel.filteredWorkspaces.collectAsStateWithLifecycle()

    val isVisible = LocalBackButtonVisibility.current

    AppScaffold(
        title = "Business",
        floatingActionButton = {
            FloatingActionButton(onClick = onSetupNewWorkspace) {
                Icon(AppIcons.Add, null)
            }
        },
    ) { padding ->
        UiResultGridView(
            state = workspacesState,
            columns = GridCells.Adaptive(360.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { _ ->
            // Header Search & Filter Bar
            item(span = { GridItemSpan(maxLineSpan) }) {
                BusinessSearchFilterHeader(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.searchQuery.value = it },
                    selectedType = selectedTypeFilter,
                    onTypeSelect = { viewModel.selectedTypeFilter.value = it }
                )
            }

            if (filteredWorkspaces.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
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
                items(filteredWorkspaces, key = { it.id }) { workspace ->
                    val isActive = workspace.id == activeWorkspaceId
                    BusinessListCCard(
                        workspace = workspace,
                        isActive = isActive,
                        isSelected = selectedWsId == workspace.id && isVisible,
                        onClick = {
                            onWorkspaceClick(workspace.id)
                            selectedWsId = workspace.id
                        },
                        onLaunch = { branchId ->
                            launchTarget = workspace to branchId
                        }
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        launchTarget?.let { (workspace, branchId) ->
            val selectedBranch = workspace.branches.find { it.id == branchId }
            WorkspaceLaunchDialog(
                workspaceName = workspace.name,
                branchName = selectedBranch?.name ?: "Main Store",
                isActive = workspace.id == activeWorkspaceId,
                isLaunching = false,
                onDismiss = { launchTarget = null },
                onConfirmLaunch = { isWsLocked, isBranchLocked ->
                    viewModel.launchWs(isWsLocked, isBranchLocked, workspace, branchId)
                }
            )
        }
    }
}

@Composable
private fun BusinessSearchFilterHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedType: ProductType?,
    onTypeSelect: (ProductType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search businesses & stores...") },
            leadingIcon = { Icon(AppIcons.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(AppIcons.Close, "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        ScrollableRowWithButtons(
            contentPadding = PaddingValues(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onTypeSelect(null) },
                    label = { Text("All Types") }
                )
            }
            item {
                FilterChip(
                    selected = selectedType == ProductType.PHARMACY,
                    onClick = { onTypeSelect(if (selectedType == ProductType.PHARMACY) null else ProductType.PHARMACY) },
                    label = { Text("Pharmacy") },
                    leadingIcon = {
                        Icon(
                            AppIcons.Medication,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = selectedType == ProductType.RETAIL,
                    onClick = { onTypeSelect(if (selectedType == ProductType.RETAIL) null else ProductType.RETAIL) },
                    label = { Text("Retail & POS") },
                    leadingIcon = {
                        Icon(
                            AppIcons.Storefront,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusinessListCCard(
    workspace: WorkspaceSummary,
    isActive: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLaunch: (
        branchId: String
    ) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(isActive) }

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

    val (modeIcon, modeLabel, modeContainerColor) = when (workspace.deploymentMode) {
        DeploymentMode.OFFLINE_LOCAL -> Triple(
            AppIcons.CloudOff,
            "Offline Local",
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )

        DeploymentMode.CLOUD_SYNCED -> Triple(
            AppIcons.CloudDone,
            "Cloud Synced",
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    }

    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        isSelected = isSelected
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Header Row: Icon + Workspace Name + Type + Mode Badge + Expand Arrow
            ProjectLogos.ProductDashboardLogo(
                title = workspace.name,
                type = workspace.type,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cloud / Offline Deployment Mode Badge
                Surface(
                    shape = CircleShape,
                    color = modeContainerColor,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = modeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded Controls: Store Selection + Launch Button
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                                    label = {
                                        Text(
                                            "${branch.name} (${branch.locationCode})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                AppIcons.Check,
                                                null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                )
                            }
                        }
                    }

                    if (workspace.status == WorkspaceStatus.ACTIVE.name && selectedBranchId != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onLaunch(selectedBranchId!!) },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isActive) AppIcons.Check else AppIcons.Dashboard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isActive) "Active (Re-open)" else "Launch Store",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

val business = listOf(
    WorkspaceSummary(
        id = "1",
        name = "Accra Pharmacy",
        type = ProductType.PHARMACY,
        status = WorkspaceStatus.ACTIVE.name,
        branches = listOf(
            BranchSummary(
                id = "1",
                name = "Main Branch",
                locationCode = "ACC",
            ),
        ),
        packageTier = PackageTier.STARTER,
        deploymentMode = DeploymentMode.OFFLINE_LOCAL

    ),
    WorkspaceSummary(
        id = "1",
        name = "Accra Retail",
        type = ProductType.RETAIL,
        status = WorkspaceStatus.ACTIVE.name,
        branches = listOf(
            BranchSummary(
                id = "1",
                name = "Main Branch",
                locationCode = "ACC",
            ),
        ),
        packageTier = PackageTier.PRO,
        deploymentMode = DeploymentMode.CLOUD_SYNCED
    ),
)


@Preview
@Composable
fun BusinessListCCardPreview() {
    AppTheme {
        BusinessListCCard(
            workspace = business.first(),
            isActive = true,
            isSelected = true,
            onClick = {},
            onLaunch = {}
        )
    }
}
