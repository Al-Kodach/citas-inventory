package com.citacomsys.module.inventory.ui.screen.batch

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.PrimaryButton
import com.citacomsys.core.components.SecondaryButton
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.management.layout.ModalScaffold
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.core.ui.result.isLoading
import com.citacomsys.module.inventory.request.PriceType
import org.koin.compose.koinInject

/**
 * Screen for updating a batch's price (Cost or Selling).
 * Optimized for transient administrative tasks with a stationary Top Bar and adaptive layout.
 */
@Composable
fun PriceUpdateFormView(
    batchId: String,
    priceType: PriceType,
    onCancel: () -> Unit,
    viewModel: BatchPriceEditViewModel = koinInject()
) {
    val batchState by viewModel.batchState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val newPriceStr by viewModel.newPriceStr.collectAsStateWithLifecycle()
    val reason by viewModel.reason.collectAsStateWithLifecycle()
    val isCostPrice = priceType == PriceType.COST


    LaunchedEffect(batchId, priceType) {
        viewModel.loadBatch(batchId, priceType)
    }

    UiResultView(
        state = batchState,
        onRetry = { viewModel.loadBatch(batchId, priceType) }
    ) { batch ->
        val currencyCode = batch.sellingPrice.currencyCode

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Batch: ${batch.batchNumber}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // --- OPERATIONAL GUIDANCE ---
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        AppIcons.Info,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isCostPrice)
                            "Updating cost price will adjust the valuation of this specific batch. A justification is required for the ledger."
                        else
                            "Updating the selling price will affect all future transactions from this specific batch.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // --- INPUT FIELDS ---
            OutlinedTextField(
                value = newPriceStr,
                onValueChange = { viewModel.newPriceStr.value = it },
                label = { Text("New ${if (isCostPrice) "Cost" else "Selling"} Price") },
                prefix = { Text("$currencyCode ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (isCostPrice) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { viewModel.reason.value = it },
                    label = { Text("Reason for Adjustment*") },
                    placeholder = { Text("e.g. Correcting entry error") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            Spacer(Modifier.weight(1f))

            // Error display
            saveState.getErrorOrNull()?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }


            // --- ACTIONS ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(56.dp),
                )

                PrimaryButton(
                    text = "Update price",
                    onClick = {
                        viewModel.savePriceUpdate(priceType, onSuccess = onCancel)
                    },
                    isLoading = saveState.isLoading,
                    modifier = Modifier.weight(1f).height(56.dp),
                )
            }
        }
    }
}
