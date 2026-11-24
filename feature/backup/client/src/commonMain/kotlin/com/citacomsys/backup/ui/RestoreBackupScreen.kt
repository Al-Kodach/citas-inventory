package com.citacomsys.backup.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.backup.api.RestoreScope
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.spi.LocalNavigator
import com.citacomsys.core.ui.management.layout.ModalScaffold
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupScreen(
    initPassword: String? = null,
    onRestoreCompleted: (() -> Unit)? = null,
    viewModel: BackupRestoreViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    var password by remember { mutableStateOf(initPassword ?: "") }
    var restoreScope by remember { mutableStateOf(RestoreScope.BOTH) }

    LaunchedEffect(Unit) {
        if (initPassword != null) {
            viewModel.restoreBackup(password, restoreScope) {
                onRestoreCompleted?.invoke() ?: navigator.goBack()
            }
        }
    }

    ModalScaffold(
        title = "Restore Backup",
        onBack = onRestoreCompleted ?: { navigator.goBack() },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Backup Password")
                },
                visualTransformation = PasswordVisualTransformation()
            )

            Text(
                text = "Restore Scope",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RestoreScopeOption(
                    title = "Restore Both (Inventory & Customer Data)",
                    description = "Full system restore of business catalog and CRM records.",
                    selected = restoreScope == RestoreScope.BOTH,
                    onClick = { restoreScope = RestoreScope.BOTH }
                )

                RestoreScopeOption(
                    title = "Restore Inventory Only",
                    description = "Restores inventory database and catalog, keeping existing customer data intact.",
                    selected = restoreScope == RestoreScope.INVENTORY,
                    onClick = { restoreScope = RestoreScope.INVENTORY }
                )

                RestoreScopeOption(
                    title = "Restore Customers Only (CRM / PII)",
                    description = "Restores customer database only, leaving inventory and catalog intact.",
                    selected = restoreScope == RestoreScope.CUSTOMER,
                    onClick = { restoreScope = RestoreScope.CUSTOMER }
                )
            }

            BackupRestoreStatus(uiState)

            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = password.length >= 6,
                onClick = {
                    viewModel.restoreBackup(password, restoreScope) {
                        onRestoreCompleted?.invoke() ?: navigator.goBack()
                    }
                }
            ) {
                Text("Restore")
            }
        }
    }
}

@Composable
fun RestoreScopeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BackupRestoreStatus(state: BackupRestoreUiState) {
    when (state) {
        BackupRestoreUiState.Idle -> Unit

        is BackupRestoreUiState.Processing -> {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = state.title, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is BackupRestoreUiState.Success -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = state.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        is BackupRestoreUiState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
