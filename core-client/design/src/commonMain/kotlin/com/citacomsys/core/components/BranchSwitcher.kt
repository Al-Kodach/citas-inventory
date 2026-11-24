package com.citacomsys.core.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.domain.security.BranchUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchSwitcher(
    branches: List<BranchUiModel>,
    onBranchSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCompact: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedBranch = branches.find { it.isSelected } ?: branches.firstOrNull()
    val isCeoActive = selectedBranch?.id == "CEO_MODE"

    Box(modifier = modifier) {
        if (isCompact) {
            Surface(
                onClick = { if (enabled) expanded = true },
                modifier = Modifier.wrapContentWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                enabled = enabled
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCeoActive) AppIcons.AdminPanelSettings else AppIcons.LocationCity,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isCeoActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = selectedBranch?.name ?: "No branch",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCeoActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            OutlinedCard(
                onClick = { if (enabled) expanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        AppIcons.LocationCity,
                        null, tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Active Branch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            selectedBranch?.name ?: "No branch selected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(AppIcons.ExpandMore, null)
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            branches.forEach { branch ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = branch.name,
                            fontWeight = if (branch.id == "CEO_MODE") FontWeight.Bold else FontWeight.Normal,
                            color = if (branch.id == "CEO_MODE") MaterialTheme.colorScheme.tertiary else Color.Unspecified
                        )
                    },
                    onClick = {
                        onBranchSelected(branch.id)
                        expanded = false
                    },
                    leadingIcon = {
                        if (branch.isSelected) {
                            Icon(AppIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(
                                if (branch.id == "CEO_MODE") AppIcons.AdminPanelSettings else AppIcons.LocationCity,
                                null
                            )
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualBranchSwitcher(
    viewBranches: List<BranchUiModel>,
    writeBranches: List<BranchUiModel>,
    onViewBranchSelected: (String) -> Unit,
    onWriteBranchSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCompact: Boolean = false
) {
    var viewExpanded by remember { mutableStateOf(false) }
    var writeExpanded by remember { mutableStateOf(false) }

    val selectedView = viewBranches.find { it.isSelected } ?: viewBranches.firstOrNull()
    val selectedWrite = writeBranches.find { it.isSelected } ?: writeBranches.firstOrNull()
    val isCeoActive = selectedView?.id == "CEO_MODE"

    if (isCompact) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                onClick = { if (enabled) viewExpanded = true },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                enabled = enabled
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCeoActive) AppIcons.AdminPanelSettings else AppIcons.LocationCity,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isCeoActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = selectedView?.name ?: "No View Branch",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            DropdownMenu(expanded = viewExpanded, onDismissRequest = { viewExpanded = false }) {
                viewBranches.forEach { branch ->
                    DropdownMenuItem(
                        text = { Text(branch.name, fontWeight = if (branch.id == "CEO_MODE") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onViewBranchSelected(branch.id); viewExpanded = false },
                        leadingIcon = {
                            if (branch.isSelected) Icon(AppIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
                            else Icon(if (branch.id == "CEO_MODE") AppIcons.AdminPanelSettings else AppIcons.LocationCity, null)
                        }
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedCard(
                onClick = { if (enabled) viewExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.LocationCity, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("View Analytics Scope", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedView?.name ?: "All Branches (CEO Mode)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Icon(AppIcons.ExpandMore, null)
                }
            }

            DropdownMenu(expanded = viewExpanded, onDismissRequest = { viewExpanded = false }) {
                viewBranches.forEach { branch ->
                    DropdownMenuItem(
                        text = { Text(branch.name, fontWeight = if (branch.id == "CEO_MODE") FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onViewBranchSelected(branch.id); viewExpanded = false },
                        leadingIcon = {
                            if (branch.isSelected) Icon(AppIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
                            else Icon(if (branch.id == "CEO_MODE") AppIcons.AdminPanelSettings else AppIcons.LocationCity, null)
                        }
                    )
                }
            }

            if (writeBranches.isNotEmpty()) {
                OutlinedCard(
                    onClick = { if (enabled) writeExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Edit, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Write Target Branch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedWrite?.name ?: "Select Write Branch", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Icon(AppIcons.ExpandMore, null)
                    }
                }

                DropdownMenu(expanded = writeExpanded, onDismissRequest = { writeExpanded = false }) {
                    writeBranches.forEach { branch ->
                        DropdownMenuItem(
                            text = { Text(branch.name) },
                            onClick = { onWriteBranchSelected(branch.id); writeExpanded = false },
                            leadingIcon = {
                                if (branch.isSelected) Icon(AppIcons.Check, null, tint = MaterialTheme.colorScheme.primary)
                                else Icon(AppIcons.LocationCity, null)
                            }
                        )
                    }
                }
            }
        }
    }
}
