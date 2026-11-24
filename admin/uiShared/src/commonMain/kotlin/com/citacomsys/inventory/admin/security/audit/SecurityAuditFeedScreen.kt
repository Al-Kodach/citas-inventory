package com.citacomsys.inventory.admin.security.audit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.citacomsys.core.components.BackButton
import com.citacomsys.core.components.LazyVerticalGridView
import com.citacomsys.core.ui.UiResultView
import com.citacomsys.core.ui.management.layout.DetailsPlaceholder
import com.citacomsys.inventory.admin.security.components.SecurityEventRow
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAuditFeedScreen(
    onNavigateToTrace: (String) -> Unit,
    goback: () -> Unit,
    viewModel: SecurityAuditViewModel = koinViewModel()
) {
    val feedState by viewModel.auditFeed.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadGlobalFeed()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "Security Event Feed",
                    style = MaterialTheme.typography.headlineMedium,
                )
            },
            navigationIcon = { BackButton(goback) }
        )

        Spacer(Modifier.height(16.dp))

        UiResultView(
            state = feedState,
            onRetry = { viewModel.loadGlobalFeed() }
        ) { events ->
            LazyVerticalGridView(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(events) { event ->
                    SecurityEventRow(
                        entry = event,
                        onClick = {
                            onNavigateToTrace(event.requestId)
                        }
                    )
                }
            }
        }
    }
}