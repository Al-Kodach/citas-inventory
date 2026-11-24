package com.citacomsys.feature.support.client.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.citacomsys.feature.support.api.TicketPriority
import com.citacomsys.feature.support.api.TicketType
import com.citacomsys.feature.support.client.ui.SupportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitTicketScreen(
    viewModel: SupportViewModel,
    initialType: String = "SUPPORT",
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember {
        mutableStateOf(
            try { TicketType.valueOf(initialType) } catch (e: Exception) { TicketType.SUPPORT }
        )
    }
    var selectedPriority by remember { mutableStateOf(TicketPriority.MEDIUM) }
    var rating by remember { mutableStateOf(5) }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submit ${selectedType.name}") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector
            Text("Type of Request", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TicketType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type.name) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Summary") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detailed Description / Feedback") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            if (selectedType == TicketType.REVIEW) {
                Text("Rating (1 to 5 Stars)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..5).forEach { star ->
                        FilterChip(
                            selected = rating == star,
                            onClick = { rating = star },
                            label = { Text("$star ⭐") }
                        )
                    }
                }
            }

            Text("Priority", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TicketPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { selectedPriority = priority },
                        label = { Text(priority.name) }
                    )
                }
            }

            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        viewModel.submitTicket(
                            title = title,
                            description = description,
                            type = selectedType,
                            priority = selectedPriority,
                            rating = if (selectedType == TicketType.REVIEW) rating else null,
                            onSuccess = onBack
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && description.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Submit to Support Team")
                }
            }
        }
    }
}
