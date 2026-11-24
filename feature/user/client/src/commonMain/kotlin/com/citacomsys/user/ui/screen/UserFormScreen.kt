package com.citacomsys.user.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.components.SaveButton
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.result.UiResultButton
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.types.ProductType
import com.citacomsys.types.TenantUserRole
import com.citacomsys.types.UserCapability
import com.design.components.AppListView
import com.design.components.AppScaffold
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserFormScreen(
    userId: String? = null,
    onBackClick: () -> Unit,
    viewModel: UserFormViewModel = koinViewModel()
) {
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()
    val registrationMode by viewModel.registrationMode.collectAsStateWithLifecycle()
    val existingUsers by viewModel.existingUsers.collectAsStateWithLifecycle()
    val selectedExistingUserId by viewModel.selectedExistingUserId.collectAsStateWithLifecycle()

    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val organizationConfig by viewModel.organizationConfig.collectAsStateWithLifecycle()

    val name by viewModel.name.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val selectedRole by viewModel.selectedRole.collectAsStateWithLifecycle()
    val selectedCapabilities by viewModel.selectedCapabilities.collectAsStateWithLifecycle()
    val selectedWorkspaceId by viewModel.selectedWorkspaceId.collectAsStateWithLifecycle()
    val selectedBranchId by viewModel.selectedBranchId.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.confirmPassword.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }
    var userDropdownExpanded by remember { mutableStateOf(false) }
    var workspaceDropdownExpanded by remember { mutableStateOf(false) }
    var branchDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.setUserId(userId)
    }

    val availableWorkspaces = organizationConfig?.workspaces ?: emptyList()
    val activeWorkspace = availableWorkspaces.find { it.id == selectedWorkspaceId }
        ?: availableWorkspaces.firstOrNull()
    val availableBranches = activeWorkspace?.branches ?: emptyList()
    var productType by remember { mutableStateOf(ProductType.INVENTORY) }

    // Sync default selection if empty
    LaunchedEffect(availableWorkspaces) {
        if (selectedWorkspaceId.isBlank() && availableWorkspaces.isNotEmpty()) {
            val firstWs = availableWorkspaces.first()
            productType = firstWs.type
            viewModel.selectedWorkspaceId.value = firstWs.id
            viewModel.selectedBranchId.value = ""
        }
    }

    // Validation Logic
    val isPasswordValid = password.length >= 6
    val passwordsMatch = password == confirmPassword
    val canSave = if (registrationMode == RegistrationMode.ASSIGN_EXISTING) {
        selectedExistingUserId != null
    } else {
        name.isNotBlank() &&
                phone.isNotBlank() &&
                username.isNotBlank() &&
                (isEditing || (isPasswordValid && passwordsMatch))
    }

    AppScaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Account" else "Register Staff") },
                navigationIcon = { BackButton(onBackClick) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dual Mode Selection (Create New vs Assign Existing)
            if (!isEditing) {
                TabRow(selectedTabIndex = registrationMode.ordinal) {
                    Tab(
                        selected = registrationMode == RegistrationMode.CREATE_NEW,
                        onClick = {
                            viewModel.registrationMode.value = RegistrationMode.CREATE_NEW
                        },
                        text = { Text("Create New User Account") }
                    )
                    Tab(
                        selected = registrationMode == RegistrationMode.ASSIGN_EXISTING,
                        onClick = {
                            viewModel.registrationMode.value = RegistrationMode.ASSIGN_EXISTING
                        },
                        text = { Text("Assign Existing Staff") }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Target Business & Store Assignment
            Text(
                "Business & Store Assignment",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Workspace / Business Dropdown
                ExposedDropdownMenuBox(
                    expanded = workspaceDropdownExpanded,
                    onExpandedChange = { workspaceDropdownExpanded = !workspaceDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    val activeValue = activeWorkspace?.name ?: "Select Business"

                    OutlinedTextField(
                        value = activeValue,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Business*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workspaceDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    ExposedDropdownMenu(
                        expanded = workspaceDropdownExpanded,
                        onDismissRequest = { workspaceDropdownExpanded = false }
                    ) {
                        availableWorkspaces.forEach { ws ->
                            val typeLabel =
                                ws.type.name.lowercase().replaceFirstChar { it.uppercase() }
                            DropdownMenuItem(
                                text = { Text("${ws.name} ($typeLabel)") },
                                onClick = {
                                    viewModel.selectedWorkspaceId.value = ws.id
                                    viewModel.selectedBranchId.value = ""
                                    productType = ws.type
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
                            value = activeBranch?.name ?: "🌐 All Stores",
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

            if (registrationMode == RegistrationMode.ASSIGN_EXISTING && !isEditing) {
                // --- Assign Existing User Mode ---
                Text(
                    "Select Existing Staff Member",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                val selectedUserItem = existingUsers.find { it.id == selectedExistingUserId }

                ExposedDropdownMenuBox(
                    expanded = userDropdownExpanded,
                    onExpandedChange = { userDropdownExpanded = !userDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedUserItem?.name ?: "Select staff member...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Staff Member*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    ExposedDropdownMenu(
                        expanded = userDropdownExpanded,
                        onDismissRequest = { userDropdownExpanded = false }
                    ) {
                        existingUsers.forEach { user ->
                            DropdownMenuItem(
                                text = { Text("${user.name} (${user.username})") },
                                onClick = {
                                    viewModel.selectedExistingUserId.value = user.id
                                    userDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                // --- Create New / Edit User Mode ---
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.name.value = it },
                    label = { Text("Full Name*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { viewModel.username.value = it },
                    label = { Text("Username*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } || input.startsWith("+")) {
                            viewModel.phone.value = input
                        }
                    },
                    label = { Text("Phone Number*") },
                    placeholder = { Text("+233...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.email.value = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            Text(
                "Assign Functional Role Identity",
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
                        label = { Text(role.displayName(productType)) },
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            // Capability Customization Matrix Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Functional Capabilities & Access Rights",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Selected role auto-populates default capabilities. You can toggle specific permissions on or off below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserCapability.entries.filter { it.isTenantApplicable() }.forEach { cap ->
                            val isChecked = selectedCapabilities.contains(cap)
                            FilterChip(
                                selected = isChecked,
                                onClick = { viewModel.toggleCapability(cap) },
                                label = {
                                    Text(
                                        cap.name.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall
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

            // Password Section (Only for new users in CREATE_NEW mode)
            if (!isEditing && registrationMode == RegistrationMode.CREATE_NEW) {
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    color = DividerDefaults.color.copy(alpha = 0.5f)
                )

                Text(
                    "Security Credentials",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.password.value = it },
                    label = { Text("Password*") },
                    placeholder = { Text("Min. 6 characters") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) AppIcons.Visibility else AppIcons.VisibilityOff,
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    isError = password.isNotEmpty() && !isPasswordValid
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.confirmPassword.value = it },
                    label = { Text("Confirm Password*") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                            Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            UiResultButton(
                result = actionState,
                showErrorInline = true,
                text = if (registrationMode == RegistrationMode.ASSIGN_EXISTING) "Assign Access" else "Save Profile",
                onClick = {
                    viewModel.save(userId) {
                        onBackClick()
                    }
                },
                enabled = canSave && !actionState.isLoading
            )
        }
    }

}
