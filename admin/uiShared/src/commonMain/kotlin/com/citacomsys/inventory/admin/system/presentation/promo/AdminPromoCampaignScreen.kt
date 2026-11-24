package com.citacomsys.inventory.admin.system.presentation.promo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.api.platform.model.Promotion
import com.citacomsys.api.platform.model.PromotionCampaign
import com.citacomsys.api.platform.model.PromotionType
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.inventory.admin.common.ui.AdminListScreenScaffold
import com.citacomsys.shared.displayName
import kotlin.time.Clock
import kotlin.time.Instant
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for managing platform-wide marketing campaigns and discount codes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPromoCampaignScreen(
    onBack: () -> Unit,
    viewModel: AdminPromoCampaignViewModel = koinViewModel()
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var campaignToEdit by remember { mutableStateOf<PromotionCampaign?>(null) }

    AdminListScreenScaffold(
        viewModel = viewModel,
        title = "Promotion Campaigns",
        onBack = onBack,
        searchPlaceholder = "Search by id or code...",
        emptyTitle = "No Campaigns Found",
        emptyDescription = "Launch a new marketing drive by adding a campaign code and specific offers.",
        emptyIcon = AppIcons.AutoAwesome,
        topBarActions = {
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(AppIcons.Add, contentDescription = "New Campaign")
            }
        },
        onClick = { campaignToEdit = it }
    ) { campaign, _ ->
        CampaignCard(
            campaign = campaign,
            onToggle = { viewModel.toggleCampaign(campaign) },
            onEdit = { campaignToEdit = campaign }
        )
    }

    // --- DIALOGS ---

    if (showCreateDialog) {
        CampaignEditorDialog(
            title = "New Promotion Campaign",
            onDismiss = { showCreateDialog = false },
            onSave = {
                viewModel.saveCampaign(it)
                showCreateDialog = false
            }
        )
    }

    campaignToEdit?.let { campaign ->
        CampaignEditorDialog(
            title = "Edit Campaign",
            campaign = campaign,
            onDismiss = { campaignToEdit = null },
            onSave = {
                viewModel.saveCampaign(it)
                campaignToEdit = null
            }
        )
    }
}

@Composable
private fun CampaignCard(
    campaign: PromotionCampaign,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        ListItem(
            headlineContent = { Text(campaign.name, fontWeight = FontWeight.Bold) },
            supportingContent = {
                Column {
                    Text(
                        text = "Code: ${campaign.code}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Active: ${
                            campaign.startsAt.toString().take(10)
                        } to ${campaign.expiresAt?.toString()?.take(10) ?: "Indefinite"}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (campaign.promotions.isNotEmpty()) {
                        Text(
                            text = "${campaign.promotions.size} offers: ${campaign.promotions.joinToString { it.name }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }
            },
            leadingContent = {
                Icon(
                    imageVector = AppIcons.AutoAwesome,
                    contentDescription = null,
                    tint = if (campaign.active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                )
            },
            trailingContent = {
                Switch(
                    checked = campaign.active,
                    onCheckedChange = { onToggle() }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CampaignEditorDialog(
    title: String,
    campaign: PromotionCampaign? = null,
    onDismiss: () -> Unit,
    onSave: (PromotionCampaign) -> Unit
) {
    var name by remember { mutableStateOf(campaign?.name ?: "") }
    var code by remember { mutableStateOf(campaign?.code ?: "") }
    var description by remember { mutableStateOf(campaign?.description ?: "") }
    var active by remember { mutableStateOf(campaign?.active ?: true) }
    var startsAtStr by remember {
        mutableStateOf(
            campaign?.startsAt?.toString()?.take(10) ?: Clock.System.now().toString().take(10)
        )
    }
    var expiresAtStr by remember { mutableStateOf(campaign?.expiresAt?.toString()?.take(10) ?: "") }
    var promotions by remember { mutableStateOf(campaign?.promotions ?: emptyList()) }

    var showAddPromo by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        modifier = Modifier.widthIn(max = 500.dp),
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Campaign Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Global Promo Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startsAtStr,
                        onValueChange = { startsAtStr = it },
                        label = { Text("Start Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = expiresAtStr,
                        onValueChange = { expiresAtStr = it },
                        label = { Text("Expiry Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Offers", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { showAddPromo = true }) {
                        Icon(AppIcons.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    promotions.forEach { promo ->
                        InputChip(
                            selected = true,
                            onClick = { promotions = promotions - promo },
                            label = { Text("${promo.name}: ${promo.value}") },
                            trailingIcon = { Icon(AppIcons.Close, null, Modifier.size(16.dp)) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = active, onCheckedChange = { active = it })
                    Text(
                        "Campaign is currently active",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            val canSave =
                name.isNotBlank() && code.isNotBlank() && runCatching { Instant.parse("${startsAtStr}T00:00:00Z") }.isSuccess
            Button(
                onClick = {
                    onSave(
                        PromotionCampaign(
                            id = campaign?.id ?: "",
                            code = code,
                            name = name,
                            description = description,
                            active = active,
                            startsAt = Instant.parse("${startsAtStr}T00:00:00Z"),
                            expiresAt = expiresAtStr.takeIf { it.isNotBlank() }?.let {
                                Instant.parse("${it}T23:59:59Z")
                            },
                            promotions = promotions
                        )
                    )
                },
                enabled = canSave
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )

    if (showAddPromo) {
        PromotionAddDialog(
            onDismiss = { showAddPromo = false },
            onConfirm = {
                promotions = promotions + it
                showAddPromo = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromotionAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (Promotion) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PromotionType.PERCENTAGE_DISCOUNT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Specific Offer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Offer Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = type.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Offer Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        ).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        PromotionType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName()) },
                                onClick = { type = t; expanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Value") },
                    placeholder = { Text(if (type == PromotionType.PERCENTAGE_DISCOUNT) "% (e.g. 10.0)" else "Units/Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Promotion(
                            code = code,
                            name = name,
                            type = type,
                            value = value.toDoubleOrNull() ?: 0.0
                        )
                    )
                },
                enabled = name.isNotBlank() && code.isNotBlank() && value.toDoubleOrNull() != null
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}
