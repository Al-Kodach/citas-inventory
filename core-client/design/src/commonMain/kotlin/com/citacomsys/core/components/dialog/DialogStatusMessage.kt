package com.citacomsys.core.components.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A helper composable to display either a success or an error message.
 */
@Composable
fun DialogStatusMessage(uiState: DialogManager.DialogRuntimeState) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.successMessage != null) {
            Text(
                text = uiState.successMessage,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}