package com.citacomsys.inventory.admin.system.presentation.smsPkg

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.platform.model.SmsPackage
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.inventory.admin.common.ui.AdminListScreenScaffold
import com.citacomsys.types.Money
import com.citacomsys.types.generateUUID
import kotlin.time.Clock
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for managing SMS bundles. 
 * Designed to be rendered as a Bottom Sheet via SystemNavEntry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSmsPackageScreen(
    onBack: () -> Unit,
    viewModel: AdminSmsPackageViewModel = koinViewModel()
) {
    var showSmsDialog by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<SmsPackage?>(null) }

    AdminListScreenScaffold(
        viewModel = viewModel,
        title = "SMS Bundle Inventory",
        onBack = onBack,
        searchPlaceholder = "Search by unit count...",
        emptyTitle = "No SMS Bundles",
        emptyDescription = "No SMS bundles have been added yet.",
        emptyIcon = AppIcons.Sms,
        topBarActions = {
            IconButton(onClick = {
                editingPackage = null
                showSmsDialog = true
            }) {
                Icon(AppIcons.Add, contentDescription = "Add Bundle")
            }
        },
        onClick = { pkg ->
            editingPackage = pkg
            showSmsDialog = true
        }
    ) { pkg, _ ->
        SmsPackageItem(
            pkg = pkg,
            onEdit = {
                editingPackage = it
                showSmsDialog = true
            }
        )
    }

    if (showSmsDialog) {
        SmsPackageFormDialog(
            initialPackage = editingPackage,
            onDismiss = { showSmsDialog = false },
            onSave = { 
                viewModel.savePackage(it)
                showSmsDialog = false
            }
        )
    }
}

@Composable
private fun SmsPackageItem(
    pkg: SmsPackage,
    onEdit: (SmsPackage) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text("${pkg.units} Units") },
            supportingContent = { Text("Price: ${pkg.price.formatted}") },
            trailingContent = {
                IconButton(onClick = { onEdit(pkg) }) {
                    Icon(AppIcons.Edit, contentDescription = "Edit")
                }
            },
            leadingContent = {
                Icon(AppIcons.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        )
    }
}

@Composable
private fun SmsPackageFormDialog(
    initialPackage: SmsPackage?,
    onDismiss: () -> Unit,
    onSave: (SmsPackage) -> Unit
) {
    var units by remember { mutableStateOf(initialPackage?.units?.toString() ?: "") }
    var priceMajor by remember { mutableStateOf(if (initialPackage != null) (initialPackage.price.amountMinor / 100).toString() else "") }
    var campaignId by remember { mutableStateOf(initialPackage?.campaignId ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPackage == null) "Add SMS Bundle" else "Edit SMS Bundle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it },
                    label = { Text("Units (SMS Count)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceMajor,
                    onValueChange = { priceMajor = it },
                    label = { Text("Price (GHS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = campaignId,
                    onValueChange = { campaignId = it },
                    label = { Text("Campaign ID (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val u = units.toIntOrNull() ?: 0
                    val p = (priceMajor.toDoubleOrNull() ?: 0.0) * 100
                    val newPackage = SmsPackage(
                        id = initialPackage?.id ?: generateUUID(),
                        units = u,
                        price = Money(p.toLong(), "GHS"),
                        campaignId = campaignId.takeIf { it.isNotBlank() },
                        createdAt = initialPackage?.createdAt ?: Clock.System.now(),
                        updatedAt = Clock.System.now()
                    )
                    onSave(newPackage)
                },
                enabled = units.toIntOrNull() != null && priceMajor.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
