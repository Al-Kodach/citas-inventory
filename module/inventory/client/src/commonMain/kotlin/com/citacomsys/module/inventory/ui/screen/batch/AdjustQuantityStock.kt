package com.citacomsys.module.inventory.ui.screen.batch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.SubmitButtons

/**
 * A focused dialog for performing manual stock adjustments.
 * Supports adding or removing stock with a required reason for the audit log.
 */
@Composable
fun AdjustQuantityStock(
    currentQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, reason: String) -> Unit
) {
    var adjustmentAmount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Current stock level: $currentQuantity units",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = adjustmentAmount,
            onValueChange = {
                if (it.all { it.isDigit() } || it.startsWith("-")) {
                    adjustmentAmount = it
                }
            },
            label = { Text("Quantity Adjustment") },
            placeholder = { Text("e.g. -5 for damage, 10 for correction") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Adjustment Reason*") },
            placeholder = { Text("Required for audit trail") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(Modifier.weight(1f))
        SubmitButtons(
            onConfirmationClick = {
                val qty = adjustmentAmount.toIntOrNull() ?: 0
                if (qty != 0 && reason.isNotBlank()) {
                    onConfirm(qty, reason)
                }
            },
            onDismiss = onDismiss,
            enabled = adjustmentAmount.isNotBlank() && reason.isNotBlank() && adjustmentAmount.toIntOrNull() != null,
        )
    }
}
