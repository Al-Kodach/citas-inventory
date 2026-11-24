package com.citacomsys.inventory.admin.security.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.components.LazyListView
import com.citacomsys.core.spi.scenes.LocalBackButtonVisibility
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.inventory.admin.device.presentation.DeviceHardwareView
import com.citacomsys.inventory.admin.security.components.SecurityEventRow
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDeviceDetailsScreen(
    deviceId: String,
    onNavigateToTrace: (String) -> Unit,
    goBack: () -> Unit,
    viewModel: SecurityDeviceDetailsViewModel = koinInject()
) {
    val detailsState by viewModel.selectedItemState.collectAsStateWithLifecycle()
    val securityEventsState by viewModel.endpointEvents.collectAsStateWithLifecycle()

    LaunchedEffect(deviceId) {
        viewModel.loadDetails(deviceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Node Details")
                },
                navigationIcon = {
                    if (LocalBackButtonVisibility.current) {
                        BackButton(goBack)
                    }
                }
            )
        }
    ) { paddingValues ->
        UiResultView(
            state = detailsState,
            onRetry = { viewModel.loadDetails(deviceId) },
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
        ) { device ->
            LazyListView(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Embedded Metadata Component
                item {
                    DeviceHardwareView(device = device)
                }

                // Device-Specific SIEM / Activity Section
                item {
                    Text("Endpoint Security Events", style = MaterialTheme.typography.titleLarge)
                }

                item {
                    UiResultView(state = securityEventsState) { events ->
                        if (events.isEmpty()) {
                            Text("No security incidents recorded for this endpoint.")
                        } else {
                            events.forEach { logEntry ->
                                SecurityEventRow(
                                    entry = logEntry,
                                    onClick = { onNavigateToTrace(logEntry.requestId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
