package com.citacomsys.user.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.components.LazyVerticalGridView
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getOrNull
import com.citacomsys.shared.displayName
import com.citacomsys.shared.util.toRoleDisplayName
import com.citacomsys.types.ProductType
import com.citacomsys.types.UserCapability
import com.citacomsys.types.UserStatus
import com.citacomsys.types.UserWorkspaceBinding
import com.design.components.AppScaffold
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    userId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: UserDetailViewModel = koinViewModel(),
    sessionManager: AppSessionManager = koinInject()
) {
    val userResult by viewModel.userState.collectAsStateWithLifecycle()
    val bindingsResult by viewModel.bindingsState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val currentUser by sessionManager.appUser.collectAsStateWithLifecycle()

    var showSuspendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    val user = userResult.getOrNull()
    val isOwner = user?.role == "OWNER"
    val isSelf = user?.id == currentUser?.id
    val canManage = currentUser?.capabilities?.contains(UserCapability.MANAGE_USERS) == true

    AppScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    AppIcons.AccountCircle,
                                    null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                user?.name ?: "Staff Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${user?.role?.toRoleDisplayName(viewModel.productType) ?: "---"} • ${user?.status?.displayName() ?: "---"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user?.status == UserStatus.SUSPENDED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (LocalBackButtonVisibility.current) {
                        BackButton(onBack)
                    }
                },
                actions = {
                    if (canManage && !isOwner && !isSelf) {
                        IconButton(onClick = onEdit) {
                            Icon(AppIcons.Edit, "Edit")
                        }
                    }
                },
            )
        }
    ) { padding ->
        UiResultView(
            state = userResult,
            onRetry = { viewModel.loadUser(userId) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { user ->


            Column(Modifier.fillMaxSize()) {
                if (actionState is UiResult.Loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                LazyVerticalGridView(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp)
                ) {
                    // Info Cards
                    item {
                        DetailCard(
                            label = "Direct Phone",
                            value = if (isOwner) user.phoneMask else user.phone.value,
                            icon = AppIcons.Call
                        )
                    }
                    item {
                        DetailCard(
                            label = "Username",
                            value = user.username,
                            icon = AppIcons.Person2
                        )
                    }
                    item {
                        DetailCard(
                            label = "Official Email",
                            value = (if (isOwner) user.emailMask else user.email) ?: "Not added",
                            icon = AppIcons.Email
                        )
                    }
                    item {
                        DetailCard(
                            label = "System ID",
                            value = user.id.take(8).uppercase(),
                            icon = AppIcons.Info
                        )
                    }
                    item {
                        DetailCard(
                            label = "Joined On",
                            value = user.createdAt.toString().split("T").first(),
                            icon = AppIcons.History
                        )
                    }

                    // Workspace Bindings Section
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Business Assignment & Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        UiResultView(
                            state = bindingsResult,
                            onRetry = { viewModel.loadOrRefreshBinding() }
                        ) { bindings ->
                            if (bindings.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "No active business bindings configured for this staff member.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        OutlinedButton(
                                            onClick = { viewModel.loadOrRefreshBinding() },
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Icon(
                                                AppIcons.Sync,
                                                null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Refresh")
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    bindings.forEach { binding ->
                                        UserBindingCard(
                                            binding = binding,
                                            productType = viewModel.productType,
                                            canManage = canManage && !isOwner && !isSelf,
                                            onUnbind = {
                                                viewModel.unbindUserFromWorkspace(
                                                    user.id,
                                                    binding.workspaceId
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canManage && !isOwner && !isSelf) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Administrative Controls",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }

                        item {
                            ActionCard(
                                title = if (user.status != UserStatus.SUSPENDED) "Suspend Access" else "Restore Access",
                                icon = if (user.status != UserStatus.SUSPENDED) AppIcons.Lock else AppIcons.LockOpen,
                                color = if (user.status != UserStatus.SUSPENDED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                onClick = {
                                    if (user.status != UserStatus.SUSPENDED) {
                                        showSuspendDialog = true
                                    } else {
                                        viewModel.activateUser(user.id)
                                    }
                                }
                            )
                        }

                        item {
                            ActionCard(
                                title = "Remove Account",
                                icon = AppIcons.Delete,
                                color = MaterialTheme.colorScheme.error,
                                onClick = { viewModel.deleteUser(user.id, onBack) }
                            )
                        }
                    } else if (isOwner) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        AppIcons.VerifiedUser,
                                        null,
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Owner account cannot be modified by other administrators.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSuspendDialog) {
            SuspendReasonDialog(
                onDismiss = { showSuspendDialog = false },
                onConfirm = { reason ->
                    viewModel.suspendUser(userId, reason)
                    showSuspendDialog = false
                }
            )
        }
    }
}

@Composable
private fun UserBindingCard(
    binding: UserWorkspaceBinding,
    productType: ProductType,
    canManage: Boolean,
    onUnbind: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (binding.isActive) MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Business: $binding",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Role: ${binding.role.toRoleDisplayName(productType)} • ${if (binding.isActive) "Active" else "Inactive"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (binding.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }

            if (canManage && binding.isActive) {
                OutlinedButton(
                    onClick = onUnbind,
                    shape = MaterialTheme.shapes.extraSmall,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(AppIcons.Business, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Unbind", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun SuspendReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suspend User Access") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Please provide a reason for suspending this staff member's access to the system.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Suspension Reason") },
                    placeholder = { Text("e.g. Temporary leave of absence") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Confirm Suspension") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DetailCard(
    label: String,
    value: String,
    icon: ImageVector
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        isSelected = true
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
