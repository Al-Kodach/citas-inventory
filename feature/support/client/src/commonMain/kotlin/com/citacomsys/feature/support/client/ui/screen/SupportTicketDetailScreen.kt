package com.citacomsys.feature.support.client.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.citacomsys.feature.support.api.SupportMessageRecord
import com.citacomsys.feature.support.client.ui.SupportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTicketDetailScreen(
    viewModel: SupportViewModel,
    ticketId: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var replyContent by remember { mutableStateOf("") }

    LaunchedEffect(ticketId) {
        viewModel.loadTicketDetail(ticketId)
    }

    val ticket = uiState.selectedTicket

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ticket?.title ?: "Support Ticket Detail") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        if (ticket == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Ticket info header
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Type: ${ticket.type} | Status: ${ticket.status}", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ticket.description, style = MaterialTheme.typography.bodyMedium)
                        if (ticket.rating != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Rating: ${ticket.rating} ⭐", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Conversation History", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ticket.messages) { message ->
                        MessageBubble(message = message)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Reply input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = replyContent,
                        onValueChange = { replyContent = it },
                        placeholder = { Text("Type a reply...") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (replyContent.isNotBlank()) {
                                viewModel.addMessage(ticket.ticketId, replyContent) {
                                    replyContent = ""
                                }
                            }
                        },
                        enabled = replyContent.isNotBlank()
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: SupportMessageRecord) {
    val alignment = if (message.isSupportAgent) androidx.compose.ui.Alignment.Start else androidx.compose.ui.Alignment.End
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = if (message.isSupportAgent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(message.senderName, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
