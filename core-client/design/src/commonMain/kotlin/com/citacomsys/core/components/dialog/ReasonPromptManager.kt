package com.citacomsys.core.components.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Specialized manager for non-sensitive actions requiring a technical reason.
 * Follows the generic DialogManager pattern.
 */
class ReasonPromptManager() : DialogManager<String>() {
    override val _dialogStateFlow = MutableStateFlow<DialogState<String?>>(
        DialogState(data = null)
    )

    fun show(title: String, description: String? = null, onConfirm: (String) -> Unit) {
        showDialog(
            initialData = "",
            dialogContent = { uiState, data ->
                ReasonPromptDialogContent(
                    title = title,
                    description = description,
                    uiState = uiState,
                    reason = data ?: "",
                    onReasonChange = { newValue -> updateData { newValue } },
                    onConfirm = {
                        onConfirm(data ?: "")
                        dismissAndClearState()
                    },
                    onDismiss = { dismissAndClearState() }
                )
            }
        )
    }
}

@Composable
fun ReasonPromptDialogContent(
    title: String,
    description: String?,
    uiState: DialogManager.DialogRuntimeState,
    reason: String,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            label = { Text("Reason for action") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("Please provide a technical justification...") },
            isError = uiState.errorMessage != null
        )

        if (uiState.errorMessage != null) {
            Text(
                uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onConfirm, enabled = reason.isNotBlank() && !uiState.isLoading) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirm")
                }
            }
        }
    }
}
