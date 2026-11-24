package com.citacomsys.inventory.admin.system.presentation.capability

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.modifiers.glassEffect
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.types.PlatformAdminRole
import com.citacomsys.types.ProductType
import com.citacomsys.types.TenantUserRole
import com.citacomsys.types.UserCapability
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CapabilityMatrixScreen(
    onBack: () -> Unit,
    viewModel: CapabilityMatrixViewModel = koinViewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Capability Matrix", "Role Hierarchy")

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isCompact = windowSizeClass.minWidthDp < 840 // Threshold for multi-column matrix

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Access Control Kernel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveChanges() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("Save Defaults")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (selectedTabIndex == 0) {
                    CapabilityMatrixWorkspace(viewModel, isCompact)
                } else {
                    RoleHierarchySection(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun CapabilityMatrixWorkspace(
    viewModel: CapabilityMatrixViewModel,
    isCompact: Boolean
) {
    val tier by viewModel.roleTier.collectAsState()

    Column(Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = if (tier == RoleTier.TENANT) 0 else 1,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            Tab(
                selected = tier == RoleTier.TENANT,
                onClick = { viewModel.setRoleTier(RoleTier.TENANT) },
                text = { Text("Tenant Roles") }
            )
            Tab(
                selected = tier == RoleTier.PLATFORM,
                onClick = { viewModel.setRoleTier(RoleTier.PLATFORM) },
                text = { Text("Platform Admin Roles") }
            )
        }

        Box(Modifier.weight(1f)) {
            if (tier == RoleTier.TENANT) {
                if (isCompact) {
                    CompactCapabilityList(
                        capabilities = UserCapability.entries.filter { it.isTenantApplicable() },
                        roles = TenantUserRole.entries,
                        matrixState = viewModel.tenantMatrix.collectAsState().value,
                        onToggle = viewModel::toggleTenantCapability,
                        businessType = ProductType.PHARMACY
                    )
                } else {
                    CapabilityMatrixTable(
                        capabilities = UserCapability.entries.filter { it.isTenantApplicable() },
                        roles = TenantUserRole.entries,
                        matrixState = viewModel.tenantMatrix.collectAsState().value,
                        onToggle = viewModel::toggleTenantCapability,
                        businessType = ProductType.PHARMACY
                    )
                }
            } else {
                if (isCompact) {
                    CompactCapabilityList(
                        capabilities = UserCapability.entries,
                        roles = PlatformAdminRole.entries,
                        matrixState = viewModel.platformMatrix.collectAsState().value,
                        onToggle = viewModel::togglePlatformCapability,
                        businessType = ProductType.INVENTORY
                    )
                } else {
                    CapabilityMatrixTable(
                        capabilities = UserCapability.entries,
                        roles = PlatformAdminRole.entries,
                        matrixState = viewModel.platformMatrix.collectAsState().value,
                        onToggle = viewModel::togglePlatformCapability,
                        businessType = ProductType.INVENTORY
                    )
                }
            }
        }
    }
}

@Composable
private fun <R : Enum<R>> CapabilityMatrixTable(
    capabilities: List<UserCapability>,
    roles: List<R>,
    matrixState: Map<R, Set<UserCapability>>,
    onToggle: (R, UserCapability) -> Unit,
    businessType: ProductType
) {
    val roleColumnWidth = 180.dp
    val horizontalScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().horizontalScroll(horizontalScrollState)) {
        // Sticky Header Row
        Surface(
            modifier = Modifier.fillMaxWidth().glassEffect(opacity = 0.1f),
            tonalElevation = 4.dp,
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(vertical = 20.dp).width(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAPABILITIES",
                    modifier = Modifier.width(300.dp).padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                roles.forEach { role ->
                    Column(
                        modifier = Modifier.width(roleColumnWidth),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = role.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val roleDisplayName =
                            if (role is TenantUserRole) role.displayName(businessType) else role.name
                        Text(
                            text = roleDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Body
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(capabilities) { capability ->
                val index = capabilities.indexOf(capability)
                val isAlternate = index % 2 != 0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .width(IntrinsicSize.Max)
                        .background(
                            if (isAlternate) MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.1f
                            ) else Color.Transparent
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.width(300.dp).padding(horizontal = 24.dp)) {
                        Text(
                            text = capability.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = capability.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    roles.forEach { role ->
                        val allowed = matrixState[role]?.contains(capability) == true
                        Box(
                            modifier = Modifier.width(roleColumnWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Checkbox(
                                checked = allowed,
                                onCheckedChange = { onToggle(role, capability) }
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun <R : Enum<R>> CompactCapabilityList(
    capabilities: List<UserCapability>,
    roles: List<R>,
    matrixState: Map<R, Set<UserCapability>>,
    onToggle: (R, UserCapability) -> Unit,
    businessType: ProductType
) {
    LazyListView(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(capabilities) { capability ->
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(
                            text = capability.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = capability.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Text(
                        "Assigned Roles",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roles.forEach { role ->
                            val isSelected = matrixState[role]?.contains(capability) == true
                            val roleDisplayName =
                                if (role is TenantUserRole) role.displayName(businessType) else role.name
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggle(role, capability) },
                                label = { Text(roleDisplayName) },
                                leadingIcon = if (isSelected) {
                                    { Icon(AppIcons.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleHierarchySection(viewModel: CapabilityMatrixViewModel) {
    val inheritance by viewModel.inheritanceState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Inheritance Rules",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Define which roles automatically include capabilities from others. " +
                    "Higher tiers should inherit from their immediate subordinates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(inheritance) { rule ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                rule.roleName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Inherits all permissions from",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            AppIcons.ChevronRight,
                            null,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            rule.parentRoleName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.weight(1f))

                        IconButton(onClick = {
                            viewModel.deleteInheritanceRule(
                                rule.roleName,
                                rule.parentRoleName
                            )
                        }) {
                            Icon(
                                AppIcons.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(AppIcons.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Inheritance Rule")
                }
            }
        }
    }

    if (showAddDialog) {
        AddInheritanceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { role, parent ->
                viewModel.addInheritanceRule(role, parent)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddInheritanceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    val tierChoices = listOf("Tenant Roles", "Platform Roles")
    var selectedTierIndex by remember { mutableStateOf(0) }

    val roles = remember(selectedTierIndex) {
        if (selectedTierIndex == 0) TenantUserRole.entries.map { "TENANT_${it.name}" }
        else PlatformAdminRole.entries.map { "PLATFORM_${it.name}" }
    }

    var selectedRole by remember { mutableStateOf<String?>(null) }
    var selectedParent by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Inheritance Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    tierChoices.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedTierIndex == index,
                            onClick = {
                                selectedTierIndex = index; selectedRole = null;
                                selectedParent = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = tierChoices.size
                            )
                        ) { Text(label) }
                    }
                }

                Text("Role that inherits permissions:", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    roles.forEach { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            label = { Text(role.removePrefix("TENANT_").removePrefix("PLATFORM_")) }
                        )
                    }
                }

                Text("Permissions to inherit from:", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    roles.forEach { role ->
                        FilterChip(
                            selected = selectedParent == role,
                            onClick = { selectedParent = role },
                            label = {
                                Text(
                                    role.removePrefix("TENANT_").removePrefix("PLATFORM_")
                                )
                            },
                            enabled = selectedRole != role
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRole != null && selectedParent != null) {
                        onAdd(selectedRole!!, selectedParent!!)
                    }
                },
                enabled = selectedRole != null && selectedParent != null && selectedRole != selectedParent
            ) {
                Text("Add Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
