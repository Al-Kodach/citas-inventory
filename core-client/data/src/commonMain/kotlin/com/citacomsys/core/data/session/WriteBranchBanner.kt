package com.citacomsys.core.data.session

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.resources.AppIcons
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * A specialized target branch selection banner for multi-branch operations.
 * Allows administrators or support operators to specify a target physical branch for data entries
 * without altering global main write branch session state. Automatically hidden when locked to a single branch.
 */
@Composable
fun WriteBranchBanner(
    viewModel: BranchSwitcherViewModel = koinViewModel(),
    sessionManager: AppSessionManager = koinInject(),
    modifier: Modifier = Modifier,
    label: String = "Target Write Branch",
    enabled: Boolean = true,
    onWriteBranchSelect: (String?) -> Unit
) {
    val isBranchLocked by sessionManager.isBranchLocked.collectAsStateWithLifecycle()
    val defaultWriteBranchId by viewModel.currentWriteTargetBranchId.collectAsStateWithLifecycle()
    val availableBranches by viewModel.writeBranches.collectAsStateWithLifecycle()

    // Hide banner if branch is locked to a specific physical store or if workspace has <= 1 branch
    val canOverrideTarget = enabled && !isBranchLocked && availableBranches.size > 1
    if (isBranchLocked || availableBranches.size <= 1) return

    var targetBranchId by remember(defaultWriteBranchId) {
        mutableStateOf(defaultWriteBranchId)
    }

    LaunchedEffect(targetBranchId) {
        onWriteBranchSelect(targetBranchId)
    }

    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.AdminPanelSettings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Specify the target physical branch for this transaction or stock record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Form Target Branch Selector
            var expanded by remember { mutableStateOf(false) }
            val selectedName = availableBranches.find { it.id == targetBranchId }?.name
                ?: "Select Target Branch*"

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { if (canOverrideTarget) expanded = true },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canOverrideTarget
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Branch: $selectedName",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (canOverrideTarget) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                }

                if (canOverrideTarget) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(300.dp)
                    ) {
                        availableBranches.forEach { branch ->
                            DropdownMenuItem(
                                text = { Text(branch.name) },
                                onClick = {
                                    targetBranchId = branch.id
                                    onWriteBranchSelect(branch.id)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(AppIcons.LocationCity, null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
