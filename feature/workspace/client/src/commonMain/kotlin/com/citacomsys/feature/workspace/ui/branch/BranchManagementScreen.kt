package com.citacomsys.feature.workspace.ui.branch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.workspace.branch.Branch
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.service.WorkStationInfo
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultGridView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.core.ui.result.isSuccess
import com.citacomsys.types.Address
import com.design.components.AppScaffold
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchManagementScreen(
    workspaceId: String,
    goBack: () -> Unit,
    viewModel: BranchManagementViewModel = koinViewModel(parameters = { parametersOf(workspaceId) })
) {
    val branchesResult by viewModel.branches.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val workStationInfo by viewModel.workStationInfo.collectAsStateWithLifecycle()
    var branchToEdit by remember { mutableStateOf<Branch?>(null) }
    var branchToDelete by remember { mutableStateOf<Branch?>(null) }

    var showCreate by remember { mutableStateOf(false) }

    val title = remember(branchToDelete, branchToEdit) {
        if (branchToDelete != null) {
            "Delete Branch"
        } else if (branchToEdit != null) {
            "Edit Branch"
        } else {
            "Create Branch"
        }
    }

    LaunchedEffect(actionState) {
        if (actionState.isSuccess) {
            goBack()
            branchToDelete = null
            branchToEdit = null
        }
    }

    AppScaffold(
        title = "Branches",
        onBack = goBack,
        actionIcon = AppIcons.Add,
        showInfo = showCreate || branchToDelete != null || branchToEdit != null,
        supportTitle = title,
        supportContent = {
            BranchFormView(
                workspaceId = workspaceId,
                branchToDelete = branchToDelete,
                branchToEdit = branchToEdit,
                workStationInfo = workStationInfo,
                clear = {
                    branchToEdit = null
                    branchToDelete = null
                },
                viewModel = viewModel
            )
        },
        onDismiss = { showCreate = false }
    ) { padding ->
        UiResultGridView(
            state = branchesResult,
            modifier = Modifier.fillMaxSize()
                .padding(padding),
            columns = GridCells.Adaptive(340.dp),
            emptyPlaceholder = {
                EmptyBranchesPlaceholder { showCreate = true }
            },
            onRetry = viewModel::loadBranches,
            itemLabel = "Branch"
        ) { branches ->
            items(branches.size, key = { branches[it].id }) { index ->
                val branch = branches[index]
                val isMainBranch = branch.id == workspaceId

                BranchCard(
                    branch = branch,
                    canDelete = branches.size > 1 && !isMainBranch,
                    onToggleStatus = {
                        viewModel.update(
                            branch.copy(
                                isActive = !branch.isActive
                            )
                        )
                    },
                    onEdit = { branchToEdit = branch },
                    onDelete = { branchToDelete = branch }
                )
            }
        }
    }
}

@Composable
private fun BranchCard(
    branch: Branch,
    canDelete: Boolean,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        isSelected = true,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = AppIcons.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = branch.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Code: ${branch.locationCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = branch.isActive,
                    onCheckedChange = { onToggleStatus() }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                branch.address?.let { addr ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.LocationCity,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${addr.street}, ${addr.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                branch.contactPhone?.let { phone ->
                    if (phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(AppIcons.Edit, "Edit")
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            AppIcons.Delete,
                            "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyBranchesPlaceholder(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            AppIcons.Storefront,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text("No Stores found", style = MaterialTheme.typography.titleMedium)
        Text(
            "Add your first physical location to start tracking stock.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(AppIcons.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Create Store")
        }
    }
}

@Composable
private fun BranchForm(
    branch: Branch,
    actionState: UiResult<Unit>,
    workStationInfo: WorkStationInfo? = null,
    confirmLabel: String = "Create",
    onDismiss: () -> Unit,
    onConfirm: (Branch) -> Unit
) {
    var name by remember { mutableStateOf(branch.name) }
    var code by remember { mutableStateOf(branch.locationCode) }
    var street by remember { mutableStateOf(branch.address?.street ?: "") }
    var city by remember { mutableStateOf(branch.address?.city ?: "") }
    var phone by remember { mutableStateOf(branch.contactPhone ?: "") }
    var website by remember { mutableStateOf(branch.website ?: "") }
    var email by remember { mutableStateOf(branch.email ?: "") }
    var useWorkspaceContact by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (workStationInfo != null) {
                FlowRow(
                    itemVerticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = useWorkspaceContact,
                        onCheckedChange = { checked ->
                            useWorkspaceContact = checked
                            if (checked) {
                                phone = workStationInfo.phone ?: phone
                                email = workStationInfo.email ?: email
                                website = workStationInfo.website ?: website
                                workStationInfo.address?.let { addr ->
                                    if (addr.contains(",")) {
                                        street = addr.substringBefore(",").trim()
                                        city = addr.substringAfter(",").trim()
                                    } else {
                                        city = addr
                                    }
                                }
                            }
                        }
                    )
                    Text(
                        text = "Use Main Store / Business Contact Info",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Branch Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Location Code (e.g. ACC-01)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = street,
                onValueChange = { street = it },
                label = { Text("Street Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Contact Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

        }

        Spacer(Modifier.height(16.dp))
        actionState.getErrorOrNull()?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = onDismiss,
                enabled = !actionState.isLoading
            ) {
                Text("Close")
            }

            Button(
                onClick = {
                    onConfirm(
                        Branch(
                            id = branch.id,
                            workspaceId = branch.workspaceId,
                            name = name,
                            locationCode = code,
                            address = Address(street, city, "Ghana"),
                            contactPhone = phone,
                            email = email,
                            website = website
                        )
                    )
                },
                enabled = name.isNotBlank() && code.isNotBlank() && !actionState.isLoading
            ) {
                Text(confirmLabel)
            }
        }
    }
}

@Composable
private fun BranchFormView(
    workspaceId: String,
    branchToDelete: Branch? = null,
    branchToEdit: Branch? = null,
    clear: () -> Unit,
    workStationInfo: WorkStationInfo? = null,
    viewModel: BranchManagementViewModel,
) {
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth()) {
        if (branchToEdit != null) {
            val b = branchToEdit
            BranchForm(
                branch = b,
                workStationInfo = workStationInfo,
                confirmLabel = "Save Changes",
                onDismiss = clear,
                actionState = actionState
            ) {
                viewModel.update(it)
            }

        } else if (branchToDelete != null) {
            val b = branchToDelete
            Column(Modifier.fillMaxSize()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Deleting branch '${b.name}' (${b.locationCode}) will automatically migrate all its stock levels, inventory records, sales orders, and audit history to the Main Branch before decommissioning.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Warning: This action cannot be undone.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = clear) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.deleteBranch(b.id)
                                clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Migrate & Delete")
                        }
                    }
                }

            }
        } else {
            BranchForm(
                branch = Branch(
                    workspaceId = workspaceId,
                    name = "",
                    locationCode = "",
                ),
                workStationInfo = workStationInfo,
                onDismiss = clear,
                onConfirm = {
                    viewModel.createBranch(it)
                },
                actionState = actionState
            )
        }
    }
}