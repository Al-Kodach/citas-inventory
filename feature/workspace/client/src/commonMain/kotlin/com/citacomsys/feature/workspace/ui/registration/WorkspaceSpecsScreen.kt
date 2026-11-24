package com.citacomsys.feature.workspace.ui.registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.layout.AdaptiveRegistrationLayout
import com.citacomsys.feature.workspace.ui.registration.orchestrator.WorkspaceSetupDraft
import com.citacomsys.types.ProductType
import com.citacomsys.shared.displayName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkspaceSpecsScreen(
    companyName: String? = null,
    draft: WorkspaceSetupDraft,
    onUpdate: (WorkspaceSetupDraft) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var businessType by remember { mutableStateOf(draft.businessType) }
    var name by remember { mutableStateOf(draft.name) }
    var street by remember { mutableStateOf(draft.street) }
    var city by remember { mutableStateOf(draft.city) }
    var mainBranchName by remember { mutableStateOf(draft.mainBranchName.ifBlank { draft.name }) }
    var email by remember { mutableStateOf(draft.email ?: "") }
    var phone by remember { mutableStateOf(draft.phone ?: "") }
    var website by remember { mutableStateOf(draft.website ?: "") }
    var taxId by remember { mutableStateOf(draft.taxId ?: "") }

    var expanded by remember { mutableStateOf(false) }

    AdaptiveRegistrationLayout(
        title = "Business & Store Setup",
        onBack = onBack,
        icon = AppIcons.Storefront,
        instructionTitle = "Configure your industry platform",
        instructionText = "Provide the operating details for this business and its initial branches branch. You can save multiple distinct business units under your company (e.g. Apex - Cosmetics, Apex - Food)."
    ) {
        if (!companyName.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        AppIcons.Business,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Parent Company: $companyName",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = businessType.displayName(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Business Type (Industry)*") },
                leadingIcon = { Icon(AppIcons.Category, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ProductType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName()) },
                        enabled = type.isActive,
                        onClick = {
                            businessType = type
                            onUpdate(
                                draft.copy(businessType = type)
                            )
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onUpdate(draft.copy(name = it))
            },
            label = { Text("Business Name*") },
            placeholder = { Text("e.g. Apex - Cosmetics or Apex - Food") },
            leadingIcon = { Icon(AppIcons.Business, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Words
            )
        )

        // Quick Suffix Append Suggestion Chips based on selected business type
        val appendSuggestions = when (businessType) {
            ProductType.RETAIL -> listOf(
                "Cosmetics",
                "Food & Groceries",
                "Electronics",
                "Fashion",
                "Mart"
            )

            ProductType.PHARMACY -> listOf("Central", "Express", "Clinic", "Dispensing")
            ProductType.RESTAURANT -> listOf("Café", "Bistro", "Grill", "Bakery")
            ProductType.AUTOMOTIVE -> listOf("Parts", "Service", "Hub")
            ProductType.AGRICULTURE -> listOf("Depot", "Farm Supply", "Inputs")
            ProductType.INVENTORY -> listOf("Central Warehouse", "Distribution Hub", "Storage")
        }

        if (appendSuggestions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Quick Suffix:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                appendSuggestions.forEach { suffix ->
                    SuggestionChip(
                        onClick = {
                            val baseName = name.substringBefore(" - ").trim()
                            val newName =
                                if (baseName.isNotBlank() && !baseName.contains(suffix)) "$baseName - $suffix" else suffix
                            name = newName
                            onUpdate(draft.copy(name = newName))
                        },
                        label = { Text(suffix, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
        }

        HorizontalDivider(
            Modifier.padding(vertical = 8.dp),
            color = DividerDefaults.color.copy(alpha = 0.5f)
        )

        Text(
            text = "Branch / Store Details",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = mainBranchName,
            onValueChange = {
                mainBranchName = it
                onUpdate(draft.copy(mainBranchName = it))
            },
            label = { Text("Branch / Store Name*") },
            supportingText = { Text("Gye Nyame Enterprises or $name") },
            leadingIcon = { Icon(AppIcons.Storefront, null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Words
            )
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = street,
                onValueChange = {
                    street = it
                    onUpdate(
                        draft.copy(street = it)
                    )
                },
                label = { Text("Street Address") },
                supportingText = { Text("e.g. 123 Main Street") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words

                )
            )
            OutlinedTextField(
                value = city,
                onValueChange = {
                    city = it
                    onUpdate(draft.copy(city = it))
                },
                label = { Text("City*") },
                supportingText = { Text("e.g. Accra") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words

                )
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    onUpdate(draft.copy(phone = it.ifBlank { null }))
                },
                prefix = { Text("+233") },
                label = { Text("Contact Phone") },
                leadingIcon = { Icon(AppIcons.Call, null) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    onUpdate(draft.copy(email = it.ifBlank { null }))
                },
                label = { Text("Email") },
                leadingIcon = { Icon(AppIcons.Email, null) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = website,
                onValueChange = {
                    website = it
                    onUpdate(draft.copy(website = it.ifBlank { null }))
                },
                label = { Text("Website (Optional)") },
                placeholder = { Text("www.gyenyameenterprises.com") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = taxId,
                onValueChange = {
                    taxId = it
                    onUpdate(draft.copy(taxId = it.ifBlank { null }))
                },
                label = { Text("TIN / Tax ID (Optional)") },
                placeholder = { Text("e.g. C000123456") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                )
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "💡 Multi-Branch Note: This configures your branches location. Customer receipts, invoices, and purchase orders for this branch will print these contact details. You can save additional branches or stores later in Branch Management.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = name.isNotBlank() && city.isNotBlank() && mainBranchName.isNotBlank(),
            shape = MaterialTheme.shapes.large
        ) {
            Text(
                "Select Growth Plan",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
