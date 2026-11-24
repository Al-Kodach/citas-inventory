package com.citacomsys.core.ui.productPackage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.citacomsys.api.platform.uiModel.BillTermUiState
import com.citacomsys.api.platform.uiModel.ProductPackageUiState
import com.citacomsys.core.components.AppCard
import com.citacomsys.core.resources.AppIcons

@Composable
fun SelectablePackageCard(
    details: ProductPackageUiState,
    selectedBillingTerm: BillTermUiState?,
    selectedPackage: ProductPackageUiState?,
    onSelect: () -> Unit,
    onTermSelect: (BillTermUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = details.id == selectedPackage?.id

    AppCard(
        onClick = onSelect,
        isSelected = isSelected,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth()
//            .heightIn(min = 650.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (details.recommended) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "MOST POPULAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = details.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )

            val isOffline = details.deploymentMode.isOffline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = if (isOffline) AppIcons.PhonelinkOff else AppIcons.CloudDone,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isOffline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isOffline) "Offline-First" else "Cloud-Synced",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOffline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        details.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                minLines = 2
            )
        }

        Spacer(Modifier.height(24.dp))

        // --- BILLING CYCLES ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "CHOOSE BILLING PLAN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    details.billingTerms.forEach { term ->
                        val isTermSelected = term.id == selectedBillingTerm?.id
                        val canSelect = term.packageId == selectedPackage?.id

                        SelectableBillingTermCard(
                            billingTerm = term,
                            isSelected = isTermSelected,
                            canSelect = canSelect,
                            onSelect = { onTermSelect(term) },
                            modifier = Modifier
                                .widthIn(max = 190.dp)
                                .weight(1f)
                        )
                    }
                }

                // --- PROMO EFFECTS (FOOTER OF BILLING SECTION) ---
                selectedBillingTerm?.let { term ->
                    Spacer(Modifier.height(8.dp))
                    if (term.hasPromo) {
                        PromoEffectBadge(term)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(24.dp))

        // Resource Summary
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FeatureDetail("Users", "${details.maxUsers}")
            FeatureDetail("Devices", "${details.maxDevices}")
            FeatureDetail("Branches", "${details.maxBranches}")
            FeatureDetail(
                "SMS",
                if (details.smsQuotaPerCycle > 0) "${details.smsQuotaPerCycle}" else "Pay-go"
            )
            FeatureDetail(
                "Max Product Catalog",
                if (details.maxProductsLimit > 0) "${details.maxProductsLimit}" else "Unlimited"
            )
            FeatureDetail(
                "Max Active batches Limit",
                if (details.maxActiveBatchesLimit > 0) "${details.maxActiveBatchesLimit}" else "Unlimited"
            )
            FeatureDetail(
                "Retention Policy",
                "${details.historyRetentionMonths} months"
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(24.dp))

        if (!isSelected) {
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = if (details.recommended) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
            ) {
                Text(text = "Select ${details.name}", fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        AppIcons.Check,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "PLAN SELECTED",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@Composable
fun PromoEffectBadge(term: BillTermUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //  Promo Expiry Countdown
        if (term.remainingDaysToEnd != null || term.remainingDaysToEnd!! > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Offer ends in ${term.remainingDaysToEnd} day(s)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        //  Dynamic Promotion Effects
        term.effects.forEach { effect ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = AppIcons.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = effect.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (effect.description.isNotBlank()) {
                        Text(
                            text = effect.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureDetail(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}