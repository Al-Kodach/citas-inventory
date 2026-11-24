package com.citacomsys.inventory.admin.security.audit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.inventory.admin.security.components.SecurityEventRow
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityTraceDetailsScreen(
    requestId: String,
    onBack: () -> Unit,
    viewModel: SecurityAuditViewModel = koinViewModel()
) {
    val traceState by viewModel.traceDetails.collectAsStateWithLifecycle()

    LaunchedEffect(requestId) {
        viewModel.loadRequestTrace(requestId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Incident Trace", style = MaterialTheme.typography.titleMedium)
                        Text(
                            requestId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        UiResultView(state = traceState, modifier = Modifier.padding(padding)) { timeline ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        "Sequence of Events",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                items(timeline) { entry ->
                    SecurityEventRow(entry = entry)
                }
            }
        }
    }
}