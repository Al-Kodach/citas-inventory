package com.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AppChangePinDialog(
    onConfirm: (String) -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text("Update Admin PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Your PIN has expired or needs to be set. Please enter a new 6-digit PIN.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length != 6) {
                        error = "PIN must be 6 digits"
                    } else if (newPin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        onConfirm(newPin)
                    }
                },
                enabled = newPin.isNotEmpty() && confirmPin.isNotEmpty()
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = onDismiss?.let {
            { TextButton(onClick = it) { Text("Cancel") } }
        }
    )
}
