package com.citacomsys.inventory.admin.system.presentation.pkg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.platform.model.*
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.sudo.ActorSudoAuthDialogs
import com.citacomsys.types.BillingCycle
import com.citacomsys.types.DeploymentMode
import com.citacomsys.types.Money
import com.citacomsys.types.ProductModule
import com.citacomsys.types.generateUUID
import org.koin.compose.viewmodel.koinViewModel

/**
 * Administrative screen for configuring product package details.
 * Optimized for non-technical users by converting technical values (MB) to readable ones (GB).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPackageDetailsScreen(
    packageId: String,
    onBack: () -> Unit,
    viewModel: AdminPackageDetailsViewModel = koinViewModel()
) {
    val detailsState by viewModel.selectedItemState.collectAsStateWithLifecycle()

    LaunchedEffect(packageId) {
        viewModel.loadDetails(packageId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Package Configuration") },
                navigationIcon = {
                    if (LocalBackButtonVisibility.current) {
                        BackButton(onClick = onBack)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            UiResultView(
                state = detailsState,
                onRetry = { viewModel.loadDetails(packageId) }
            ) { details ->
                PackageDetailsContent(
                    details = details,
                    onSave = { viewModel.savePackage(it) }
                )
            }
        }
    }

    ActorSudoAuthDialogs(sudoManager = viewModel.sudoAuthHandler)
}

@Composable
private fun PackageDetailsContent(
    details: ProductPackageDetails,
    onSave: (ProductPackageDetails) -> Unit
) {
    var editedDetails by remember(details) { mutableStateOf(details) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoSection(editedDetails.packageInfo) {
                    editedDetails = editedDetails.copy(packageInfo = it)
                }
            }
            item {
                ResourcesSection(
                    res = editedDetails.limits,
                    smsQuota = editedDetails.packageInfo.smsQuotaPerCycle,
                    onUpdateResources = { editedDetails = editedDetails.copy(limits = it) },
                    onUpdateSms = { 
                        editedDetails = editedDetails.copy(
                            packageInfo = editedDetails.packageInfo.copy(smsQuotaPerCycle = it)
                        ) 
                    }
                )
            }
            item {
                ModulesSection(editedDetails.modules) {
                    editedDetails = editedDetails.copy(modules = it)
                }
            }
            item {
                BillingTermsSection(
                    packageId = editedDetails.packageInfo.id,
                    terms = editedDetails.billingTerms
                ) {
                    editedDetails = editedDetails.copy(billingTerms = it)
                }
            }
        }

        Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
            Button(
                onClick = { onSave(editedDetails) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save Platform Configuration")
            }
        }
    }
}

@Composable
private fun InfoSection(info: ProductPackage, onUpdate: (ProductPackage) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Package Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = info.name,
                onValueChange = { onUpdate(info.copy(name = it)) },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = info.description ?: "",
                onValueChange = { onUpdate(info.copy(description = it)) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recommended Package Banner", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = info.recommended,
                    onCheckedChange = { onUpdate(info.copy(recommended = it)) }
                )
            }
        }
    }
}

@Composable
private fun ResourcesSection(
    res: PackageLimits,
    smsQuota: Int,
    onUpdateResources: (PackageLimits) -> Unit,
    onUpdateSms: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Resource Boundaries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Configure hard technical limits enforced by the platform runtime and database policies.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ResourceInput("Max Users", res.maxUsers) {
                onUpdateResources(res.copy(maxUsers = it))
            }

            ResourceInput("Max Devices / Nodes", res.maxDevice) {
                onUpdateResources(res.copy(maxDevice = it))
            }

            ResourceInput("Max Branches", res.maxBranches) {
                onUpdateResources(res.copy(maxBranches = it))
            }

            ResourceInput("Products Limit", res.maxProductsLimit) {
                onUpdateResources(res.copy(maxProductsLimit = it))
            }

            ResourceInput("Active Batches Limit", res.maxActiveBatchesLimit) {
                onUpdateResources(res.copy(maxActiveBatchesLimit = it))
            }

            ResourceInput("SMS Quota per Cycle", smsQuota) {
                onUpdateSms(it)
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp).alpha(0.5f))

            Text(
                text = "Lifecycle & Retention",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Data Retention
            ResourceInput("History Retention (Months)", res.historyRetentionMonths) {
                onUpdateResources(res.copy(historyRetentionMonths = it))
            }

            Text(
                text = if (res.historyRetentionMonths <= 0)
                    "Note: 0 = Local Only (No auto-purge/Cloud sync)."
                else "Data older than ${res.historyRetentionMonths} months will be automatically purged from cloud.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModulesSection(modules: Set<ProductModule>, onUpdate: (Set<ProductModule>) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Feature Modules", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProductModule.entries.forEach { module ->
                    val isSelected = modules.contains(module)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val next = if (isSelected) modules - module else modules + module
                            onUpdate(next)
                        },
                        label = { Text(module.name) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BillingTermsSection(
    packageId: String,
    terms: List<PackageBillingTerm>,
    onUpdate: (List<PackageBillingTerm>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Subscription Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Configure pricing across deployment modes (Cloud Synced vs Local Offline Edge Mesh).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            terms.forEachIndexed { index, term ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${if (term.deploymentMode == DeploymentMode.OFFLINE_LOCAL) "Local Mesh" else "Cloud Synced"} (${term.billingCycle.name})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black
                                )
                                if (term.isDefault) {
                                    Spacer(Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                        Text("DEFAULT", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Set Default", style = MaterialTheme.typography.labelSmall)
                                RadioButton(
                                    selected = term.isDefault,
                                    onClick = {
                                        val next = terms.mapIndexed { i, t ->
                                            t.copy(isDefault = i == index)
                                        }
                                        onUpdate(next)
                                    }
                                )
                                
                                IconButton(onClick = {
                                    val next = terms.toMutableList()
                                    next.removeAt(index)
                                    onUpdate(next)
                                }) {
                                    Icon(AppIcons.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // Deployment Mode FilterChips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mode:", style = MaterialTheme.typography.labelMedium)
                            DeploymentMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = term.deploymentMode == mode,
                                    onClick = {
                                        val next = terms.toMutableList()
                                        val cycle = if (mode == DeploymentMode.OFFLINE_LOCAL) BillingCycle.YEARLY else term.billingCycle
                                        next[index] = term.copy(deploymentMode = mode, billingCycle = cycle)
                                        onUpdate(next)
                                    },
                                    label = { Text(if (mode == DeploymentMode.CLOUD_SYNCED) "Cloud Synced" else "Local Offline Mesh (Yearly)") }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = (term.priceAmount.amountMinor / 100).toString(),
                                onValueChange = { newValue ->
                                    val minor = (newValue.toLongOrNull() ?: 0L) * 100
                                    val next = terms.toMutableList()
                                    next[index] = term.copy(
                                        priceAmount = term.priceAmount.copy(amountMinor = minor)
                                    )
                                    onUpdate(next)
                                },
                                label = { Text("Price (GHS)") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )

                            OutlinedTextField(
                                value = term.trialDays.toString(),
                                onValueChange = { newValue ->
                                    val days = newValue.toIntOrNull() ?: 0
                                    val next = terms.toMutableList()
                                    next[index] = term.copy(trialDays = days)
                                    onUpdate(next)
                                },
                                label = { Text("Trial") },
                                modifier = Modifier.weight(0.8f),
                                suffix = { Text("Days") },
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(AppIcons.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Pricing Tier")
                }
            }
        }
    }

    if (showAddDialog) {
        AddTierDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { mode, cycle ->
                val newTerm = PackageBillingTerm(
                    id = generateUUID(),
                    packageId = packageId,
                    deploymentMode = mode,
                    billingCycle = cycle,
                    priceAmount = Money(0L, "GHS"),
                    campaignId = null,
                    trialDays = 14,
                    isDefault = terms.isEmpty()
                )
                onUpdate(terms + newTerm)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddTierDialog(
    onDismiss: () -> Unit,
    onAdd: (DeploymentMode, BillingCycle) -> Unit
) {
    var selectedMode by remember { mutableStateOf(DeploymentMode.CLOUD_SYNCED) }
    var selectedCycle by remember { mutableStateOf(BillingCycle.MONTHLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Pricing Tier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select deployment mode and billing cycle:")
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeploymentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                if (mode == DeploymentMode.OFFLINE_LOCAL) selectedCycle = BillingCycle.YEARLY
                            },
                            label = { Text(if (mode == DeploymentMode.CLOUD_SYNCED) "Cloud" else "Local Mesh") }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BillingCycle.entries.forEach { cycle ->
                        val enabled = selectedMode == DeploymentMode.CLOUD_SYNCED || cycle == BillingCycle.YEARLY
                        FilterChip(
                            selected = selectedCycle == cycle,
                            onClick = { if (enabled) selectedCycle = cycle },
                            label = { Text(cycle.name) },
                            enabled = enabled
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(selectedMode, selectedCycle) }) {
                Text("Add Tier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ResourceInput(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onValueChange(it) }
            },
            modifier = Modifier.width(100.dp),
            singleLine = true
        )
    }
}
