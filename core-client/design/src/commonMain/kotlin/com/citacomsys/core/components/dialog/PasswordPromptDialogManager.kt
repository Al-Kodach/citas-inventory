package com.citacomsys.core.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

data class PasswordPromptData(
    val password: String = "",
    val reason: String = ""
)

/**
 * A specialized dialog manager for sensitive actions requiring a password and an optional reason.
 */
class PasswordPromptManager(
    private val scope: CoroutineScope,
    private val onPasswordConfirmed: suspend (password: String, reason: String?) -> Result<Unit>
) : DialogManager<PasswordPromptData>(), KoinComponent {

    override val _dialogStateFlow = MutableStateFlow<DialogState<PasswordPromptData?>>(
        DialogState(data = PasswordPromptData())
    )

    fun show(
        title: String = "Authorize Action",
        description: String? = "Please confirm your password to proceed.",
        includeReason: Boolean = false,
        onCancel: () -> Unit = {}
    ) {
        showDialog(
            initialData = PasswordPromptData(),
            dialogContent = { uiState, data ->
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

                    OutlinedTextField(
                        value = data?.password ?: "",
                        onValueChange = { updateData { it?.copy(password = it.password) } },
                        label = { Text("Administrator Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (includeReason) {
                        OutlinedTextField(
                            value = data?.reason ?: "",
                            onValueChange = { updateData { it?.copy(reason = it.reason) } },
                            label = { Text("Reason for Action") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            placeholder = { Text("Enter a technical justification...") }
                        )
                    }

                    if (uiState.errorMessage != null) {
                        Text(
                            uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            onCancel()
                            dismissAndClearState()
                        }, enabled = !uiState.isLoading) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    updateRuntimeState { it.copy(isLoading = true) }
                                    val result = onPasswordConfirmed(
                                        data?.password ?: "",
                                        data?.reason?.ifBlank { null })
                                    if (result.isSuccess) {
                                        dismissAndClearState()
                                    } else {
                                        updateRuntimeState {
                                            it.copy(
                                                errorMessage = result.exceptionOrNull()?.message,
                                                isLoading = false
                                            )
                                        }
                                    }
                                }
                            },
                            enabled = !uiState.isLoading && (data?.password?.isNotBlank() == true) && (!includeReason || data.reason.isNotBlank())
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        )
    }
}
