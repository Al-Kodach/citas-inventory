package com.citacomsys.client.support.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.citacomsys.backup.ui.BackupRestoreStatus
import com.citacomsys.backup.ui.BackupRestoreUiState
import com.citacomsys.backup.ui.BackupRestoreViewModel
import com.citacomsys.core.resources.AppIcons
import org.koin.compose.koinInject

@Composable
fun DataRestoreScreen(
    viewModel: BackupRestoreViewModel = koinInject(),
    onSkipAndStartFresh: () -> Unit,
    onRestoreSuccessful: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var password by remember { mutableStateOf("") }
    val isProcessing = uiState is BackupRestoreUiState.Processing

    // Reset the state to Idle whenever this setup layout mounts
    LaunchedEffect(Unit) {
        viewModel.resetStateToIdle()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = AppIcons.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Text(
                text = "Restore Your Pharmacy Data",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Enter the password used to protect your encrypted archive, then select your historical '.cita-backup' file to recover your operational data.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Backup Encryption Password") },
                enabled = !isProcessing,
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = password.length >= 6 && !isProcessing,
                    onClick = {
                        // Pass completion callback directly into the model handler
                        viewModel.restoreBackup(password = password) {
                            onRestoreSuccessful()
                        }
                    }
                ) {
                    Text("Select File & Restore Data")
                }

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    onClick = onSkipAndStartFresh
                ) {
                    Text(
                        text = "Skip Restoration, Start Fresh",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Renders your custom rich progress, success, or error status boxes
            BackupRestoreStatus(uiState)
        }
    }
}
