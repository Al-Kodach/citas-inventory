package com.citacomsys.sales.ui.screen.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.citacomsys.core.components.PrimaryButton
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.result.UiResult
import com.citacomsys.core.ui.result.getErrorOrNull
import com.citacomsys.sales.ui.screen.share.SmsReceiptDialogState
import com.citacomsys.types.PhoneNumber

@Composable
fun DigitalReceiptDialog(
    state: SmsReceiptDialogState,
    onPhoneChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    onSmsTop: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row {
                Text("Send Digital Receipt")
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(state.state is UiResult.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter the patient's mobile number to send a summary of this transaction via SMS.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '+' }) {
                            onPhoneChange(input)
                        }
                    },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(AppIcons.Call, null) },
                    placeholder = { Text("e.g. 020XXXXXXX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                state.state.getErrorOrNull()?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                AnimatedVisibility(state.smsTopAppRequired) {
                    Button(
                        onClick = onSmsTop,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Top Up SMS Credits")
                    }
                }
            }
        },
        confirmButton = {
            val phone = try {
                PhoneNumber.create(state.phone)
            } catch (e: Exception) {
                null
            }
            PrimaryButton(
                onClick = onSend,
                text = "Send Receipt",
                isEnabled = phone != null && state.state !is UiResult.Loading,
                isLoading = state.state is UiResult.Loading,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
