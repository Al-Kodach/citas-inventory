package com.citacomsys.app.shared.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.citacomsys.database.PendingRequestEntity

@Composable
fun OutboxConflictResolutionDialog(
    item: PendingRequestEntity,
    onDismiss: () -> Unit,
    onResolveAndRetry: (editedPayload: String?) -> Unit,
    onDrop: () -> Unit
) {
    var editablePayload by remember { mutableStateOf(item.payload ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Outbox Conflict Resolution") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Action: ${item.eventAction} (${item.entityType})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Endpoint: ${item.endpoint}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                item.lastFailureReason?.let { reason ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Diagnostic Error: $reason",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text("Edit Payload / Resolve Conflict:", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = editablePayload,
                    onValueChange = { editablePayload = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolveAndRetry(editablePayload.takeIf { it.isNotBlank() }) }
            ) {
                Text("Resolve & Requeue")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDrop) {
                    Text("Drop", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
