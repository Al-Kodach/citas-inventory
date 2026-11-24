package com.citacomsys.user.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
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
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.result.UiResultButton
import com.citacomsys.shared.displayName
import com.citacomsys.types.TenantUserRole
import com.citacomsys.types.UserCapability
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserWorkspaceBindingScreen(
    onBackClick: () -> Unit,
    viewModel: UserWorkspaceBindingViewModel = koinViewModel()
) {
    val existingUsers by viewModel.existingUsers.collectAsStateWithLifecycle()
    val selectedUserId by viewModel.selectedUserId.collectAsStateWithLifecycle()
    val selectedWorkspaceId by viewModel.selectedWorkspaceId.collectAsStateWithLifecycle()
    val selectedBranchId by viewModel.selectedBranchId.collectAsStateWithLifecycle()
    val selectedRole by viewModel.selectedRole.collectAsStateWithLifecycle()
    val useOrgCapabilities by viewModel.useOrgCapabilities.collectAsStateWithLifecycle()
    val selectedCapabilities by viewModel.selectedCapabilities.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val organizationConfig by viewModel.organizationConfig.collectAsStateWithLifecycle()

    var userDropdownExpanded by remember { mutableStateOf(false) }
    var workspaceDropdownExpanded by remember { mutableStateOf(false) }
    var branchDropdownExpanded by remember { mutableStateOf(false) }

    val availableWorkspaces = organizationConfig?.workspaces ?: emptyList()
    val activeWorkspace = availableWorkspaces.find { it.id == selectedWorkspaceId }
        ?: availableWorkspaces.firstOrNull()
    val availableBranches = activeWorkspace?.branches ?: emptyList()

    // Sync default workspace selection if empty
    LaunchedEffect(availableWorkspaces) {
        if (selectedWorkspaceId.isBlank() && availableWorkspaces.isNotEmpty()) {
            val firstWs = availableWorkspaces.first()
            viewModel.selectedWorkspaceId.value = firstWs.id
            viewModel.selectedBranchId.value = ""
        }
    }

    val selectedUserItem = existingUsers.find { it.id == selectedUserId }
    val canBind = selectedUserId != null && selectedWorkspaceId.isNotBlank()
    val isExpand = currentWindowAdaptiveInfoV2().windowSizeClass.minWidthDp > 600

    AppScaffold(
        title = "Assign Staff to Business",
        onBack = onBackClick,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(if (isExpand) 24.dp else 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (isExpand) 24.dp else 16.dp)
        ) {
            Surface {
                Text(
                    text = "Assign business access and functional role permissions to staff members.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                "Select Staff Member*",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            ExposedDropdownMenuBox(
                expanded = userDropdownExpanded,
                onExpandedChange = { userDropdownExpanded = !userDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedUserItem?.let { "${it.name} (@${it.username})" }
                        ?: "Select staff member...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userDropdownExpanded) },
                    modifier = Modifier.menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryEditable,
                        true
                    )
                        .fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                ExposedDropdownMenu(
                    expanded = userDropdownExpanded,
                    onDismissRequest = { userDropdownExpanded = false }
                ) {
                    existingUsers.forEach { user ->
                        DropdownMenuItem(
                            text = { Text("${user.name} (@${user.username})") },
                            onClick = {
                                viewModel.selectedUserId.value = user.id
                                userDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Target Business & Store Assignment
            Text(
                "Target Business & Store Assignment",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                // Workspace / Business Dropdown
                ExposedDropdownMenuBox(
                    expanded = workspaceDropdownExpanded,
                    onExpandedChange = { workspaceDropdownExpanded = !workspaceDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    val activeTypeLabel = activeWorkspace?.type?.displayName()
                    val activeValue =
                        activeWorkspace?.let { "${it.name} ($activeTypeLabel)" }
                            ?: "Select Business"

                    OutlinedTextField(
                        value = activeValue,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Business*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workspaceDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    ExposedDropdownMenu(
                        expanded = workspaceDropdownExpanded,
                        onDismissRequest = { workspaceDropdownExpanded = false }
                    ) {
                        availableWorkspaces.forEach { ws ->
                            val typeLabel = ws.type.displayName()
                            DropdownMenuItem(
                                text = { Text("${ws.name} ($typeLabel)") },
                                onClick = {
                                    viewModel.selectedWorkspaceId.value = ws.id
                                    viewModel.selectedBranchId.value = ""
                                    workspaceDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Branch / Store Dropdown
                if (availableBranches.isNotEmpty()) {
                    val activeBranch = availableBranches.find { it.id == selectedBranchId }

                    ExposedDropdownMenuBox(
                        expanded = branchDropdownExpanded,
                        onExpandedChange = { branchDropdownExpanded = !branchDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = activeBranch?.name ?: "All Stores (Business-Wide)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Store") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )

                        ExposedDropdownMenu(
                            expanded = branchDropdownExpanded,
                            onDismissRequest = { branchDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🌐 All Stores (Business-Wide)") },
                                onClick = {
                                    viewModel.selectedBranchId.value = ""
                                    branchDropdownExpanded = false
                                }
                            )
                            HorizontalDivider()
                            availableBranches.forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text(branch.name) },
                                    onClick = {
                                        viewModel.selectedBranchId.value = branch.id
                                        branchDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Informational Note on Scope
            val isBranchRestricted = selectedBranchId.isNotBlank()
            val branchName = availableBranches.find { it.id == selectedBranchId }?.name
            val scopeText = if (isBranchRestricted && branchName != null) {
                "🔒 Store-Restricted: Staff member can only process transactions & view inventory within '$branchName'."
            } else {
                "🌐 Business-Wide Access: Staff member can manage inventory, sales & reports across ALL stores in this business."
            }

            Surface(
                color = if (isBranchRestricted) MaterialTheme.colorScheme.tertiaryContainer.copy(
                    alpha = 0.3f
                ) else MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.3f
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scopeText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isBranchRestricted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                "Business-Specific Role*",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TenantUserRole.entries.filter { it != TenantUserRole.OWNER }.forEach { role ->
                    FilterChip(
                        selected = selectedRole == role.name,
                        onClick = { viewModel.onRoleSelected(role.name) },
                        label = { Text(role.displayName(viewModel.productType), maxLines = 1) },
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            // Capability Inheritance Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Inherit Primary Organization Capabilities",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (useOrgCapabilities) "Staff member uses their default organization capabilities for this business."
                                else "Customize specific workspace capability overrides for this business assignment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = useOrgCapabilities,
                            onCheckedChange = { viewModel.useOrgCapabilities.value = it }
                        )
                    }

                    if (!useOrgCapabilities) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Workspace Capability Overrides",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserCapability.entries.filter { it.isTenantApplicable() }
                                .forEach { cap ->
                                    val isChecked = selectedCapabilities.contains(cap)
                                    FilterChip(
                                        selected = isChecked,
                                        onClick = { viewModel.toggleCapability(cap) },
                                        label = {
                                            Text(
                                                text = cap.displayName(),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        },
                                        leadingIcon = {
                                            if (isChecked) {
                                                Icon(
                                                    AppIcons.Check,
                                                    null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                                }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            UiResultButton(
                result = actionState,
                text = "Assign Access",
                showErrorInline = true,
                onClick = {
                    viewModel.bindUser(onSuccess = onBackClick)
                },
                modifier = Modifier.padding(16.dp),
                enabled = canBind
            )
        }
    }
}