package com.citacomsys.feature.workspace.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.navigationsuite.AllAdaptivePreviews
import com.citacomsys.core.resources.AppIcons
import com.design.AppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceLaunchPad(
    branchName: String,
    isActive: Boolean,
    isLaunching: Boolean,
    onLaunch: (isWsLocked: Boolean, isBranchLocked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isWsLocked by remember { mutableStateOf(false) }
    var isBranchLocked by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Suggestion Toggle Chips Row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isWsLocked,
                    onClick = { isWsLocked = !isWsLocked },
                    label = {
                        Text(
                            text = if (isWsLocked) "Lock Business" else "Business Unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isWsLocked) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isWsLocked) AppIcons.Lock else AppIcons.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = isBranchLocked,
                    onClick = { isBranchLocked = !isBranchLocked },
                    label = {
                        Text(
                            text = if (isBranchLocked) "Lock Store ($branchName)" else "Store Unlocked",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isBranchLocked) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isBranchLocked) AppIcons.Lock else AppIcons.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }

            // Launch Terminal Action Button
            Button(
                onClick = { onLaunch(isWsLocked, isBranchLocked) },
                enabled = !isLaunching,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLaunching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Connecting Workstation...")
                } else {
                    Icon(
                        imageVector = AppIcons.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isActive) "Resume Terminal" else "Launch Terminal",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun WorkspaceLaunchDialog(
    workspaceName: String,
    branchName: String,
    isActive: Boolean,
    isLaunching: Boolean,
    onDismiss: () -> Unit,
    onConfirmLaunch: (isWsLocked: Boolean, isBranchLocked: Boolean) -> Unit,
) {
    var isWsLocked by remember { mutableStateOf(false) }
    var isBranchLocked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Launch ${if (isActive) "Active Workstation" else workspaceName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Configure terminal isolation and security parameters before connecting to $branchName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Lock to $workspaceName",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Isolate terminal to this business",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Checkbox(
                        checked = isWsLocked,
                        onCheckedChange = { isWsLocked = it },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Lock to Store ($branchName)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Restrict POS operations to this branches exclusively",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Checkbox(
                        checked = isBranchLocked,
                        onCheckedChange = { isBranchLocked = it },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmLaunch(isWsLocked, isBranchLocked) },
                enabled = !isLaunching,
            ) {
                if (isLaunching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Connecting...")
                } else {
                    Icon(AppIcons.Login, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isActive) "Resume Terminal" else "Launch Terminal")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLaunching) {
                Text("Cancel")
            }
        },
    )
}

@AllAdaptivePreviews
@Composable
fun WorkspaceLaunchPadPreview() {
    AppTheme {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WorkspaceLaunchPad(
                branchName = "Main Store",
                isActive = false,
                isLaunching = false,
                onLaunch = { _, _ -> },
                modifier = Modifier.weight(1f)
            )

            WorkspaceLaunchPad(
                branchName = "Adum Store",
                isActive = true,
                isLaunching = false,
                onLaunch = { _, _ -> },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
