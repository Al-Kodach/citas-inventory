package com.citacomsys.feature.billing.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.api.billing.BillingRecord
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SubmitPaymentDialog(
    invoice: BillingRecord,
    state: UiResult<Unit>,
    onDismiss: () -> Unit,
    onConfirm: (transactionId: String, payerName: String) -> Unit
) {
    var transactionId by remember { mutableStateOf("") }
    var payerName by remember { mutableStateOf("") }

    // Validation logic: Most MoMo Transaction IDs are alphanumeric and at least 8 chars
    val isTxIdValid = transactionId.length >= 6 && transactionId.isNotEmpty()
    val isNameValid = payerName.isNotBlank()
    val canSubmit = isTxIdValid && isNameValid && state !is UiResult.Loading

    AlertDialog(
        onDismissRequest = { if (state !is UiResult.Loading) onDismiss() },
        icon = { Icon(AppIcons.Info, contentDescription = null) },
        title = {
            Text(
                text = "Confirm Payment",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Please provide the Transaction ID and id from your MoMo confirmation SMS for reference:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = invoice.referenceCode,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it.uppercase() },
                    label = { Text("MoMo Transaction ID") },
                    placeholder = { Text("From your network SMS") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    isError = transactionId.isNotEmpty() && !isTxIdValid,
                    supportingText = {
                        if (transactionId.isNotEmpty() && !isTxIdValid) {
                            Text("Please enter a valid Transaction ID")
                        }
                    }
                )

                OutlinedTextField(
                    value = payerName,
                    onValueChange = { payerName = it },
                    label = { Text("Sender Name") },
                    placeholder = { Text("e.g. John Doe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = payerName.isNotEmpty() && !isNameValid
                )

                if (state is UiResult.Error) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(transactionId, payerName) },
                enabled = canSubmit,
                modifier = Modifier.widthIn(min = 100.dp)
            ) {
                if (state is UiResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit Details")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = state !is UiResult.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}
