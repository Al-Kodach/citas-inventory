package com.citacomsys.inventory.admin.system.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.api.platform.model.SystemSettings
import com.citacomsys.core.resources.AppIcons
import com.citacomsys.core.ui.UiResultView
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for managing global platform configurations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSystemSettingsScreen(
    onBack: () -> Unit,
    viewModel: AdminSystemSettingsViewModel = koinViewModel()
) {
    val state by viewModel.settingsState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Platform Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            UiResultView(
                state = state,
                onRetry = { viewModel.loadSettings() }
            ) { settings ->
                SettingsContent(
                    initialSettings = settings,
                    onSave = { viewModel.updateSettings(it) }
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    initialSettings: SystemSettings,
    onSave: (SystemSettings) -> Unit
) {
    var settings by remember(initialSettings) { mutableStateOf(initialSettings) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Payment Configuration
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "MoMo Collection Settings", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = settings.momoCollectionNumber,
                        onValueChange = { settings = settings.copy(momoCollectionNumber = it) },
                        label = { Text("Collection Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(AppIcons.Call, null) },
                        shape = MaterialTheme.shapes.medium
                    )

                    OutlinedTextField(
                        value = settings.momoCollectionName,
                        onValueChange = { settings = settings.copy(momoCollectionName = it) },
                        label = { Text("Recipient Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(AppIcons.Person, null) },
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            // 2. Support & Operations
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Support & Operations", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = settings.supportPhoneNumber,
                        onValueChange = { settings = settings.copy(supportPhoneNumber = it) },
                        label = { Text("Support Contact Line") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Maintenance Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Prevents new workspace logins during critical updates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.maintenanceWindowEnabled,
                            onCheckedChange = { settings = settings.copy(maintenanceWindowEnabled = it) }
                        )
                    }
                }
            }

            // 3. Background Infrastructure
            Button(
                onClick = { onSave(settings) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Apply Global Settings", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}
